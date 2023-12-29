package com.x8bit.bitwarden.ui.vault.feature.itemlisting

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.x8bit.bitwarden.ui.platform.base.util.composableWithPushTransitions
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType

private const val CARD: String = "card"
private const val COLLECTION: String = "collection"
private const val FOLDER: String = "folder"
private const val IDENTITY: String = "identity"
private const val LOGIN: String = "login"
private const val SECURE_NOTE: String = "secure_note"
private const val TRASH: String = "trash"
private const val VAULT_ITEM_LISTING_PREFIX: String = "vault_item_listing"
private const val VAULT_ITEM_LISTING_TYPE: String = "vault_item_listing_type"
private const val ID: String = "id"
private const val VAULT_ITEM_LISTING_ROUTE: String =
    "$VAULT_ITEM_LISTING_PREFIX/{$VAULT_ITEM_LISTING_TYPE}" +
        "?$ID={$ID}"

/**
 * Class to retrieve vault item listing arguments from the [SavedStateHandle].
 */
class VaultItemListingArgs(
    val vaultItemListingType: VaultItemListingType,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        vaultItemListingType = determineVaultItemListingType(
            vaultItemListingTypeString = checkNotNull(
                savedStateHandle[VAULT_ITEM_LISTING_TYPE],
            ) as String,
            id = savedStateHandle[ID],
        ),
    )
}

/**
 * Add the [VaultItemListingScreen] to the nav graph.
 */
fun NavGraphBuilder.vaultItemListingDestination(
    onNavigateBack: () -> Unit,
    onNavigateToVaultItemScreen: (id: String) -> Unit,
    onNavigateToVaultAddItemScreen: () -> Unit,
) {
    composableWithPushTransitions(
        route = VAULT_ITEM_LISTING_ROUTE,
        arguments = listOf(
            navArgument(
                name = VAULT_ITEM_LISTING_TYPE,
                builder = { type = NavType.StringType },
            ),
            navArgument(
                name = ID,
                builder = {
                    type = NavType.StringType
                    nullable = true
                },
            ),
        ),
    ) {
        VaultItemListingScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToVaultItem = onNavigateToVaultItemScreen,
            onNavigateToVaultAddItemScreen = onNavigateToVaultAddItemScreen,
        )
    }
}

/**
 * Navigate to the [VaultItemListingScreen].
 */
fun NavController.navigateToVaultItemListing(
    vaultItemListingType: VaultItemListingType,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = "$VAULT_ITEM_LISTING_PREFIX/${vaultItemListingType.toTypeString()}" +
            "?$ID=${vaultItemListingType.toIdOrNull()}",
        navOptions = navOptions,
    )
}

private fun VaultItemListingType.toTypeString(): String {
    return when (this) {
        is VaultItemListingType.Card -> CARD
        is VaultItemListingType.Collection -> COLLECTION
        is VaultItemListingType.Folder -> FOLDER
        is VaultItemListingType.Identity -> IDENTITY
        is VaultItemListingType.Login -> LOGIN
        is VaultItemListingType.SecureNote -> SECURE_NOTE
        is VaultItemListingType.Trash -> TRASH
    }
}

private fun VaultItemListingType.toIdOrNull(): String? =
    when (this) {
        is VaultItemListingType.Collection -> collectionId
        is VaultItemListingType.Folder -> folderId
        is VaultItemListingType.Card -> null
        is VaultItemListingType.Identity -> null
        is VaultItemListingType.Login -> null
        is VaultItemListingType.SecureNote -> null
        is VaultItemListingType.Trash -> null
    }

private fun determineVaultItemListingType(
    vaultItemListingTypeString: String,
    id: String?,
): VaultItemListingType {
    return when (vaultItemListingTypeString) {
        LOGIN -> VaultItemListingType.Login
        CARD -> VaultItemListingType.Card
        IDENTITY -> VaultItemListingType.Identity
        SECURE_NOTE -> VaultItemListingType.SecureNote
        TRASH -> VaultItemListingType.Trash
        FOLDER -> VaultItemListingType.Folder(folderId = id)
        COLLECTION -> VaultItemListingType.Collection(collectionId = requireNotNull(id))
        // This should never occur, vaultItemListingTypeString must match
        else -> throw IllegalStateException()
    }
}
