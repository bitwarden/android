package com.x8bit.bitwarden.ui.tools.feature.send.util

import com.bitwarden.send.SendType
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType

/**
 * Converts the [SendType] to its corresponding [SendItemType].
 */
fun SendType.toSendItemType(): SendItemType =
    when (this) {
        SendType.FILE -> SendItemType.FILE
        SendType.TEXT -> SendItemType.TEXT
    }
