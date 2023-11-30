package com.x8bit.bitwarden.ui.vault.feature.vault.util

import com.bitwarden.core.CipherRepromptType
import com.bitwarden.core.CipherType
import com.bitwarden.core.CipherView
import com.bitwarden.core.LoginUriView
import com.bitwarden.core.LoginView
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
 * Transforms a [VaultAddItemState.ItemType] into [CipherView].
 */
fun VaultAddItemState.ItemType.toCipherView(): CipherView =
    when (this) {
        is VaultAddItemState.ItemType.Card -> toCardCipherView()
        is VaultAddItemState.ItemType.Identity -> toIdentityCipherView()
        is VaultAddItemState.ItemType.Login -> toLoginCipherView()
        is VaultAddItemState.ItemType.SecureNotes -> toSecureNotesCipherView()
    }

/**
 * Transforms [VaultAddItemState.ItemType.SecureNotes] into [CipherView].
 */
private fun VaultAddItemState.ItemType.SecureNotes.toSecureNotesCipherView(): CipherView =
    TODO("create SecureNotes CipherView BIT-509")

/**
 * Transforms [VaultAddItemState.ItemType.Login] into [CipherView].
 */
private fun VaultAddItemState.ItemType.Login.toLoginCipherView(): CipherView =
    CipherView(
        id = null,
        // TODO use real organization id BIT-780
        organizationId = null,
        // TODO use real folder id BIT-528
        folderId = null,
        collectionIds = emptyList(),
        key = null,
        name = name,
        notes = notes,
        type = CipherType.LOGIN,
        login = LoginView(
            username = username,
            password = password,
            passwordRevisionDate = null,
            uris = listOf(
                LoginUriView(
                    uri = uri,
                    // TODO implement uri settings in BIT-1094
                    match = UriMatchType.DOMAIN,
                ),
            ),
            // TODO implement totp in BIT-1066
            totp = null,
            autofillOnPageLoad = false,
        ),
        identity = null,
        card = null,
        secureNote = null,
        favorite = favorite,
        reprompt = if (masterPasswordReprompt) {
            CipherRepromptType.PASSWORD
        } else {
            CipherRepromptType.NONE
        },
        organizationUseTotp = false,
        edit = true,
        viewPassword = true,
        localData = null,
        attachments = null,
        // TODO implement custom fields BIT-529
        fields = null,
        passwordHistory = null,
        creationDate = Instant.now(),
        deletedDate = null,
        // This is a throw away value.
        // The SDK will eventually remove revisionDate via encryption.
        revisionDate = Instant.now(),
    )

/**
 * Transforms [VaultAddItemState.ItemType.Identity] into [CipherView].
 */
private fun VaultAddItemState.ItemType.Identity.toIdentityCipherView(): CipherView =
    TODO("create Identity CipherView BIT-508")

/**
 * Transforms [VaultAddItemState.ItemType.Card] into [CipherView].
 */
private fun VaultAddItemState.ItemType.Card.toCardCipherView(): CipherView =
    TODO("create Card CipherView BIT-668")
