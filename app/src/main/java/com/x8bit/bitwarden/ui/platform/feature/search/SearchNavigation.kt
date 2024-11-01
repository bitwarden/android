package com.x8bit.bitwarden.ui.platform.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType

private const val SEARCH_TYPE: String = "search_type"
private const val SEARCH_TYPE_SEND_ALL: String = "search_type_sends_all"
private const val SEARCH_TYPE_SEND_TEXT: String = "search_type_sends_text"
private const val SEARCH_TYPE_SEND_FILE: String = "search_type_sends_file"
private const val SEARCH_TYPE_VAULT_ALL: String = "search_type_vault_all"
private const val SEARCH_TYPE_VAULT_LOGINS: String = "search_type_vault_logins"
private const val SEARCH_TYPE_VAULT_CARDS: String = "search_type_vault_cards"
private const val SEARCH_TYPE_VAULT_IDENTITIES: String = "search_type_vault_identities"
private const val SEARCH_TYPE_VAULT_SECURE_NOTES: String = "search_type_vault_secure_notes"
private const val SEARCH_TYPE_VAULT_COLLECTION: String = "search_type_vault_collection"
private const val SEARCH_TYPE_VAULT_NO_FOLDER: String = "search_type_vault_no_folder"
private const val SEARCH_TYPE_VAULT_FOLDER: String = "search_type_vault_folder"
private const val SEARCH_TYPE_VAULT_TRASH: String = "search_type_vault_trash"
private const val SEARCH_TYPE_VAULT_VERIFICATION_CODES: String =
    "search_type_vault_verification_codes"
private const val SEARCH_TYPE_ID: String = "search_type_id"
private const val SEARCH_TYPE_VAULT_SSH_KEYS: String = "search_type_vault_ssh_keys"

private const val SEARCH_ROUTE_PREFIX: String = "search"
private const val SEARCH_ROUTE: String = "$SEARCH_ROUTE_PREFIX/{$SEARCH_TYPE}/{$SEARCH_TYPE_ID}"

/**
 * Class to retrieve search arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class SearchArgs(
    val type: SearchType,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        type = determineSearchType(
            searchTypeString = requireNotNull(savedStateHandle.get<String>(SEARCH_TYPE)),
            id = savedStateHandle.get<String>(SEARCH_TYPE_ID),
        ),
    )
}

/**
 * Add search destinations to the nav graph.
 */
fun NavGraphBuilder.searchDestination(
    onNavigateBack: () -> Unit,
    onNavigateToEditSend: (sendId: String) -> Unit,
    onNavigateToEditCipher: (cipherId: String) -> Unit,
    onNavigateToViewCipher: (cipherId: String) -> Unit,
) {
    composableWithSlideTransitions(
        route = SEARCH_ROUTE,
        arguments = listOf(
            navArgument(SEARCH_TYPE) { type = NavType.StringType },
            navArgument(SEARCH_TYPE_ID) {
                type = NavType.StringType
                nullable = true
            },
        ),
    ) {
        SearchScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToEditSend = onNavigateToEditSend,
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
    navigate(
        route = "$SEARCH_ROUTE_PREFIX/${searchType.toTypeString()}/${searchType.toIdOrNull()}",
        navOptions = navOptions,
    )
}

private fun determineSearchType(
    searchTypeString: String,
    id: String?,
): SearchType =
    when (searchTypeString) {
        SEARCH_TYPE_SEND_ALL -> SearchType.Sends.All
        SEARCH_TYPE_SEND_TEXT -> SearchType.Sends.Texts
        SEARCH_TYPE_SEND_FILE -> SearchType.Sends.Files
        SEARCH_TYPE_VAULT_ALL -> SearchType.Vault.All
        SEARCH_TYPE_VAULT_LOGINS -> SearchType.Vault.Logins
        SEARCH_TYPE_VAULT_CARDS -> SearchType.Vault.Cards
        SEARCH_TYPE_VAULT_IDENTITIES -> SearchType.Vault.Identities
        SEARCH_TYPE_VAULT_SECURE_NOTES -> SearchType.Vault.SecureNotes
        SEARCH_TYPE_VAULT_COLLECTION -> SearchType.Vault.Collection(requireNotNull(id))
        SEARCH_TYPE_VAULT_NO_FOLDER -> SearchType.Vault.NoFolder
        SEARCH_TYPE_VAULT_FOLDER -> SearchType.Vault.Folder(requireNotNull(id))
        SEARCH_TYPE_VAULT_TRASH -> SearchType.Vault.Trash
        SEARCH_TYPE_VAULT_VERIFICATION_CODES -> SearchType.Vault.VerificationCodes
        SEARCH_TYPE_VAULT_SSH_KEYS -> SearchType.Vault.SshKeys
        else -> throw IllegalArgumentException("Invalid Search Type")
    }

private fun SearchType.toTypeString(): String =
    when (this) {
        SearchType.Sends.All -> SEARCH_TYPE_SEND_ALL
        SearchType.Sends.Files -> SEARCH_TYPE_SEND_FILE
        SearchType.Sends.Texts -> SEARCH_TYPE_SEND_TEXT
        SearchType.Vault.All -> SEARCH_TYPE_VAULT_ALL
        SearchType.Vault.Cards -> SEARCH_TYPE_VAULT_CARDS
        is SearchType.Vault.Collection -> SEARCH_TYPE_VAULT_COLLECTION
        is SearchType.Vault.Folder -> SEARCH_TYPE_VAULT_FOLDER
        SearchType.Vault.Identities -> SEARCH_TYPE_VAULT_IDENTITIES
        SearchType.Vault.Logins -> SEARCH_TYPE_VAULT_LOGINS
        SearchType.Vault.NoFolder -> SEARCH_TYPE_VAULT_NO_FOLDER
        SearchType.Vault.SecureNotes -> SEARCH_TYPE_VAULT_SECURE_NOTES
        SearchType.Vault.Trash -> SEARCH_TYPE_VAULT_TRASH
        SearchType.Vault.VerificationCodes -> SEARCH_TYPE_VAULT_VERIFICATION_CODES
        SearchType.Vault.SshKeys -> SEARCH_TYPE_VAULT_SSH_KEYS
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
