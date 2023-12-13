package com.x8bit.bitwarden.ui.vault.feature.itemlisting.util

import androidx.annotation.DrawableRes
import com.bitwarden.core.CipherType
import com.bitwarden.core.CipherView
import com.bitwarden.core.FolderView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingState

/**
 * Determines a predicate to filter a list of [CipherView] based on the
 * [VaultItemListingState.ItemListingType].
 */
fun CipherView.determineListingPredicate(
    itemListingType: VaultItemListingState.ItemListingType,
): Boolean =
    when (itemListingType) {
        is VaultItemListingState.ItemListingType.Card -> {
            type == CipherType.CARD && deletedDate == null
        }

        is VaultItemListingState.ItemListingType.Folder -> {
            folderId == itemListingType.folderId && deletedDate == null
        }

        is VaultItemListingState.ItemListingType.Identity -> {
            type == CipherType.IDENTITY && deletedDate == null
        }

        is VaultItemListingState.ItemListingType.Login -> {
            type == CipherType.LOGIN && deletedDate == null
        }

        is VaultItemListingState.ItemListingType.SecureNote -> {
            type == CipherType.SECURE_NOTE && deletedDate == null
        }

        is VaultItemListingState.ItemListingType.Trash -> {
            deletedDate != null
        }
    }

/**
 * Transforms a list of [CipherView] into [VaultItemListingState.ViewState].
 */
fun List<CipherView>.toViewState(): VaultItemListingState.ViewState =
    if (isNotEmpty()) {
        VaultItemListingState.ViewState.Content(displayItemList = toDisplayItemList())
    } else {
        VaultItemListingState.ViewState.NoItems
    }

/** * Updates a [VaultItemListingState.ItemListingType] with the given data if necessary. */
fun VaultItemListingState.ItemListingType.updateWithAdditionalDataIfNecessary(
    folderList: List<FolderView>,
): VaultItemListingState.ItemListingType =
    when (this) {
        is VaultItemListingState.ItemListingType.Card -> this
        is VaultItemListingState.ItemListingType.Folder -> copy(
            folderName = folderList.first { it.id == folderId }.name,
        )

        is VaultItemListingState.ItemListingType.Identity -> this
        is VaultItemListingState.ItemListingType.Login -> this
        is VaultItemListingState.ItemListingType.SecureNote -> this
        is VaultItemListingState.ItemListingType.Trash -> this
    }

private fun List<CipherView>.toDisplayItemList(): List<VaultItemListingState.DisplayItem> =
    this.map { it.toDisplayItem() }

private fun CipherView.toDisplayItem(): VaultItemListingState.DisplayItem =
    VaultItemListingState.DisplayItem(
        id = id.orEmpty(),
        title = name,
        subtitle = subtitle,
        iconRes = type.iconRes,
        uri = uri,
    )

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

private val CipherView.uri: String?
    get() = when (type) {
        CipherType.LOGIN -> {
            login
                ?.uris
                ?.firstOrNull()
                ?.uri
                .orEmpty()
        }

        CipherType.SECURE_NOTE -> null
        CipherType.CARD -> null
        CipherType.IDENTITY -> null
    }
