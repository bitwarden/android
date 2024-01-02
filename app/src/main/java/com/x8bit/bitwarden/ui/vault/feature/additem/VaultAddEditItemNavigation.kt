package com.x8bit.bitwarden.ui.vault.feature.additem

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType

private const val ADD_TYPE: String = "add"
private const val EDIT_TYPE: String = "edit"
private const val EDIT_ITEM_ID: String = "vault_edit_id"

private const val ADD_EDIT_ITEM_PREFIX: String = "vault_add_edit_item"
private const val ADD_EDIT_ITEM_TYPE: String = "vault_add_edit_type"

private const val ADD_EDIT_ITEM_ROUTE: String =
    "$ADD_EDIT_ITEM_PREFIX/{$ADD_EDIT_ITEM_TYPE}?$EDIT_ITEM_ID={$EDIT_ITEM_ID}"

/**
 * Class to retrieve vault add & edit arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class VaultAddEditItemArgs(
    val vaultAddEditType: VaultAddEditType,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        vaultAddEditType = when (requireNotNull(savedStateHandle[ADD_EDIT_ITEM_TYPE])) {
            ADD_TYPE -> VaultAddEditType.AddItem
            EDIT_TYPE -> VaultAddEditType.EditItem(requireNotNull(savedStateHandle[EDIT_ITEM_ID]))
            else -> throw IllegalStateException("Unknown VaultAddEditType.")
        },
    )
}

/**
 * Add the vault add & edit item screen to the nav graph.
 */
fun NavGraphBuilder.vaultAddEditItemDestination(
    onNavigateToQrCodeScanScreen: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = ADD_EDIT_ITEM_ROUTE,
        arguments = listOf(
            navArgument(ADD_EDIT_ITEM_TYPE) { type = NavType.StringType },
        ),
    ) {
        VaultAddItemScreen(onNavigateBack, onNavigateToQrCodeScanScreen)
    }
}

/**
 * Navigate to the vault add & edit item screen.
 */
fun NavController.navigateToVaultAddEditItem(
    vaultAddEditType: VaultAddEditType,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = "$ADD_EDIT_ITEM_PREFIX/${vaultAddEditType.toTypeString()}" +
            "?$EDIT_ITEM_ID=${vaultAddEditType.toIdOrNull()}",
        navOptions = navOptions,
    )
}

private fun VaultAddEditType.toTypeString(): String =
    when (this) {
        is VaultAddEditType.AddItem -> ADD_TYPE
        is VaultAddEditType.EditItem -> EDIT_TYPE
    }

private fun VaultAddEditType.toIdOrNull(): String? =
    (this as? VaultAddEditType.EditItem)?.vaultItemId
