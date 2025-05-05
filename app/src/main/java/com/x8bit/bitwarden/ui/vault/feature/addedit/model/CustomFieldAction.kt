package com.x8bit.bitwarden.ui.vault.feature.addedit.model

import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.R

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
