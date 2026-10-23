package com.x8bit.bitwarden.data.pam.repository

import com.bitwarden.core.data.util.asFailure
import com.bitwarden.pam.AccessApprovalMode
import com.bitwarden.pam.AccessBadgeState
import com.bitwarden.pam.AccessLeaseExtensionRequest
import com.bitwarden.pam.AccessLeaseRevokeRequest
import com.bitwarden.pam.AccessLeaseStatus
import com.bitwarden.pam.AccessLeaseView
import com.bitwarden.pam.AccessRequestCreateRequest
import com.bitwarden.pam.AccessRequestException
import com.bitwarden.pam.AccessRequestStatus
import com.bitwarden.pam.AccessRequestView
import com.bitwarden.sdk.BitwardenException
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.pam.datasource.sdk.PamSdkSource
import com.x8bit.bitwarden.data.pam.manager.PamLeaseReminderManager
import com.x8bit.bitwarden.data.pam.repository.model.PamAccessRequest
import com.x8bit.bitwarden.data.pam.repository.model.PamAccessStatus
import com.x8bit.bitwarden.data.pam.repository.model.PamApprovalMode
import com.x8bit.bitwarden.data.pam.repository.model.PamExtensionPolicy
import com.x8bit.bitwarden.data.pam.repository.model.PamItemAccess
import com.x8bit.bitwarden.data.pam.repository.model.PamLease
import com.x8bit.bitwarden.data.pam.repository.model.PamLeaseStatus
import com.x8bit.bitwarden.data.pam.repository.model.PamMyAccessRequest
import com.x8bit.bitwarden.data.pam.repository.model.PamRequestStatus
import com.x8bit.bitwarden.data.pam.repository.model.RequestPamAccessResult
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.Clock

private const val HTTP_BAD_REQUEST = 400
private const val HTTP_CONFLICT = 409
private const val HTTP_UNPROCESSABLE_ENTITY = 422

/**
 * Matches the SDK's rendering of an HTTP error response, `error in response: status code 409
 * Conflict: <body>`. It is anchored to the start of the message so identifiers or server text in
 * the body can never be mistaken for the status.
 */
private val HTTP_STATUS_REGEX = Regex("""^error in response: status code (\d{3})\b""")

/**
 * The SDK does not expose HTTP status codes as a field, so the status is read from the fixed
 * prefix of the message of an API failure. Returns null for any other message.
 */
private fun String?.toHttpStatus(): Int? = this
    ?.let { HTTP_STATUS_REGEX.find(it.trimStart()) }
    ?.groupValues
    ?.get(1)
    ?.toIntOrNull()

/**
 * Default implementation of [PamRepository].
 */
@Suppress("TooManyFunctions")
class PamRepositoryImpl(
    private val authDiskSource: AuthDiskSource,
    private val pamSdkSource: PamSdkSource,
    private val pamLeaseReminderManager: PamLeaseReminderManager,
    private val clock: Clock,
) : PamRepository {

    private val mutableAccessChangedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override val accessChangedFlow: SharedFlow<Unit> = mutableAccessChangedFlow.asSharedFlow()

    private val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    override suspend fun getItemAccess(cipherId: String): Result<PamItemAccess> {
        val userId = activeUserId ?: return NoActiveUserException().asFailure()
        return pamSdkSource
            .preCheck(userId = userId, cipherId = cipherId)
            .mapCatching { preCheck ->
                val state = pamSdkSource
                    .getCipherAccessState(userId = userId, cipherId = cipherId)
                    .getOrThrow()
                PamItemAccess(
                    approvalMode = when (preCheck.approvalMode) {
                        AccessApprovalMode.AUTOMATIC -> PamApprovalMode.AUTOMATIC
                        AccessApprovalMode.HUMAN -> PamApprovalMode.HUMAN
                        AccessApprovalMode.UNKNOWN -> PamApprovalMode.UNKNOWN
                    },
                    status = when (val badge = state.badgeState) {
                        is AccessBadgeState.Active -> PamAccessStatus.Active(badge.expiresAt)
                        AccessBadgeState.Ready -> PamAccessStatus.Approved(
                            requestId = state.approvedRequest?.id,
                        )
                        AccessBadgeState.Pending -> PamAccessStatus.Pending
                        AccessBadgeState.Privileged -> PamAccessStatus.Privileged
                    },
                    defaultDurationSeconds = preCheck.defaultDurationSeconds.toLong(),
                    maxDurationSeconds = preCheck.maxDurationSeconds.toLong(),
                    canStartLease = preCheck.canStartLease,
                    slotFreesAt = preCheck.slotFreesAt,
                )
            }
    }

    override suspend fun requestAccess(
        cipherId: String,
        request: PamAccessRequest,
    ): RequestPamAccessResult {
        val userId = activeUserId
            ?: return RequestPamAccessResult.Error(error = NoActiveUserException())
        val sdkRequest = when (request) {
            is PamAccessRequest.Automatic -> {
                AccessRequestCreateRequest(
                    durationSeconds = request.durationSeconds.toUInt(),
                    start = null,
                    end = null,
                    reason = null,
                )
            }

            is PamAccessRequest.Human -> {
                AccessRequestCreateRequest(
                    durationSeconds = null,
                    start = request.start,
                    end = request.end,
                    reason = request.reason,
                )
            }
        }
        return pamSdkSource
            .requestAccess(userId = userId, cipherId = cipherId, request = sdkRequest)
            .fold(
                onSuccess = { RequestPamAccessResult.Success },
                onFailure = { it.toRequestPamAccessResult() },
            )
    }

    override suspend fun getLeasedCipher(cipherId: String): Result<CipherView?> {
        val userId = activeUserId ?: return NoActiveUserException().asFailure()
        return pamSdkSource.getLeasedCipher(userId = userId, cipherId = cipherId)
    }

    override suspend fun getMyAccessRequests(): Result<List<PamMyAccessRequest>> {
        val userId = activeUserId ?: return NoActiveUserException().asFailure()
        return pamSdkSource
            .listMyAccessRequests(userId = userId)
            .mapCatching { requests ->
                val leases = pamSdkSource
                    .listMyLeases(userId = userId)
                    .getOrNull()
                    .orEmpty()
                val now = clock.instant()
                val items = requests
                    .map { request ->
                        val lease = request.toPamLease(
                            leases = leases,
                        )
                        val extensionPolicy = lease
                            ?.takeIf { it.isActiveAt(now) }
                            ?.let { getExtensionPolicy(userId, request.cipherId) }
                        request.toPamMyAccessRequest(lease, extensionPolicy)
                    }
                    .sortedByDescending { it.submittedAt }
                syncReminders(userId = userId, leases = leases, requests = items)
                items
            }
    }

    override suspend fun getCurrentAccessRequestCount(): Result<Int> {
        val userId = activeUserId ?: return NoActiveUserException().asFailure()
        return pamSdkSource
            .listMyAccessRequests(userId = userId)
            .mapCatching { requests ->
                val leases = pamSdkSource
                    .listMyLeases(userId = userId)
                    .getOrNull()
                    .orEmpty()
                val now = clock.instant()
                requests.count { request ->
                    request
                        .toPamMyAccessRequest(
                            lease = request.toPamLease(leases = leases),
                            extensionPolicy = null,
                        )
                        .isCurrentAt(now)
                }
            }
    }

    override suspend fun startAccess(requestId: String): Result<Unit> {
        val userId = activeUserId ?: return NoActiveUserException().asFailure()
        return pamSdkSource
            .activateAccessRequest(userId = userId, requestId = requestId)
            .onSuccess { lease ->
                if (lease.status == AccessLeaseStatus.ACTIVE) {
                    pamLeaseReminderManager.scheduleReminder(
                        userId = userId,
                        leaseId = lease.id,
                        leaseEndsAt = lease.notAfter,
                    )
                }
                mutableAccessChangedFlow.tryEmit(Unit)
            }
            .map { }
    }

    override suspend fun cancelAccessRequest(requestId: String): Result<Unit> {
        val userId = activeUserId ?: return NoActiveUserException().asFailure()
        return pamSdkSource
            .cancelAccessRequest(userId = userId, requestId = requestId)
            .onSuccess { mutableAccessChangedFlow.tryEmit(Unit) }
    }

    override suspend fun endAccess(leaseId: String): Result<Unit> {
        val userId = activeUserId ?: return NoActiveUserException().asFailure()
        return pamSdkSource
            .endLease(
                userId = userId,
                leaseId = leaseId,
                request = AccessLeaseRevokeRequest(reason = null),
            )
            .onSuccess {
                pamLeaseReminderManager.cancelReminder(leaseId = leaseId)
                mutableAccessChangedFlow.tryEmit(Unit)
            }
    }

    override suspend fun extendAccess(
        leaseId: String,
        durationSeconds: Long,
        reason: String,
    ): Result<Unit> {
        val userId = activeUserId ?: return NoActiveUserException().asFailure()
        return pamSdkSource
            .extendLease(
                userId = userId,
                leaseId = leaseId,
                request = AccessLeaseExtensionRequest(
                    durationSeconds = durationSeconds.toUInt(),
                    reason = reason,
                ),
            )
            .onSuccess {
                getMyAccessRequests()
                mutableAccessChangedFlow.tryEmit(Unit)
            }
            .map { }
    }

    private suspend fun getExtensionPolicy(
        userId: String,
        cipherId: String,
    ): PamExtensionPolicy? = pamSdkSource
        .getCipherAccessState(userId = userId, cipherId = cipherId)
        .getOrNull()
        ?.takeIf { it.extensionsAllowed }
        ?.let { PamExtensionPolicy(maxDurationSeconds = it.maxExtensionDurationSeconds?.toLong()) }

    /**
     * Schedules reminders for active leases and cancels them for every lease that is no longer
     * active (ended, expired or revoked).
     */
    private fun syncReminders(
        userId: String,
        leases: List<AccessLeaseView>,
        requests: List<PamMyAccessRequest>,
    ) {
        val now = clock.instant()
        val allLeases = (leases.map { it.toPamLease() } + requests.mapNotNull { it.lease })
            .distinctBy { it.id }
        allLeases.forEach { lease ->
            if (lease.isActiveAt(now)) {
                pamLeaseReminderManager.scheduleReminder(
                    userId = userId,
                    leaseId = lease.id,
                    leaseEndsAt = lease.notAfter,
                )
            } else {
                pamLeaseReminderManager.cancelReminder(leaseId = lease.id)
            }
        }
    }
}

private fun Throwable.toRequestPamAccessResult(): RequestPamAccessResult {
    val pamError = (this as? BitwardenException.AccessRequest)?.v1
    val message = pamError?.message ?: message
    return when {
        pamError is AccessRequestException.Validation -> {
            RequestPamAccessResult.ValidationError(message = message)
        }

        pamError is AccessRequestException.Api && message.toHttpStatus() == HTTP_CONFLICT -> {
            RequestPamAccessResult.Conflict(message = message)
        }

        pamError is AccessRequestException.Api &&
            message.toHttpStatus() in listOf(HTTP_BAD_REQUEST, HTTP_UNPROCESSABLE_ENTITY) -> {
            RequestPamAccessResult.ValidationError(message = message)
        }

        else -> RequestPamAccessResult.Error(error = this)
    }
}

private fun AccessRequestStatus.toPamRequestStatus(): PamRequestStatus = when (this) {
    AccessRequestStatus.PENDING -> PamRequestStatus.PENDING
    AccessRequestStatus.APPROVED -> PamRequestStatus.APPROVED
    AccessRequestStatus.DENIED -> PamRequestStatus.DENIED
    AccessRequestStatus.CANCELED -> PamRequestStatus.CANCELLED
    AccessRequestStatus.EXPIRED -> PamRequestStatus.EXPIRED
    AccessRequestStatus.UNKNOWN -> PamRequestStatus.UNKNOWN
}

private fun AccessLeaseView.toPamLease(): PamLease = PamLease(
    id = id,
    status = status.toPamLeaseStatus(),
    notBefore = notBefore,
    notAfter = notAfter,
)

private fun AccessLeaseStatus.toPamLeaseStatus(): PamLeaseStatus = when (this) {
    AccessLeaseStatus.ACTIVE -> PamLeaseStatus.ACTIVE
    AccessLeaseStatus.EXPIRED -> PamLeaseStatus.EXPIRED
    AccessLeaseStatus.REVOKED -> PamLeaseStatus.REVOKED
    AccessLeaseStatus.CANCELED -> PamLeaseStatus.CANCELLED
    AccessLeaseStatus.UNKNOWN -> PamLeaseStatus.UNKNOWN
}

/**
 * Finds the lease produced by this request, preferring the full lease record and falling back to
 * the summary the request carries.
 */
private fun AccessRequestView.toPamLease(leases: List<AccessLeaseView>): PamLease? =
    leases.firstOrNull { it.requestId == id }?.toPamLease()
        ?: producedLeaseId?.let { leaseId ->
            PamLease(
                id = leaseId,
                status = producedLeaseStatus?.toPamLeaseStatus() ?: PamLeaseStatus.UNKNOWN,
                notBefore = leaseNotBefore,
                notAfter = producedLeaseNotAfter ?: leaseNotAfter,
            )
        }

private fun AccessRequestView.toPamMyAccessRequest(
    lease: PamLease?,
    extensionPolicy: PamExtensionPolicy?,
): PamMyAccessRequest = PamMyAccessRequest(
    id = id,
    cipherId = cipherId,
    status = status.toPamRequestStatus(),
    reason = reason,
    submittedAt = submittedAt,
    resolvedAt = resolvedAt,
    windowStart = leaseNotBefore,
    windowEnd = leaseNotAfter,
    isExtension = extensionOfLeaseId != null,
    awaitingActivation = awaitingActivation,
    lease = lease,
    extensionPolicy = extensionPolicy,
)
