package com.x8bit.bitwarden.ui.vault.feature.vault.util

import com.bitwarden.core.CipherType
import com.bitwarden.core.CipherView
import com.bitwarden.core.CollectionView
import com.bitwarden.core.FolderView
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultState
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType

/**
 * Transforms [VaultData] into [VaultState.ViewState] using the given [vaultFilterType].
 */
fun VaultData.toViewState(
    isPremium: Boolean,
    vaultFilterType: VaultFilterType,
): VaultState.ViewState {
    val filteredCipherViewList = cipherViewList.toFilteredList(vaultFilterType)
    val filteredFolderViewList = folderViewList.toFilteredList(vaultFilterType)
    val filteredCollectionViewList = collectionViewList.toFilteredList(vaultFilterType)

    return if (filteredCipherViewList.isEmpty()) {
        VaultState.ViewState.NoItems
    } else {
        VaultState.ViewState.Content(
            totpItemsCount = if (isPremium) {
                filteredCipherViewList.count { it.login?.totp != null }
            } else {
                0
            },
            loginItemsCount = filteredCipherViewList.count { it.type == CipherType.LOGIN },
            cardItemsCount = filteredCipherViewList.count { it.type == CipherType.CARD },
            identityItemsCount = filteredCipherViewList.count { it.type == CipherType.IDENTITY },
            secureNoteItemsCount = filteredCipherViewList
                .count { it.type == CipherType.SECURE_NOTE },
            favoriteItems = filteredCipherViewList
                .filter { it.favorite }
                .mapNotNull { it.toVaultItemOrNull() },
            folderItems = filteredFolderViewList.map { folderView ->
                VaultState.ViewState.FolderItem(
                    id = folderView.id,
                    name = folderView.name.asText(),
                    itemCount = filteredCipherViewList
                        .count { !it.id.isNullOrBlank() && folderView.id == it.folderId },
                )
            },
            noFolderItems = filteredCipherViewList
                .filter { it.folderId.isNullOrBlank() }
                .mapNotNull { it.toVaultItemOrNull() },
            collectionItems = filteredCollectionViewList
                .filter { it.id != null }
                .map { collectionView ->
                    VaultState.ViewState.CollectionItem(
                        id = requireNotNull(collectionView.id),
                        name = collectionView.name,
                        itemCount = filteredCipherViewList
                            .count {
                                !it.id.isNullOrBlank() &&
                                    collectionView.id in it.collectionIds
                            },
                    )
                },
            // TODO need to populate trash item count in BIT-969
            trashItemsCount = 0,
        )
    }
}

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

@JvmName("toFilteredCipherList")
private fun List<CipherView>.toFilteredList(
    vaultFilterType: VaultFilterType,
): List<CipherView> =
    this
        // Filter out any items with invalid IDs in the unlikely case they exist
        .filterNot { it.id.isNullOrBlank() }
        .filter {
            when (vaultFilterType) {
                VaultFilterType.AllVaults -> true
                VaultFilterType.MyVault -> it.organizationId == null
                is VaultFilterType.OrganizationVault -> {
                    it.organizationId == vaultFilterType.organizationId
                }
            }
        }

@JvmName("toFilteredFolderList")
private fun List<FolderView>.toFilteredList(
    vaultFilterType: VaultFilterType,
): List<FolderView> =
    this
        .filter {
            when (vaultFilterType) {
                // Folders are only included when including the user's personal data.
                VaultFilterType.AllVaults,
                VaultFilterType.MyVault,
                -> true

                is VaultFilterType.OrganizationVault -> false
            }
        }

@JvmName("toFilteredCollectionList")
private fun List<CollectionView>.toFilteredList(
    vaultFilterType: VaultFilterType,
): List<CollectionView> =
    this
        .filter {
            when (vaultFilterType) {
                VaultFilterType.AllVaults -> true
                VaultFilterType.MyVault -> false
                is VaultFilterType.OrganizationVault -> {
                    it.organizationId == vaultFilterType.organizationId
                }
            }
        }
