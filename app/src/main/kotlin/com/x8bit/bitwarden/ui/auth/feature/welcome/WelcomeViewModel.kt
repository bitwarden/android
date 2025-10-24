package com.x8bit.bitwarden.ui.auth.feature.welcome

import android.os.Parcelable
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * Manages application state for the welcome screen.
 */
@HiltViewModel
class WelcomeViewModel @Inject constructor() :
    BaseViewModel<WelcomeState, WelcomeEvent, WelcomeAction>(
        initialState = WelcomeState(
            index = 0,
            pages = listOf(
                WelcomeState.WelcomeCard.CardOne,
                WelcomeState.WelcomeCard.CardTwo,
                WelcomeState.WelcomeCard.CardThree,
                WelcomeState.WelcomeCard.CardFour,
            ),
        ),
    ) {
    override fun handleAction(action: WelcomeAction) {
        when (action) {
            is WelcomeAction.PagerSwipe -> handlePagerSwipe(action)
            is WelcomeAction.DotClick -> handleDotClick(action)
            WelcomeAction.CreateAccountClick -> handleCreateAccountClick()
            WelcomeAction.LoginClick -> handleLoginClick()
        }
    }

    private fun handlePagerSwipe(action: WelcomeAction.PagerSwipe) {
        mutableStateFlow.update { it.copy(index = action.index) }
    }

    private fun handleDotClick(action: WelcomeAction.DotClick) {
        mutableStateFlow.update { it.copy(index = action.index) }
        sendEvent(WelcomeEvent.UpdatePager(index = action.index))
    }

    private fun handleCreateAccountClick() {
        sendEvent(WelcomeEvent.NavigateToStartRegistration)
    }

    private fun handleLoginClick() {
        sendEvent(WelcomeEvent.NavigateToLogin)
    }
}

/**
 * Models state of the welcome screen.
 */
@Parcelize
data class WelcomeState(
    val index: Int,
    val pages: List<WelcomeCard>,
) : Parcelable {
    /**
     * A sealed class to represent the different cards the user can view on the welcome screen.
     */
    sealed class WelcomeCard : Parcelable {
        abstract val imageRes: Int
        abstract val titleRes: Int
        abstract val messageRes: Int

        /**
         * Represents the first card the user should see on the welcome screen.
         */
        @Parcelize
        data object CardOne : WelcomeCard() {
            override val imageRes: Int get() = BitwardenDrawable.ill_vault_items
            override val titleRes: Int get() = BitwardenString.security_prioritized
            override val messageRes: Int get() = BitwardenString.welcome_message_1
        }

        /**
         * Represents the second card the user should see on the welcome screen.
         */
        @Parcelize
        data object CardTwo : WelcomeCard() {
            override val imageRes: Int get() = BitwardenDrawable.ill_welcome_2
            override val titleRes: Int get() = BitwardenString.quick_and_easy_login
            override val messageRes: Int get() = BitwardenString.welcome_message_2
        }

        /**
         * Represents the third card the user should see on the welcome screen.
         */
        @Parcelize
        data object CardThree : WelcomeCard() {
            override val imageRes: Int get() = BitwardenDrawable.ill_welcome_3
            override val titleRes: Int get() = BitwardenString.level_up_your_logins
            override val messageRes: Int get() = BitwardenString.welcome_message_3
        }

        /**
         * Represents the fourth card the user should see on the welcome screen.
         */
        @Parcelize
        data object CardFour : WelcomeCard() {
            override val imageRes: Int get() = BitwardenDrawable.ill_welcome_4
            override val titleRes: Int get() = BitwardenString.your_data_when_and_where_you_need_it
            override val messageRes: Int get() = BitwardenString.welcome_message_4
        }
    }
}

/**
 * Models events for the welcome screen.
 */
sealed class WelcomeEvent {
    /**
     * Updates the current index of the pager.
     */
    data class UpdatePager(
        val index: Int,
    ) : WelcomeEvent()

    /**
     * Navigates to the login screen.
     */
    data object NavigateToLogin : WelcomeEvent()

    /**
     * Navigates to the start registration screen.
     */
    data object NavigateToStartRegistration : WelcomeEvent()
}

/**
 * Models actions for the welcome screen.
 */
sealed class WelcomeAction {
    /**
     * Swipe the pager to the given [index].
     */
    data class PagerSwipe(
        val index: Int,
    ) : WelcomeAction()

    /**
     * Click one of the page indicator dots at the given [index].
     */
    data class DotClick(
        val index: Int,
    ) : WelcomeAction()

    /**
     * Click the create account button.
     */
    data object CreateAccountClick : WelcomeAction()

    /**
     * Click the login button.
     */
    data object LoginClick : WelcomeAction()
}
