package com.x8bit.bitwarden.data.pam.repository

import app.cash.turbine.test
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.pam.AccessApprovalMode
import com.bitwarden.pam.AccessApprover
import com.bitwarden.pam.AccessBadgeState
import com.bitwarden.pam.AccessDecider
import com.bitwarden.pam.AccessDecisionRequest
import com.bitwarden.pam.AccessDecisionVerdict
import com.bitwarden.pam.AccessLeaseExtensionRequest
import com.bitwarden.pam.AccessLeaseException
import com.bitwarden.pam.AccessLeaseRevokeRequest
import com.bitwarden.pam.AccessLeaseStatus
import com.bitwarden.pam.AccessLeaseView
import com.bitwarden.pam.AccessPreCheckView
import com.bitwarden.pam.AccessRequestCreateRequest
import com.bitwarden.pam.AccessRequestDecisionView
import com.bitwarden.pam.AccessRequestException
import com.bitwarden.pam.AccessRequestResultView
import com.bitwarden.pam.AccessRequestStatus
import com.bitwarden.pam.AccessRequestView
import com.bitwarden.pam.ApprovalException
import com.bitwarden.pam.CipherAccessStateView
import com.bitwarden.sdk.BitwardenException
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.pam.datasource.sdk.PamSdkSource
import com.x8bit.bitwarden.data.pam.manager.PamLeaseReminderManager
import com.x8bit.bitwarden.data.pam.repository.model.DecidePamAccessResult
import com.x8bit.bitwarden.data.pam.repository.model.PamAccessRequest
import com.x8bit.bitwarden.data.pam.repository.model.PamAccessStatus
import com.x8bit.bitwarden.data.pam.repository.model.PamApprovalDecision
import com.x8bit.bitwarden.data.pam.repository.model.PamApprovalMode
import com.x8bit.bitwarden.data.pam.repository.model.PamApprovalRequest
import com.x8bit.bitwarden.data.pam.repository.model.PamDecisionVerdict
import com.x8bit.bitwarden.data.pam.repository.model.PamExtensionPolicy
import com.x8bit.bitwarden.data.pam.repository.model.PamItemAccess
import com.x8bit.bitwarden.data.pam.repository.model.PamLease
import com.x8bit.bitwarden.data.pam.repository.model.PamLeaseStatus
import com.x8bit.bitwarden.data.pam.repository.model.PamRequestStatus
import com.x8bit.bitwarden.data.pam.repository.model.RequestPamAccessResult
import com.x8bit.bitwarden.data.pam.repository.model.RevokePamAccessResult
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Suppress("LargeClass")
class PamRepositoryTest {
    private val authDiskSource = mockk<AuthDiskSource> {
        every { userState } returns mockk<UserStateJson> {
            every { activeUserId } returns USER_ID
        }
    }
    private val pamSdkSource = mockk<PamSdkSource>()
    private val pamLeaseReminderManager = mockk<PamLeaseReminderManager> {
        every { scheduleReminder(any(), any(), any()) } just runs
        every { cancelReminder(any()) } just runs
    }
    private val repository: PamRepository = PamRepositoryImpl(
        authDiskSource = authDiskSource,
        pamSdkSource = pamSdkSource,
        pamLeaseReminderManager = pamLeaseReminderManager,
        clock = FIXED_CLOCK,
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
    fun `getMyAccessRequests should map requests with their leases and extension policy`() =
        runTest {
            val activeRequest = createRequestView(
                id = "active",
                status = AccessRequestStatus.APPROVED,
                submittedAt = NOW.minusSeconds(100),
                producedLeaseId = LEASE_ID,
            )
            val pendingRequest = createRequestView(
                id = "pending",
                status = AccessRequestStatus.PENDING,
                submittedAt = NOW.minusSeconds(10),
            )
            val lease = createLeaseView(requestId = "active", notAfter = NOW.plusSeconds(1_800))
            coEvery {
                pamSdkSource.listMyAccessRequests(USER_ID)
            } returns listOf(activeRequest, pendingRequest).asSuccess()
            coEvery { pamSdkSource.listMyLeases(USER_ID) } returns listOf(lease).asSuccess()
            coEvery {
                pamSdkSource.getCipherAccessState(USER_ID, CIPHER_ID)
            } returns CipherAccessStateView(
                cipherId = CIPHER_ID,
                activeLease = lease,
                pendingRequest = null,
                approvedRequest = null,
                badgeState = AccessBadgeState.Active(expiresAt = lease.notAfter),
                extensionsAllowed = true,
                maxExtensionDurationSeconds = 900,
            ).asSuccess()

            val result = repository.getMyAccessRequests().getOrThrow()

            assertEquals(listOf("pending", "active"), result.map { it.id })
            val active = result.last()
            assertEquals(PamRequestStatus.APPROVED, active.status)
            assertEquals(
                PamLease(
                    id = LEASE_ID,
                    status = PamLeaseStatus.ACTIVE,
                    notBefore = lease.notBefore,
                    notAfter = lease.notAfter,
                ),
                active.lease,
            )
            assertEquals(PamExtensionPolicy(maxDurationSeconds = 900L), active.extensionPolicy)
            assertNull(result.first().lease)
            assertNull(result.first().extensionPolicy)
        }

    @Test
    fun `getMyAccessRequests should omit the extension policy when extensions are not allowed`() =
        runTest {
            val lease = createLeaseView(requestId = REQUEST_ID, notAfter = NOW.plusSeconds(1_800))
            coEvery {
                pamSdkSource.listMyAccessRequests(USER_ID)
            } returns listOf(createRequestView(producedLeaseId = LEASE_ID)).asSuccess()
            coEvery { pamSdkSource.listMyLeases(USER_ID) } returns listOf(lease).asSuccess()
            stubAccess()

            val result = repository.getMyAccessRequests().getOrThrow()

            assertNull(result.single().extensionPolicy)
        }

    @Test
    fun `getMyAccessRequests should schedule reminders for active leases and cancel the rest`() =
        runTest {
            val active = createLeaseView(
                id = LEASE_ID,
                requestId = REQUEST_ID,
                notAfter = NOW.plusSeconds(1_800),
            )
            val revoked = createLeaseView(
                id = "revokedLease",
                requestId = "other",
                status = AccessLeaseStatus.REVOKED,
                notAfter = NOW.plusSeconds(1_800),
            )
            coEvery {
                pamSdkSource.listMyAccessRequests(USER_ID)
            } returns listOf(createRequestView(producedLeaseId = LEASE_ID)).asSuccess()
            coEvery {
                pamSdkSource.listMyLeases(USER_ID)
            } returns listOf(active, revoked).asSuccess()
            stubAccess()

            repository.getMyAccessRequests()

            verify(exactly = 1) {
                pamLeaseReminderManager.scheduleReminder(USER_ID, LEASE_ID, active.notAfter)
            }
            verify(exactly = 1) { pamLeaseReminderManager.cancelReminder("revokedLease") }
        }

    @Test
    fun `getMyAccessRequests should return a failure when the requests cannot be loaded`() =
        runTest {
            val error = IllegalStateException("fail")
            coEvery { pamSdkSource.listMyAccessRequests(USER_ID) } returns error.asFailure()

            assertEquals(error.asFailure(), repository.getMyAccessRequests())
        }

    @Test
    fun `getMyAccessRequests should return a failure without an active user`() = runTest {
        every { authDiskSource.userState } returns null

        assertTrue(repository.getMyAccessRequests().exceptionOrNull() is NoActiveUserException)
    }

    @Test
    fun `getCurrentAccessRequestCount should count only pending, ready and active requests`() =
        runTest {
            val requests = listOf(
                createRequestView(id = "pending", status = AccessRequestStatus.PENDING),
                createRequestView(id = "ready", status = AccessRequestStatus.APPROVED),
                createRequestView(
                    id = "active",
                    status = AccessRequestStatus.APPROVED,
                    producedLeaseId = "activeLease",
                ),
                createRequestView(
                    id = "ended",
                    status = AccessRequestStatus.APPROVED,
                    producedLeaseId = "endedLease",
                ),
                createRequestView(
                    id = "extension",
                    status = AccessRequestStatus.APPROVED,
                    extensionOf = "activeLease",
                ),
                createRequestView(id = "denied", status = AccessRequestStatus.DENIED),
                createRequestView(id = "cancelled", status = AccessRequestStatus.CANCELED),
                createRequestView(id = "expired", status = AccessRequestStatus.EXPIRED),
            )
            val leases = listOf(
                createLeaseView(
                    id = "activeLease",
                    requestId = "active",
                    notAfter = NOW.plusSeconds(1_800),
                ),
                createLeaseView(
                    id = "endedLease",
                    requestId = "ended",
                    status = AccessLeaseStatus.REVOKED,
                    notAfter = NOW.plusSeconds(1_800),
                ),
            )
            coEvery { pamSdkSource.listMyAccessRequests(USER_ID) } returns requests.asSuccess()
            coEvery { pamSdkSource.listMyLeases(USER_ID) } returns leases.asSuccess()

            assertEquals(3, repository.getCurrentAccessRequestCount().getOrThrow())
        }

    @Test
    fun `getCurrentAccessRequestCount should not count an active lease that already ended`() =
        runTest {
            coEvery {
                pamSdkSource.listMyAccessRequests(USER_ID)
            } returns listOf(createRequestView(producedLeaseId = LEASE_ID)).asSuccess()
            coEvery {
                pamSdkSource.listMyLeases(USER_ID)
            } returns listOf(createLeaseView(notAfter = NOW.minusSeconds(1))).asSuccess()

            assertEquals(0, repository.getCurrentAccessRequestCount().getOrThrow())
        }

    @Test
    fun `getCurrentAccessRequestCount should return a failure when requests cannot be loaded`() =
        runTest {
            val error = IllegalStateException("fail")
            coEvery { pamSdkSource.listMyAccessRequests(USER_ID) } returns error.asFailure()

            assertEquals(error.asFailure(), repository.getCurrentAccessRequestCount())
        }

    @Test
    fun `getCurrentAccessRequestCount should return a failure without an active user`() =
        runTest {
            every { authDiskSource.userState } returns null

            assertTrue(
                repository.getCurrentAccessRequestCount().exceptionOrNull()
                    is NoActiveUserException,
            )
        }

    @Test
    fun `startAccess should schedule the reminder and emit accessChangedFlow`() = runTest {
        val lease = createLeaseView(notAfter = NOW.plusSeconds(3_600))
        coEvery {
            pamSdkSource.activateAccessRequest(USER_ID, REQUEST_ID)
        } returns lease.asSuccess()

        repository.accessChangedFlow.test {
            assertEquals(Unit.asSuccess(), repository.startAccess(requestId = REQUEST_ID))
            assertEquals(Unit, awaitItem())
        }

        verify(exactly = 1) {
            pamLeaseReminderManager.scheduleReminder(USER_ID, LEASE_ID, lease.notAfter)
        }
    }

    @Test
    fun `startAccess should not schedule anything or emit on failure`() = runTest {
        coEvery {
            pamSdkSource.activateAccessRequest(USER_ID, REQUEST_ID)
        } returns IllegalStateException().asFailure()

        repository.accessChangedFlow.test {
            assertTrue(repository.startAccess(requestId = REQUEST_ID).isFailure)
            expectNoEvents()
        }

        verify(exactly = 0) { pamLeaseReminderManager.scheduleReminder(any(), any(), any()) }
    }

    @Test
    fun `cancelAccessRequest should call the SDK and emit accessChangedFlow`() = runTest {
        coEvery { pamSdkSource.cancelAccessRequest(USER_ID, REQUEST_ID) } returns Unit.asSuccess()

        repository.accessChangedFlow.test {
            assertEquals(Unit.asSuccess(), repository.cancelAccessRequest(REQUEST_ID))
            assertEquals(Unit, awaitItem())
        }
    }

    @Test
    fun `endAccess should end the lease, cancel the reminder and emit accessChangedFlow`() =
        runTest {
            coEvery {
                pamSdkSource.endLease(USER_ID, LEASE_ID, AccessLeaseRevokeRequest(reason = null))
            } returns Unit.asSuccess()

            repository.accessChangedFlow.test {
                assertEquals(Unit.asSuccess(), repository.endAccess(leaseId = LEASE_ID))
                assertEquals(Unit, awaitItem())
            }

            verify(exactly = 1) { pamLeaseReminderManager.cancelReminder(LEASE_ID) }
        }

    @Test
    fun `endAccess should keep the reminder when ending fails`() = runTest {
        coEvery {
            pamSdkSource.endLease(USER_ID, LEASE_ID, any())
        } returns IllegalStateException().asFailure()

        assertTrue(repository.endAccess(leaseId = LEASE_ID).isFailure)

        verify(exactly = 0) { pamLeaseReminderManager.cancelReminder(any()) }
    }

    @Test
    fun `extendAccess should send the duration and reason then reschedule the reminder`() =
        runTest {
            val request = AccessLeaseExtensionRequest(durationSeconds = 600u, reason = "More time")
            coEvery {
                pamSdkSource.extendLease(USER_ID, LEASE_ID, request)
            } returns createRequestView(extensionOf = LEASE_ID).asSuccess()
            val extendedLease = createLeaseView(notAfter = NOW.plusSeconds(2_400))
            coEvery {
                pamSdkSource.listMyAccessRequests(USER_ID)
            } returns listOf(createRequestView(producedLeaseId = LEASE_ID)).asSuccess()
            coEvery { pamSdkSource.listMyLeases(USER_ID) } returns listOf(extendedLease).asSuccess()
            stubAccess()

            val result = repository.extendAccess(
                leaseId = LEASE_ID,
                durationSeconds = 600L,
                reason = "More time",
            )

            assertEquals(Unit.asSuccess(), result)
            verify(exactly = 1) {
                pamLeaseReminderManager.scheduleReminder(
                    USER_ID,
                    LEASE_ID,
                    extendedLease.notAfter,
                )
            }
        }

    @Test
    fun `extendAccess should return a failure when the SDK fails`() = runTest {
        coEvery {
            pamSdkSource.extendLease(USER_ID, LEASE_ID, any())
        } returns IllegalStateException().asFailure()

        val result = repository.extendAccess(LEASE_ID, durationSeconds = 600L, reason = "r")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getApprovalInbox should map pending requests and drop lapsed windows`() = runTest {
        coEvery { pamSdkSource.listApprovalInbox(USER_ID) } returns listOf(
            createRequestView(
                id = "newer",
                status = AccessRequestStatus.PENDING,
                requesterId = OTHER_USER_ID,
                submittedAt = NOW.minusSeconds(10),
            ),
            createRequestView(
                id = "older",
                status = AccessRequestStatus.PENDING,
                requesterId = OTHER_USER_ID,
                submittedAt = NOW.minusSeconds(100),
            ),
            createRequestView(
                id = "lapsed",
                status = AccessRequestStatus.PENDING,
                requesterId = OTHER_USER_ID,
                notAfter = NOW.minusSeconds(1),
            ),
            createRequestView(
                id = "own",
                status = AccessRequestStatus.PENDING,
                requesterId = USER_ID,
                submittedAt = NOW.minusSeconds(5),
            ),
        ).asSuccess()

        val result = repository.getApprovalInbox().getOrThrow()

        assertEquals(listOf("older", "newer", "own"), result.map { it.id })
        assertEquals(listOf(false, false, true), result.map { it.isOwnRequest })
        val first = result.first()
        assertEquals(PamRequestStatus.PENDING, first.status)
        assertEquals("Grace", first.requesterName)
        assertEquals("grace@example.com", first.requesterEmail)
        assertEquals("Because", first.reason)
        assertEquals("collectionId", first.collectionId)
    }

    @Test
    fun `getApprovalInbox should return a failure when the SDK fails`() = runTest {
        coEvery {
            pamSdkSource.listApprovalInbox(USER_ID)
        } returns IllegalStateException().asFailure()

        assertTrue(repository.getApprovalInbox().isFailure)
    }

    @Test
    fun `getApprovalInbox should fail without an active user`() = runTest {
        every { authDiskSource.userState } returns null

        assertTrue(
            repository.getApprovalInbox().exceptionOrNull() is NoActiveUserException,
        )
    }

    @Test
    fun `getApprovalHistory should map the human decision and sort newest first`() = runTest {
        val decidedAt = NOW.minusSeconds(30)
        coEvery { pamSdkSource.listApprovalHistory(USER_ID) } returns listOf(
            createRequestView(
                id = "old",
                status = AccessRequestStatus.DENIED,
                requesterId = OTHER_USER_ID,
                resolvedAt = NOW.minusSeconds(3_000),
            ),
            createRequestView(
                id = "new",
                status = AccessRequestStatus.APPROVED,
                requesterId = OTHER_USER_ID,
                resolvedAt = decidedAt,
                humanDecision = AccessRequestDecisionView(
                    decider = AccessDecider.Human(
                        v1 = AccessApprover(id = USER_ID, name = null, email = "me@example.com"),
                    ),
                    verdict = AccessDecisionVerdict.APPROVE,
                    comment = "Fine",
                    decidedAt = decidedAt,
                ),
            ),
        ).asSuccess()

        val result = repository.getApprovalHistory().getOrThrow()

        assertEquals(listOf("new", "old"), result.map { it.id })
        assertEquals(
            PamApprovalDecision(
                verdict = PamDecisionVerdict.APPROVE,
                comment = "Fine",
                deciderName = "me@example.com",
                decidedAt = decidedAt,
            ),
            result.first().decision,
        )
    }

    @Test
    fun `getApprovalHistory should expose the lease the request produced`() = runTest {
        coEvery { pamSdkSource.listApprovalHistory(USER_ID) } returns listOf(
            createRequestView(
                requesterId = OTHER_USER_ID,
                producedLeaseId = LEASE_ID,
                producedLeaseStatus = AccessLeaseStatus.ACTIVE,
                producedLeaseNotAfter = NOW.plusSeconds(900),
            ),
        ).asSuccess()

        val result = repository.getApprovalHistory().getOrThrow()

        assertEquals(
            PamLease(
                id = LEASE_ID,
                status = PamLeaseStatus.ACTIVE,
                notBefore = NOW.minusSeconds(600),
                notAfter = NOW.plusSeconds(900),
            ),
            result.single().lease,
        )
        assertTrue(result.single().canRevokeAccessAt(NOW))
    }

    @Test
    fun `revokeAccess should end the lease without a reason and emit accessChangedFlow`() =
        runTest {
            coEvery { pamSdkSource.endLease(USER_ID, LEASE_ID, any()) } returns Unit.asSuccess()

            repository.accessChangedFlow.test {
                val result = repository.revokeAccess(request = createRevocableRequest())

                assertEquals(RevokePamAccessResult.Success, result)
                assertEquals(Unit, awaitItem())
            }

            coVerify {
                pamSdkSource.endLease(
                    userId = USER_ID,
                    leaseId = LEASE_ID,
                    request = AccessLeaseRevokeRequest(reason = null),
                )
            }
        }

    @Test
    fun `revokeAccess should never end the user's own lease`() = runTest {
        val result = repository.revokeAccess(
            request = createRevocableRequest(requesterId = USER_ID),
        )

        assertEquals(RevokePamAccessResult.NotAllowed, result)
        coVerify(exactly = 0) { pamSdkSource.endLease(any(), any(), any()) }
    }

    @Test
    fun `revokeAccess should not send a request that produced no lease`() = runTest {
        val result = repository.revokeAccess(
            request = createRevocableRequest().copy(lease = null),
        )

        assertEquals(RevokePamAccessResult.NotAllowed, result)
        coVerify(exactly = 0) { pamSdkSource.endLease(any(), any(), any()) }
    }

    @Test
    fun `revokeAccess should report a lease that already ended`() = runTest {
        coEvery {
            pamSdkSource.endLease(USER_ID, LEASE_ID, any())
        } returns BitwardenException
            .AccessLease(
                AccessLeaseException.Api(apiMessage(409, "Conflict", "This lease is not active.")),
            )
            .asFailure()

        val result = repository.revokeAccess(request = createRevocableRequest())

        assertEquals(RevokePamAccessResult.AlreadyEnded, result)
    }

    @Test
    fun `revokeAccess should report other failures as errors`() = runTest {
        listOf(
            BitwardenException.AccessLease(
                AccessLeaseException.Api(apiMessage(404, "Not Found", "409 conflict")),
            ),
            IllegalStateException(),
        ).forEach { error ->
            coEvery {
                pamSdkSource.endLease(USER_ID, LEASE_ID, any())
            } returns error.asFailure()

            val result = repository.revokeAccess(request = createRevocableRequest())

            assertEquals(RevokePamAccessResult.Error(error), result)
        }
    }

    @Test
    fun `decideAccessRequest should send the trimmed comment and emit accessChangedFlow`() =
        runTest {
            val request = createApprovalRequest()
            coEvery {
                pamSdkSource.decideAccessRequest(USER_ID, REQUEST_ID, any())
            } returns createRequestView().asSuccess()

            repository.accessChangedFlow.test {
                val result = repository.decideAccessRequest(
                    request = request,
                    verdict = PamDecisionVerdict.APPROVE,
                    comment = "  ok  ",
                )

                assertEquals(DecidePamAccessResult.Success, result)
                assertEquals(Unit, awaitItem())
            }
            coVerify {
                pamSdkSource.decideAccessRequest(
                    userId = USER_ID,
                    requestId = REQUEST_ID,
                    request = AccessDecisionRequest(
                        verdict = AccessDecisionVerdict.APPROVE,
                        comment = "ok",
                    ),
                )
            }
        }

    @Test
    fun `decideAccessRequest should send a blank comment as null for a denial`() = runTest {
        coEvery {
            pamSdkSource.decideAccessRequest(USER_ID, REQUEST_ID, any())
        } returns createRequestView().asSuccess()

        repository.decideAccessRequest(
            request = createApprovalRequest(),
            verdict = PamDecisionVerdict.DENY,
            comment = "   ",
        )

        coVerify {
            pamSdkSource.decideAccessRequest(
                userId = USER_ID,
                requestId = REQUEST_ID,
                request = AccessDecisionRequest(
                    verdict = AccessDecisionVerdict.DENY,
                    comment = null,
                ),
            )
        }
    }

    @Test
    fun `decideAccessRequest should never send a decision on the user's own request`() = runTest {
        val result = repository.decideAccessRequest(
            request = createApprovalRequest(requesterId = USER_ID),
            verdict = PamDecisionVerdict.APPROVE,
            comment = null,
        )

        assertEquals(DecidePamAccessResult.NotAllowed, result)
        coVerify(exactly = 0) { pamSdkSource.decideAccessRequest(any(), any(), any()) }
    }

    @Test
    fun `decideAccessRequest should not send an unknown verdict`() = runTest {
        val result = repository.decideAccessRequest(
            request = createApprovalRequest(),
            verdict = PamDecisionVerdict.UNKNOWN,
            comment = null,
        )

        assertEquals(DecidePamAccessResult.NotAllowed, result)
        coVerify(exactly = 0) { pamSdkSource.decideAccessRequest(any(), any(), any()) }
    }

    @Test
    fun `decideAccessRequest should report a request someone else already decided`() = runTest {
        coEvery {
            pamSdkSource.decideAccessRequest(USER_ID, REQUEST_ID, any())
        } returns BitwardenException
            .Approval(
                ApprovalException.Api(
                    apiMessage(409, "Conflict", "This request has already been resolved."),
                ),
            )
            .asFailure()

        val result = repository.decideAccessRequest(
            request = createApprovalRequest(),
            verdict = PamDecisionVerdict.APPROVE,
            comment = null,
        )

        assertEquals(DecidePamAccessResult.AlreadyDecided, result)
    }

    @Test
    fun `decideAccessRequest should not report AlreadyDecided for unrelated failures`() = runTest {
        listOf(
            apiMessage(500, "Internal Server Error", "request $GUID_WITH_409 already decided"),
            apiMessage(403, "Forbidden", "409 conflict decided already"),
            "error in reqwest: connection to 409.example.com failed, already decided",
        ).forEach {
            coEvery {
                pamSdkSource.decideAccessRequest(USER_ID, REQUEST_ID, any())
            } returns BitwardenException.Approval(ApprovalException.Api(it)).asFailure()

            val result = repository.decideAccessRequest(
                request = createApprovalRequest(),
                verdict = PamDecisionVerdict.APPROVE,
                comment = null,
            )

            assertTrue(result is DecidePamAccessResult.Error)
        }
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

    @Test
    fun `decideAccessRequest should return an error for other failures`() = runTest {
        val error = BitwardenException.Approval(
            ApprovalException.Api(apiMessage(500, "Internal Server Error", "boom")),
        )
        coEvery {
            pamSdkSource.decideAccessRequest(USER_ID, REQUEST_ID, any())
        } returns error.asFailure()

        val result = repository.decideAccessRequest(
            request = createApprovalRequest(),
            verdict = PamDecisionVerdict.APPROVE,
            comment = null,
        )

        assertEquals(DecidePamAccessResult.Error(error), result)
    }

    private fun createApprovalRequest(
        requesterId: String = OTHER_USER_ID,
    ): PamApprovalRequest = PamApprovalRequest(
        id = REQUEST_ID,
        cipherId = CIPHER_ID,
        collectionId = "collectionId",
        organizationId = null,
        requesterId = requesterId,
        requesterName = "Grace",
        requesterEmail = "grace@example.com",
        status = PamRequestStatus.PENDING,
        reason = "Because",
        submittedAt = NOW,
        resolvedAt = null,
        windowStart = NOW,
        windowEnd = NOW.plusSeconds(3_600),
        isExtension = false,
        isOwnRequest = requesterId == USER_ID,
        decision = null,
    )

    private fun createRevocableRequest(
        requesterId: String = OTHER_USER_ID,
    ): PamApprovalRequest = createApprovalRequest(requesterId = requesterId).copy(
        status = PamRequestStatus.APPROVED,
        lease = PamLease(
            id = LEASE_ID,
            status = PamLeaseStatus.ACTIVE,
            notBefore = NOW.minusSeconds(600),
            notAfter = NOW.plusSeconds(900),
        ),
    )

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
private const val OTHER_USER_ID = "otherUserId"
private const val CIPHER_ID = "cipherId"
private const val REQUEST_ID = "requestId"
private const val LEASE_ID = "leaseId"
private const val GUID_WITH_409 = "0a409b00-4000-4409-8409-409040904090"
private const val GUID_WITH_400 = "400a0400-0400-4400-9400-040004000400"

/** Renders a message the way the SDK renders an HTTP error response. */
private fun apiMessage(status: Int, reason: String, body: String): String =
    "error in response: status code $status $reason: $body"
private val NOW: Instant = Instant.parse("2026-01-01T12:00:00Z")
private val FIXED_CLOCK: Clock = Clock.fixed(NOW, ZoneOffset.UTC)

private fun createLeaseView(
    id: String = LEASE_ID,
    requestId: String = REQUEST_ID,
    status: AccessLeaseStatus = AccessLeaseStatus.ACTIVE,
    notAfter: Instant,
): AccessLeaseView = AccessLeaseView(
    id = id,
    requestId = requestId,
    cipherId = CIPHER_ID,
    collectionId = "collectionId",
    organizationId = null,
    requesterId = USER_ID,
    status = status,
    notBefore = NOW.minusSeconds(600),
    notAfter = notAfter,
    termination = null,
)

@Suppress("LongParameterList")
private fun createRequestView(
    id: String = REQUEST_ID,
    status: AccessRequestStatus = AccessRequestStatus.APPROVED,
    submittedAt: Instant = NOW.minusSeconds(60),
    producedLeaseId: String? = null,
    producedLeaseStatus: AccessLeaseStatus? = null,
    producedLeaseNotAfter: Instant? = null,
    extensionOf: String? = null,
    requesterId: String = USER_ID,
    notAfter: Instant = NOW.plusSeconds(1_800),
    resolvedAt: Instant? = null,
    humanDecision: AccessRequestDecisionView? = null,
): AccessRequestView = AccessRequestView(
    id = id,
    cipherId = CIPHER_ID,
    collectionId = "collectionId",
    organizationId = null,
    requesterId = requesterId,
    ruleId = null,
    status = status,
    leaseNotBefore = NOW.minusSeconds(600),
    leaseNotAfter = notAfter,
    reason = "Because",
    submittedAt = submittedAt,
    resolvedAt = resolvedAt,
    decisions = emptyList(),
    producedLeaseId = producedLeaseId,
    producedLeaseStatus = producedLeaseStatus,
    producedLeaseNotAfter = producedLeaseNotAfter,
    extensionOfLeaseId = extensionOf,
    requesterName = "Grace",
    requesterEmail = "grace@example.com",
    awaitingActivation = producedLeaseId == null && status == AccessRequestStatus.APPROVED,
    humanDecision = humanDecision,
)
