package com.x8bit.bitwarden.ui.platform.feature.premium.upgraded

import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.data.billing.manager.PremiumStateManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UpgradedToPremiumViewModelTest : BaseViewModelTest() {

    private val premiumStateManager: PremiumStateManager = mockk {
        every { dismissUpgradedToPremiumCard() } just runs
    }

    @Test
    fun `LearnMoreClick consumes the card and emits NavigateToUrl then NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(UpgradedToPremiumAction.LearnMoreClick)
            assertEquals(
                UpgradedToPremiumEvent.NavigateToUrl(
                    url = "https://bitwarden.com/help/password-manager-plans/",
                ),
                awaitItem(),
            )
            assertEquals(UpgradedToPremiumEvent.NavigateBack, awaitItem())
        }
        verify(exactly = 1) {
            premiumStateManager.dismissUpgradedToPremiumCard()
        }
    }

    @Test
    fun `CloseClick consumes the card and emits NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(UpgradedToPremiumAction.CloseClick)
            assertEquals(UpgradedToPremiumEvent.NavigateBack, awaitItem())
        }
        verify(exactly = 1) {
            premiumStateManager.dismissUpgradedToPremiumCard()
        }
    }

    private fun createViewModel(): UpgradedToPremiumViewModel = UpgradedToPremiumViewModel(
        premiumStateManager = premiumStateManager,
    )
}
