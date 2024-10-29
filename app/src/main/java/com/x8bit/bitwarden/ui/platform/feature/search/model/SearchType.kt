package com.x8bit.bitwarden.ui.platform.feature.search.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents the difference between searching sends and searching vault items.
 */
sealed class SearchType : Parcelable {
    /**
     * Indicates that we should be searching sends.
     */
    @Parcelize
    sealed class Sends : SearchType() {
        /**
         * Indicates that we should be searching all sends.
         */
        data object All : Sends()

        /**
         * Indicates that we should be searching only text sends.
         */
        data object Texts : Sends()

        /**
         * Indicates that we should be searching only file sends.
         */
        data object Files : Sends()
    }

    /**
     * Indicates that we should be searching vault items.
     */
    @Parcelize
    sealed class Vault : SearchType() {
        /**
         * Indicates that we should be searching all vault items.
         */
        data object All : Vault()

        /**
         * Indicates that we should be searching only login ciphers.
         */
        data object Logins : Vault()

        /**
         * Indicates that we should be searching only card ciphers.
         */
        data object Cards : Vault()

        /**
         * Indicates that we should be searching only identity ciphers.
         */
        data object Identities : Vault()

        /**
         * Indicates that we should be searching only secure note ciphers.
         */
        data object SecureNotes : Vault()

        /**
         * Indicates that we should be searching only SSH key ciphers.
         */
        data object SshKeys : Vault()

        /**
         * Indicates that we should be searching only ciphers in the given collection.
         */
        data class Collection(
            val collectionId: String,
        ) : Vault()

        /**
         * Indicates that we should be searching only ciphers not in a folder.
         */
        data object NoFolder : Vault()

        /**
         * Indicates that we should be searching only ciphers in the given folder.
         */
        data class Folder(
            val folderId: String,
        ) : Vault()

        /**
         * Indicates that we should be searching only ciphers in the trash.
         */
        data object Trash : Vault()

        /**
         * Indicates that we should be searching only for verification code items.
         */
        data object VerificationCodes : Vault()
    }
}
