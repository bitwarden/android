package com.x8bit.bitwarden

import android.content.Intent
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.data.autofill.util.getAutofillSelectionDataOrNull
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
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

private const val SPECIAL_CIRCUMSTANCE_KEY = "special-circumstance"

/**
 * A view model that helps launch actions for the [MainActivity].
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val specialCircumstanceManager: SpecialCircumstanceManager,
    private val intentManager: IntentManager,
    settingsRepository: SettingsRepository,
    private val savedStateHandle: SavedStateHandle,
) : BaseViewModel<MainState, MainEvent, MainAction>(
    MainState(
        theme = settingsRepository.appTheme,
    ),
) {
    private var specialCircumstance: SpecialCircumstance?
        get() = savedStateHandle[SPECIAL_CIRCUMSTANCE_KEY]
        set(value) {
            savedStateHandle[SPECIAL_CIRCUMSTANCE_KEY] = value
        }

    init {
        // Immediately restore the special circumstance if we have one and then listen for changes
        specialCircumstanceManager.specialCircumstance = specialCircumstance

        specialCircumstanceManager
            .specialCircumstanceStateFlow
            .onEach { specialCircumstance = it }
            .launchIn(viewModelScope)

        settingsRepository
            .appThemeStateFlow
            .onEach { trySendAction(MainAction.Internal.ThemeUpdate(it)) }
            .launchIn(viewModelScope)

        settingsRepository
            .isScreenCaptureAllowedStateFlow
            .onEach { isAllowed ->
                sendEvent(MainEvent.ScreenCaptureSettingChange(isAllowed))
            }
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
        val autofillSelectionData = intent.getAutofillSelectionDataOrNull()
        val shareData = intentManager.getShareDataFromIntent(intent)
        when {
            autofillSelectionData != null -> {
                specialCircumstanceManager.specialCircumstance =
                    SpecialCircumstance.AutofillSelection(
                        autofillSelectionData = autofillSelectionData,
                        // Allow users back into the already-running app when completing the
                        // autofill task when this is not the first intent.
                        shouldFinishWhenComplete = isFirstIntent,
                    )
            }

            shareData != null -> {
                specialCircumstanceManager.specialCircumstance =
                    SpecialCircumstance.ShareNewSend(
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

/**
 * Represents events that are emitted by the [MainViewModel].
 */
sealed class MainEvent {

    /**
     * Event indicating a change in the screen capture setting.
     */
    data class ScreenCaptureSettingChange(val isAllowed: Boolean) : MainEvent()
}
