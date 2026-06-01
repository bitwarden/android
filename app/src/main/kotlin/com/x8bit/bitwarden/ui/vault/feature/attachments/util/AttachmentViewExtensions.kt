package com.x8bit.bitwarden.ui.vault.feature.attachments.util

import com.bitwarden.vault.AttachmentView

private const val TEN_MB_IN_BYTES: Long = 10485760L

/**
 * @return `true` if the file is larger than 10MB, `false` otherwise.
 */
fun AttachmentView.isLargeFile(): Boolean =
    try {
        (this.size?.toLong() ?: 0L) >= TEN_MB_IN_BYTES
    } catch (_: NumberFormatException) {
        false
    }
