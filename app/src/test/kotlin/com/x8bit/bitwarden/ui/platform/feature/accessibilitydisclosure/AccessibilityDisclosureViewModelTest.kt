package com.x8bit.bitwarden.ui.platform.feature.accessibilitydisclosure

import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AccessibilityDisclosureViewModelTest : BaseViewModelTest() {

    private val settingsRepository: SettingsRepository = mockk {
        every { accessibilityDisclaimerHasBeenShown() } just runs
    }

    @Test
    fun `initial state should be correct`() {
        val viewModel = createViewModel()
        assertEquals(AccessibilityDisclosureState, viewModel.stateFlow.value)
    }

    @Test
    fun `AcceptClicked should mark disclaimer as shown and emit Dismiss event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AccessibilityDisclosureAction.AcceptClicked)
            assertEquals(AccessibilityDisclosureEvent.Dismiss, awaitItem())
        }
        verify(exactly = 1) {
            settingsRepository.accessibilityDisclaimerHasBeenShown()
        }
    }

    @Test
    fun `CloseAppClick should emit CloseApp event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AccessibilityDisclosureAction.CloseAppClick)
            assertEquals(AccessibilityDisclosureEvent.CloseApp, awaitItem())
        }
        verify(exactly = 0) {
            settingsRepository.accessibilityDisclaimerHasBeenShown()
        }
    }

    private fun createViewModel(): AccessibilityDisclosureViewModel =
        AccessibilityDisclosureViewModel(
            settingsRepository = settingsRepository,
        )
}
