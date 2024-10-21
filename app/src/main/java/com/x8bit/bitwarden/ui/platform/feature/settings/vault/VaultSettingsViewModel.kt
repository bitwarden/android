package com.x8bit.bitwarden.ui.platform.feature.settings.vault

import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
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
@Suppress("TooManyFunctions")
@HiltViewModel
class VaultSettingsViewModel @Inject constructor(
    environmentRepository: EnvironmentRepository,
    featureFlagManager: FeatureFlagManager,
    private val authRepository: AuthRepository,
    private val firstTimeActionManager: FirstTimeActionManager,
) : BaseViewModel<VaultSettingsState, VaultSettingsEvent, VaultSettingsAction>(
    initialState = run {
        val firstTimeState = firstTimeActionManager.currentOrDefaultUserFirstTimeState
        VaultSettingsState(
            importUrl = environmentRepository
                .environment
                .environmentUrlData
                .toBaseWebVaultImportUrl,
            isNewImportLoginsFlowEnabled = featureFlagManager
                .getFeatureFlag(FlagKey.ImportLoginsFlow),
            showImportActionCard = firstTimeState.showImportLoginsCard,
        )
    },
) {
    init {
        featureFlagManager
            .getFeatureFlagFlow(FlagKey.ImportLoginsFlow)
            .map { VaultSettingsAction.Internal.ImportLoginsFeatureFlagChanged(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        firstTimeActionManager
            .firstTimeStateFlow
            .map {
                VaultSettingsAction.Internal.UserFirstTimeStateChanged(
                    showImportLoginsCard = it.showImportLoginsCard,
                )
            }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: VaultSettingsAction): Unit = when (action) {
        VaultSettingsAction.BackClick -> handleBackClicked()
        VaultSettingsAction.ExportVaultClick -> handleExportVaultClicked()
        VaultSettingsAction.FoldersButtonClick -> handleFoldersButtonClicked()
        VaultSettingsAction.ImportItemsClick -> handleImportItemsClicked()
        VaultSettingsAction.ImportLoginsCardCtaClick -> handleImportLoginsCardClicked()
        VaultSettingsAction.ImportLoginsCardDismissClick -> handleImportLoginsCardDismissClicked()
        is VaultSettingsAction.Internal -> handleInternalAction(action)
    }

    private fun handleInternalAction(action: VaultSettingsAction.Internal) {
        when (action) {
            is VaultSettingsAction.Internal.ImportLoginsFeatureFlagChanged -> {
                handleImportLoginsFeatureFlagChanged(action)
            }

            is VaultSettingsAction.Internal.UserFirstTimeStateChanged -> {
                handleUserFirstTimeStateChanged(action)
            }
        }
    }

    private fun handleImportLoginsCardDismissClicked() {
        dismissImportLoginsCard()
    }

    private fun handleImportLoginsCardClicked() {
        dismissImportLoginsCard()
        sendEvent(VaultSettingsEvent.NavigateToImportVault(state.importUrl))
    }

    private fun handleUserFirstTimeStateChanged(
        action: VaultSettingsAction.Internal.UserFirstTimeStateChanged,
    ) {
        mutableStateFlow.update {
            it.copy(showImportActionCard = action.showImportLoginsCard)
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

    private fun dismissImportLoginsCard() {
        if (!state.shouldShowImportCard) return
        authRepository.setShowImportLogins(showImportLogins = false)
    }
}

/**
 * Models the state for the VaultSettingScreen.
 */
data class VaultSettingsState(
    val importUrl: String,
    val isNewImportLoginsFlowEnabled: Boolean,
    private val showImportActionCard: Boolean,
) {
    /**
     * Should only show the import action card if the import logins feature flag is enabled.
     */
    val shouldShowImportCard: Boolean
        get() = showImportActionCard && isNewImportLoginsFlowEnabled
}

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
     * Indicates that the user clicked the CTA on the action card.
     */
    data object ImportLoginsCardCtaClick : VaultSettingsAction()

    /**
     * Indicates that the user dismissed the action card.
     */
    data object ImportLoginsCardDismissClick : VaultSettingsAction()

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

        /**
         * Indicates user first time state has changed.
         */
        data class UserFirstTimeStateChanged(
            val showImportLoginsCard: Boolean,
        ) : Internal()
    }
}
