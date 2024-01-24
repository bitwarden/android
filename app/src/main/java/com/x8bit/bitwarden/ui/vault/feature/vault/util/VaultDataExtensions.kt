package com.x8bit.bitwarden.ui.vault.feature.vault.util

import android.net.Uri
import com.bitwarden.core.CipherType
import com.bitwarden.core.CipherView
import com.bitwarden.core.CollectionView
import com.bitwarden.core.FolderView
import com.bitwarden.core.LoginUriView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.vault.feature.util.toOverflowActions
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultState
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType

private const val ANDROID_URI = "androidapp://"
private const val IOS_URI = "iosapp://"

/**
 * Transforms [VaultData] into [VaultState.ViewState] using the given [vaultFilterType].
 */
@Suppress("LongMethod")
fun VaultData.toViewState(
    isPremium: Boolean,
    isIconLoadingDisabled: Boolean,
    baseIconUrl: String,
    vaultFilterType: VaultFilterType,
): VaultState.ViewState {

    val filteredCipherViewListWithDeletedItems =
        cipherViewList.toFilteredList(vaultFilterType)

    val filteredCipherViewList = filteredCipherViewListWithDeletedItems
        .filter { it.deletedDate == null }
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
                .mapNotNull {
                    it.toVaultItemOrNull(
                        isIconLoadingDisabled = isIconLoadingDisabled,
                        baseIconUrl = baseIconUrl,
                    )
                },
            folderItems = filteredFolderViewList.map { folderView ->
                VaultState.ViewState.FolderItem(
                    id = folderView.id,
                    name = folderView.name.asText(),
                    itemCount = filteredCipherViewList
                        .count {
                            !it.id.isNullOrBlank() &&
                                folderView.id == it.folderId
                        },
                )
            },
            noFolderItems = filteredCipherViewList
                .filter { it.folderId.isNullOrBlank() }
                .mapNotNull {
                    it.toVaultItemOrNull(
                        isIconLoadingDisabled = isIconLoadingDisabled,
                        baseIconUrl = baseIconUrl,
                    )
                },
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
            trashItemsCount = filteredCipherViewListWithDeletedItems.count {
                it.deletedDate != null
            },
        )
    }
}

/**
 * Method to build the icon data for login item icons.
 */
@Suppress("ReturnCount")
fun List<LoginUriView>?.toLoginIconData(
    isIconLoadingDisabled: Boolean,
    baseIconUrl: String,
): IconData {
    val localIconData = IconData.Local(R.drawable.ic_login_item)

    var uri = this
        ?.map { it.uri }
        ?.firstOrNull { uri -> uri?.contains(".") == true }
        ?: return localIconData

    if (uri.startsWith(ANDROID_URI)) {
        return IconData.Local(R.drawable.ic_android)
    }

    if (uri.startsWith(IOS_URI)) {
        return IconData.Local(R.drawable.ic_ios)
    }

    if (isIconLoadingDisabled) {
        return localIconData
    }

    if (!uri.contains("://")) {
        uri = "http://$uri"
    }

    val iconUri = Uri.parse(uri)
    val hostname = iconUri.host

    val url = "$baseIconUrl/$hostname/icon.png"

    return IconData.Network(
        uri = url,
        fallbackIconRes = R.drawable.ic_login_item,
    )
}

/**
 * Transforms a [CipherView] into a [VaultState.ViewState.VaultItem].
 */
@Suppress("MagicNumber")
private fun CipherView.toVaultItemOrNull(
    isIconLoadingDisabled: Boolean,
    baseIconUrl: String,
): VaultState.ViewState.VaultItem? {
    val id = this.id ?: return null
    return when (type) {
        CipherType.LOGIN -> VaultState.ViewState.VaultItem.Login(
            id = id,
            name = name.asText(),
            username = login?.username?.asText(),
            startIcon = login?.uris.toLoginIconData(
                isIconLoadingDisabled = isIconLoadingDisabled,
                baseIconUrl = baseIconUrl,
            ),
            overflowOptions = toOverflowActions(),
        )

        CipherType.SECURE_NOTE -> VaultState.ViewState.VaultItem.SecureNote(
            id = id,
            name = name.asText(),
            overflowOptions = toOverflowActions(),
        )

        CipherType.CARD -> VaultState.ViewState.VaultItem.Card(
            id = id,
            name = name.asText(),
            brand = card?.brand?.asText(),
            lastFourDigits = card?.number
                ?.takeLast(4)
                ?.asText(),
            overflowOptions = toOverflowActions(),
        )

        CipherType.IDENTITY -> VaultState.ViewState.VaultItem.Identity(
            id = id,
            name = name.asText(),
            firstName = identity?.firstName?.asText(),
            overflowOptions = toOverflowActions(),
        )
    }
}

/**
 * Filters out all [CipherView]s that are not part of the given [VaultFilterType].
 */
@JvmName("toFilteredCipherList")
fun List<CipherView>.toFilteredList(
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

/**
 * Filters out all [FolderView]s that are not part of the given [VaultFilterType].
 */
@JvmName("toFilteredFolderList")
fun List<FolderView>.toFilteredList(
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

/**
 * Filters out all [CollectionView]s that are not part of the given [VaultFilterType].
 */
@JvmName("toFilteredCollectionList")
fun List<CollectionView>.toFilteredList(
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
