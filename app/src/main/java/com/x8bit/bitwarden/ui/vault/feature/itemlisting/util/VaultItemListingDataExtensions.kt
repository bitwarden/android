package com.x8bit.bitwarden.ui.vault.feature.itemlisting.util

import androidx.annotation.DrawableRes
import com.bitwarden.core.CipherType
import com.bitwarden.core.CipherView
import com.bitwarden.core.CollectionView
import com.bitwarden.core.FolderView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingState
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toLoginIconData

/**
 * Determines a predicate to filter a list of [CipherView] based on the
 * [VaultItemListingState.ItemListingType].
 */
fun CipherView.determineListingPredicate(
    itemListingType: VaultItemListingState.ItemListingType,
): Boolean =
    when (itemListingType) {
        is VaultItemListingState.ItemListingType.Vault.Card -> {
            type == CipherType.CARD && deletedDate == null
        }

        is VaultItemListingState.ItemListingType.Vault.Collection -> {
            itemListingType.collectionId in this.collectionIds && deletedDate == null
        }

        is VaultItemListingState.ItemListingType.Vault.Folder -> {
            folderId == itemListingType.folderId && deletedDate == null
        }

        is VaultItemListingState.ItemListingType.Vault.Identity -> {
            type == CipherType.IDENTITY && deletedDate == null
        }

        is VaultItemListingState.ItemListingType.Vault.Login -> {
            type == CipherType.LOGIN && deletedDate == null
        }

        is VaultItemListingState.ItemListingType.Vault.SecureNote -> {
            type == CipherType.SECURE_NOTE && deletedDate == null
        }

        is VaultItemListingState.ItemListingType.Vault.Trash -> {
            deletedDate != null
        }
    }

/**
 * Transforms a list of [CipherView] into [VaultItemListingState.ViewState].
 */
fun List<CipherView>.toViewState(
    baseIconUrl: String,
    isIconLoadingDisabled: Boolean,
): VaultItemListingState.ViewState =
    if (isNotEmpty()) {
        VaultItemListingState.ViewState.Content(
            displayItemList = toDisplayItemList(
                baseIconUrl = baseIconUrl,
                isIconLoadingDisabled = isIconLoadingDisabled,
            ),
        )
    } else {
        VaultItemListingState.ViewState.NoItems
    }

/** * Updates a [VaultItemListingState.ItemListingType] with the given data if necessary. */
fun VaultItemListingState.ItemListingType.updateWithAdditionalDataIfNecessary(
    folderList: List<FolderView>,
    collectionList: List<CollectionView>,
): VaultItemListingState.ItemListingType =
    when (this) {
        is VaultItemListingState.ItemListingType.Vault.Card -> this
        is VaultItemListingState.ItemListingType.Vault.Collection -> copy(
            collectionName = collectionList
                .find { it.id == collectionId }
                ?.name
                .orEmpty(),
        )

        is VaultItemListingState.ItemListingType.Vault.Folder -> copy(
            folderName = folderList
                .find { it.id == folderId }
                ?.name
                .orEmpty(),
        )

        is VaultItemListingState.ItemListingType.Vault.Identity -> this
        is VaultItemListingState.ItemListingType.Vault.Login -> this
        is VaultItemListingState.ItemListingType.Vault.SecureNote -> this
        is VaultItemListingState.ItemListingType.Vault.Trash -> this
    }

private fun List<CipherView>.toDisplayItemList(
    baseIconUrl: String,
    isIconLoadingDisabled: Boolean,
): List<VaultItemListingState.DisplayItem> =
    this.map {
        it.toDisplayItem(
            baseIconUrl = baseIconUrl,
            isIconLoadingDisabled = isIconLoadingDisabled,
        )
    }

private fun CipherView.toDisplayItem(
    baseIconUrl: String,
    isIconLoadingDisabled: Boolean,
): VaultItemListingState.DisplayItem =
    VaultItemListingState.DisplayItem(
        id = id.orEmpty(),
        title = name,
        subtitle = subtitle,
        iconData = this.toIconData(
            baseIconUrl = baseIconUrl,
            isIconLoadingDisabled = isIconLoadingDisabled,
        ),
    )

private fun CipherView.toIconData(
    baseIconUrl: String,
    isIconLoadingDisabled: Boolean,
): IconData {
    return when (this.type) {
        CipherType.LOGIN -> {
            login?.uris.toLoginIconData(
                baseIconUrl = baseIconUrl,
                isIconLoadingDisabled = isIconLoadingDisabled,
            )
        }

        else -> {
            IconData.Local(iconRes = this.type.iconRes)
        }
    }
}

@Suppress("MagicNumber")
private val CipherView.subtitle: String?
    get() = when (type) {
        CipherType.LOGIN -> login?.username.orEmpty()
        CipherType.SECURE_NOTE -> null
        CipherType.CARD -> {
            card
                ?.number
                ?.takeLast(4)
                .orEmpty()
        }

        CipherType.IDENTITY -> {
            identity
                ?.firstName
                .orEmpty()
                .plus(identity?.lastName.orEmpty())
        }
    }

@get:DrawableRes
private val CipherType.iconRes: Int
    get() = when (this) {
        CipherType.LOGIN -> R.drawable.ic_login_item
        CipherType.SECURE_NOTE -> R.drawable.ic_secure_note_item
        CipherType.CARD -> R.drawable.ic_card_item
        CipherType.IDENTITY -> R.drawable.ic_identity_item
    }
