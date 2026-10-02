package com.bitwarden.network.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Represents a minimal cipher response from the API, typically returned from bulk operations.
 * Contains cipher metadata and encrypted fields, but not user-specific fields (folder,
 * favorite, permissions).
 *
 * @property cipherMiniResponse The list of mini responses.
 */
@Serializable
data class CipherMiniResponseJson(
    @SerialName("data")
    val cipherMiniResponse: List<CipherMiniResponse>,
) {
    /**
     * @property id The ID of the cipher.
     * @property organizationId The organization ID (nullable).
     * @property type The type of cipher.
     * @property data Serialized cipher data (newer API format).
     * @property attachments List of attachments (nullable).
     * @property shouldOrganizationUseTotp If the organization should use TOTP.
     * @property revisionDate The revision date.
     * @property creationDate The creation date.
     * @property deletedDate The deleted date (nullable).
     * @property reprompt The reprompt type.
     * @property key The cipher key (nullable).
     * @property archivedDate The archived date (nullable).
     * @property name The encrypted name (nullable, `null` for blob-encrypted ciphers).
     * @property notes The encrypted notes (nullable).
     * @property login The login data (nullable).
     * @property card The card data (nullable).
     * @property identity The identity data (nullable).
     * @property secureNote The secure note data (nullable).
     * @property sshKey The SSH key data (nullable).
     * @property bankAccount The bank account data (nullable).
     * @property driversLicense The driver's license data (nullable).
     * @property passport The passport data (nullable).
     * @property fields List of custom fields (nullable).
     * @property passwordHistory List of password history entries (nullable).
     */
    @Serializable
    data class CipherMiniResponse(
        @SerialName("id")
        val id: String,

        @SerialName("organizationId")
        val organizationId: String?,

        @SerialName("type")
        val type: CipherTypeJson,

        @SerialName("data")
        val data: String?,

        @SerialName("attachments")
        val attachments: List<SyncResponseJson.Cipher.Attachment>?,

        @SerialName("organizationUseTotp")
        val shouldOrganizationUseTotp: Boolean,

        @SerialName("revisionDate")
        @Contextual
        val revisionDate: Instant,

        @SerialName("creationDate")
        @Contextual
        val creationDate: Instant,

        @SerialName("deletedDate")
        @Contextual
        val deletedDate: Instant?,

        @SerialName("reprompt")
        val reprompt: CipherRepromptTypeJson,

        @SerialName("key")
        val key: String?,

        @SerialName("archivedDate")
        @Contextual
        val archivedDate: Instant?,

        @SerialName("name")
        val name: String?,

        @SerialName("notes")
        val notes: String?,

        @SerialName("login")
        val login: SyncResponseJson.Cipher.Login?,

        @SerialName("card")
        val card: SyncResponseJson.Cipher.Card?,

        @SerialName("identity")
        val identity: SyncResponseJson.Cipher.Identity?,

        @SerialName("secureNote")
        val secureNote: SyncResponseJson.Cipher.SecureNote?,

        @SerialName("sshKey")
        val sshKey: SyncResponseJson.Cipher.SshKey?,

        @SerialName("bankAccount")
        val bankAccount: SyncResponseJson.Cipher.BankAccount?,

        @SerialName("driversLicense")
        val driversLicense: SyncResponseJson.Cipher.DriversLicense?,

        @SerialName("passport")
        val passport: SyncResponseJson.Cipher.Passport?,

        @SerialName("fields")
        val fields: List<SyncResponseJson.Cipher.Field>?,

        @SerialName("passwordHistory")
        val passwordHistory: List<SyncResponseJson.Cipher.PasswordHistory>?,
    )
}
