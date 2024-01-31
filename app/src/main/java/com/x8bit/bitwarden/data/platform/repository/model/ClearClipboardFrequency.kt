package com.x8bit.bitwarden.data.platform.repository.model

/**
 * Represents the frequency for clearing the clipboard for the current user.
 */
@Suppress("MagicNumber")
enum class ClearClipboardFrequency(
    val frequencySeconds: Int?,
) {
    NEVER(null),
    TEN_SECONDS(10),
    TWENTY_SECONDS(20),
    THIRTY_SECONDS(30),
    ONE_MINUTE(60),
    TWO_MINUTES(120),
    FIVE_MINUTES(300),
}
