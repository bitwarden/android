package com.x8bit.bitwarden.data.pam.repository.model

import java.time.Instant

/**
 * An access request made by the active user, together with the lease it produced (if any).
 *
 * @property id The ID of the request.
 * @property cipherId The ID of the gated item the request is for.
 * @property status The status of the request.
 * @property reason The reason given for the request.
 * @property submittedAt When the request was submitted.
 * @property resolvedAt When the request was decided, if it was.
 * @property windowStart The start of the requested access window.
 * @property windowEnd The end of the requested access window.
 * @property isExtension Whether this request asks to extend an existing lease.
 * @property awaitingActivation Whether the request is approved but access has not been started.
 * @property lease The lease produced by the request, if access was started.
 * @property extensionPolicy Whether (and how far) the active lease can be extended. Only set
 * when [lease] is active.
 */
data class PamMyAccessRequest(
    val id: String,
    val cipherId: String,
    val status: PamRequestStatus,
    val reason: String?,
    val submittedAt: Instant,
    val resolvedAt: Instant?,
    val windowStart: Instant,
    val windowEnd: Instant,
    val isExtension: Boolean,
    val awaitingActivation: Boolean,
    val lease: PamLease?,
    val extensionPolicy: PamExtensionPolicy?,
) {
    /**
     * Whether the request is still in play at [now]: pending, approved and ready to start, or
     * producing an active lease. Everything else belongs to the history.
     */
    fun isCurrentAt(now: Instant): Boolean = when {
        lease != null -> lease.isActiveAt(now)
        status == PamRequestStatus.PENDING -> true
        status == PamRequestStatus.APPROVED -> awaitingActivation && !isExtension
        else -> false
    }
}

/**
 * The status of an access request.
 */
enum class PamRequestStatus {
    PENDING,
    APPROVED,
    DENIED,
    CANCELLED,
    EXPIRED,
    UNKNOWN,
}

/**
 * A lease granting temporary access to a gated item.
 *
 * @property id The ID of the lease.
 * @property status The status of the lease.
 * @property notBefore When the lease started.
 * @property notAfter When the lease ends.
 */
data class PamLease(
    val id: String,
    val status: PamLeaseStatus,
    val notBefore: Instant,
    val notAfter: Instant,
) {
    /**
     * Whether the lease is active at [now].
     */
    fun isActiveAt(now: Instant): Boolean =
        status == PamLeaseStatus.ACTIVE && notAfter.isAfter(now)
}

/**
 * The status of a lease.
 */
enum class PamLeaseStatus {
    ACTIVE,
    EXPIRED,
    REVOKED,
    CANCELLED,
    UNKNOWN,
}

/**
 * Describes how an active lease can be extended.
 *
 * @property maxDurationSeconds The maximum extension, in seconds, or `null` if there is no limit.
 */
data class PamExtensionPolicy(
    val maxDurationSeconds: Long?,
)
