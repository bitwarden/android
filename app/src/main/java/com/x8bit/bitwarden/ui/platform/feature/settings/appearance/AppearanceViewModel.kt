package com.x8bit.bitwarden.ui.platform.feature.settings.appearance

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the appearance screen.
 */
@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<AppearanceState, AppearanceEvent, AppearanceAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: AppearanceState(
            language = settingsRepository.appLanguage,
            showWebsiteIcons = !settingsRepository.isIconLoadingDisabled,
            theme = settingsRepository.appTheme,
            isDynamicColorsEnabled = settingsRepository.isDynamicColorsEnabled,
            dialogState = null,
        ),
) {

    init {
        settingsRepository
            .appLanguageStateFlow
            .map { AppearanceAction.Internal.AppLanguageStateUpdateReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: AppearanceAction): Unit = when (action) {
        AppearanceAction.BackClick -> handleBackClicked()
        is AppearanceAction.LanguageChange -> handleLanguageChanged(action)
        is AppearanceAction.ShowWebsiteIconsToggle -> handleShowWebsiteIconsToggled(action)
        is AppearanceAction.ThemeChange -> handleThemeChanged(action)
        is AppearanceAction.DynamicColorsToggle -> handleDynamicColorsToggled(action)
        AppearanceAction.ConfirmEnableDynamicColorsClick -> {
            handleConfirmEnableDynamicColorsClicked()
        }

        AppearanceAction.DismissDialog -> {
            handleDismissDialog()
        }
        is AppearanceAction.Internal.AppLanguageStateUpdateReceive -> {
            handleLanguageStateChange(action)
        }
    }

    private fun handleLanguageStateChange(
        action: AppearanceAction.Internal.AppLanguageStateUpdateReceive,
    ) {
        mutableStateFlow.update {
            it.copy(language = action.language)
        }
    }

    private fun handleBackClicked() {
        sendEvent(AppearanceEvent.NavigateBack)
    }

    private fun handleLanguageChanged(action: AppearanceAction.LanguageChange) {
        settingsRepository.appLanguage = action.language
    }

    private fun handleShowWebsiteIconsToggled(action: AppearanceAction.ShowWebsiteIconsToggle) {
        mutableStateFlow.update {
            it.copy(showWebsiteIcons = action.showWebsiteIcons)
        }

        // Negate the boolean to properly update the settings repository
        settingsRepository.isIconLoadingDisabled = !action.showWebsiteIcons
    }

    private fun handleThemeChanged(action: AppearanceAction.ThemeChange) {
        mutableStateFlow.update { it.copy(theme = action.theme) }
        settingsRepository.appTheme = action.theme
    }

    private fun handleDynamicColorsToggled(action: AppearanceAction.DynamicColorsToggle) {
        if (action.isEnabled) {
            mutableStateFlow.update {
                it.copy(dialogState = AppearanceState.DialogState.EnableDynamicColors)
            }
        } else {
            settingsRepository.isDynamicColorsEnabled = false
            mutableStateFlow.update { it.copy(isDynamicColorsEnabled = false) }
        }
    }

    private fun handleConfirmEnableDynamicColorsClicked() {
        settingsRepository.isDynamicColorsEnabled = true
        mutableStateFlow.update {
            it.copy(
                isDynamicColorsEnabled = true,
                dialogState = null,
            )
        }
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }
}

/**
 * Models state of the Appearance screen.
 */
@Parcelize
data class AppearanceState(
    val language: AppLanguage,
    val showWebsiteIcons: Boolean,
    val theme: AppTheme,
    val isDynamicColorsEnabled: Boolean,
    val dialogState: DialogState?,
) : Parcelable {

    /**
     * Models dialogs that can be shown on the Appearance screen.
     */
    sealed class DialogState : Parcelable {

        /**
         * Dialog to confirm enabling Dynamic Colors.
         */
        @Parcelize
        data object EnableDynamicColors : DialogState()
    }
}

/**
 * Models events for the appearance screen.
 */
sealed class AppearanceEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : AppearanceEvent()
}

/**
 * Models actions for the appearance screen.
 */
sealed class AppearanceAction {
    /**
     * User clicked back button.
     */
    data object BackClick : AppearanceAction()

    /**
     * Indicates that the user changed the Language.
     */
    data class LanguageChange(
        val language: AppLanguage,
    ) : AppearanceAction()

    /**
     * Indicates that the user toggled the Show Website Icons switch to [showWebsiteIcons].
     */
    data class ShowWebsiteIconsToggle(
        val showWebsiteIcons: Boolean,
    ) : AppearanceAction()

    /**
     * Indicates that the user selected a new theme.
     */
    data class ThemeChange(
        val theme: AppTheme,
    ) : AppearanceAction()

    /**
     * Internal actions not sent through the UI.
     */
    sealed class Internal : AppearanceAction() {

        /**
         * The AppLanguageState value has updated.
         */
        data class AppLanguageStateUpdateReceive(val language: AppLanguage) : Internal()
    }

    /**
     * Indicates that the user toggled the Dynamic Colors switch to [isEnabled].
     */
    data class DynamicColorsToggle(
        val isEnabled: Boolean,
    ) : AppearanceAction()

    /**
     * Indicates that the user confirmed enabling Dynamic Colors.
     */
    data object ConfirmEnableDynamicColorsClick : AppearanceAction()

    /**
     * Indicates the user dismissed the dialog.
     */
    data object DismissDialog : AppearanceAction()
}
