package com.x8bit.bitwarden.ui.vault.feature.itemlisting.model

import android.os.Parcelable
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import kotlinx.parcelize.Parcelize

/**
 * Represents the actions for an individual item's overflow menu.
 */
sealed class ListingItemOverflowAction : Parcelable {

    /**
     * The display title of the option.
     */
    abstract val title: Text

    /**
     * Represents the send actions.
     */
    sealed class SendAction : ListingItemOverflowAction() {
        /**
         * Click on the edit send overflow option.
         */
        @Parcelize
        data class EditClick(val sendId: String) : SendAction() {
            override val title: Text get() = R.string.edit.asText()
        }

        /**
         * Click on the copy send URL overflow option.
         */
        @Parcelize
        data class CopyUrlClick(val sendUrl: String) : SendAction() {
            override val title: Text get() = R.string.copy_link.asText()
        }

        /**
         * Click on the share send URL overflow option.
         */
        @Parcelize
        data class ShareUrlClick(val sendUrl: String) : SendAction() {
            override val title: Text get() = R.string.share_link.asText()
        }

        /**
         * Click on the remove password send overflow option.
         */
        @Parcelize
        data class RemovePasswordClick(val sendId: String) : SendAction() {
            override val title: Text get() = R.string.remove_password.asText()
        }

        /**
         * Click on the delete send overflow option.
         */
        @Parcelize
        data class DeleteClick(val sendId: String) : SendAction() {
            override val title: Text get() = R.string.delete.asText()
        }
    }

    /**
     * Represents the vault actions.
     */
    sealed class VaultAction : ListingItemOverflowAction() {
        /**
         * Whether the action requires a master password re-prompt if that
         * setting is enabled for the selected item.
         */
        abstract val requiresPasswordReprompt: Boolean

        /**
         * Click on the view cipher overflow option.
         */
        @Parcelize
        data class ViewClick(val cipherId: String) : VaultAction() {
            override val title: Text get() = R.string.view.asText()
            override val requiresPasswordReprompt: Boolean get() = false
        }

        /**
         * Click on the edit cipher overflow option.
         */
        @Parcelize
        data class EditClick(
            val cipherId: String,
            override val requiresPasswordReprompt: Boolean,
        ) : VaultAction() {
            override val title: Text get() = R.string.edit.asText()
        }

        /**
         * Click on the copy username overflow option.
         */
        @Parcelize
        data class CopyUsernameClick(val username: String) : VaultAction() {
            override val title: Text get() = R.string.copy_username.asText()
            override val requiresPasswordReprompt: Boolean get() = false
        }

        /**
         * Click on the copy password overflow option.
         */
        @Parcelize
        data class CopyPasswordClick(
            val cipherId: String,
            val password: String,
            override val requiresPasswordReprompt: Boolean,
        ) : VaultAction() {
            override val title: Text get() = R.string.copy_password.asText()
        }

        /**
         * Click on the copy TOTP code overflow option.
         */
        @Parcelize
        data class CopyTotpClick(val totpCode: String) : VaultAction() {
            override val title: Text get() = R.string.copy_totp.asText()
            override val requiresPasswordReprompt: Boolean get() = false
        }

        /**
         * Click on the copy number overflow option.
         */
        @Parcelize
        data class CopyNumberClick(
            val number: String,
            override val requiresPasswordReprompt: Boolean,
        ) : VaultAction() {
            override val title: Text get() = R.string.copy_number.asText()
        }

        /**
         * Click on the copy security code overflow option.
         */
        @Parcelize
        data class CopySecurityCodeClick(
            val securityCode: String,
            val cipherId: String,
            override val requiresPasswordReprompt: Boolean,
        ) : VaultAction() {
            override val title: Text get() = R.string.copy_security_code.asText()
        }

        /**
         * Click on the copy secure note overflow option.
         */
        @Parcelize
        data class CopyNoteClick(val notes: String) : VaultAction() {
            override val title: Text get() = R.string.copy_notes.asText()
            override val requiresPasswordReprompt: Boolean get() = false
        }

        /**
         * Click on the launch overflow option.
         */
        @Parcelize
        data class LaunchClick(val url: String) : VaultAction() {
            override val title: Text get() = R.string.launch.asText()
            override val requiresPasswordReprompt: Boolean get() = false
        }
    }
}
