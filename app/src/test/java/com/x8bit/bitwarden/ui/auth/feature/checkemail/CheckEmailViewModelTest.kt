package com.x8bit.bitwarden.ui.auth.feature.checkemail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CheckEmailViewModelTest : BaseViewModelTest() {
    private val mutableFeatureFlagFlow = MutableStateFlow(false)
    private val featureFlagManager = mockk<FeatureFlagManager>(relaxed = true) {
        every { getFeatureFlag(FlagKey.OnboardingFlow) } returns false
        every { getFeatureFlagFlow(FlagKey.OnboardingFlow) } returns mutableFeatureFlagFlow
    }

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    @Test
    fun `initial state should pull from handle when present`() = runTest {
        val expectedState = DEFAULT_STATE.copy(
            email = "another@email.com",
        )
        val viewModel = createViewModel(expectedState)
        viewModel.stateFlow.test {
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `CloseTap should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(CheckEmailAction.BackClick)
            assertEquals(
                CheckEmailEvent.NavigateBack,
                awaitItem(),
            )
        }
    }

    @Test
    fun `OpenEmailTap should emit NavigateToEmailApp`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(CheckEmailAction.OpenEmailClick)
            assertEquals(
                CheckEmailEvent.NavigateToEmailApp,
                awaitItem(),
            )
        }
    }

    @Test
    fun `ChangeEmailTap should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(CheckEmailAction.ChangeEmailClick)
            assertEquals(
                CheckEmailEvent.NavigateBack,
                awaitItem(),
            )
        }
    }

    @Test
    fun `OnboardingFeatureFlagUpdated should update showNewOnboardingUi in state`() {
        val viewModel = createViewModel()
        mutableFeatureFlagFlow.value = true
        val expectedState = DEFAULT_STATE.copy(
            showNewOnboardingUi = true,
        )
        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    fun `OnLoginClick action should send NavigateToLanding event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(CheckEmailAction.LoginClick)
            assertEquals(
                CheckEmailEvent.NavigateBackToLanding,
                awaitItem(),
            )
        }
    }

    private fun createViewModel(state: CheckEmailState? = null): CheckEmailViewModel =
        CheckEmailViewModel(
            featureFlagManager = featureFlagManager,
            savedStateHandle = SavedStateHandle().also {
                it["email"] = EMAIL
                it["state"] = state
            },
        )

    companion object {
        private const val EMAIL = "test@gmail.com"
        private val DEFAULT_STATE = CheckEmailState(
            email = EMAIL,
            showNewOnboardingUi = false,
        )
    }
}
