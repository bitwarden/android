package com.x8bit.bitwarden.data.platform.repository.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.platform.repository.model.ClearClipboardFrequency

/**
 * Provides a human-readable display label for the given [ClearClipboardFrequency].
 */
val ClearClipboardFrequency.displayLabel: Text
    get() = when (this) {
        ClearClipboardFrequency.NEVER -> BitwardenString.never
        ClearClipboardFrequency.TEN_SECONDS -> BitwardenString.ten_seconds
        ClearClipboardFrequency.TWENTY_SECONDS -> BitwardenString.twenty_seconds
        ClearClipboardFrequency.THIRTY_SECONDS -> BitwardenString.thirty_seconds
        ClearClipboardFrequency.ONE_MINUTE -> BitwardenString.one_minute
        ClearClipboardFrequency.TWO_MINUTES -> BitwardenString.two_minutes
        ClearClipboardFrequency.FIVE_MINUTES -> BitwardenString.five_minutes
    }
        .asText()
