package com.x8bit.bitwarden.ui.platform.feature.settings.appearance

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppearanceViewModelTest : BaseViewModelTest() {
    @Test
    fun `initial state should be correct when not set`() {
        val viewModel = createViewModel(state = null)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct when set`() {
        val state = DEFAULT_STATE.copy(theme = AppearanceState.Theme.DARK)
        val viewModel = createViewModel(state = state)
        assertEquals(state, viewModel.stateFlow.value)
    }

    @Test
    fun `on BackClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AppearanceAction.BackClick)
            assertEquals(AppearanceEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on LanguageChange should update state`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE,
                awaitItem(),
            )
            viewModel.trySendAction(
                AppearanceAction.LanguageChange(AppearanceState.Language.ENGLISH),
            )
            assertEquals(
                DEFAULT_STATE.copy(language = AppearanceState.Language.ENGLISH),
                awaitItem(),
            )
        }
    }

    @Test
    fun `on ShowWebsiteIconsToggle should update value in state`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE,
                awaitItem(),
            )
            viewModel.trySendAction(AppearanceAction.ShowWebsiteIconsToggle(true))
            assertEquals(
                DEFAULT_STATE.copy(showWebsiteIcons = true),
                awaitItem(),
            )
        }
    }

    @Test
    fun `on ThemeChange should update state`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE,
                awaitItem(),
            )
            viewModel.trySendAction(AppearanceAction.ThemeChange(AppearanceState.Theme.DARK))
            assertEquals(
                DEFAULT_STATE.copy(theme = AppearanceState.Theme.DARK),
                awaitItem(),
            )
        }
    }

    private fun createViewModel(
        state: AppearanceState? = null,
    ) = AppearanceViewModel(
        savedStateHandle = SavedStateHandle().apply {
            set("state", state)
        },
    )

    companion object {
        private val DEFAULT_STATE = AppearanceState(
            language = AppearanceState.Language.DEFAULT,
            showWebsiteIcons = false,
            theme = AppearanceState.Theme.DEFAULT,
        )
    }
}
