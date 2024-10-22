package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.core.DateTime
import com.bitwarden.core.Uuid
import com.bitwarden.crypto.EncString
import com.bitwarden.vault.Attachment
import com.bitwarden.vault.Card
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.Field
import com.bitwarden.vault.Identity
import com.bitwarden.vault.LocalData
import com.bitwarden.vault.Login
import com.bitwarden.vault.PasswordHistory
import com.bitwarden.vault.SecureNote
import com.x8bit.bitwarden.data.vault.datasource.network.model.CipherRepromptTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.CipherTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

/**
 * Represents a vault item that has failed to be uploaded and must be stored locally
 *
 *
 */
data class OfflineCipher (
    val id: Uuid?,
    val organizationId: Uuid?,
    val folderId: Uuid?,
    val collectionIds: List<Uuid>,
    /**
     * More recent ciphers uses individual encryption keys to encrypt the other fields of the
     * Cipher.
     */
    val key: EncString?,
    val name: EncString,
    val notes: EncString?,
    val type: CipherType,
    val login: Login?,
    val identity: Identity?,
    val card: Card?,
    val secureNote: SecureNote?,
    val favorite: kotlin.Boolean,
    val reprompt: CipherRepromptType,
    val attachments: List<Attachment>?,
    val fields: List<Field>?,
    val passwordHistory: List<PasswordHistory>?,
    val creationDate: DateTime,
    val deletedDate: DateTime?,
    val revisionDate: DateTime,
    val mergeConflict: Boolean
)