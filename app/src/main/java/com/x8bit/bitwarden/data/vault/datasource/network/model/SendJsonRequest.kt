package com.x8bit.bitwarden.data.vault.datasource.network.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

/**
 * Represents a send request.
 *
 * @property type The type of send.
 * @property name The name of the send (nullable).
 * @property notes The notes of the send (nullable).
 * @property key The send key.
 * @property maxAccessCount The maximum number of people who can access this send (nullable).
 * @property expirationDate The date in which the send will expire (nullable).
 * @property deletionDate The date in which the send will be deleted.
 * @property file The file associated with this send (nullable).
 * @property fileLength The length of the file in bytes (nullable).
 * @property text The text associated with this send (nullable).
 * @property password The password protecting this send (nullable).
 * @property isDisabled Indicate if this send is disabled.
 * @property shouldHideEmail Should the email address of the sender be hidden (nullable).
 */
@Serializable
data class SendJsonRequest(
    @SerialName("type")
    val type: SendTypeJson,

    @SerialName("name")
    val name: String?,

    @SerialName("notes")
    val notes: String?,

    @SerialName("key")
    val key: String,

    @SerialName("maxAccessCount")
    val maxAccessCount: Int?,

    @SerialName("expirationDate")
    @Contextual
    val expirationDate: ZonedDateTime?,

    @SerialName("deletionDate")
    @Contextual
    val deletionDate: ZonedDateTime,

    @SerialName("fileLength")
    val fileLength: Long?,

    @SerialName("file")
    val file: SyncResponseJson.Send.File?,

    @SerialName("text")
    val text: SyncResponseJson.Send.Text?,

    @SerialName("password")
    val password: String?,

    @SerialName("disabled")
    val isDisabled: Boolean,

    @SerialName("hideEmail")
    val shouldHideEmail: Boolean?,
)
