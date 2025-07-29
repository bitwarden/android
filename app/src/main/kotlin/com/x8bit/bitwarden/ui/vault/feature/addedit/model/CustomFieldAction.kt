package com.x8bit.bitwarden.ui.vault.feature.addedit.model

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText

/**
 * Represents the different actions that can be taken in a custom
 * item edit menu.
 */
enum class CustomFieldAction(val actionText: Text) {
    EDIT(BitwardenString.edit.asText()),
    MOVE_UP(BitwardenString.move_up.asText()),
    MOVE_DOWN(BitwardenString.move_down.asText()),
    REMOVE(BitwardenString.remove.asText()),
}
