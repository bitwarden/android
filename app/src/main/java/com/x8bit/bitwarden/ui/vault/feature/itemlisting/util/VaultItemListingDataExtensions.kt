package com.x8bit.bitwarden.ui.vault.feature.itemlisting.util

import androidx.annotation.DrawableRes
import com.bitwarden.core.CipherRepromptType
import com.bitwarden.core.CipherType
import com.bitwarden.core.CipherView
import com.bitwarden.core.CollectionView
import com.bitwarden.core.FolderView
import com.bitwarden.core.SendType
import com.bitwarden.core.SendView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.platform.util.subtitle
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.toHostOrPathOrNull
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.util.toFormattedPattern
import com.x8bit.bitwarden.ui.tools.feature.send.util.toLabelIcons
import com.x8bit.bitwarden.ui.tools.feature.send.util.toOverflowActions
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingState
import com.x8bit.bitwarden.ui.vault.feature.util.toLabelIcons
import com.x8bit.bitwarden.ui.vault.feature.util.toOverflowActions
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toLoginIconData
import java.time.Clock

private const val DELETION_DATE_PATTERN: String = "MMM d, uuuu, hh:mm a"

/**
 * Determines a predicate to filter a list of [CipherView] based on the
 * [VaultItemListingState.ItemListingType].
 */
fun CipherView.determineListingPredicate(
    itemListingType: VaultItemListingState.ItemListingType.Vault,
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
 * Determines a predicate to filter a list of [CipherView] based on the
 * [VaultItemListingState.ItemListingType].
 */
fun SendView.determineListingPredicate(
    itemListingType: VaultItemListingState.ItemListingType.Send,
): Boolean =
    when (itemListingType) {
        is VaultItemListingState.ItemListingType.Send.SendFile -> {
            type == SendType.FILE
        }

        is VaultItemListingState.ItemListingType.Send.SendText -> {
            type == SendType.TEXT
        }
    }

/**
 * Transforms a list of [CipherView] into [VaultItemListingState.ViewState].
 */
fun List<CipherView>.toViewState(
    itemListingType: VaultItemListingState.ItemListingType.Vault,
    baseIconUrl: String,
    isIconLoadingDisabled: Boolean,
    autofillSelectionData: AutofillSelectionData?,
): VaultItemListingState.ViewState =
    if (isNotEmpty()) {
        VaultItemListingState.ViewState.Content(
            displayItemList = toDisplayItemList(
                baseIconUrl = baseIconUrl,
                isIconLoadingDisabled = isIconLoadingDisabled,
                isAutofill = autofillSelectionData != null,
            ),
        )
    } else {
        // Use the autofill empty message if necessary, otherwise use normal type-specific message
        val message = autofillSelectionData
            ?.uri
            ?.toHostOrPathOrNull()
            ?.let { R.string.no_items_for_uri.asText(it) }
            ?: run {
                when (itemListingType) {
                    is VaultItemListingState.ItemListingType.Vault.Folder -> {
                        R.string.no_items_folder
                    }

                    VaultItemListingState.ItemListingType.Vault.Trash -> {
                        R.string.no_items_trash
                    }

                    else -> R.string.no_items
                }
                    .asText()
            }
        val shouldShowAddButton = when (itemListingType) {
            is VaultItemListingState.ItemListingType.Vault.Folder,
            VaultItemListingState.ItemListingType.Vault.Trash,
            -> false

            else -> true
        }
        VaultItemListingState.ViewState.NoItems(
            message = message,
            shouldShowAddButton = shouldShowAddButton,
        )
    }

/**
 * Transforms a list of [CipherView] into [VaultItemListingState.ViewState].
 */
fun List<SendView>.toViewState(
    baseWebSendUrl: String,
    clock: Clock,
): VaultItemListingState.ViewState =
    if (isNotEmpty()) {
        VaultItemListingState.ViewState.Content(
            displayItemList = toDisplayItemList(
                baseWebSendUrl = baseWebSendUrl,
                clock = clock,
            ),
        )
    } else {
        VaultItemListingState.ViewState.NoItems(
            message = R.string.no_items.asText(),
            shouldShowAddButton = true,
        )
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
        is VaultItemListingState.ItemListingType.Send.SendFile -> this
        is VaultItemListingState.ItemListingType.Send.SendText -> this
    }

private fun List<CipherView>.toDisplayItemList(
    baseIconUrl: String,
    isIconLoadingDisabled: Boolean,
    isAutofill: Boolean,
): List<VaultItemListingState.DisplayItem> =
    this.map {
        it.toDisplayItem(
            baseIconUrl = baseIconUrl,
            isIconLoadingDisabled = isIconLoadingDisabled,
            isAutofill = isAutofill,
        )
    }

private fun List<SendView>.toDisplayItemList(
    baseWebSendUrl: String,
    clock: Clock,
): List<VaultItemListingState.DisplayItem> =
    this.map {
        it.toDisplayItem(
            baseWebSendUrl = baseWebSendUrl,
            clock = clock,
        )
    }

private fun CipherView.toDisplayItem(
    baseIconUrl: String,
    isIconLoadingDisabled: Boolean,
    isAutofill: Boolean,
): VaultItemListingState.DisplayItem =
    VaultItemListingState.DisplayItem(
        id = id.orEmpty(),
        title = name,
        subtitle = subtitle,
        iconData = this.toIconData(
            baseIconUrl = baseIconUrl,
            isIconLoadingDisabled = isIconLoadingDisabled,
        ),
        extraIconList = toLabelIcons(),
        overflowOptions = toOverflowActions(),
        shouldShowMasterPasswordReprompt = isAutofill && reprompt == CipherRepromptType.PASSWORD,
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

private fun SendView.toDisplayItem(
    baseWebSendUrl: String,
    clock: Clock,
): VaultItemListingState.DisplayItem =
    VaultItemListingState.DisplayItem(
        id = id.orEmpty(),
        title = name,
        subtitle = deletionDate.toFormattedPattern(DELETION_DATE_PATTERN, clock.zone),
        iconData = IconData.Local(
            iconRes = when (type) {
                SendType.TEXT -> R.drawable.ic_send_text
                SendType.FILE -> R.drawable.ic_send_file
            },
        ),
        extraIconList = toLabelIcons(clock = clock),
        overflowOptions = toOverflowActions(baseWebSendUrl = baseWebSendUrl),
        shouldShowMasterPasswordReprompt = false,
    )

@get:DrawableRes
private val CipherType.iconRes: Int
    get() = when (this) {
        CipherType.LOGIN -> R.drawable.ic_login_item
        CipherType.SECURE_NOTE -> R.drawable.ic_secure_note_item
        CipherType.CARD -> R.drawable.ic_card_item
        CipherType.IDENTITY -> R.drawable.ic_identity_item
    }
