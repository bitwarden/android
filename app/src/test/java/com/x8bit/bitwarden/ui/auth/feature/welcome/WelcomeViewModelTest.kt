package com.x8bit.bitwarden.ui.auth.feature.welcome

import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WelcomeViewModelTest : BaseViewModelTest() {

    private val featureFlagManager = mockk<FeatureFlagManager>()

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = WelcomeViewModel(featureFlagManager = featureFlagManager)

        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE,
                awaitItem(),
            )
        }
    }

    @Test
    fun `PagerSwipe should update state`() = runTest {
        val viewModel = WelcomeViewModel(featureFlagManager = featureFlagManager)
        val newIndex = 2

        viewModel.trySendAction(WelcomeAction.PagerSwipe(index = newIndex))

        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(index = newIndex),
                awaitItem(),
            )
        }
    }

    @Test
    fun `DotClick should update state and emit UpdatePager`() = runTest {
        val viewModel = WelcomeViewModel(featureFlagManager = featureFlagManager)
        val newIndex = 2

        viewModel.trySendAction(WelcomeAction.DotClick(index = newIndex))

        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(index = newIndex),
                awaitItem(),
            )
        }
        viewModel.eventFlow.test {
            assertEquals(
                WelcomeEvent.UpdatePager(index = newIndex),
                awaitItem(),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `CreateAccountClick should emit NavigateToCreateAccount when email verification is disabled`() =
        runTest {
            val viewModel = WelcomeViewModel(featureFlagManager = featureFlagManager)
            every { featureFlagManager.getFeatureFlag(FlagKey.EmailVerification) } returns false
            viewModel.trySendAction(WelcomeAction.CreateAccountClick)

            viewModel.eventFlow.test {
                assertEquals(
                    WelcomeEvent.NavigateToCreateAccount,
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `CreateAccountClick should emit NavigateToStartRegistration when email verification is enabled`() =
        runTest {
            val viewModel = WelcomeViewModel(featureFlagManager = featureFlagManager)
            every { featureFlagManager.getFeatureFlag(FlagKey.EmailVerification) } returns true
            viewModel.trySendAction(WelcomeAction.CreateAccountClick)

            viewModel.eventFlow.test {
                assertEquals(
                    WelcomeEvent.NavigateToStartRegistration,
                    awaitItem(),
                )
            }
        }

    @Test
    fun `LoginClick should emit NavigateToLogin`() = runTest {
        val viewModel = WelcomeViewModel(featureFlagManager = featureFlagManager)

        viewModel.trySendAction(WelcomeAction.LoginClick)

        viewModel.eventFlow.test {
            assertEquals(
                WelcomeEvent.NavigateToLogin,
                awaitItem(),
            )
        }
    }
}

private val DEFAULT_STATE = WelcomeState(
    index = 0,
    pages = listOf(
        WelcomeState.WelcomeCard.CardOne,
        WelcomeState.WelcomeCard.CardTwo,
        WelcomeState.WelcomeCard.CardThree,
        WelcomeState.WelcomeCard.CardFour,
    ),
)
