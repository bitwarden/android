package com.x8bit.bitwarden.data.platform.repository.util

import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.ClearClipboardFrequency

/**
 * Provides a human-readable display label for the given [ClearClipboardFrequency].
 */
val ClearClipboardFrequency.displayLabel: Text
    get() = when (this) {
        ClearClipboardFrequency.NEVER -> R.string.never
        ClearClipboardFrequency.TEN_SECONDS -> R.string.ten_seconds
        ClearClipboardFrequency.TWENTY_SECONDS -> R.string.twenty_seconds
        ClearClipboardFrequency.THIRTY_SECONDS -> R.string.thirty_seconds
        ClearClipboardFrequency.ONE_MINUTE -> R.string.one_minute
        ClearClipboardFrequency.TWO_MINUTES -> R.string.two_minutes
        ClearClipboardFrequency.FIVE_MINUTES -> R.string.five_minutes
    }
        .asText()
