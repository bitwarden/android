package com.x8bit.bitwarden.data.vault.repository.model

import com.bitwarden.core.DateTime
import com.bitwarden.core.Uuid
import com.bitwarden.crypto.EncString
import com.bitwarden.vault.AttachmentView
import com.bitwarden.vault.CardView
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.FieldView
import com.bitwarden.vault.IdentityView
import com.bitwarden.vault.LocalDataView
import com.bitwarden.vault.LoginView
import com.bitwarden.vault.PasswordHistoryView
import com.bitwarden.vault.SecureNoteView

data class OfflineCipherView (
    val id: Uuid?,
    val organizationId: Uuid?,
    val folderId: Uuid?,
    val collectionIds: List<Uuid>,
    /**
     * Temporary, required to support re-encrypting existing items.
     */
    val key: EncString?,
    val name: String,
    val notes: String?,
    val type: CipherType,
    val login: LoginView?,
    val identity: IdentityView?,
    val card: CardView?,
    val secureNote: SecureNoteView?,
    val favorite: Boolean,
    val reprompt: CipherRepromptType,
    val organizationUseTotp: Boolean,
    val edit: Boolean,
    val viewPassword: Boolean,
    val localData: LocalDataView?,
    val attachments: List<AttachmentView>?,
    val fields: List<FieldView>?,
    val passwordHistory: List<PasswordHistoryView>?,
    val creationDate: DateTime,
    val deletedDate: DateTime?,
    val revisionDate: DateTime,
    val mergeConflict: Boolean
)