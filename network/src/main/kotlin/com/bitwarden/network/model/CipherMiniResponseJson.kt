package com.bitwarden.network.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

/**
 * Represents a minimal cipher response from the API, typically returned from bulk operations.
 * Contains core cipher metadata without detailed type-specific fields.
 *
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
 */
@Serializable
data class CipherMiniResponseJson(
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
    val revisionDate: ZonedDateTime,

    @SerialName("creationDate")
    @Contextual
    val creationDate: ZonedDateTime,

    @SerialName("deletedDate")
    @Contextual
    val deletedDate: ZonedDateTime?,

    @SerialName("reprompt")
    val reprompt: CipherRepromptTypeJson,

    @SerialName("key")
    val key: String?,

    @SerialName("archivedDate")
    @Contextual
    val archivedDate: ZonedDateTime?,
)
