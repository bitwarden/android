package com.bitwarden.authenticator

import android.content.Intent
import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.data.repository.ServerConfigRepository
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * A view model that helps launch actions for the [MainActivity].
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    configRepository: ServerConfigRepository,
) : BaseViewModel<MainState, MainEvent, MainAction>(
    MainState(
        theme = settingsRepository.appTheme,
        isScreenCaptureAllowed = settingsRepository.isScreenCaptureAllowed,
        isDynamicColorsEnabled = settingsRepository.isDynamicColorsEnabled,
    ),
) {

    init {
        settingsRepository
            .appThemeStateFlow
            .map { MainAction.Internal.ThemeUpdate(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
        settingsRepository
            .isDynamicColorsEnabledFlow
            .map { MainAction.Internal.DynamicColorUpdate(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
        settingsRepository
            .isScreenCaptureAllowedStateFlow
            .map { MainAction.Internal.ScreenCaptureUpdate(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        viewModelScope.launch {
            configRepository.getServerConfig(forceRefresh = false)
        }
    }

    override fun handleAction(action: MainAction) {
        when (action) {
            is MainAction.ReceiveFirstIntent -> handleFirstIntentReceived(action)
            is MainAction.ReceiveNewIntent -> handleNewIntentReceived(action)
            MainAction.OpenDebugMenu -> handleOpenDebugMenu()
            is MainAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleInternalAction(action: MainAction.Internal) {
        when (action) {
            is MainAction.Internal.DynamicColorUpdate -> handleDynamicColorUpdate(action)
            is MainAction.Internal.ThemeUpdate -> handleThemeUpdated(action)

            is MainAction.Internal.ScreenCaptureUpdate -> handleScreenCaptureUpdate(
                screenCaptureUpdateAction = action,
            )
        }
    }

    private fun handleOpenDebugMenu() {
        sendEvent(MainEvent.NavigateToDebugMenu)
    }

    private fun handleDynamicColorUpdate(action: MainAction.Internal.DynamicColorUpdate) {
        mutableStateFlow.update { it.copy(isDynamicColorsEnabled = action.isEnabled) }
    }

    private fun handleThemeUpdated(action: MainAction.Internal.ThemeUpdate) {
        mutableStateFlow.update { it.copy(theme = action.theme) }
        sendEvent(MainEvent.UpdateAppTheme(osTheme = action.theme.osValue))
    }

    private fun handleFirstIntentReceived(action: MainAction.ReceiveFirstIntent) {
        handleIntent(
            intent = action.intent,
            isFirstIntent = true,
        )
    }

    private fun handleNewIntentReceived(action: MainAction.ReceiveNewIntent) {
        handleIntent(
            intent = action.intent,
            isFirstIntent = false,
        )
    }

    private fun handleScreenCaptureUpdate(
        screenCaptureUpdateAction: MainAction.Internal.ScreenCaptureUpdate,
    ) {
        mutableStateFlow.update {
            it.copy(
                isScreenCaptureAllowed = screenCaptureUpdateAction.isScreenCaptureEnabled,
            )
        }
    }

    private fun handleIntent(
        intent: Intent,
        isFirstIntent: Boolean,
    ) {
        // RFU
    }
}

/**
 * Models state for the [MainActivity].
 */
@Parcelize
data class MainState(
    val theme: AppTheme,
    val isDynamicColorsEnabled: Boolean,
    val isScreenCaptureAllowed: Boolean,
) : Parcelable

/**
 * Models actions for the [MainActivity].
 */
sealed class MainAction {
    /**
     * Receive first Intent by the application.
     */
    data class ReceiveFirstIntent(val intent: Intent) : MainAction()

    /**
     * Receive Intent by the application.
     */
    data class ReceiveNewIntent(val intent: Intent) : MainAction()

    /**
     * Receive event to open the debug menu.
     */
    data object OpenDebugMenu : MainAction()

    /**
     * Actions for internal use by the ViewModel.
     */
    sealed class Internal : MainAction() {
        /**
         * Indicates that dynamic colors have been enabled or disabled.
         */
        data class DynamicColorUpdate(
            val isEnabled: Boolean,
        ) : Internal()

        /**
         * Indicates that the app theme has changed.
         */
        data class ThemeUpdate(
            val theme: AppTheme,
        ) : Internal()

        /**
         * Indicates that the screen capture state has changed.
         */
        data class ScreenCaptureUpdate(
            val isScreenCaptureEnabled: Boolean,
        ) : Internal()
    }
}

/**
 * Represents events that are emitted by the [MainViewModel].
 */
sealed class MainEvent {

    /**
     * Navigate to the debug menu.
     */
    data object NavigateToDebugMenu : MainEvent()

    /**
     * Indicates that the app theme has been updated.
     */
    data class UpdateAppTheme(val osTheme: Int) : MainEvent()
}
