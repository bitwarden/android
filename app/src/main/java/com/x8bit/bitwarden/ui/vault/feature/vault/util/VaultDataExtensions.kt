package com.x8bit.bitwarden.ui.vault.feature.vault.util

import com.bitwarden.core.CipherType
import com.bitwarden.core.CipherView
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultState

/**
 * Transforms a [CipherView] into a [VaultState.ViewState.VaultItem].
 */
@Suppress("MagicNumber")
private fun CipherView.toVaultItem(): VaultState.ViewState.VaultItem =
    when (type) {
        CipherType.LOGIN -> VaultState.ViewState.VaultItem.Login(
            id = id.toString(),
            name = name.asText(),
            username = login?.username?.asText(),
        )

        CipherType.SECURE_NOTE -> VaultState.ViewState.VaultItem.SecureNote(
            id = id.toString(),
            name = name.asText(),
        )

        CipherType.CARD -> VaultState.ViewState.VaultItem.Card(
            id = id.toString(),
            name = name.asText(),
            brand = card?.brand?.asText(),
            lastFourDigits = card?.number
                ?.takeLast(4)
                ?.asText(),
        )

        CipherType.IDENTITY -> VaultState.ViewState.VaultItem.Identity(
            id = id.toString(),
            name = name.asText(),
            firstName = identity?.firstName?.asText(),
        )
    }

/**
 * Transforms [VaultData] into [VaultState.ViewState].
 */
fun VaultData.toViewState(): VaultState.ViewState =
    if (cipherViewList.isEmpty() && folderViewList.isEmpty()) {
        VaultState.ViewState.NoItems
    } else {
        VaultState.ViewState.Content(
            loginItemsCount = cipherViewList.count { it.type == CipherType.LOGIN },
            cardItemsCount = cipherViewList.count { it.type == CipherType.CARD },
            identityItemsCount = cipherViewList.count { it.type == CipherType.IDENTITY },
            secureNoteItemsCount = cipherViewList.count { it.type == CipherType.SECURE_NOTE },
            favoriteItems = cipherViewList
                .filter { it.favorite }
                .map { it.toVaultItem() },
            folderItems = folderViewList.map { folderView ->
                VaultState.ViewState.FolderItem(
                    id = folderView.id,
                    name = folderView.name.asText(),
                    itemCount = cipherViewList.count { folderView.id == it.folderId },
                )
            },
            noFolderItems = cipherViewList
                .filter { it.folderId.isNullOrBlank() }
                .map { it.toVaultItem() },
            // TODO need to populate trash item count in BIT-969
            trashItemsCount = 0,
        )
    }
