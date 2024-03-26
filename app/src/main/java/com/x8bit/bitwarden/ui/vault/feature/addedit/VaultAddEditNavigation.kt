package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorMode
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType

private const val ADD_TYPE: String = "add"
private const val EDIT_TYPE: String = "edit"
private const val CLONE_TYPE: String = "clone"
private const val EDIT_ITEM_ID: String = "vault_edit_id"

private const val ADD_ITEM_TYPE: String = "vault_add_item_type"

private const val ADD_EDIT_ITEM_PREFIX: String = "vault_add_edit_item"
private const val ADD_EDIT_ITEM_TYPE: String = "vault_add_edit_type"

private const val ADD_EDIT_ITEM_ROUTE: String =
    ADD_EDIT_ITEM_PREFIX +
        "/{$ADD_EDIT_ITEM_TYPE}" +
        "?$EDIT_ITEM_ID={$EDIT_ITEM_ID}" +
        "?$ADD_ITEM_TYPE={$ADD_ITEM_TYPE}"

/**
 * Class to retrieve vault add & edit arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class VaultAddEditArgs(
    val vaultAddEditType: VaultAddEditType,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        vaultAddEditType = when (requireNotNull(savedStateHandle[ADD_EDIT_ITEM_TYPE])) {
            ADD_TYPE -> VaultAddEditType.AddItem(
                vaultItemCipherType = requireNotNull(
                    savedStateHandle.get<VaultItemCipherType>(ADD_ITEM_TYPE),
                ),
            )

            EDIT_TYPE -> VaultAddEditType.EditItem(requireNotNull(savedStateHandle[EDIT_ITEM_ID]))
            CLONE_TYPE -> VaultAddEditType.CloneItem(requireNotNull(savedStateHandle[EDIT_ITEM_ID]))
            else -> throw IllegalStateException("Unknown VaultAddEditType.")
        },
    )
}

/**
 * Add the vault add & edit screen to the nav graph.
 */
@Suppress("LongParameterList")
fun NavGraphBuilder.vaultAddEditDestination(
    onNavigateBack: () -> Unit,
    onNavigateToManualCodeEntryScreen: () -> Unit,
    onNavigateToQrCodeScanScreen: () -> Unit,
    onNavigateToGeneratorModal: (GeneratorMode.Modal) -> Unit,
    onNavigateToAttachments: (cipherId: String) -> Unit,
    onNavigateToMoveToOrganization: (cipherId: String, showOnlyCollections: Boolean) -> Unit,
) {
    composableWithSlideTransitions(
        route = ADD_EDIT_ITEM_ROUTE,
        arguments = listOf(
            navArgument(ADD_EDIT_ITEM_TYPE) { type = NavType.StringType },
            navArgument(ADD_ITEM_TYPE) { type = NavType.EnumType(VaultItemCipherType::class.java) },
        ),
    ) {
        VaultAddEditScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToManualCodeEntryScreen = onNavigateToManualCodeEntryScreen,
            onNavigateToQrCodeScanScreen = onNavigateToQrCodeScanScreen,
            onNavigateToGeneratorModal = onNavigateToGeneratorModal,
            onNavigateToAttachments = onNavigateToAttachments,
            onNavigateToMoveToOrganization = onNavigateToMoveToOrganization,
        )
    }
}

/**
 * Navigate to the vault add & edit screen.
 */
fun NavController.navigateToVaultAddEdit(
    vaultAddEditType: VaultAddEditType,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = "$ADD_EDIT_ITEM_PREFIX/${vaultAddEditType.toTypeString()}" +
            "?$EDIT_ITEM_ID=${vaultAddEditType.toIdOrNull()}" +
            "?$ADD_ITEM_TYPE=${vaultAddEditType.toVaultItemCipherTypeOrNull()}",
        navOptions = navOptions,
    )
}

private fun VaultAddEditType.toTypeString(): String =
    when (this) {
        is VaultAddEditType.AddItem -> ADD_TYPE
        is VaultAddEditType.EditItem -> EDIT_TYPE
        is VaultAddEditType.CloneItem -> CLONE_TYPE
    }

private fun VaultAddEditType.toIdOrNull(): String? =
    when (this) {
        is VaultAddEditType.AddItem -> null
        is VaultAddEditType.CloneItem -> vaultItemId
        is VaultAddEditType.EditItem -> vaultItemId
    }

private fun VaultAddEditType.toVaultItemCipherTypeOrNull(): VaultItemCipherType? =
    when (this) {
        is VaultAddEditType.AddItem -> vaultItemCipherType
        is VaultAddEditType.CloneItem,
        is VaultAddEditType.EditItem,
        -> null
    }
