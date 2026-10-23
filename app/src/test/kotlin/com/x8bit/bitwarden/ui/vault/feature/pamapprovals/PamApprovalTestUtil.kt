package com.x8bit.bitwarden.ui.vault.feature.pamapprovals

import com.x8bit.bitwarden.data.pam.repository.model.PamApprovalRequest
import com.x8bit.bitwarden.data.pam.repository.model.PamLease
import com.x8bit.bitwarden.data.pam.repository.model.PamRequestStatus
import java.time.Instant

val PAM_TEST_NOW: Instant = Instant.parse("2026-01-01T12:00:00Z")

/**
 * Creates a [PamApprovalRequest] for tests.
 */
@Suppress("LongParameterList")
fun createMockPamApprovalRequest(
    id: String = "requestId",
    cipherId: String = "cipherId",
    collectionId: String = "mockId-1",
    organizationId: String? = "organizationId",
    requesterId: String = "requesterId",
    requesterName: String? = "Grace",
    requesterEmail: String? = "grace@example.com",
    status: PamRequestStatus = PamRequestStatus.PENDING,
    reason: String? = "Prod incident",
    submittedAt: Instant = PAM_TEST_NOW.minusSeconds(60),
    windowStart: Instant = PAM_TEST_NOW,
    windowEnd: Instant = PAM_TEST_NOW.plusSeconds(5_400),
    isOwnRequest: Boolean = false,
    lease: PamLease? = null,
): PamApprovalRequest = PamApprovalRequest(
    id = id,
    cipherId = cipherId,
    collectionId = collectionId,
    organizationId = organizationId,
    requesterId = requesterId,
    requesterName = requesterName,
    requesterEmail = requesterEmail,
    status = status,
    reason = reason,
    submittedAt = submittedAt,
    resolvedAt = null,
    windowStart = windowStart,
    windowEnd = windowEnd,
    isExtension = false,
    isOwnRequest = isOwnRequest,
    decision = null,
    lease = lease,
)
