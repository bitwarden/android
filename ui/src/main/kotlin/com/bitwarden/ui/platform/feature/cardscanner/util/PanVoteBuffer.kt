package com.bitwarden.ui.platform.feature.cardscanner.util

/**
 * A small rolling buffer that records the Luhn-valid PAN parsed from each recent frame and
 * answers "should we emit this PAN now?" using a temporal voting threshold.
 *
 * A single-frame Luhn-valid PAN is treated as a fluke and not emitted; a PAN must appear in at
 * least [voteThreshold] of the last [windowSize] frames before the buffer reports it as
 * confirmed. Frames where no PAN was parsed are recorded as `null` so that brief disappearances
 * naturally age out prior observations.
 *
 * @property windowSize The maximum number of recent frames retained.
 * @property voteThreshold The minimum number of matching observations required to emit a PAN.
 */
internal class PanVoteBuffer(
    private val windowSize: Int = TEMPORAL_VOTE_WINDOW_SIZE,
    private val voteThreshold: Int = TEMPORAL_VOTE_THRESHOLD,
) {
    private val recent = ArrayDeque<String?>(windowSize)

    /**
     * Records [pan] (or `null` for a frame with no Luhn-valid PAN) as the latest observation and
     * returns the PAN if it has now been observed in at least [voteThreshold] of the last
     * [windowSize] frames; otherwise returns `null`.
     */
    fun record(pan: String?): String? {
        if (recent.size >= windowSize) recent.removeFirst()
        recent.addLast(pan)
        if (pan == null) return null
        val occurrences = recent.count { it == pan }
        return pan.takeIf { occurrences >= voteThreshold }
    }
}
