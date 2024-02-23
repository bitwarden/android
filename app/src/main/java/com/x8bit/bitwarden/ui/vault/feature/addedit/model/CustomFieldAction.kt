package com.x8bit.bitwarden.ui.vault.feature.addedit.model

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText

/**
 * Represents the different actions that can be taken in a custom
 * item edit menu.
 */
enum class CustomFieldAction(val actionText: Text) {
    EDIT(R.string.edit.asText()),
    MOVE_UP(R.string.move_up.asText()),
    MOVE_DOWN(R.string.move_down.asText()),
    REMOVE(R.string.remove.asText()),
}
