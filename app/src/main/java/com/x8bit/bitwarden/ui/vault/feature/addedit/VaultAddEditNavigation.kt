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

private const val LOGIN: String = "login"
private const val CARD: String = "card"
private const val IDENTITY: String = "identity"
private const val SECURE_NOTE: String = "secure_note"
private const val SSH_KEY: String = "ssh_key"
private const val ADD_ITEM_TYPE: String = "vault_add_item_type"

private const val ADD_EDIT_ITEM_PREFIX: String = "vault_add_edit_item"
private const val ADD_EDIT_ITEM_TYPE: String = "vault_add_edit_type"
private const val ADD_SELECTED_FOLDER_ID: String = "vault_add_selected_folder_id"
private const val ADD_SELECTED_COLLECTION_ID: String = "vault_add_selected_collection_id"

private const val ADD_EDIT_ITEM_ROUTE: String =
    ADD_EDIT_ITEM_PREFIX +
        "/{$ADD_EDIT_ITEM_TYPE}" +
        "?$EDIT_ITEM_ID={$EDIT_ITEM_ID}" +
        "?$ADD_ITEM_TYPE={$ADD_ITEM_TYPE}" +
        "?$ADD_SELECTED_FOLDER_ID={$ADD_SELECTED_FOLDER_ID}" +
        "?$ADD_SELECTED_COLLECTION_ID={$ADD_SELECTED_COLLECTION_ID}"

/**
 * Class to retrieve vault add & edit arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class VaultAddEditArgs(
    val vaultAddEditType: VaultAddEditType,
    val selectedFolderId: String? = null,
    val selectedCollectionId: String? = null,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        vaultAddEditType = when (requireNotNull(savedStateHandle[ADD_EDIT_ITEM_TYPE])) {
            ADD_TYPE -> VaultAddEditType.AddItem(
                vaultItemCipherType = requireNotNull(
                    value = savedStateHandle.get<String>(ADD_ITEM_TYPE),
                )
                    .toVaultItemCipherType(),
            )

            EDIT_TYPE -> VaultAddEditType.EditItem(requireNotNull(savedStateHandle[EDIT_ITEM_ID]))
            CLONE_TYPE -> VaultAddEditType.CloneItem(requireNotNull(savedStateHandle[EDIT_ITEM_ID]))
            else -> throw IllegalStateException("Unknown VaultAddEditType.")
        },
        selectedFolderId = savedStateHandle[ADD_SELECTED_FOLDER_ID],
        selectedCollectionId = savedStateHandle[ADD_SELECTED_COLLECTION_ID],
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
            navArgument(ADD_SELECTED_FOLDER_ID) {
                type = NavType.StringType
                nullable = true
            },
            navArgument(ADD_SELECTED_COLLECTION_ID) {
                type = NavType.StringType
                nullable = true
            },
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
    selectedFolderId: String? = null,
    selectedCollectionId: String? = null,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = "$ADD_EDIT_ITEM_PREFIX/${vaultAddEditType.toTypeString()}" +
            "?$EDIT_ITEM_ID=${vaultAddEditType.toIdOrNull()}" +
            "?$ADD_ITEM_TYPE=${vaultAddEditType.toVaultItemCipherTypeOrNull()}" +
            "?$ADD_SELECTED_FOLDER_ID=$selectedFolderId" +
            "?$ADD_SELECTED_COLLECTION_ID=$selectedCollectionId",
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

private fun VaultAddEditType.toVaultItemCipherTypeOrNull(): String? =
    when (this) {
        is VaultAddEditType.AddItem -> vaultItemCipherType.toTypeString()
        is VaultAddEditType.CloneItem,
        is VaultAddEditType.EditItem,
            -> null
    }

private fun VaultItemCipherType.toTypeString(): String =
    when (this) {
        VaultItemCipherType.LOGIN -> LOGIN
        VaultItemCipherType.CARD -> CARD
        VaultItemCipherType.IDENTITY -> IDENTITY
        VaultItemCipherType.SECURE_NOTE -> SECURE_NOTE
        VaultItemCipherType.SSH_KEY -> SSH_KEY
    }

private fun String.toVaultItemCipherType(): VaultItemCipherType =
    when (this) {
        LOGIN -> VaultItemCipherType.LOGIN
        CARD -> VaultItemCipherType.CARD
        IDENTITY -> VaultItemCipherType.IDENTITY
        SECURE_NOTE -> VaultItemCipherType.SECURE_NOTE
        SSH_KEY -> VaultItemCipherType.SSH_KEY
        else -> throw IllegalStateException(
            "Edit Item string arguments for VaultAddEditNavigation must match!",
        )
    }
