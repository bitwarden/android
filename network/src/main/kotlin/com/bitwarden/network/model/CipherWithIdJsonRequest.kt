package com.bitwarden.network.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

/**
 * Represents a cipher request with an ID, typically used in bulk operations
 * where the server needs to identify which cipher is being updated/shared.
 * Contains all properties from [CipherJsonRequest] plus an ID field.
 *
 * @property id The unique identifier of the cipher.
 */
@Serializable
data class CipherWithIdJsonRequest(
    @SerialName("Id")
    val id: String,

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

    @SerialName("encryptedFor")
    val encryptedFor: String?,
)

/**
 * Converts a [CipherJsonRequest] and an ID into a [CipherWithIdJsonRequest].
 */
fun CipherJsonRequest.toCipherWithIdJsonRequest(id: String): CipherWithIdJsonRequest =
    CipherWithIdJsonRequest(
        id = id,
        notes = notes,
        attachments = attachments,
        reprompt = reprompt,
        passwordHistory = passwordHistory,
        lastKnownRevisionDate = lastKnownRevisionDate,
        type = type,
        login = login,
        secureNote = secureNote,
        sshKey = sshKey,
        folderId = folderId,
        organizationId = organizationId,
        identity = identity,
        name = name,
        fields = fields,
        isFavorite = isFavorite,
        card = card,
        key = key,
        encryptedFor = encryptedFor,
    )
