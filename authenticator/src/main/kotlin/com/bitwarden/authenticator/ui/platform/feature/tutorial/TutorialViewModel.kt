package com.bitwarden.authenticator.ui.platform.feature.tutorial

import android.os.Parcelable
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * View model for the [TutorialScreen].
 */
@HiltViewModel
class TutorialViewModel @Inject constructor() :
    BaseViewModel<TutorialState, TutorialEvent, TutorialAction>(
        initialState = TutorialState(
            index = 0,
            pages = listOf(
                TutorialState.TutorialSlide.IntroSlide,
                TutorialState.TutorialSlide.QrScannerSlide,
                TutorialState.TutorialSlide.UniqueCodesSlide,
            ),
        ),
    ) {
    override fun handleAction(action: TutorialAction) {
        when (action) {
            is TutorialAction.PagerSwipe -> handlePagerSwipe(action)
            is TutorialAction.DotClick -> handleDotClick(action)
            is TutorialAction.ContinueClick -> handleContinueClick(action)
            TutorialAction.SkipClick -> handleSkipClick()
        }
    }

    private fun handlePagerSwipe(action: TutorialAction.PagerSwipe) {
        mutableStateFlow.update { it.copy(index = action.index) }
    }

    private fun handleDotClick(action: TutorialAction.DotClick) {
        mutableStateFlow.update { it.copy(index = action.index) }
        sendEvent(TutorialEvent.UpdatePager(index = action.index))
    }

    private fun handleContinueClick(action: TutorialAction.ContinueClick) {
        if (mutableStateFlow.value.isLastPage) {
            sendEvent(TutorialEvent.NavigateToAuthenticator)
        } else {
            mutableStateFlow.update { it.copy(index = action.index + 1) }
            sendEvent(TutorialEvent.UpdatePager(index = action.index + 1))
        }
    }

    private fun handleSkipClick() {
        sendEvent(TutorialEvent.NavigateToAuthenticator)
    }
}

/**
 * Models state for the Tutorial screen.
 */
@Parcelize
data class TutorialState(
    val index: Int,
    val pages: List<TutorialSlide>,
) : Parcelable {
    /**
     * Provides the text for the action button based on the current page index.
     * - Displays "Continue" if the user is not on the last page.
     * - Displays "Get Started" if the user is on the last page.
     */
    val actionButtonText: String
        get() = if (index != pages.lastIndex) "Continue" else "Get Started"

    /**
     * Indicates whether the current slide is the last in the pages array.
     */
    val isLastPage: Boolean
        get() = index == pages.lastIndex

    /**
     * A sealed class to represent the different slides the user can view on the tutorial screen.
     */
    @Suppress("MaxLineLength")
    sealed class TutorialSlide : Parcelable {
        abstract val image: Int
        abstract val title: Int
        abstract val message: Int

        /**
         * Tutorial should display the introduction slide.
         */
        @Parcelize
        data object IntroSlide : TutorialSlide() {
            override val image: Int get() = BitwardenDrawable.ill_authenticator
            override val title: Int get() = BitwardenString.secure_your_accounts_with_bitwarden_authenticator
            override val message: Int get() = BitwardenString.get_verification_codes_for_all_your_accounts
        }

        /**
         * Tutorial should display the QR code scanner description slide.
         */
        @Parcelize
        data object QrScannerSlide : TutorialSlide() {
            override val image: Int get() = BitwardenDrawable.ill_lock
            override val title: Int get() = BitwardenString.use_your_device_camera_to_scan_codes
            override val message: Int get() = BitwardenString.scan_the_qr_code_in_your_2_step_verification_settings_for_any_account
        }

        /**
         * Tutorial should display the 2FA code description slide.
         */
        @Parcelize
        data object UniqueCodesSlide : TutorialSlide() {
            override val image: Int get() = BitwardenDrawable.ill_pin
            override val title: Int get() = BitwardenString.sign_in_using_unique_codes
            override val message: Int get() = BitwardenString.when_using_2_step_verification_youll_enter_your_username_and_password_and_a_code_generated_in_this_app
        }
    }
}

/**
 * Represents a set of events related to the tutorial screen.
 */
sealed class TutorialEvent {
    /**
     * Updates the current index of the pager.
     */
    data class UpdatePager(val index: Int) : TutorialEvent()

    /**
     * Navigate to the authenticator tutorial slide.
     */
    data object NavigateToAuthenticator : TutorialEvent()
}

/**
 * Models actions that can be taken on the tutorial screen.
 */
sealed class TutorialAction {
    /**
     * Swipe the pager to the given [index].
     */
    data class PagerSwipe(val index: Int) : TutorialAction()

    /**
     * Click one of the page indicator dots at the given [index].
     */
    data class DotClick(val index: Int) : TutorialAction()

    /**
     * The user clicked the continue button at the given [index].
     */
    data class ContinueClick(val index: Int) : TutorialAction()

    /**
     * The user clicked the skip button on one of the tutorial slides.
     */
    data object SkipClick : TutorialAction()
}
