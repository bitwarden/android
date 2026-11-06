package com.x8bit.bitwarden.data.pam.repository

import com.bitwarden.core.data.util.asFailure
import com.bitwarden.pam.AccessApprovalMode
import com.bitwarden.pam.AccessBadgeState
import com.bitwarden.pam.AccessRequestCreateRequest
import com.bitwarden.pam.AccessRequestException
import com.bitwarden.sdk.BitwardenException
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.pam.datasource.sdk.PamSdkSource
import com.x8bit.bitwarden.data.pam.repository.model.PamAccessRequest
import com.x8bit.bitwarden.data.pam.repository.model.PamAccessStatus
import com.x8bit.bitwarden.data.pam.repository.model.PamApprovalMode
import com.x8bit.bitwarden.data.pam.repository.model.PamItemAccess
import com.x8bit.bitwarden.data.pam.repository.model.RequestPamAccessResult
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException

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
class PamRepositoryImpl(
    private val authDiskSource: AuthDiskSource,
    private val pamSdkSource: PamSdkSource,
) : PamRepository {

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
