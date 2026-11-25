package com.x8bit.bitwarden.ui.platform.feature.search.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Represents the difference between searching sends and searching vault items.
 */
@Serializable
sealed class SearchType : Parcelable {
    /**
     * Indicates that we should be searching sends.
     */
    @Parcelize
    @Serializable
    sealed class Sends : SearchType() {
        /**
         * Indicates that we should be searching all sends.
         */
        @Serializable
        data object All : Sends()

        /**
         * Indicates that we should be searching only text sends.
         */
        @Serializable
        data object Texts : Sends()

        /**
         * Indicates that we should be searching only file sends.
         */
        @Serializable
        data object Files : Sends()
    }

    /**
     * Indicates that we should be searching vault items.
     */
    @Parcelize
    @Serializable
    sealed class Vault : SearchType() {
        /**
         * Indicates that we should be searching all vault items.
         */
        @Serializable
        data object All : Vault()

        /**
         * Indicates that we should be searching all archived vault items.
         */
        @Serializable
        data object Archive : Vault()

        /**
         * Indicates that we should be searching only login ciphers.
         */
        @Serializable
        data object Logins : Vault()

        /**
         * Indicates that we should be searching only card ciphers.
         */
        @Serializable
        data object Cards : Vault()

        /**
         * Indicates that we should be searching only identity ciphers.
         */
        @Serializable
        data object Identities : Vault()

        /**
         * Indicates that we should be searching only secure note ciphers.
         */
        @Serializable
        data object SecureNotes : Vault()

        /**
         * Indicates that we should be searching only SSH key ciphers.
         */
        @Serializable
        data object SshKeys : Vault()

        /**
         * Indicates that we should be searching only bank account ciphers.
         */
        @Serializable
        data object BankAccounts : Vault()

        /**
         * Indicates that we should be searching only license ciphers.
         */
        @Serializable
        data object Licenses : Vault()

        /**
         * Indicates that we should be searching only passport ciphers.
         */
        @Serializable
        data object Passports : Vault()

        /**
         * Indicates that we should be searching only ciphers in the given collection.
         */
        @Serializable
        data class Collection(
            val collectionId: String,
        ) : Vault()

        /**
         * Indicates that we should be searching only ciphers not in a folder.
         */
        @Serializable
        data object NoFolder : Vault()

        /**
         * Indicates that we should be searching only ciphers in the given folder.
         */
        @Serializable
        data class Folder(
            val folderId: String,
        ) : Vault()

        /**
         * Indicates that we should be searching only ciphers in the trash.
         */
        @Serializable
        data object Trash : Vault()

        /**
         * Indicates that we should be searching only for verification code items.
         */
        @Serializable
        data object VerificationCodes : Vault()
    }
}
