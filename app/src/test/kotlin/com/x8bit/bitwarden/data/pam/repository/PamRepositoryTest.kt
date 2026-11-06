package com.x8bit.bitwarden.data.pam.repository

import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.pam.AccessApprovalMode
import com.bitwarden.pam.AccessBadgeState
import com.bitwarden.pam.AccessPreCheckView
import com.bitwarden.pam.AccessRequestCreateRequest
import com.bitwarden.pam.AccessRequestException
import com.bitwarden.pam.AccessRequestResultView
import com.bitwarden.pam.AccessRequestStatus
import com.bitwarden.pam.AccessRequestView
import com.bitwarden.pam.CipherAccessStateView
import com.bitwarden.sdk.BitwardenException
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.pam.datasource.sdk.PamSdkSource
import com.x8bit.bitwarden.data.pam.repository.model.PamAccessRequest
import com.x8bit.bitwarden.data.pam.repository.model.PamAccessStatus
import com.x8bit.bitwarden.data.pam.repository.model.PamApprovalMode
import com.x8bit.bitwarden.data.pam.repository.model.PamItemAccess
import com.x8bit.bitwarden.data.pam.repository.model.RequestPamAccessResult
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class PamRepositoryTest {
    private val authDiskSource = mockk<AuthDiskSource> {
        every { userState } returns mockk<UserStateJson> {
            every { activeUserId } returns USER_ID
        }
    }
    private val pamSdkSource = mockk<PamSdkSource>()
    private val repository: PamRepository = PamRepositoryImpl(
        authDiskSource = authDiskSource,
        pamSdkSource = pamSdkSource,
    )

    private fun stubAccess(
        mode: AccessApprovalMode = AccessApprovalMode.AUTOMATIC,
        badge: AccessBadgeState = AccessBadgeState.Privileged,
        canStartLease: Boolean = true,
        slotFreesAt: Instant? = null,
    ) {
        coEvery { pamSdkSource.preCheck(USER_ID, CIPHER_ID) } returns AccessPreCheckView(
            cipherId = CIPHER_ID,
            approvalMode = mode,
            hasActiveLease = badge is AccessBadgeState.Active,
            defaultDurationSeconds = 3_600u,
            maxDurationSeconds = 28_800u,
            canStartLease = canStartLease,
            slotFreesAt = slotFreesAt,
        ).asSuccess()
        coEvery {
            pamSdkSource.getCipherAccessState(USER_ID, CIPHER_ID)
        } returns CipherAccessStateView(
            cipherId = CIPHER_ID,
            activeLease = null,
            pendingRequest = null,
            approvedRequest = null,
            badgeState = badge,
            extensionsAllowed = false,
            maxExtensionDurationSeconds = null,
        ).asSuccess()
    }

    @Test
    fun `getItemAccess should map the pre-check and state for a gated item`() = runTest {
        val freesAt = Instant.parse("2026-01-01T00:00:00Z")
        stubAccess(
            mode = AccessApprovalMode.HUMAN,
            canStartLease = false,
            slotFreesAt = freesAt,
        )

        val result = repository.getItemAccess(cipherId = CIPHER_ID)

        assertEquals(
            PamItemAccess(
                approvalMode = PamApprovalMode.HUMAN,
                status = PamAccessStatus.Privileged,
                defaultDurationSeconds = 3_600L,
                maxDurationSeconds = 28_800L,
                canStartLease = false,
                slotFreesAt = freesAt,
            ).asSuccess(),
            result,
        )
        assertTrue(result.getOrThrow().isSlotHeldByAnotherMember)
    }

    @Test
    fun `getItemAccess should map every badge state`() = runTest {
        val expiresAt = Instant.parse("2026-01-01T00:00:00Z")
        listOf(
            AccessBadgeState.Pending to PamAccessStatus.Pending,
            AccessBadgeState.Ready to PamAccessStatus.Approved(requestId = null),
            AccessBadgeState.Active(expiresAt = expiresAt) to
                PamAccessStatus.Active(expiresAt = expiresAt),
        ).forEach { (badge, expected) ->
            stubAccess(badge = badge)

            val result = repository.getItemAccess(cipherId = CIPHER_ID)

            assertEquals(expected, result.getOrThrow().status)
        }
    }

    @Test
    fun `getItemAccess should carry the id of the approved request`() = runTest {
        stubAccess(badge = AccessBadgeState.Ready)
        coEvery { pamSdkSource.getCipherAccessState(USER_ID, CIPHER_ID) } returns
            CipherAccessStateView(
                cipherId = CIPHER_ID,
                activeLease = null,
                pendingRequest = null,
                approvedRequest = createRequestView(id = REQUEST_ID),
                badgeState = AccessBadgeState.Ready,
                extensionsAllowed = false,
                maxExtensionDurationSeconds = null,
            ).asSuccess()

        val result = repository.getItemAccess(cipherId = CIPHER_ID)

        assertEquals(
            PamAccessStatus.Approved(requestId = REQUEST_ID),
            result.getOrThrow().status,
        )
    }

    @Test
    fun `getItemAccess should map the unknown approval mode`() = runTest {
        stubAccess(mode = AccessApprovalMode.UNKNOWN)

        val result = repository.getItemAccess(cipherId = CIPHER_ID)

        assertEquals(PamApprovalMode.UNKNOWN, result.getOrThrow().approvalMode)
    }

    @Test
    fun `getItemAccess should return a failure when the pre-check fails`() = runTest {
        val error = IllegalStateException("fail")
        coEvery { pamSdkSource.preCheck(USER_ID, CIPHER_ID) } returns error.asFailure()

        val result = repository.getItemAccess(cipherId = CIPHER_ID)

        assertEquals(error.asFailure(), result)
    }

    @Test
    fun `getItemAccess should return a failure when the access state fails`() = runTest {
        val error = IllegalStateException("fail")
        stubAccess()
        coEvery { pamSdkSource.getCipherAccessState(USER_ID, CIPHER_ID) } returns error.asFailure()

        val result = repository.getItemAccess(cipherId = CIPHER_ID)

        assertEquals(error.asFailure(), result)
    }

    @Test
    fun `getItemAccess should fail with NoActiveUserException without an active user`() = runTest {
        every { authDiskSource.userState } returns null

        val result = repository.getItemAccess(cipherId = CIPHER_ID)

        assertTrue(result.exceptionOrNull() is NoActiveUserException)
    }

    @Test
    fun `requestAccess with an automatic request should send only the duration`() = runTest {
        coEvery {
            pamSdkSource.requestAccess(USER_ID, CIPHER_ID, any())
        } returns mockk<AccessRequestResultView>().asSuccess()

        val result = repository.requestAccess(
            cipherId = CIPHER_ID,
            request = PamAccessRequest.Automatic(durationSeconds = 1_800L),
        )

        assertEquals(RequestPamAccessResult.Success, result)
        coVerify(exactly = 1) {
            pamSdkSource.requestAccess(
                userId = USER_ID,
                cipherId = CIPHER_ID,
                request = AccessRequestCreateRequest(
                    durationSeconds = 1_800u,
                    start = null,
                    end = null,
                    reason = null,
                ),
            )
        }
    }

    @Test
    fun `requestAccess with a human request should send the window and reason`() = runTest {
        val start = Instant.parse("2026-01-01T00:00:00Z")
        val end = Instant.parse("2026-01-01T08:00:00Z")
        coEvery {
            pamSdkSource.requestAccess(USER_ID, CIPHER_ID, any())
        } returns mockk<AccessRequestResultView>().asSuccess()

        repository.requestAccess(
            cipherId = CIPHER_ID,
            request = PamAccessRequest.Human(start = start, end = end, reason = "reason"),
        )

        coVerify(exactly = 1) {
            pamSdkSource.requestAccess(
                userId = USER_ID,
                cipherId = CIPHER_ID,
                request = AccessRequestCreateRequest(
                    durationSeconds = null,
                    start = start,
                    end = end,
                    reason = "reason",
                ),
            )
        }
    }

    @Test
    fun `requestAccess should map a validation exception to ValidationError`() = runTest {
        stubRequestFailure(
            BitwardenException.AccessRequest(AccessRequestException.Validation("end before start")),
        )

        val result = requestAutomatic()

        assertEquals(RequestPamAccessResult.ValidationError("end before start"), result)
    }

    @Test
    fun `requestAccess should map an API 409 to Conflict`() = runTest {
        stubRequestFailure(
            BitwardenException.AccessRequest(
                AccessRequestException.Api(apiMessage(409, "Conflict", "held")),
            ),
        )

        val result = requestAutomatic()

        assertEquals(RequestPamAccessResult.Conflict(apiMessage(409, "Conflict", "held")), result)
    }

    @Test
    fun `requestAccess should map an API 400 to ValidationError`() = runTest {
        stubRequestFailure(
            BitwardenException.AccessRequest(
                AccessRequestException.Api(apiMessage(400, "Bad Request", "bad")),
            ),
        )

        val result = requestAutomatic()

        assertEquals(
            RequestPamAccessResult.ValidationError(apiMessage(400, "Bad Request", "bad")),
            result,
        )
    }

    @Test
    fun `requestAccess should map other failures to Error`() = runTest {
        val error = IllegalStateException("boom")
        stubRequestFailure(error)

        val result = requestAutomatic()

        assertEquals(RequestPamAccessResult.Error(error), result)
    }

    @Test
    fun `requestAccess should return Error without an active user`() = runTest {
        every { authDiskSource.userState } returns null

        val result = requestAutomatic()

        assertTrue((result as RequestPamAccessResult.Error).error is NoActiveUserException)
    }

    @Test
    fun `getLeasedCipher should return the SDK result`() = runTest {
        val cipher = mockk<CipherView>()
        coEvery { pamSdkSource.getLeasedCipher(USER_ID, CIPHER_ID) } returns cipher.asSuccess()

        assertEquals(cipher.asSuccess(), repository.getLeasedCipher(cipherId = CIPHER_ID))
    }

    @Test
    fun `requestAccess should not classify by status text found outside the status`() = runTest {
        listOf(
            apiMessage(500, "Internal Server Error", "lease $GUID_WITH_409 conflict validation"),
            apiMessage(403, "Forbidden", "cipher $GUID_WITH_400 and $GUID_WITH_409"),
            apiMessage(404, "Not Found", "400 422 409"),
        ).forEach {
            stubRequestFailure(BitwardenException.AccessRequest(AccessRequestException.Api(it)))

            val result = requestAutomatic()

            assertTrue(result is RequestPamAccessResult.Error)
        }
    }

    @Test
    fun `requestAccess should map an API 422 to ValidationError`() = runTest {
        val message = apiMessage(422, "Unprocessable Entity", "bad")
        stubRequestFailure(BitwardenException.AccessRequest(AccessRequestException.Api(message)))

        val result = requestAutomatic()

        assertEquals(RequestPamAccessResult.ValidationError(message), result)
    }

    private fun stubRequestFailure(error: Throwable) {
        coEvery {
            pamSdkSource.requestAccess(USER_ID, CIPHER_ID, any())
        } returns error.asFailure()
    }

    private suspend fun requestAutomatic(): RequestPamAccessResult = repository.requestAccess(
        cipherId = CIPHER_ID,
        request = PamAccessRequest.Automatic(durationSeconds = 60L),
    )
}

private const val USER_ID = "userId"
private const val CIPHER_ID = "cipherId"
private const val REQUEST_ID = "requestId"
private const val GUID_WITH_409 = "0a409b00-4000-4409-8409-409040904090"
private const val GUID_WITH_400 = "400a0400-0400-4400-9400-040004000400"
private val NOW: Instant = Instant.parse("2026-01-01T12:00:00Z")

/** Renders a message the way the SDK renders an HTTP error response. */
private fun apiMessage(status: Int, reason: String, body: String): String =
    "error in response: status code $status $reason: $body"

private fun createRequestView(
    id: String = REQUEST_ID,
): AccessRequestView = AccessRequestView(
    id = id,
    cipherId = CIPHER_ID,
    collectionId = "collectionId",
    organizationId = null,
    requesterId = USER_ID,
    ruleId = null,
    status = AccessRequestStatus.APPROVED,
    leaseNotBefore = NOW.minusSeconds(600),
    leaseNotAfter = NOW.plusSeconds(1_800),
    reason = "Because",
    submittedAt = NOW.minusSeconds(60),
    resolvedAt = null,
    decisions = emptyList(),
    producedLeaseId = null,
    producedLeaseStatus = null,
    producedLeaseNotAfter = null,
    extensionOfLeaseId = null,
    requesterName = "Grace",
    requesterEmail = "grace@example.com",
    awaitingActivation = true,
    humanDecision = null,
)
