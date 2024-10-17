package com.x8bit.bitwarden.ui.platform.feature.settings.vault

import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.util.toBaseWebVaultImportUrl
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * View model for the vault screen.
 */
@HiltViewModel
class VaultSettingsViewModel @Inject constructor(
    environmentRepository: EnvironmentRepository,
    val featureFlagManager: FeatureFlagManager,
) : BaseViewModel<VaultSettingsState, VaultSettingsEvent, VaultSettingsAction>(
    initialState = run {
        VaultSettingsState(
            importUrl = environmentRepository
                .environment
                .environmentUrlData
                .toBaseWebVaultImportUrl,
            isNewImportLoginsFlowEnabled = featureFlagManager
                .getFeatureFlag(FlagKey.ImportLoginsFlow),
        )
    },
) {
    init {
        featureFlagManager
            .getFeatureFlagFlow(FlagKey.ImportLoginsFlow)
            .map { VaultSettingsAction.Internal.ImportLoginsFeatureFlagChanged(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: VaultSettingsAction): Unit = when (action) {
        VaultSettingsAction.BackClick -> handleBackClicked()
        VaultSettingsAction.ExportVaultClick -> handleExportVaultClicked()
        VaultSettingsAction.FoldersButtonClick -> handleFoldersButtonClicked()
        VaultSettingsAction.ImportItemsClick -> handleImportItemsClicked()
        is VaultSettingsAction.Internal.ImportLoginsFeatureFlagChanged -> {
            handleImportLoginsFeatureFlagChanged(action)
        }
    }

    private fun handleImportLoginsFeatureFlagChanged(
        action: VaultSettingsAction.Internal.ImportLoginsFeatureFlagChanged,
    ) {
        mutableStateFlow.update {
            it.copy(isNewImportLoginsFlowEnabled = action.isEnabled)
        }
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
    val isNewImportLoginsFlowEnabled: Boolean,
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

    /**
     * Internal actions not performed by user interation
     */
    sealed class Internal : VaultSettingsAction() {

        /**
         * Indicates that the import logins feature flag has changed.
         */
        data class ImportLoginsFeatureFlagChanged(
            val isEnabled: Boolean,
        ) : Internal()
    }
}
