package com.x8bit.bitwarden.ui.vault.feature.vault.util

import androidx.core.net.toUri
import com.bitwarden.collections.CollectionView
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherListViewType
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.FolderView
import com.bitwarden.vault.LoginUriView
import com.x8bit.bitwarden.data.autofill.util.card
import com.x8bit.bitwarden.data.autofill.util.login
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.data.vault.repository.util.toFailureCipherListView
import com.x8bit.bitwarden.ui.vault.feature.util.getFilteredCollections
import com.x8bit.bitwarden.ui.vault.feature.util.getFilteredFolders
import com.x8bit.bitwarden.ui.vault.feature.util.toLabelIcons
import com.x8bit.bitwarden.ui.vault.feature.util.toOverflowActions
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultState
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.model.findVaultCardBrandWithNameOrNull

private const val ANDROID_URI = "androidapp://"
private const val IOS_URI = "iosapp://"

/**
 * The maximum number of no folder items that can be displayed before the UI creates a
 * no folder "folder".
 */
private const val NO_FOLDER_ITEM_THRESHOLD: Int = 100

/**
 * Transforms [VaultData] into [VaultState.ViewState] using the given [vaultFilterType].
 */
@Suppress("LongMethod", "LongParameterList")
fun VaultData.toViewState(
    isPremium: Boolean,
    hasMasterPassword: Boolean,
    isIconLoadingDisabled: Boolean,
    baseIconUrl: String,
    vaultFilterType: VaultFilterType,
    restrictItemTypesPolicyOrgIds: List<String>,
): VaultState.ViewState {
    val allCipherViews =
        decryptCipherListResult
            .successes
            .plus(
                elements = decryptCipherListResult
                    .failures
                    .map { cipher ->
                        cipher.toFailureCipherListView()
                    },
            )
            .applyFilters(
                vaultFilterType = vaultFilterType,
                restrictItemTypesPolicyOrgIds = restrictItemTypesPolicyOrgIds,
                excludeDeleted = false,
            )

    val activeCipherViews = allCipherViews
        .filter { it.deletedDate == null }

    val activeDecryptedCipherViews = decryptCipherListResult
        .successes
        .applyFilters(
            vaultFilterType = vaultFilterType,
            restrictItemTypesPolicyOrgIds = restrictItemTypesPolicyOrgIds,
            excludeDeleted = true,
        )

    val activeUndecryptableCipherViews = decryptCipherListResult
        .failures
        .map { cipher ->
            cipher.toFailureCipherListView()
        }
        .applyFilters(
            vaultFilterType = vaultFilterType,
            restrictItemTypesPolicyOrgIds = restrictItemTypesPolicyOrgIds,
            excludeDeleted = true,
        )

    val filteredFolderViewList = folderViewList
        .toFilteredList(
            cipherList = activeCipherViews,
            vaultFilterType = vaultFilterType,
        )
        .getFilteredFolders()

    val filteredCollectionViewList = collectionViewList
        .toFilteredList(vaultFilterType)
        .getFilteredCollections()

    return if (allCipherViews.isEmpty()) {
        VaultState.ViewState.NoItems
    } else {
        val itemTypesCount: Int = CipherType.entries.size
        val noFolderItems = activeDecryptedCipherViews
            .filter { it.folderId.isNullOrBlank() }
            .mapNotNull {
                it.toVaultItemOrNull(
                    hasMasterPassword = hasMasterPassword,
                    isIconLoadingDisabled = isIconLoadingDisabled,
                    baseIconUrl = baseIconUrl,
                    isPremiumUser = isPremium,
                    hasDecryptionError = false,
                )
            }
            .plus(
                elements = activeUndecryptableCipherViews
                    .filter { it.folderId.isNullOrBlank() }
                    .mapNotNull {
                        it.toVaultItemOrNull(
                            hasMasterPassword = hasMasterPassword,
                            isIconLoadingDisabled = isIconLoadingDisabled,
                            baseIconUrl = baseIconUrl,
                            isPremiumUser = isPremium,
                            hasDecryptionError = true,
                        )
                    },
            )
        val shouldShowUnGroupedItems = filteredCollectionViewList.isEmpty() &&
            noFolderItems.size < NO_FOLDER_ITEM_THRESHOLD
        val totpItems = activeCipherViews.filter { it.login?.totp != null }
        val cardCount = activeCipherViews.count { it.type is CipherListViewType.Card }
        VaultState.ViewState.Content(
            itemTypesCount = itemTypesCount,
            totpItemsCount = if (isPremium) {
                totpItems.count()
            } else {
                totpItems.count { it.organizationUseTotp }
            },
            loginItemsCount = activeCipherViews.count { it.type is CipherListViewType.Login },
            cardItemsCount = cardCount,
            identityItemsCount = activeCipherViews
                .count { it.type is CipherListViewType.Identity },
            secureNoteItemsCount = activeCipherViews
                .count { it.type is CipherListViewType.SecureNote },
            sshKeyItemsCount = activeCipherViews
                .count { it.type is CipherListViewType.SshKey },
            favoriteItems = activeDecryptedCipherViews
                .filter { it.favorite }
                .mapNotNull {
                    it.toVaultItemOrNull(
                        hasMasterPassword = hasMasterPassword,
                        isIconLoadingDisabled = isIconLoadingDisabled,
                        baseIconUrl = baseIconUrl,
                        isPremiumUser = isPremium,
                        hasDecryptionError = false,
                    )
                }
                .plus(
                    elements = activeUndecryptableCipherViews
                        .filter { it.favorite }
                        .mapNotNull {
                            it.toVaultItemOrNull(
                                hasMasterPassword = hasMasterPassword,
                                isIconLoadingDisabled = isIconLoadingDisabled,
                                baseIconUrl = baseIconUrl,
                                isPremiumUser = isPremium,
                                hasDecryptionError = true,
                            )
                        },
                ),
            folderItems = filteredFolderViewList
                .map { folderView ->
                    VaultState.ViewState.FolderItem(
                        id = folderView.id,
                        name = folderView.name.asText(),
                        itemCount = activeCipherViews
                            .count {
                                !it.id.isNullOrBlank() &&
                                    folderView.id == it.folderId
                            },
                    )
                }
                .let { folderItems ->
                    if (shouldShowUnGroupedItems) {
                        folderItems
                    } else {
                        folderItems.plus(
                            VaultState.ViewState.FolderItem(
                                id = null,
                                name = BitwardenString.folder_none.asText(),
                                itemCount = noFolderItems.size,
                            ),
                        )
                    }
                },
            noFolderItems = noFolderItems
                .takeIf { shouldShowUnGroupedItems }
                .orEmpty(),
            collectionItems = filteredCollectionViewList
                .filter { it.id != null }
                .map { collectionView ->
                    VaultState.ViewState.CollectionItem(
                        id = requireNotNull(collectionView.id),
                        name = collectionView.name,
                        itemCount = activeCipherViews
                            .count {
                                !it.id.isNullOrBlank() &&
                                    collectionView.id in it.collectionIds
                            },
                    )
                },
            trashItemsCount = allCipherViews.count {
                it.deletedDate != null
            },
            showCardGroup = cardCount != 0 || restrictItemTypesPolicyOrgIds.isEmpty(),
        )
    }
}

/**
 * Method to build the icon data for login item icons.
 */
fun List<LoginUriView>?.toLoginIconData(
    isIconLoadingDisabled: Boolean,
    baseIconUrl: String,
    usePasskeyDefaultIcon: Boolean,
): IconData {
    val defaultIconRes = if (usePasskeyDefaultIcon) {
        BitwardenDrawable.ic_bw_passkey
    } else {
        BitwardenDrawable.ic_globe
    }

    var uri = this
        ?.map { it.uri }
        ?.firstOrNull { uri -> uri?.contains(".") == true }
        ?: return IconData.Local(defaultIconRes)

    if (uri.startsWith(ANDROID_URI)) {
        return IconData.Local(BitwardenDrawable.ic_android)
    }

    if (uri.startsWith(IOS_URI)) {
        return IconData.Local(BitwardenDrawable.ic_ios)
    }

    if (isIconLoadingDisabled) {
        return IconData.Local(defaultIconRes)
    }

    if (!uri.contains("://")) {
        uri = "http://$uri"
    }

    val iconUri = uri.toUri()
    val hostname = iconUri.host

    val url = "$baseIconUrl/$hostname/icon.png"

    return IconData.Network(
        uri = url,
        fallbackIconRes = defaultIconRes,
    )
}

/**
 * Transforms a [CipherListView] into a [VaultState.ViewState.VaultItem].
 */
@Suppress("MagicNumber", "LongMethod", "CyclomaticComplexMethod")
private fun CipherListView.toVaultItemOrNull(
    hasMasterPassword: Boolean,
    isIconLoadingDisabled: Boolean,
    baseIconUrl: String,
    isPremiumUser: Boolean,
    hasDecryptionError: Boolean,
): VaultState.ViewState.VaultItem? {
    val id = this.id ?: return null
    return when (type) {
        is CipherListViewType.Login -> VaultState.ViewState.VaultItem.Login(
            id = id,
            name = if (hasDecryptionError) {
                BitwardenString.error_cannot_decrypt.asText()
            } else {
                name.asText()
            },
            username = login?.username?.asText(),
            startIcon = login?.uris.toLoginIconData(
                isIconLoadingDisabled = isIconLoadingDisabled,
                baseIconUrl = baseIconUrl,
                usePasskeyDefaultIcon = false,
            ),
            overflowOptions = if (hasDecryptionError) {
                emptyList()
            } else {
                toOverflowActions(
                    hasMasterPassword = hasMasterPassword,
                    isPremiumUser = isPremiumUser,
                )
            },
            extraIconList = toLabelIcons(),
            shouldShowMasterPasswordReprompt = hasMasterPassword &&
                reprompt == CipherRepromptType.PASSWORD,
            hasDecryptionError = hasDecryptionError,
        )

        CipherListViewType.SecureNote -> VaultState.ViewState.VaultItem.SecureNote(
            id = id,
            name = name.asText(),
            overflowOptions = toOverflowActions(
                hasMasterPassword = hasMasterPassword,
                isPremiumUser = isPremiumUser,
            ),
            extraIconList = toLabelIcons(),
            shouldShowMasterPasswordReprompt = hasMasterPassword &&
                reprompt == CipherRepromptType.PASSWORD,
            hasDecryptionError = hasDecryptionError,
        )

        is CipherListViewType.Card -> VaultState.ViewState.VaultItem.Card(
            id = id,
            name = name.asText(),
            brand = card?.brand?.findVaultCardBrandWithNameOrNull(),
            lastFourDigits = subtitle.asText(),
            overflowOptions = toOverflowActions(
                hasMasterPassword = hasMasterPassword,
                isPremiumUser = isPremiumUser,
            ),
            extraIconList = toLabelIcons(),
            shouldShowMasterPasswordReprompt = hasMasterPassword &&
                reprompt == CipherRepromptType.PASSWORD,
            hasDecryptionError = hasDecryptionError,
        )

        CipherListViewType.Identity -> VaultState.ViewState.VaultItem.Identity(
            id = id,
            name = name.asText(),
            fullName = subtitle.asText(),
            overflowOptions = toOverflowActions(
                hasMasterPassword = hasMasterPassword,
                isPremiumUser = isPremiumUser,
            ),
            extraIconList = toLabelIcons(),
            shouldShowMasterPasswordReprompt = hasMasterPassword &&
                reprompt == CipherRepromptType.PASSWORD,
            hasDecryptionError = hasDecryptionError,
        )

        CipherListViewType.SshKey -> VaultState.ViewState.VaultItem.SshKey(
            id = id,
            name = name.asText(),
            extraIconList = toLabelIcons(),
            overflowOptions = toOverflowActions(
                hasMasterPassword = hasMasterPassword,
                isPremiumUser = isPremiumUser,
            ),
            shouldShowMasterPasswordReprompt = hasMasterPassword &&
                reprompt == CipherRepromptType.PASSWORD,
            hasDecryptionError = hasDecryptionError,
        )
    }
}

/**
 * Filters out all [CipherListView]s that are not part of the given [VaultFilterType].
 */
@JvmName("toFilteredCipherList")
fun List<CipherListView>.toFilteredList(
    vaultFilterType: VaultFilterType,
): List<CipherListView> =
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
    cipherList: List<CipherListView>,
    vaultFilterType: VaultFilterType,
): List<FolderView> =
    this
        .filter { folder ->
            when (vaultFilterType) {
                VaultFilterType.AllVaults,
                VaultFilterType.MyVault,
                    -> true

                // Only include folders containing an item associated with this organization.
                is VaultFilterType.OrganizationVault -> {
                    cipherList.any { it.folderId == folder.id }
                }
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

/**
 * Filters out [CipherType.CARD] [CipherListView]s that are in [restrictItemTypesPolicyOrgIds] list.
 * When [restrictItemTypesPolicyOrgIds] is not empty, individual vault items are also removed.
 */
fun List<CipherListView>.applyRestrictItemTypesPolicy(
    restrictItemTypesPolicyOrgIds: List<String>,
): List<CipherListView> =
    this
        .filterNot { cipherListView ->
            if (restrictItemTypesPolicyOrgIds.isEmpty()) {
                // No policy, so don't apply removal
                false
            } else if (cipherListView.type !is CipherListViewType.Card) {
                // Policy only for cards
                false
            } else {
                // If a policy is enable for a given organization then
                // also hide cards from individual vault
                cipherListView.organizationId.isNullOrEmpty() ||
                    restrictItemTypesPolicyOrgIds.contains(cipherListView.organizationId)
            }
        }

private fun List<CipherListView>.applyFilters(
    vaultFilterType: VaultFilterType,
    restrictItemTypesPolicyOrgIds: List<String>,
    excludeDeleted: Boolean,
): List<CipherListView> = this
    .let {
        if (excludeDeleted) {
            it.filter { cipher -> cipher.deletedDate == null }
        } else {
            it
        }
    }
    .applyRestrictItemTypesPolicy(restrictItemTypesPolicyOrgIds)
    .toFilteredList(vaultFilterType)
