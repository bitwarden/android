package com.x8bit.bitwarden.data.pam.repository.model

import java.time.Instant

/**
 * An access request to submit for a gated item.
 */
sealed class PamAccessRequest {
    /**
     * A request for an item with automatic approval.
     *
     * @property durationSeconds The requested duration of the lease, in seconds.
     */
    data class Automatic(val durationSeconds: Long) : PamAccessRequest()

    /**
     * A request for an item that needs human approval.
     *
     * @property start The start of the requested access window.
     * @property end The end of the requested access window.
     * @property reason The reason for the request.
     */
    data class Human(
        val start: Instant,
        val end: Instant,
        val reason: String,
    ) : PamAccessRequest()
}
