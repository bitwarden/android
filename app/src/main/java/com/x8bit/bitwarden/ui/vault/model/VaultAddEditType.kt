package com.x8bit.bitwarden.ui.vault.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents the difference between create a completely new cipher and editing an existing one.
 */
sealed class VaultAddEditType : Parcelable {
    /**
     * Indicates that we want to create a completely new vault item.
     */
    @Parcelize
    data object AddItem : VaultAddEditType()

    /**
     * Indicates that we want to edit an existing item.
     *
     * @param vaultItemId The ID of the vault item to edit.
     */
    @Parcelize
    data class EditItem(
        val vaultItemId: String,
    ) : VaultAddEditType()
}
