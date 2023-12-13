package com.x8bit.bitwarden.ui.vault.feature.vault.util

import com.bitwarden.core.CardView
import com.bitwarden.core.CipherRepromptType
import com.bitwarden.core.CipherType
import com.bitwarden.core.CipherView
import com.bitwarden.core.FieldType
import com.bitwarden.core.FieldView
import com.bitwarden.core.IdentityView
import com.bitwarden.core.LoginUriView
import com.bitwarden.core.LoginView
import com.bitwarden.core.SecureNoteType
import com.bitwarden.core.SecureNoteView
import com.bitwarden.core.UriMatchType
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.vault.feature.additem.VaultAddItemState
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultState
import java.time.Instant

/**
 * Transforms a [CipherView] into a [VaultState.ViewState.VaultItem].
 */
@Suppress("MagicNumber")
private fun CipherView.toVaultItemOrNull(): VaultState.ViewState.VaultItem? {
    val id = this.id ?: return null
    return when (type) {
        CipherType.LOGIN -> VaultState.ViewState.VaultItem.Login(
            id = id,
            name = name.asText(),
            username = login?.username?.asText(),
        )

        CipherType.SECURE_NOTE -> VaultState.ViewState.VaultItem.SecureNote(
            id = id,
            name = name.asText(),
        )

        CipherType.CARD -> VaultState.ViewState.VaultItem.Card(
            id = id,
            name = name.asText(),
            brand = card?.brand?.asText(),
            lastFourDigits = card?.number
                ?.takeLast(4)
                ?.asText(),
        )

        CipherType.IDENTITY -> VaultState.ViewState.VaultItem.Identity(
            id = id,
            name = name.asText(),
            firstName = identity?.firstName?.asText(),
        )
    }
}

/**
 * Transforms [VaultData] into [VaultState.ViewState].
 */
fun VaultData.toViewState(): VaultState.ViewState =
    if (cipherViewList.isEmpty() && folderViewList.isEmpty()) {
        VaultState.ViewState.NoItems
    } else {
        // Filter out any items with invalid IDs in the unlikely case they exist
        val filteredCipherViewList = cipherViewList.filterNot { it.id.isNullOrBlank() }
        VaultState.ViewState.Content(
            loginItemsCount = filteredCipherViewList.count { it.type == CipherType.LOGIN },
            cardItemsCount = filteredCipherViewList.count { it.type == CipherType.CARD },
            identityItemsCount = filteredCipherViewList.count { it.type == CipherType.IDENTITY },
            secureNoteItemsCount = filteredCipherViewList
                .count { it.type == CipherType.SECURE_NOTE },
            favoriteItems = cipherViewList
                .filter { it.favorite }
                .mapNotNull { it.toVaultItemOrNull() },
            folderItems = folderViewList.map { folderView ->
                VaultState.ViewState.FolderItem(
                    id = folderView.id,
                    name = folderView.name.asText(),
                    itemCount = cipherViewList
                        .count { !it.id.isNullOrBlank() && folderView.id == it.folderId },
                )
            },
            noFolderItems = cipherViewList
                .filter { it.folderId.isNullOrBlank() }
                .mapNotNull { it.toVaultItemOrNull() },
            // TODO need to populate trash item count in BIT-969
            trashItemsCount = 0,
        )
    }

/**
 * Transforms a [VaultAddItemState.ViewState.Content] into [CipherView].
 */
fun VaultAddItemState.ViewState.Content.toCipherView(): CipherView =
    CipherView(
        // Pulled from original cipher when editing, otherwise uses defaults
        id = this.originalCipher?.id,
        collectionIds = this.originalCipher?.collectionIds.orEmpty(),
        key = this.originalCipher?.key,
        edit = this.originalCipher?.edit ?: true,
        viewPassword = this.originalCipher?.viewPassword ?: true,
        localData = this.originalCipher?.localData,
        attachments = this.originalCipher?.attachments,
        organizationUseTotp = this.originalCipher?.organizationUseTotp ?: false,
        passwordHistory = this.originalCipher?.passwordHistory,
        creationDate = this.originalCipher?.creationDate ?: Instant.now(),
        deletedDate = this.originalCipher?.deletedDate,
        revisionDate = this.originalCipher?.revisionDate ?: Instant.now(),

        // Type specific section
        type = this.toCipherType(),
        identity = this.toIdentityView(),
        secureNote = this.toSecureNotesView(),
        login = this.toLoginView(),
        card = this.toCardView(),

        // Fields we always grab from the UI
        name = this.name,
        notes = this.notes,
        favorite = this.favorite,
        // TODO Use real folder ID (BIT-528)
        folderId = this.originalCipher?.folderId,
        // TODO Use real organization ID (BIT-780)
        organizationId = this.originalCipher?.organizationId,
        reprompt = this.toCipherRepromptType(),
        fields = this.customFieldData.map { it.toFieldView() },
    )

private fun VaultAddItemState.ViewState.Content.toCipherType(): CipherType =
    when (this) {
        is VaultAddItemState.ViewState.Content.Card -> CipherType.CARD
        is VaultAddItemState.ViewState.Content.Identity -> CipherType.IDENTITY
        is VaultAddItemState.ViewState.Content.Login -> CipherType.LOGIN
        is VaultAddItemState.ViewState.Content.SecureNotes -> CipherType.SECURE_NOTE
    }

private fun VaultAddItemState.ViewState.Content.toCardView(): CardView? =
    (this as? VaultAddItemState.ViewState.Content.Card)?.let {
        // TODO Create real CardView from Content (BIT-668)
        CardView(
            cardholderName = null,
            expMonth = null,
            expYear = null,
            code = null,
            brand = null,
            number = null,
        )
    }

private fun VaultAddItemState.ViewState.Content.toIdentityView(): IdentityView? =
    (this as? VaultAddItemState.ViewState.Content.Identity)?.let {
        // TODO Create real IdentityView from Content (BIT-508)
        IdentityView(
            title = null,
            firstName = null,
            lastName = null,
            middleName = null,
            address1 = null,
            address2 = null,
            address3 = null,
            city = null,
            state = null,
            postalCode = null,
            country = null,
            company = null,
            email = null,
            phone = null,
            ssn = null,
            username = null,
            passportNumber = null,
            licenseNumber = null,
        )
    }

private fun VaultAddItemState.ViewState.Content.toLoginView(): LoginView? =
    (this as? VaultAddItemState.ViewState.Content.Login)?.let {
        LoginView(
            username = it.username,
            password = it.password,
            passwordRevisionDate = it.originalCipher?.login?.passwordRevisionDate,
            uris = listOf(
                // TODO Implement URI list (BIT-1094)
                LoginUriView(
                    uri = it.uri,
                    // TODO Implement URI settings in (BIT-1094)
                    match = UriMatchType.DOMAIN,
                ),
            ),
            // TODO Implement TOTP (BIT-1066)
            totp = it.originalCipher?.login?.totp,
            autofillOnPageLoad = it.originalCipher?.login?.autofillOnPageLoad,
        )
    }

private fun VaultAddItemState.ViewState.Content.toSecureNotesView(): SecureNoteView? =
    (this as? VaultAddItemState.ViewState.Content.SecureNotes)?.let {
        SecureNoteView(type = SecureNoteType.GENERIC)
    }

private fun VaultAddItemState.ViewState.Content.toCipherRepromptType(): CipherRepromptType =
    if (this.masterPasswordReprompt) {
        CipherRepromptType.PASSWORD
    } else {
        CipherRepromptType.NONE
    }

/**
 * Transforms [VaultAddItemState.Custom into [FieldView].
 */
private fun VaultAddItemState.Custom.toFieldView(): FieldView =
    when (val item = this) {
        is VaultAddItemState.Custom.BooleanField -> {
            FieldView(
                name = item.name,
                value = item.value.toString(),
                type = FieldType.BOOLEAN,
                linkedId = null,
            )
        }

        is VaultAddItemState.Custom.HiddenField -> {
            FieldView(
                name = item.name,
                value = item.value,
                type = FieldType.HIDDEN,
                linkedId = null,
            )
        }

        is VaultAddItemState.Custom.LinkedField -> {
            FieldView(
                name = item.name,
                value = null,
                type = FieldType.LINKED,
                linkedId = item.vaultLinkedFieldType.id,
            )
        }

        is VaultAddItemState.Custom.TextField -> {
            FieldView(
                name = item.name,
                value = item.value,
                type = FieldType.TEXT,
                linkedId = null,
            )
        }
    }
