package com.x8bit.bitwarden.ui.platform.feature.settings.vault

import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.util.toBaseWebVaultImportUrl
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * View model for the vault screen.
 */
@HiltViewModel
class VaultSettingsViewModel @Inject constructor(
    val environmentRepository: EnvironmentRepository,
) : BaseViewModel<VaultSettingsState, VaultSettingsEvent, VaultSettingsAction>(
    initialState = run {
        VaultSettingsState(
            importUrl = environmentRepository
                .environment
                .environmentUrlData
                .toBaseWebVaultImportUrl,
        )
    },
) {

    override fun handleAction(action: VaultSettingsAction): Unit = when (action) {
        VaultSettingsAction.BackClick -> handleBackClicked()
        VaultSettingsAction.ExportVaultClick -> handleExportVaultClicked()
        VaultSettingsAction.FoldersButtonClick -> handleFoldersButtonClicked()
        VaultSettingsAction.ImportItemsClick -> handleImportItemsClicked()
    }

    private fun handleBackClicked() {
        sendEvent(VaultSettingsEvent.NavigateBack)
    }

    private fun handleExportVaultClicked() {
        sendEvent(VaultSettingsEvent.NavigateToExportVault)
    }

    private fun handleFoldersButtonClicked() {
        sendEvent(VaultSettingsEvent.NavigateToFolders)
    }

    private fun handleImportItemsClicked() {
        sendEvent(
            VaultSettingsEvent.NavigateToImportVault(state.importUrl),
        )
    }
}

/**
 * Models the state for the VaultSettingScreen.
 */
data class VaultSettingsState(
    val importUrl: String,
)

/**
 * Models events for the vault screen.
 */
sealed class VaultSettingsEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : VaultSettingsEvent()

    /**
     * Navigate to the import vault URL.
     */
    data class NavigateToImportVault(val url: String) : VaultSettingsEvent()

    /**
     * Navigate to the Export Vault screen.
     */
    data object NavigateToExportVault : VaultSettingsEvent()

    /**
     * Navigate to the Folders screen.
     */
    data object NavigateToFolders : VaultSettingsEvent()

    /**
     * Shows a toast with the given [message].
     */
    data class ShowToast(
        val message: String,
    ) : VaultSettingsEvent()
}

/**
 * Models actions for the vault screen.
 */
sealed class VaultSettingsAction {
    /**
     * User clicked back button.
     */
    data object BackClick : VaultSettingsAction()

    /**
     * Indicates that the user clicked the Export Vault button.
     */
    data object ExportVaultClick : VaultSettingsAction()

    /**
     * Indicates that the user clicked the Folders button.
     */
    data object FoldersButtonClick : VaultSettingsAction()

    /**
     * Indicates that the user clicked the Import Items button.
     */
    data object ImportItemsClick : VaultSettingsAction()
}
