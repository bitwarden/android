package com.x8bit.bitwarden.authenticator.ui.platform.feature.tutorial

import android.os.Parcelable
import com.x8bit.bitwarden.authenticator.R
import com.x8bit.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.authenticator.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.Text
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * View model for the [TutorialScreen].
 */
@HiltViewModel
class TutorialViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) :
    BaseViewModel<TutorialState, TutorialEvent, TutorialAction>(
        initialState = TutorialState.IntroSlide
    ) {

    override fun handleAction(action: TutorialAction) {
        when (action) {
            TutorialAction.ContinueClick -> {
                handleContinueClick()
            }

            TutorialAction.SkipClick -> {
                handleSkipClick()
            }

            is TutorialAction.TutorialPageChange -> {
                handleTutorialPageChange(action.targetPage)
            }
        }
    }

    private fun handleTutorialPageChange(page: Int) {
        when (page) {
            0 -> mutableStateFlow.update { TutorialState.IntroSlide }
            1 -> mutableStateFlow.update { TutorialState.QrScannerSlide }
            2 -> mutableStateFlow.update { TutorialState.UniqueCodesSlide }
        }
    }

    private fun handleContinueClick() {
        val currentPage = mutableStateFlow.value
        val event = when (currentPage) {
            TutorialState.IntroSlide -> TutorialEvent.NavigateToQrScannerSlide
            TutorialState.QrScannerSlide -> TutorialEvent.NavigateToUniqueCodesSlide
            TutorialState.UniqueCodesSlide -> {
                settingsRepository.hasSeenWelcomeTutorial = true
                TutorialEvent.NavigateToAuthenticator
            }
        }
        sendEvent(event)
    }

    private fun handleSkipClick() {
        settingsRepository.hasSeenWelcomeTutorial = true
        sendEvent(TutorialEvent.NavigateToAuthenticator)
    }
}

/**
 * Models state for the Tutorial screen.
 */
@Parcelize
sealed class TutorialState(
    val continueButtonText: Text,
    val isLastPage: Boolean,
) : Parcelable {

    /**
     * Tutorial should display the introduction slide.
     */
    @Parcelize
    data object IntroSlide : TutorialState(
        continueButtonText = R.string.continue_button.asText(),
        isLastPage = false,
    )

    /**
     * Tutorial should display the QR code scanner description slide.
     */
    @Parcelize
    data object QrScannerSlide : TutorialState(
        continueButtonText = R.string.continue_button.asText(),
        isLastPage = false
    )

    /**
     * Tutorial should display the 2FA code description slide.
     */
    @Parcelize
    data object UniqueCodesSlide : TutorialState(
        continueButtonText = R.string.get_started.asText(),
        isLastPage = true
    )
}

/**
 * Represents a set of events related to the tutorial screen.
 */
sealed class TutorialEvent {
    /**
     * Navigate to the authenticator tutorial slide.
     */
    data object NavigateToAuthenticator : TutorialEvent()

    /**
     * Navigate to the QR Code scanner tutorial slide.
     */
    data object NavigateToQrScannerSlide : TutorialEvent()

    /**
     * Navigate to the unique codes tutorial slide.
     */
    data object NavigateToUniqueCodesSlide : TutorialEvent()
}

/**
 * Models actions that can be taken on the tutorial screen.
 */
sealed class TutorialAction {
    /**
     * The user has manually changed the tutorial page by swiping.
     */
    data class TutorialPageChange(
        val targetPage: Int,
    ) : TutorialAction()

    /**
     * The user clicked the continue button on the introduction slide.
     */
    data object ContinueClick : TutorialAction()

    /**
     * The user clicked the skip button on one of the tutorial slides.
     */
    data object SkipClick : TutorialAction()
}
