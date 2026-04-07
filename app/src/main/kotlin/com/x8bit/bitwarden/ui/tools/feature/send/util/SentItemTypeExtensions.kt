package com.x8bit.bitwarden.ui.tools.feature.send.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType

/**
 * Returns the selection text based on the given [SendItemType].
 */
val SendItemType.selectionText: Text
    get() = when (this) {
        SendItemType.FILE -> BitwardenString.file.asText()
        SendItemType.TEXT -> BitwardenString.text.asText()
    }
