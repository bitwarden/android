package com.x8bit.bitwarden.ui.vault.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Represents the difference between create a completely new cipher and editing an existing one.
 */
@Serializable
sealed class VaultAddEditType : Parcelable {

    /**
     * The ID of the vault item (nullable).
     */
    abstract val vaultItemId: String?

    /**
     * Indicates that we want to create a completely new vault item.
     */
    @Parcelize
    @Serializable
    data class AddItem(
        val initialData: InitialData? = null,
    ) : VaultAddEditType() {
        override val vaultItemId: String? get() = null

        /**
         * When present, indicates that an initial value should be displayed for the new item.
         */
        @Parcelize
        @Serializable
        sealed class InitialData : Parcelable {
            /**
             * The initial value.
             */
            abstract val value: String

            /**
             * The initial value for a new password.
             */
            @Parcelize
            @Serializable
            data class Password(override val value: String) : InitialData()

            /**
             * The initial value for a new username.
             */
            @Parcelize
            @Serializable
            data class Username(override val value: String) : InitialData()
        }
    }

    /**
     * Indicates that we want to edit an existing item.
     */
    @Parcelize
    @Serializable
    data class EditItem(
        override val vaultItemId: String,
    ) : VaultAddEditType()

    /**
     * Indicates that we want to clone an existing item.
     */
    @Parcelize
    @Serializable
    data class CloneItem(
        override val vaultItemId: String,
    ) : VaultAddEditType()
}
