package com.bitwarden.authenticator

import android.content.Intent
import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.authenticator.ui.platform.base.BaseViewModel
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : BaseViewModel<MainState, MainEvent, MainAction>(
    MainState(
        theme = settingsRepository.appTheme
    )
) {

    init {
        settingsRepository
            .appThemeStateFlow
            .onEach { trySendAction(MainAction.Internal.ThemeUpdate(it)) }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: MainAction) {
        when (action) {
            is MainAction.Internal.ThemeUpdate -> handleThemeUpdated(action)
            is MainAction.ReceiveFirstIntent -> handleFirstIntentReceived(action)
            is MainAction.ReceiveNewIntent -> handleNewIntentReceived(action)
        }
    }

    private fun handleThemeUpdated(action: MainAction.Internal.ThemeUpdate) {
        mutableStateFlow.update { it.copy(theme = action.theme) }
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
     * Actions for internal use by the ViewModel.
     */
    sealed class Internal : MainAction() {

        /**
         * Indicates that the app theme has changed.
         */
        data class ThemeUpdate(
            val theme: AppTheme,
        ) : Internal()
    }
}

/**
 * Represents events that are emitted by the [MainViewModel].
 */
sealed class MainEvent {

    /**
     * Event indicating a change in the screen capture setting.
     */
    data class ScreenCaptureSettingChange(val isAllowed: Boolean) : MainEvent()
}
