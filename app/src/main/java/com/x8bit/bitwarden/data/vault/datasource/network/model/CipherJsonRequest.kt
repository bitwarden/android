package com.x8bit.bitwarden.data.vault.datasource.network.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

/**
 * Represents a cipher request.
 *
 * @property notes The notes of the cipher (nullable).
 * @property reprompt The reprompt of the cipher.
 * @property passwordHistory A list of password history objects
 * associated with the cipher (nullable).
 * @property type The type of cipher.
 * @property login The login of the cipher.
 * @property secureNote The secure note of the cipher.
 * @property folderId The folder ID of the cipher (nullable).
 * @property organizationId The organization ID of the cipher (nullable).
 * @property identity The identity of the cipher.
 * @property name The name of the cipher (nullable).
 * @property fields A list of fields associated with the cipher (nullable).
 * @property isFavorite If the cipher is a favorite.
 * @property card The card of the cipher.
 * @property key The key of the cipher (nullable).
 */
@Serializable
data class CipherJsonRequest(
    @SerialName("notes")
    val notes: String?,

    @SerialName("attachments2")
    val attachments: Map<String, AttachmentJsonRequest>?,

    @SerialName("reprompt")
    val reprompt: CipherRepromptTypeJson,

    @SerialName("passwordHistory")
    val passwordHistory: List<SyncResponseJson.Cipher.PasswordHistory>?,

    @SerialName("lastKnownRevisionDate")
    @Contextual
    val lastKnownRevisionDate: ZonedDateTime?,

    @SerialName("type")
    val type: CipherTypeJson,

    @SerialName("login")
    val login: SyncResponseJson.Cipher.Login?,

    @SerialName("secureNote")
    val secureNote: SyncResponseJson.Cipher.SecureNote?,

    @SerialName("sshKey")
    val sshKey: SyncResponseJson.Cipher.SshKey?,

    @SerialName("folderId")
    val folderId: String?,

    @SerialName("organizationId")
    val organizationId: String?,

    @SerialName("identity")
    val identity: SyncResponseJson.Cipher.Identity?,

    @SerialName("name")
    val name: String?,

    @SerialName("fields")
    val fields: List<SyncResponseJson.Cipher.Field>?,

    @SerialName("favorite")
    val isFavorite: Boolean,

    @SerialName("card")
    val card: SyncResponseJson.Cipher.Card?,

    @SerialName("key")
    val key: String?,
)
