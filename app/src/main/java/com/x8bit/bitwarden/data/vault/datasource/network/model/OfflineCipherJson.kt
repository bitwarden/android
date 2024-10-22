package com.x8bit.bitwarden.data.vault.datasource.network.model

import com.bitwarden.core.DateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.time.ZonedDateTime

/**
 * Represents the response model for vault data fetched from the server.
 *
 * @property folders A list of folders associated with the vault data (nullable).
 * @property collections A list of collections associated with the vault data (nullable).
 * @property profile The profile associated with the vault data.
 * @property ciphers A list of ciphers associated with the vault data (nullable).
 * @property policies A list of policies associated with the vault data (nullable).
 * @property domains A domains object associated with the vault data.
 * @property sends A list of send objects associated with the vault data (nullable).
 */
@Serializable
data class OfflineCipherJson(
    @SerialName("id")
    val id: String,

    @SerialName("organizationId")
    val organizationId: String?,

    @SerialName("folderId")
    val folderId: String?,

    @SerialName("collectionIds")
    val collectionIds: List<String>,

    @SerialName("key")
    val key: String?,

    @SerialName("name")
    val name: String,

    @SerialName("notes")
    val notes: String?,

    @SerialName("login")
    val login: SyncResponseJson.Cipher.Login?,

    @SerialName("identity")
    val identity: SyncResponseJson.Cipher.Identity?,

    @SerialName("card")
    val card: SyncResponseJson.Cipher.Card?,

    @SerialName("secureNote")
    val secureNote: SyncResponseJson.Cipher.SecureNote?,

    @SerialName("favorite")
    val favorite: Boolean,

    @SerialName("reprompt")
    val reprompt: CipherRepromptTypeJson,

    @SerialName("attachments")
    val attachments: List<SyncResponseJson.Cipher.Attachment>?,

    @SerialName("fields")
    val fields: List<SyncResponseJson.Cipher.Field>?,

    @SerialName("passwordHistory")
    val passwordHistory: List<SyncResponseJson.Cipher.PasswordHistory>?,

    @SerialName("creationDate")
    @Contextual
    val creationDate: ZonedDateTime,

    @SerialName("deletionDate")
    @Contextual
    val deletedDate: ZonedDateTime?,

    @SerialName("revisionDate")
    @Contextual
    val revisionDate: ZonedDateTime,

    @SerialName("type")
    @Contextual
    val type: CipherTypeJson,

    @SerialName("mergeConflict")
    val mergeConflict: Boolean

){
    // TODO: Add password history, fields, etc
}