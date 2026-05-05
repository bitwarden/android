package com.x8bit.bitwarden.ui.platform.feature.premium.upgraded

import com.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.data.billing.manager.PremiumStateManager
import com.x8bit.bitwarden.data.billing.manager.UPGRADED_TO_PREMIUM_LEARN_MORE_URL
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for the "Upgraded to Premium" screen.
 */
@HiltViewModel
class UpgradedToPremiumViewModel @Inject constructor(
    private val premiumStateManager: PremiumStateManager,
) : BaseViewModel<Unit, UpgradedToPremiumEvent, UpgradedToPremiumAction>(
    initialState = Unit,
) {

    override fun handleAction(action: UpgradedToPremiumAction) {
        when (action) {
            UpgradedToPremiumAction.LearnMoreClick -> handleLearnMoreClick()
            UpgradedToPremiumAction.CloseClick -> handleCloseClick()
        }
    }

    private fun handleLearnMoreClick() {
        premiumStateManager.dismissUpgradedToPremiumCard()
        sendEvent(UpgradedToPremiumEvent.NavigateToUrl(url = UPGRADED_TO_PREMIUM_LEARN_MORE_URL))
        sendEvent(UpgradedToPremiumEvent.NavigateBack)
    }

    private fun handleCloseClick() {
        premiumStateManager.dismissUpgradedToPremiumCard()
        sendEvent(UpgradedToPremiumEvent.NavigateBack)
    }
}

/**
 * Events for the "Upgraded to Premium" screen.
 */
sealed class UpgradedToPremiumEvent {
    /**
     * Dismiss the screen.
     */
    data object NavigateBack : UpgradedToPremiumEvent()

    /**
     * Navigate the user to the given external [url].
     */
    data class NavigateToUrl(
        val url: String,
    ) : UpgradedToPremiumEvent()
}

/**
 * Actions for the "Upgraded to Premium" screen.
 */
sealed class UpgradedToPremiumAction {
    /**
     * User clicked the "Learn more" CTA.
     */
    data object LearnMoreClick : UpgradedToPremiumAction()

    /**
     * User clicked the "Close" CTA.
     */
    data object CloseClick : UpgradedToPremiumAction()
}
