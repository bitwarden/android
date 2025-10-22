package com.bitwarden.authenticator

import app.cash.turbine.test
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.authenticator.data.platform.repository.util.FakeServerConfigRepository
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MainViewModelTest : BaseViewModelTest() {

    private val mutableAppThemeFlow = MutableStateFlow(AppTheme.DEFAULT)
    private val mutableScreenCaptureAllowedFlow = MutableStateFlow(false)
    private val mutableIsDynamicColorsEnabledFlow = MutableStateFlow(false)
    private val settingsRepository = mockk<SettingsRepository> {
        every { appTheme } returns AppTheme.DEFAULT
        every { appThemeStateFlow } returns mutableAppThemeFlow
        every { isScreenCaptureAllowedStateFlow } returns mutableScreenCaptureAllowedFlow
        every { isDynamicColorsEnabled } returns false
        every { isDynamicColorsEnabledFlow } returns mutableIsDynamicColorsEnabledFlow
    }
    private val fakeServerConfigRepository = FakeServerConfigRepository()

    @Test
    fun `on AppThemeChanged should update state`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
            eventFlow.skipItems(count = 2)
            assertEquals(
                DEFAULT_STATE,
                stateFlow.awaitItem(),
            )
            viewModel.trySendAction(MainAction.Internal.ThemeUpdate(theme = AppTheme.DARK))
            assertEquals(
                DEFAULT_STATE.copy(theme = AppTheme.DARK),
                stateFlow.awaitItem(),
            )
            assertEquals(
                MainEvent.UpdateAppTheme(osTheme = AppTheme.DARK.osValue),
                eventFlow.awaitItem(),
            )
        }

        verify {
            settingsRepository.appTheme
            settingsRepository.appThemeStateFlow
        }
    }

    @Test
    fun `on DynamicColorUpdate should update state`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE,
                awaitItem(),
            )
            viewModel.trySendAction(MainAction.Internal.DynamicColorUpdate(isEnabled = true))
            assertEquals(
                DEFAULT_STATE.copy(isDynamicColorsEnabled = true),
                awaitItem(),
            )
        }

        verify {
            settingsRepository.isDynamicColorsEnabled
            settingsRepository.isDynamicColorsEnabledFlow
        }
    }

    @Test
    fun `send NavigateToDebugMenu action when OpenDebugMenu action is sent`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            // Ignore the events that are fired off by flows in the ViewModel init
            skipItems(2)
            viewModel.trySendAction(MainAction.OpenDebugMenu)
            assertEquals(MainEvent.NavigateToDebugMenu, awaitItem())
        }
    }

    private fun createViewModel(): MainViewModel =
        MainViewModel(
            settingsRepository = settingsRepository,
            configRepository = fakeServerConfigRepository,
        )
}

private val DEFAULT_STATE = MainState(
    theme = AppTheme.DEFAULT,
    isDynamicColorsEnabled = false,
)
