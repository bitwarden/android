package com.x8bit.bitwarden.ui.vault.feature.itemlisting

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.x8bit.bitwarden.ui.platform.base.util.composableWithPushTransitions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithStayTransitions
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditArgs
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemArgs
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the vault item listing screen.
 */
@Serializable
sealed class VaultItemListingRoute {
    /**
     * The type of item to be displayed.
     */
    abstract val type: ItemListingType

    /**
     * The optional item ID used for folder and collection types.
     */
    abstract val itemId: String?

    /**
     * The type-safe route for the cipher specific vault item listing screen.
     */
    @Serializable
    data class CipherItemListing(
        override val type: ItemListingType,
        override val itemId: String?,
    ) : VaultItemListingRoute()

    /**
     * The type-safe route for the send specific vault item listing screen.
     */
    @Serializable
    data class SendItemListing(
        override val type: ItemListingType,
        override val itemId: String?,
    ) : VaultItemListingRoute()

    /**
     * The type-safe route for the root vault item listing screen.
     */
    @Serializable
    data class AsRoot(
        override val type: ItemListingType,
        override val itemId: String?,
    ) : VaultItemListingRoute()
}

/**
 * The type of items to be displayed.
 */
@Serializable
enum class ItemListingType {
    LOGIN,
    IDENTITY,
    SECURE_NOTE,
    CARD,
    SSH_KEY,
    TRASH,
    FOLDER,
    COLLECTION,
    SEND_FILE,
    SEND_TEXT,
}

/**
 * Class to retrieve vault item listing arguments from the [SavedStateHandle].
 */
data class VaultItemListingArgs(
    val vaultItemListingType: VaultItemListingType,
)

/**
 * Constructs a [VaultItemListingArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toVaultItemListingArgs(): VaultItemListingArgs {
    // We just need to pull the serializable data out of the route, since they are always the same
    // it does not matter which instance we fetch.
    val route = this.toRoute<VaultItemListingRoute.SendItemListing>()
    return VaultItemListingArgs(
        vaultItemListingType = when (route.type) {
            ItemListingType.LOGIN -> VaultItemListingType.Login
            ItemListingType.CARD -> VaultItemListingType.Card
            ItemListingType.IDENTITY -> VaultItemListingType.Identity
            ItemListingType.SECURE_NOTE -> VaultItemListingType.SecureNote
            ItemListingType.SSH_KEY -> VaultItemListingType.SshKey
            ItemListingType.TRASH -> VaultItemListingType.Trash
            ItemListingType.SEND_FILE -> VaultItemListingType.SendFile
            ItemListingType.SEND_TEXT -> VaultItemListingType.SendText
            ItemListingType.FOLDER -> VaultItemListingType.Folder(folderId = route.itemId)
            ItemListingType.COLLECTION -> VaultItemListingType.Collection(
                collectionId = requireNotNull(route.itemId),
            )
        },
    )
}

/**
 * Add the [VaultItemListingScreen] to the nav graph.
 */
@Suppress("LongParameterList")
fun NavGraphBuilder.vaultItemListingDestination(
    onNavigateBack: () -> Unit,
    onNavigateToVaultItemScreen: (args: VaultItemArgs) -> Unit,
    onNavigateToVaultEditItemScreen: (args: VaultAddEditArgs) -> Unit,
    onNavigateToVaultItemListing: (vaultItemListingType: VaultItemListingType) -> Unit,
    onNavigateToVaultAddItemScreen: (args: VaultAddEditArgs) -> Unit,
    onNavigateToAddFolderScreen: (selectedFolderId: String?) -> Unit,
    onNavigateToSearchVault: (searchType: SearchType.Vault) -> Unit,
) {
    internalVaultItemListingDestination<VaultItemListingRoute.CipherItemListing>(
        onNavigateBack = onNavigateBack,
        onNavigateToAddSendItem = { },
        onNavigateToEditSendItem = { },
        onNavigateToVaultAddItemScreen = onNavigateToVaultAddItemScreen,
        onNavigateToVaultItemListing = onNavigateToVaultItemListing,
        onNavigateToVaultItemScreen = onNavigateToVaultItemScreen,
        onNavigateToVaultEditItemScreen = onNavigateToVaultEditItemScreen,
        onNavigateToSearch = { onNavigateToSearchVault(it as SearchType.Vault) },
        onNavigateToAddFolderScreen = onNavigateToAddFolderScreen,
    )
}

/**
 * Add the [VaultItemListingScreen] to the nav graph.
 */
@Suppress("LongParameterList")
fun NavGraphBuilder.vaultItemListingDestinationAsRoot(
    onNavigateBack: () -> Unit,
    onNavigateToVaultItemScreen: (args: VaultItemArgs) -> Unit,
    onNavigateToVaultEditItemScreen: (args: VaultAddEditArgs) -> Unit,
    onNavigateToVaultAddItemScreen: (args: VaultAddEditArgs) -> Unit,
    onNavigateToAddFolderScreen: (selectedFolderId: String?) -> Unit,
    onNavigateToSearchVault: (searchType: SearchType.Vault) -> Unit,
) {
    composableWithStayTransitions<VaultItemListingRoute.AsRoot> {
        VaultItemListingScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToVaultItemScreen = onNavigateToVaultItemScreen,
            onNavigateToVaultEditItemScreen = onNavigateToVaultEditItemScreen,
            onNavigateToVaultAddItemScreen = onNavigateToVaultAddItemScreen,
            onNavigateToSearch = { onNavigateToSearchVault(it as SearchType.Vault) },
            onNavigateToAddFolder = onNavigateToAddFolderScreen,
            onNavigateToVaultItemListing = {},
            onNavigateToAddSendItem = {},
            onNavigateToEditSendItem = {},
        )
    }
}

/**
 * Add the [VaultItemListingScreen] to the nav graph.
 */
fun NavGraphBuilder.sendItemListingDestination(
    onNavigateBack: () -> Unit,
    onNavigateToAddSendItem: () -> Unit,
    onNavigateToEditSendItem: (sendId: String) -> Unit,
    onNavigateToSearchSend: (searchType: SearchType.Sends) -> Unit,
) {
    internalVaultItemListingDestination<VaultItemListingRoute.SendItemListing>(
        onNavigateBack = onNavigateBack,
        onNavigateToAddSendItem = onNavigateToAddSendItem,
        onNavigateToEditSendItem = onNavigateToEditSendItem,
        onNavigateToVaultAddItemScreen = { },
        onNavigateToAddFolderScreen = { _ -> },
        onNavigateToVaultItemScreen = { },
        onNavigateToVaultEditItemScreen = { },
        onNavigateToVaultItemListing = { },
        onNavigateToSearch = { onNavigateToSearchSend(it as SearchType.Sends) },
    )
}

/**
 * Add the [VaultItemListingScreen] to the nav graph.
 */
@Suppress("LongParameterList", "MaxLineLength")
private inline fun <reified T : VaultItemListingRoute> NavGraphBuilder.internalVaultItemListingDestination(
    noinline onNavigateBack: () -> Unit,
    noinline onNavigateToVaultItemScreen: (args: VaultItemArgs) -> Unit,
    noinline onNavigateToVaultEditItemScreen: (args: VaultAddEditArgs) -> Unit,
    noinline onNavigateToVaultItemListing: (vaultItemListingType: VaultItemListingType) -> Unit,
    noinline onNavigateToVaultAddItemScreen: (args: VaultAddEditArgs) -> Unit,
    noinline onNavigateToAddFolderScreen: (selectedFolderId: String?) -> Unit,
    noinline onNavigateToAddSendItem: () -> Unit,
    noinline onNavigateToEditSendItem: (sendId: String) -> Unit,
    noinline onNavigateToSearch: (searchType: SearchType) -> Unit,
) {
    composableWithPushTransitions<T> {
        VaultItemListingScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToVaultItemScreen = onNavigateToVaultItemScreen,
            onNavigateToVaultEditItemScreen = onNavigateToVaultEditItemScreen,
            onNavigateToVaultAddItemScreen = onNavigateToVaultAddItemScreen,
            onNavigateToAddSendItem = onNavigateToAddSendItem,
            onNavigateToEditSendItem = onNavigateToEditSendItem,
            onNavigateToVaultItemListing = onNavigateToVaultItemListing,
            onNavigateToSearch = onNavigateToSearch,
            onNavigateToAddFolder = onNavigateToAddFolderScreen,
        )
    }
}

/**
 * Navigate to the [VaultItemListingScreen] for vault.
 */
fun NavController.navigateToVaultItemListing(
    vaultItemListingType: VaultItemListingType,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = VaultItemListingRoute.CipherItemListing(
            type = vaultItemListingType.toItemListingType(),
            itemId = vaultItemListingType.toIdOrNull(),
        ),
        navOptions = navOptions,
    )
}

/**
 * Navigate to the [VaultItemListingScreen] for vault.
 */
fun NavController.navigateToVaultItemListingAsRoot(
    vaultItemListingType: VaultItemListingType,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = VaultItemListingRoute.AsRoot(
            type = vaultItemListingType.toItemListingType(),
            itemId = vaultItemListingType.toIdOrNull(),
        ),
        navOptions = navOptions,
    )
}

/**
 * Navigate to the [VaultItemListingScreen] for sends.
 */
fun NavController.navigateToSendItemListing(
    vaultItemListingType: VaultItemListingType,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = VaultItemListingRoute.SendItemListing(
            type = vaultItemListingType.toItemListingType(),
            itemId = vaultItemListingType.toIdOrNull(),
        ),
        navOptions = navOptions,
    )
}

private fun VaultItemListingType.toItemListingType(): ItemListingType {
    return when (this) {
        is VaultItemListingType.Card -> ItemListingType.CARD
        is VaultItemListingType.Collection -> ItemListingType.COLLECTION
        is VaultItemListingType.Folder -> ItemListingType.FOLDER
        is VaultItemListingType.Identity -> ItemListingType.IDENTITY
        is VaultItemListingType.Login -> ItemListingType.LOGIN
        is VaultItemListingType.SecureNote -> ItemListingType.SECURE_NOTE
        is VaultItemListingType.Trash -> ItemListingType.TRASH
        is VaultItemListingType.SendFile -> ItemListingType.SEND_FILE
        is VaultItemListingType.SendText -> ItemListingType.SEND_TEXT
        is VaultItemListingType.SshKey -> ItemListingType.SSH_KEY
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
        is VaultItemListingType.SendFile -> null
        is VaultItemListingType.SendText -> null
        is VaultItemListingType.SshKey -> null
    }
