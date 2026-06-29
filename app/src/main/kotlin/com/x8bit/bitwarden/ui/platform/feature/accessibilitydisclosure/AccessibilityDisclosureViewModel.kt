package com.x8bit.bitwarden.ui.platform.feature.accessibilitydisclosure

import android.os.Parcelable
import com.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * ViewModel for the Accessibility Disclosure screen.
 */
@HiltViewModel
class AccessibilityDisclosureViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : BaseViewModel<
    AccessibilityDisclosureState,
    AccessibilityDisclosureEvent,
    AccessibilityDisclosureAction,
    >(
    initialState = AccessibilityDisclosureState,
) {
    override fun handleAction(action: AccessibilityDisclosureAction) {
        when (action) {
            AccessibilityDisclosureAction.IUnderstandClick -> handleIUnderstandClick()
            AccessibilityDisclosureAction.CloseAppClick -> handleCloseAppClick()
        }
    }

    private fun handleIUnderstandClick() {
        settingsRepository.accessibilityDisclaimerHasBeenShown()
        sendEvent(AccessibilityDisclosureEvent.Dismiss)
    }

    private fun handleCloseAppClick() {
        sendEvent(AccessibilityDisclosureEvent.CloseApp)
    }
}

/**
 * State for the Accessibility Disclosure screen.
 */
@Parcelize
data object AccessibilityDisclosureState : Parcelable

/**
 * Events for the Accessibility Disclosure screen.
 */
sealed class AccessibilityDisclosureEvent {
    /**
     * Navigate back, dismissing the screen.
     */
    data object Dismiss : AccessibilityDisclosureEvent()

    /**
     * Closes the app.
     */
    data object CloseApp : AccessibilityDisclosureEvent()
}

/**
 * Actions for the Accessibility Disclosure screen.
 */
sealed class AccessibilityDisclosureAction {
    /**
     * User clicked the "I understand" button.
     */
    data object IUnderstandClick : AccessibilityDisclosureAction()

    /**
     * User clicked the close app button.
     */
    data object CloseAppClick : AccessibilityDisclosureAction()
}
