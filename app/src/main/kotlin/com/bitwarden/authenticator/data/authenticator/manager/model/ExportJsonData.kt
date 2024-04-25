package com.bitwarden.authenticator.data.authenticator.manager.model

import kotlinx.serialization.Serializable

/**
 * Models exported authenticator data in JSON format.
 *
 * This model is loosely based off of Bitwarden's exported unencrypted vault data.
 */
@Serializable
data class ExportJsonData(
    val encrypted: Boolean,
    val items: List<ExportItem>,
) {

    /**
     * Represents a single exported authenticator item.
     *
     * This model is loosely based off of Bitwarden's exported Cipher JSON.
     */
    @Serializable
    data class ExportItem(
        val id: String,
        val name: String,
        val folderId: String?,
        val organizationId: String?,
        val collectionIds: List<String>?,
        val notes: String?,
        val type: Int,
        val login: ItemLoginData,
        val favorite: Boolean,
    ) {
        /**
         * Represents the login specific data of an exported item.
         *
         * This model is loosely based off of Bitwarden's Cipher.Login JSON.
         *
         * @property totp OTP secret used to generate a verification code.
         */
        @Serializable
        data class ItemLoginData(
            val totp: String,
        )
    }
}
