package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Manages bottom tab navigation of the application.
 */
@HiltViewModel
class VaultUnlockedNavBarViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    specialCircumstancesManager: SpecialCircumstanceManager,
) : BaseViewModel<VaultUnlockedNavBarState, VaultUnlockedNavBarEvent, VaultUnlockedNavBarAction>(
    initialState = VaultUnlockedNavBarState(
        vaultNavBarLabelRes = R.string.my_vault,
        vaultNavBarContentDescriptionRes = R.string.my_vault,
    ),
) {
    init {
        authRepository
            .userStateFlow
            .onEach {
                sendAction(VaultUnlockedNavBarAction.Internal.UserStateUpdateReceive(it))
            }
            .launchIn(viewModelScope)

        when (specialCircumstancesManager.specialCircumstance) {
            SpecialCircumstance.GeneratorShortcut -> {
                sendEvent(VaultUnlockedNavBarEvent.NavigateToGeneratorScreen)
                specialCircumstancesManager.specialCircumstance = null
            }

            SpecialCircumstance.VaultShortcut -> {
                sendEvent(VaultUnlockedNavBarEvent.NavigateToVaultScreen)
                specialCircumstancesManager.specialCircumstance = null
            }

            else -> Unit
        }
    }

    override fun handleAction(action: VaultUnlockedNavBarAction) {
        when (action) {
            VaultUnlockedNavBarAction.GeneratorTabClick -> handleGeneratorTabClicked()
            VaultUnlockedNavBarAction.SendTabClick -> handleSendTabClicked()
            VaultUnlockedNavBarAction.SettingsTabClick -> handleSettingsTabClicked()
            VaultUnlockedNavBarAction.VaultTabClick -> handleVaultTabClicked()
            is VaultUnlockedNavBarAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleInternalAction(action: VaultUnlockedNavBarAction.Internal) {
        when (action) {
            is VaultUnlockedNavBarAction.Internal.UserStateUpdateReceive -> {
                handleUserStateUpdateReceive(action)
            }
        }
    }
    // #region BottomTabViewModel Action Handlers
    /**
     * Attempts to send [VaultUnlockedNavBarEvent.NavigateToGeneratorScreen] event
     */
    private fun handleGeneratorTabClicked() {
        sendEvent(VaultUnlockedNavBarEvent.NavigateToGeneratorScreen)
    }

    /**
     * Attempts to send [VaultUnlockedNavBarEvent.NavigateToSendScreen] event
     */
    private fun handleSendTabClicked() {
        sendEvent(VaultUnlockedNavBarEvent.NavigateToSendScreen)
    }

    /**
     * Attempts to send [VaultUnlockedNavBarEvent.NavigateToVaultScreen] event
     */
    private fun handleVaultTabClicked() {
        sendEvent(VaultUnlockedNavBarEvent.NavigateToVaultScreen)
    }

    /**
     * Attempts to send [VaultUnlockedNavBarEvent.NavigateToSettingsScreen] event
     */
    private fun handleSettingsTabClicked() {
        sendEvent(VaultUnlockedNavBarEvent.NavigateToSettingsScreen)
    }

    /**
     * Updates the nav bar title according to whether the user is part of any organizations or not.
     */
    private fun handleUserStateUpdateReceive(
        action: VaultUnlockedNavBarAction.Internal.UserStateUpdateReceive,
    ) {
        val hasOrganizations = action
            .userState
            ?.activeAccount
            ?.organizations
            ?.isNotEmpty()
            ?: false
        val vaultRes = if (hasOrganizations) R.string.vaults else R.string.my_vault
        mutableStateFlow.update {
            it.copy(
                vaultNavBarLabelRes = vaultRes,
                vaultNavBarContentDescriptionRes = vaultRes,
            )
        }
    }
    // #endregion BottomTabViewModel Action Handlers
}

/**
 * Models state for the [VaultUnlockedNavBarViewModel].
 */
data class VaultUnlockedNavBarState(
    @StringRes val vaultNavBarLabelRes: Int,
    @StringRes val vaultNavBarContentDescriptionRes: Int,
)

/**
 * Models actions for the bottom tab of the vault unlocked portion of the app.
 */
sealed class VaultUnlockedNavBarAction {
    /**
     * Click Generator tab.
     */
    data object GeneratorTabClick : VaultUnlockedNavBarAction()

    /**
     * Click Send tab.
     */
    data object SendTabClick : VaultUnlockedNavBarAction()

    /**
     * Click Vault tab.
     */
    data object VaultTabClick : VaultUnlockedNavBarAction()

    /**
     * Click Settings tab.
     */
    data object SettingsTabClick : VaultUnlockedNavBarAction()

    /**
     * Models actions that the [VaultUnlockedNavBarViewModel] itself might send.
     */
    sealed class Internal : VaultUnlockedNavBarAction() {
        /**
         * Indicates a change in user state has been received.
         */
        data class UserStateUpdateReceive(
            val userState: UserState?,
        ) : Internal()
    }
}

/**
 * Models events for the bottom tab of the vault unlocked portion of the app.
 */
sealed class VaultUnlockedNavBarEvent {
    /**
     * Navigate to the Generator screen.
     */
    data object NavigateToGeneratorScreen : VaultUnlockedNavBarEvent()

    /**
     * Navigate to the Send screen.
     */
    data object NavigateToSendScreen : VaultUnlockedNavBarEvent()

    /**
     * Navigate to the Vault screen.
     */
    data object NavigateToVaultScreen : VaultUnlockedNavBarEvent()

    /**
     * Navigate to the Settings screen.
     */
    data object NavigateToSettingsScreen : VaultUnlockedNavBarEvent()
}
