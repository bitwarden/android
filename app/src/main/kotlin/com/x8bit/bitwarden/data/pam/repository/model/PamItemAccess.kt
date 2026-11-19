package com.x8bit.bitwarden.data.pam.repository.model

import java.time.Instant

/**
 * The privileged access information for a single gated cipher.
 *
 * @property approvalMode How a request for this item is approved.
 * @property status The current access status of the item for the active user.
 * @property defaultDurationSeconds The suggested lease duration, in seconds.
 * @property maxDurationSeconds The maximum lease duration, in seconds.
 * @property canStartLease Whether the user could start a lease right now (the single lease slot is
 * free).
 * @property slotFreesAt When the lease slot is expected to free up, if held by another member.
 */
data class PamItemAccess(
    val approvalMode: PamApprovalMode,
    val status: PamAccessStatus,
    val defaultDurationSeconds: Long,
    val maxDurationSeconds: Long,
    val canStartLease: Boolean,
    val slotFreesAt: Instant?,
) {
    /**
     * Whether the single lease slot is held by another member, so a new request cannot be started
     * right now.
     */
    val isSlotHeldByAnotherMember: Boolean
        get() = status is PamAccessStatus.Privileged && !canStartLease
}

/**
 * How an access request for an item is approved.
 */
enum class PamApprovalMode {
    /**
     * Requests are approved automatically; the user only chooses a duration.
     */
    AUTOMATIC,

    /**
     * Requests are approved by a human; the user chooses a window and gives a reason.
     */
    HUMAN,

    /**
     * The server sent a mode this client does not know about.
     */
    UNKNOWN,
}

/**
 * The access status of a gated item for the active user.
 */
sealed class PamAccessStatus {
    /**
     * The item is gated and nothing has been requested.
     */
    data object Privileged : PamAccessStatus()

    /**
     * A request is waiting for approval.
     */
    data object Pending : PamAccessStatus()

    /**
     * A request was approved but no lease is active yet.
     *
     * @property requestId The ID of the approved request, used to start or cancel it, if known.
     */
    data class Approved(val requestId: String?) : PamAccessStatus()

    /**
     * The user holds an active lease that ends at [expiresAt].
     */
    data class Active(val expiresAt: Instant) : PamAccessStatus()
}
