package com.x8bit.bitwarden.ui.tools.feature.send.addedit.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents the difference between create a completely new send and editing an existing one.
 */
sealed class AddEditSendType : Parcelable {
    /**
     * Indicates that we want to create a completely new send item.
     */
    @Parcelize
    data object AddItem : AddEditSendType()

    /**
     * Indicates that we want to create a new send item pre-filled from an existing one.
     *
     * This creates a send like [AddItem] does, but its initial values are loaded from the send
     * being copied.
     *
     * @param sendItemId The ID of the send item to copy.
     */
    @Parcelize
    data class CopyItem(val sendItemId: String) : AddEditSendType()

    /**
     * Indicates that we want to edit an existing send item.
     *
     * @param sendItemId The ID of the send item to edit.
     */
    @Parcelize
    data class EditItem(val sendItemId: String) : AddEditSendType()
}
