@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.platform.feature.search.util

import androidx.annotation.DrawableRes
import com.bitwarden.core.CipherType
import com.bitwarden.core.CipherView
import com.bitwarden.core.CollectionView
import com.bitwarden.core.FolderView
import com.bitwarden.core.SendType
import com.bitwarden.core.SendView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.util.subtitle
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.removeDiacritics
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.feature.search.SearchState
import com.x8bit.bitwarden.ui.platform.feature.search.SearchTypeData
import com.x8bit.bitwarden.ui.platform.util.toFormattedPattern
import com.x8bit.bitwarden.ui.tools.feature.send.util.toLabelIcons
import com.x8bit.bitwarden.ui.tools.feature.send.util.toOverflowActions
import com.x8bit.bitwarden.ui.vault.feature.util.toOverflowActions
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toLoginIconData
import java.time.Clock

private const val DELETION_DATE_PATTERN: String = "MMM d, uuuu, hh:mm a"

/**
 * Updates a [SearchTypeData] with the given data if necessary.
 */
fun SearchTypeData.updateWithAdditionalDataIfNecessary(
    folderList: List<FolderView>,
    collectionList: List<CollectionView>,
): SearchTypeData =
    when (this) {
        is SearchTypeData.Vault.Collection -> copy(
            collectionName = collectionList
                .find { it.id == collectionId }
                ?.name
                .orEmpty(),
        )

        is SearchTypeData.Vault.Folder -> copy(
            folderName = folderList
                .find { it.id == folderId }
                ?.name
                .orEmpty(),
        )

        SearchTypeData.Sends.All -> this
        SearchTypeData.Sends.Files -> this
        SearchTypeData.Sends.Texts -> this
        SearchTypeData.Vault.All -> this
        SearchTypeData.Vault.Cards -> this
        SearchTypeData.Vault.Identities -> this
        SearchTypeData.Vault.Logins -> this
        SearchTypeData.Vault.NoFolder -> this
        SearchTypeData.Vault.SecureNotes -> this
        SearchTypeData.Vault.Trash -> this
    }

/**
 * Filters out any [CipherView]s that do not adhere to the [searchTypeData] and [searchTerm] and
 * sorts the remaining items.
 */
fun List<CipherView>.filterAndOrganize(
    searchTypeData: SearchTypeData.Vault,
    searchTerm: String,
): List<CipherView> =
    if (searchTerm.isBlank()) {
        emptyList()
    } else {
        this
            .filter { it.filterBySearchType(searchTypeData) }
            .groupBy { it.matchedSearch(searchTerm) }
            .flatMap { (priority, sends) ->
                when (priority) {
                    SortPriority.HIGH -> sends.sortedBy { it.name }
                    SortPriority.LOW -> sends.sortedBy { it.name }
                    null -> emptyList()
                }
            }
    }

/**
 * Determines a predicate to filter a list of [SendView] based on the [SearchTypeData.Sends].
 */
private fun CipherView.filterBySearchType(
    searchTypeData: SearchTypeData.Vault,
): Boolean =
    when (searchTypeData) {
        SearchTypeData.Vault.All -> true
        is SearchTypeData.Vault.Cards -> type == CipherType.CARD
        is SearchTypeData.Vault.Collection -> searchTypeData.collectionId in this.collectionIds
        is SearchTypeData.Vault.Folder -> folderId == searchTypeData.folderId
        SearchTypeData.Vault.NoFolder -> folderId == null
        is SearchTypeData.Vault.Identities -> type == CipherType.IDENTITY
        is SearchTypeData.Vault.Logins -> type == CipherType.LOGIN
        is SearchTypeData.Vault.SecureNotes -> type == CipherType.SECURE_NOTE
        is SearchTypeData.Vault.Trash -> deletedDate != null
    }

/**
 * Determines the priority of a given [CipherView] based on the [searchTerm]. Null indicates that
 * this item should be removed from the list.
 */
@Suppress("MagicNumber")
private fun CipherView.matchedSearch(searchTerm: String): SortPriority? {
    val term = searchTerm.removeDiacritics()
    val cipherName = name.removeDiacritics()
    val cipherId = id?.takeIf { term.length > 8 }.orEmpty().removeDiacritics()
    val cipherSubtitle = subtitle.orEmpty().removeDiacritics()
    val cipherUris = login?.uris.orEmpty().map { it.uri.orEmpty().removeDiacritics() }
    return when {
        cipherName.contains(other = term, ignoreCase = true) -> SortPriority.HIGH
        cipherId.contains(other = term, ignoreCase = true) -> SortPriority.LOW
        cipherSubtitle.contains(other = term, ignoreCase = true) -> SortPriority.LOW
        cipherUris.any { it.contains(other = term, ignoreCase = true) } -> SortPriority.LOW
        else -> null
    }
}

/**
 * Transforms a list of [CipherView] into [SearchState.ViewState].
 */
fun List<CipherView>.toViewState(
    searchTerm: String,
    baseIconUrl: String,
    isIconLoadingDisabled: Boolean,
): SearchState.ViewState =
    when {
        searchTerm.isEmpty() -> SearchState.ViewState.Empty(message = null)
        isNotEmpty() -> {
            SearchState.ViewState.Content(
                displayItems = toDisplayItemList(
                    baseIconUrl = baseIconUrl,
                    isIconLoadingDisabled = isIconLoadingDisabled,
                ),
            )
        }

        else -> {
            SearchState.ViewState.Empty(
                message = R.string.there_are_no_items_that_match_the_search.asText(),
            )
        }
    }

private fun List<CipherView>.toDisplayItemList(
    baseIconUrl: String,
    isIconLoadingDisabled: Boolean,
): List<SearchState.DisplayItem> =
    this.map {
        it.toDisplayItem(
            baseIconUrl = baseIconUrl,
            isIconLoadingDisabled = isIconLoadingDisabled,
        )
    }

private fun CipherView.toDisplayItem(
    baseIconUrl: String,
    isIconLoadingDisabled: Boolean,
): SearchState.DisplayItem =
    SearchState.DisplayItem(
        id = id.orEmpty(),
        title = name,
        subtitle = subtitle,
        iconData = this.toIconData(
            baseIconUrl = baseIconUrl,
            isIconLoadingDisabled = isIconLoadingDisabled,
        ),
        extraIconList = emptyList(),
        overflowOptions = toOverflowActions(),
    )

private fun CipherView.toIconData(
    baseIconUrl: String,
    isIconLoadingDisabled: Boolean,
): IconData =
    when (this.type) {
        CipherType.LOGIN -> {
            login?.uris.toLoginIconData(
                baseIconUrl = baseIconUrl,
                isIconLoadingDisabled = isIconLoadingDisabled,
            )
        }

        else -> IconData.Local(iconRes = this.type.iconRes)
    }

@get:DrawableRes
private val CipherType.iconRes: Int
    get() = when (this) {
        CipherType.LOGIN -> R.drawable.ic_login_item
        CipherType.SECURE_NOTE -> R.drawable.ic_secure_note_item
        CipherType.CARD -> R.drawable.ic_card_item
        CipherType.IDENTITY -> R.drawable.ic_identity_item
    }

/**
 * Filters out any [SendView]s that do not adhere to the [searchTypeData] and [searchTerm] and
 * sorts the remaining items.
 */
fun List<SendView>.filterAndOrganize(
    searchTypeData: SearchTypeData.Sends,
    searchTerm: String,
): List<SendView> =
    if (searchTerm.isBlank()) {
        emptyList()
    } else {
        this
            .filter { it.filterBySearchType(searchTypeData) }
            .groupBy { it.matchedSearch(searchTerm) }
            .flatMap { (priority, sends) ->
                when (priority) {
                    SortPriority.HIGH -> sends.sortedBy { it.name }
                    SortPriority.LOW -> sends.sortedBy { it.name }
                    null -> emptyList()
                }
            }
    }

/**
 * Determines a predicate to filter a list of [SendView] based on the [SearchTypeData.Sends].
 */
private fun SendView.filterBySearchType(
    searchTypeData: SearchTypeData.Sends,
): Boolean =
    when (searchTypeData) {
        SearchTypeData.Sends.All -> true
        is SearchTypeData.Sends.Files -> type == SendType.FILE
        is SearchTypeData.Sends.Texts -> type == SendType.TEXT
    }

/**
 * Determines the priority of a given [SendView] based on the [searchTerm]. Null indicates that
 * this item should be removed from the list.
 */
private fun SendView.matchedSearch(searchTerm: String): SortPriority? {
    val term = searchTerm.removeDiacritics()
    val sendName = name.removeDiacritics()
    val sendText = text?.text.orEmpty().removeDiacritics()
    val sendFileName = file?.fileName.orEmpty().removeDiacritics()
    return when {
        sendName.contains(other = term, ignoreCase = true) -> SortPriority.HIGH
        sendText.contains(other = term, ignoreCase = true) -> SortPriority.LOW
        sendFileName.contains(other = term, ignoreCase = true) -> SortPriority.LOW
        else -> null
    }
}

/**
 * Transforms a list of [SendView] into [SearchState.ViewState].
 */
fun List<SendView>.toViewState(
    searchTerm: String,
    baseWebSendUrl: String,
    clock: Clock,
): SearchState.ViewState =
    when {
        searchTerm.isEmpty() -> SearchState.ViewState.Empty(message = null)
        isNotEmpty() -> {
            SearchState.ViewState.Content(
                displayItems = toDisplayItemList(
                    baseWebSendUrl = baseWebSendUrl,
                    clock = clock,
                ),
            )
        }

        else -> {
            SearchState.ViewState.Empty(
                message = R.string.there_are_no_items_that_match_the_search.asText(),
            )
        }
    }

private fun List<SendView>.toDisplayItemList(
    baseWebSendUrl: String,
    clock: Clock,
): List<SearchState.DisplayItem> =
    this.map {
        it.toDisplayItem(
            baseWebSendUrl = baseWebSendUrl,
            clock = clock,
        )
    }

private fun SendView.toDisplayItem(
    baseWebSendUrl: String,
    clock: Clock,
): SearchState.DisplayItem =
    SearchState.DisplayItem(
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
    )

private enum class SortPriority {
    HIGH,
    LOW,
}
