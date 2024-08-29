package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
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
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Manages bottom tab navigation of the application.
 */
@HiltViewModel
class VaultUnlockedNavBarViewModel @Inject constructor(
    authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
    specialCircumstancesManager: SpecialCircumstanceManager,
) : BaseViewModel<VaultUnlockedNavBarState, VaultUnlockedNavBarEvent, VaultUnlockedNavBarAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: VaultUnlockedNavBarState(
            vaultNavBarLabelRes = R.string.my_vault,
            vaultNavBarContentDescriptionRes = R.string.my_vault,
            currentTab = SelectedBottomTab.Vault,
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

        // As state updates, write to saved state handle:
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
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
        sendNavigationEvent(SelectedBottomTab.Generator)
    }

    /**
     * Attempts to send [VaultUnlockedNavBarEvent.NavigateToSendScreen] event
     */
    private fun handleSendTabClicked() {
        sendNavigationEvent(SelectedBottomTab.Send)
    }

    /**
     * Attempts to send [VaultUnlockedNavBarEvent.NavigateToVaultScreen] event
     */
    private fun handleVaultTabClicked() {
        sendNavigationEvent(SelectedBottomTab.Vault)
    }

    /**
     * Attempts to send [VaultUnlockedNavBarEvent.NavigateToSettingsScreen] event
     */
    private fun handleSettingsTabClicked() {
        sendNavigationEvent(SelectedBottomTab.Settings)
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

    /**
     * Send the navigation event for the selected tab. If the tab is already selected, do nothing.
     */
    private fun sendNavigationEvent(tab: SelectedBottomTab) {
        // If the tab is already selected, do nothing.
        if (stateFlow.value.currentTab == tab) return
        val navEvent = when (tab) {
            SelectedBottomTab.Vault -> VaultUnlockedNavBarEvent.NavigateToVaultScreen
            SelectedBottomTab.Send -> VaultUnlockedNavBarEvent.NavigateToSendScreen
            SelectedBottomTab.Generator -> VaultUnlockedNavBarEvent.NavigateToGeneratorScreen
            SelectedBottomTab.Settings -> VaultUnlockedNavBarEvent.NavigateToSettingsScreen
        }
        sendEvent(navEvent)
        // Update the current tab.
        mutableStateFlow.update {
            it.copy(currentTab = tab)
        }
    }
}

/**
 * Models state for the [VaultUnlockedNavBarViewModel].
 */
@Parcelize
data class VaultUnlockedNavBarState(
    @StringRes val vaultNavBarLabelRes: Int,
    @StringRes val vaultNavBarContentDescriptionRes: Int,
    val currentTab: SelectedBottomTab,
) : Parcelable

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

/**
 * Enum to represent the current bottom tab that is selected.
 */
@Parcelize
enum class SelectedBottomTab : Parcelable {
    Vault,
    Send,
    Generator,
    Settings,
}
