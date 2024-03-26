package com.x8bit.bitwarden.ui.vault.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents the difference between create a completely new cipher and editing an existing one.
 */
sealed class VaultAddEditType : Parcelable {

    /**
     *  The ID of the vault item (nullable).
     */
    abstract val vaultItemId: String?

    /**
     * Indicates that we want to create a completely new vault item.
     *
     * @property vaultItemCipherType The specified [VaultItemCipherType].
     */
    @Parcelize
    data class AddItem(
        val vaultItemCipherType: VaultItemCipherType,
    ) : VaultAddEditType() {
        override val vaultItemId: String?
            get() = null
    }

    /**
     * Indicates that we want to edit an existing item.
     */
    @Parcelize
    data class EditItem(
        override val vaultItemId: String,
    ) : VaultAddEditType()

    /**
     * Indicates that we want to clone an existing item.
     */
    @Parcelize
    data class CloneItem(
        override val vaultItemId: String,
    ) : VaultAddEditType()
}
