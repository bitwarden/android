package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorMode
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the vault add/edit screen.
 */
@Serializable
data class VaultAddEditRoute(
    val vaultAddEditMode: VaultAddEditMode,
    val vaultItemId: String?,
    val vaultItemCipherType: VaultItemCipherType,
    val initialValueType: InitialValueType?,
    val initialValue: String?,
    val selectedFolderId: String? = null,
    val selectedCollectionId: String? = null,
)

/**
 * The mode in which the vault add/edit screen should be displayed.
 */
@Serializable
enum class VaultAddEditMode {
    ADD,
    EDIT,
    CLONE,
}

/**
 * The type of initial data that is being provided.
 */
@Serializable
enum class InitialValueType {
    PASSWORD,
    USERNAME,
}

/**
 * Class to retrieve vault add & edit arguments from the [SavedStateHandle].
 */
data class VaultAddEditArgs(
    val vaultAddEditType: VaultAddEditType,
    val vaultItemCipherType: VaultItemCipherType,
    val selectedFolderId: String? = null,
    val selectedCollectionId: String? = null,
)

/**
 * Constructs a [VaultAddEditArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toVaultAddEditArgs(): VaultAddEditArgs {
    val route = this.toRoute<VaultAddEditRoute>()
    return VaultAddEditArgs(
        vaultAddEditType = when (route.vaultAddEditMode) {
            VaultAddEditMode.ADD -> {
                VaultAddEditType.AddItem(
                    initialData = when (route.initialValueType) {
                        InitialValueType.PASSWORD -> VaultAddEditType.AddItem.InitialData.Password(
                            value = requireNotNull(route.initialValue),
                        )

                        InitialValueType.USERNAME -> VaultAddEditType.AddItem.InitialData.Username(
                            value = requireNotNull(route.initialValue),
                        )

                        null -> null
                    },
                )
            }

            VaultAddEditMode.EDIT -> {
                VaultAddEditType.EditItem(vaultItemId = requireNotNull(route.vaultItemId))
            }

            VaultAddEditMode.CLONE -> {
                VaultAddEditType.CloneItem(vaultItemId = requireNotNull(route.vaultItemId))
            }
        },
        vaultItemCipherType = route.vaultItemCipherType,
        selectedFolderId = route.selectedFolderId,
        selectedCollectionId = route.selectedCollectionId,
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
    onNavigateToCardScanScreen: () -> Unit,
    onNavigateToGeneratorModal: (GeneratorMode.Modal) -> Unit,
    onNavigateToAttachments: (cipherId: String) -> Unit,
    onNavigateToMoveToOrganization: (cipherId: String, showOnlyCollections: Boolean) -> Unit,
    onNavigateToPlan: () -> Unit,
    onCloseAndNavigateToVaultItem: (cipherId: String, cipherType: VaultItemCipherType) -> Unit,
) {
    composableWithSlideTransitions<VaultAddEditRoute> {
        VaultAddEditScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToManualCodeEntryScreen = onNavigateToManualCodeEntryScreen,
            onNavigateToQrCodeScanScreen = onNavigateToQrCodeScanScreen,
            onNavigateToCardScanScreen = onNavigateToCardScanScreen,
            onNavigateToGeneratorModal = onNavigateToGeneratorModal,
            onNavigateToAttachments = onNavigateToAttachments,
            onNavigateToMoveToOrganization = onNavigateToMoveToOrganization,
            onNavigateToPlan = onNavigateToPlan,
            onCloseAndNavigateToVaultItem = onCloseAndNavigateToVaultItem,
        )
    }
}

/**
 * Navigate to the vault add & edit screen.
 */
fun NavController.navigateToVaultAddEdit(
    args: VaultAddEditArgs,
    navOptions: NavOptions? = null,
) {
    val initialData = (args.vaultAddEditType as? VaultAddEditType.AddItem)?.initialData
    navigate(
        route = VaultAddEditRoute(
            vaultAddEditMode = when (args.vaultAddEditType) {
                is VaultAddEditType.AddItem -> VaultAddEditMode.ADD
                is VaultAddEditType.CloneItem -> VaultAddEditMode.CLONE
                is VaultAddEditType.EditItem -> VaultAddEditMode.EDIT
            },
            initialValueType = when (initialData) {
                is VaultAddEditType.AddItem.InitialData.Password -> InitialValueType.PASSWORD
                is VaultAddEditType.AddItem.InitialData.Username -> InitialValueType.USERNAME
                null -> null
            },
            initialValue = initialData?.value,
            vaultItemId = args.vaultAddEditType.vaultItemId,
            vaultItemCipherType = args.vaultItemCipherType,
            selectedFolderId = args.selectedFolderId,
            selectedCollectionId = args.selectedCollectionId,
        ),
        navOptions = navOptions,
    )
}
