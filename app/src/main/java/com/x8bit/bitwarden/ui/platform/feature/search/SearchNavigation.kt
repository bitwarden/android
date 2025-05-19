package com.x8bit.bitwarden.ui.platform.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.AddEditSendRoute
import com.x8bit.bitwarden.ui.tools.feature.send.viewsend.ViewSendRoute
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditArgs
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemArgs
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the search screen.
 */
@Serializable
data class SearchRoute(
    val searchableItemType: SearchableItemType,
    val id: String?,
)

/**
 * Represents the various types of searchable items.
 */
@Serializable
enum class SearchableItemType {
    SENDS_ALL,
    SENDS_TEXTS,
    SENDS_FILES,
    VAULT_ALL,
    VAULT_LOGINS,
    VAULT_CARDS,
    VAULT_IDENTITIES,
    VAULT_SECURE_NOTES,
    VAULT_SSH_KEYS,
    VAULT_COLLECTIONS,
    VAULT_NO_FOLDER,
    VAULT_FOLDER,
    VAULT_TRASH,
    VAULT_VERIFICATION_CODES,
}

/**
 * Class to retrieve search arguments from the [SavedStateHandle].
 */
data class SearchArgs(
    val type: SearchType,
)

/**
 * Constructs a [SearchArgs] from the [SavedStateHandle] and internal route data.
 */
@Suppress("CyclomaticComplexMethod")
fun SavedStateHandle.toSearchArgs(): SearchArgs {
    val route = this.toRoute<SearchRoute>()
    return SearchArgs(
        type = when (route.searchableItemType) {
            SearchableItemType.SENDS_ALL -> SearchType.Sends.All
            SearchableItemType.SENDS_TEXTS -> SearchType.Sends.Texts
            SearchableItemType.SENDS_FILES -> SearchType.Sends.Files
            SearchableItemType.VAULT_ALL -> SearchType.Vault.All
            SearchableItemType.VAULT_LOGINS -> SearchType.Vault.Logins
            SearchableItemType.VAULT_CARDS -> SearchType.Vault.Cards
            SearchableItemType.VAULT_IDENTITIES -> SearchType.Vault.Identities
            SearchableItemType.VAULT_SECURE_NOTES -> SearchType.Vault.SecureNotes
            SearchableItemType.VAULT_SSH_KEYS -> SearchType.Vault.SshKeys
            SearchableItemType.VAULT_NO_FOLDER -> SearchType.Vault.NoFolder
            SearchableItemType.VAULT_TRASH -> SearchType.Vault.Trash
            SearchableItemType.VAULT_VERIFICATION_CODES -> SearchType.Vault.VerificationCodes
            SearchableItemType.VAULT_FOLDER -> SearchType.Vault.Folder(
                folderId = requireNotNull(route.id),
            )

            SearchableItemType.VAULT_COLLECTIONS -> SearchType.Vault.Collection(
                collectionId = requireNotNull(route.id),
            )
        },
    )
}

/**
 * Add search destinations to the nav graph.
 */
fun NavGraphBuilder.searchDestination(
    onNavigateBack: () -> Unit,
    onNavigateToAddEditSend: (route: AddEditSendRoute) -> Unit,
    onNavigateToViewSend: (route: ViewSendRoute) -> Unit,
    onNavigateToEditCipher: (args: VaultAddEditArgs) -> Unit,
    onNavigateToViewCipher: (args: VaultItemArgs) -> Unit,
) {
    composableWithSlideTransitions<SearchRoute> {
        SearchScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToAddEditSend = onNavigateToAddEditSend,
            onNavigateToViewSend = onNavigateToViewSend,
            onNavigateToEditCipher = onNavigateToEditCipher,
            onNavigateToViewCipher = onNavigateToViewCipher,
        )
    }
}

/**
 * Navigate to the search screen.
 */
fun NavController.navigateToSearch(
    searchType: SearchType,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = SearchRoute(
            searchableItemType = searchType.toSearchableItemType(),
            id = searchType.toIdOrNull(),
        ),
        navOptions = navOptions,
    )
}

private fun SearchType.toSearchableItemType(): SearchableItemType =
    when (this) {
        SearchType.Sends.All -> SearchableItemType.SENDS_ALL
        SearchType.Sends.Files -> SearchableItemType.SENDS_FILES
        SearchType.Sends.Texts -> SearchableItemType.SENDS_TEXTS
        SearchType.Vault.All -> SearchableItemType.VAULT_ALL
        SearchType.Vault.Cards -> SearchableItemType.VAULT_CARDS
        is SearchType.Vault.Collection -> SearchableItemType.VAULT_COLLECTIONS
        is SearchType.Vault.Folder -> SearchableItemType.VAULT_FOLDER
        SearchType.Vault.Identities -> SearchableItemType.VAULT_IDENTITIES
        SearchType.Vault.Logins -> SearchableItemType.VAULT_LOGINS
        SearchType.Vault.NoFolder -> SearchableItemType.VAULT_NO_FOLDER
        SearchType.Vault.SecureNotes -> SearchableItemType.VAULT_SECURE_NOTES
        SearchType.Vault.Trash -> SearchableItemType.VAULT_TRASH
        SearchType.Vault.VerificationCodes -> SearchableItemType.VAULT_VERIFICATION_CODES
        SearchType.Vault.SshKeys -> SearchableItemType.VAULT_SSH_KEYS
    }

private fun SearchType.toIdOrNull(): String? =
    when (this) {
        SearchType.Sends.All -> null
        SearchType.Sends.Files -> null
        SearchType.Sends.Texts -> null
        SearchType.Vault.All -> null
        SearchType.Vault.Cards -> null
        is SearchType.Vault.Collection -> collectionId
        is SearchType.Vault.Folder -> folderId
        SearchType.Vault.Identities -> null
        SearchType.Vault.Logins -> null
        SearchType.Vault.NoFolder -> null
        SearchType.Vault.SecureNotes -> null
        SearchType.Vault.Trash -> null
        SearchType.Vault.VerificationCodes -> null
        SearchType.Vault.SshKeys -> null
    }
