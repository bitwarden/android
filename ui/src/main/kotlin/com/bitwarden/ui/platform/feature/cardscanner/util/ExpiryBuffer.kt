package com.bitwarden.ui.platform.feature.cardscanner.util

/**
 * A small rolling buffer that accumulates the latest non-null expiration month and year parsed
 * from any frame in the recent window.
 *
 * The PAN-confirming frame and the expiry-bearing frame are not always the same frame: ML Kit may
 * resolve the card number on one pass and the embossed expiration date on another. This buffer
 * holds onto the most recent non-null expiry so the analyzer can compose a complete scan once the
 * PAN has been confirmed via [PanVoteBuffer]. Frames where no expiry was parsed do not erase
 * earlier observations within the window — they only displace the oldest entry.
 *
 * @property windowSize The maximum number of recent frames retained.
 */
class ExpiryBuffer(
    private val windowSize: Int = TEMPORAL_VOTE_WINDOW_SIZE,
) {
    private val recent = ArrayDeque<Expiry?>(windowSize)

    /**
     * Records the [month] and [year] from this frame and returns the most recent non-null
     * expiry observed within the window, or `null` if no frame in the window has produced an
     * expiration month yet.
     */
    fun record(month: String?, year: String?): Expiry? {
        if (recent.size >= windowSize) recent.removeFirst()
        recent.addLast(month?.let { Expiry(month = it, year = year) })
        return recent.filterNotNull().lastOrNull()
    }

    /**
     * A scanned expiration date. [year] may be `null` when the expiry text only included a month.
     */
    data class Expiry(
        val month: String,
        val year: String?,
    )
}
