package com.x8bit.bitwarden

import android.content.Intent
import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * A view model that helps launch actions for the [MainActivity].
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val intentManager: IntentManager,
    settingsRepository: SettingsRepository,
) : BaseViewModel<MainState, Unit, MainAction>(
    MainState(
        theme = settingsRepository.appTheme,
    ),
) {
    init {
        settingsRepository
            .appThemeStateFlow
            .onEach { trySendAction(MainAction.Internal.ThemeUpdate(it)) }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: MainAction) {
        when (action) {
            is MainAction.Internal.ThemeUpdate -> handleAppThemeUpdated(action)
            is MainAction.ReceiveFirstIntent -> handleFirstIntentReceived(action)
            is MainAction.ReceiveNewIntent -> handleNewIntentReceived(action)
        }
    }

    private fun handleAppThemeUpdated(action: MainAction.Internal.ThemeUpdate) {
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
        val shareData = intentManager.getShareDataFromIntent(intent)
        when {
            shareData != null -> {
                authRepository.specialCircumstance =
                    UserState.SpecialCircumstance.ShareNewSend(
                        data = shareData,
                        // Allow users back into the already-running app when completing the
                        // Send task when this is not the first intent.
                        shouldFinishWhenComplete = isFirstIntent,
                    )
            }
        }
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
