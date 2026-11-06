package com.x8bit.bitwarden.data.pam.repository.model

import java.time.Instant

/**
 * An access request made by another member that the active user can decide on (or has decided).
 *
 * @property id The ID of the request.
 * @property cipherId The ID of the gated item the request is for.
 * @property collectionId The ID of the collection the item belongs to.
 * @property organizationId The ID of the organization the item belongs to, if known.
 * @property requesterId The ID of the user who made the request.
 * @property requesterName The name of the requester, if known.
 * @property requesterEmail The email of the requester, if known.
 * @property status The status of the request.
 * @property reason The reason the requester gave.
 * @property submittedAt When the request was submitted.
 * @property resolvedAt When the request was decided, if it was.
 * @property windowStart The start of the requested access window.
 * @property windowEnd The end of the requested access window.
 * @property isExtension Whether the request asks to extend an existing lease.
 * @property isOwnRequest Whether the active user made the request. Approvers must never decide
 * their own requests.
 * @property decision The decision a person made on the request, if any.
 * @property lease The lease the request produced once access was started, if any.
 */
data class PamApprovalRequest(
    val id: String,
    val cipherId: String,
    val collectionId: String,
    val organizationId: String?,
    val requesterId: String,
    val requesterName: String?,
    val requesterEmail: String?,
    val status: PamRequestStatus,
    val reason: String?,
    val submittedAt: Instant,
    val resolvedAt: Instant?,
    val windowStart: Instant,
    val windowEnd: Instant,
    val isExtension: Boolean,
    val isOwnRequest: Boolean,
    val decision: PamApprovalDecision?,
    val lease: PamLease? = null,
) {
    /**
     * Whether the request is still worth a decision at [now]: it is pending and its window has
     * not fully elapsed yet.
     */
    fun isActionableAt(now: Instant): Boolean =
        status == PamRequestStatus.PENDING && windowEnd.isAfter(now)

    /**
     * Whether the active user may approve or deny the request at [now].
     */
    fun canBeDecidedAt(now: Instant): Boolean = isActionableAt(now) && !isOwnRequest

    /**
     * Whether the active user may revoke the access this request granted at [now]: the request
     * produced a lease that is still active and the lease is not held by the active user (holders
     * end their own access from My access).
     */
    fun canRevokeAccessAt(now: Instant): Boolean =
        !isOwnRequest && !isExtension && lease?.isActiveAt(now) == true
}

/**
 * A decision a person made on an access request.
 *
 * @property verdict The verdict that was given.
 * @property comment The comment given with the decision.
 * @property deciderName The name (or email) of the approver, if known.
 * @property decidedAt When the decision was made.
 */
data class PamApprovalDecision(
    val verdict: PamDecisionVerdict,
    val comment: String?,
    val deciderName: String?,
    val decidedAt: Instant,
)

/**
 * The verdict of a decision on an access request.
 */
enum class PamDecisionVerdict {
    APPROVE,
    DENY,
    UNKNOWN,
}
