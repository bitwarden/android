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
    private val settingsRepository = mockk<SettingsRepository> {
        every { appTheme } returns AppTheme.DEFAULT
        every { appThemeStateFlow } returns mutableAppThemeFlow
        every { isScreenCaptureAllowedStateFlow } returns mutableScreenCaptureAllowedFlow
        every { isScreenCaptureAllowed } returns false
    }
    private val fakeServerConfigRepository = FakeServerConfigRepository()
    private val mainViewModel: MainViewModel = MainViewModel(
        settingsRepository = settingsRepository,
        configRepository = fakeServerConfigRepository,
    )

    @Test
    fun `on AppThemeChanged should update state`() = runTest {
        mainViewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
            eventFlow.skipItems(count = 1)
            assertEquals(
                MainState(
                    theme = AppTheme.DEFAULT,
                    isScreenCaptureAllowed = false,
                ),
                stateFlow.awaitItem(),
            )
            mainViewModel.trySendAction(MainAction.Internal.ThemeUpdate(theme = AppTheme.DARK))
            assertEquals(
                MainState(
                    theme = AppTheme.DARK,
                    isScreenCaptureAllowed = false,
                ),
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
    fun `send NavigateToDebugMenu action when OpenDebugMenu action is sent`() = runTest {
        mainViewModel.eventFlow.test {
            // Ignore the events that are fired off by flows in the ViewModel init
            skipItems(1)
            mainViewModel.trySendAction(MainAction.OpenDebugMenu)
            assertEquals(MainEvent.NavigateToDebugMenu, awaitItem())
        }
    }

    @Test
    fun `changes in the allowed screen capture value should update the state`() {
        val viewModel = createViewModel()

        assertEquals(
            MainState(
                theme = AppTheme.DEFAULT,
                isScreenCaptureAllowed = false,
            ),
            viewModel.stateFlow.value,
        )

        mutableScreenCaptureAllowedFlow.value = true

        assertEquals(
            MainState(
                theme = AppTheme.DEFAULT,
                isScreenCaptureAllowed = true,
            ),
            viewModel.stateFlow.value,
        )
    }

    private fun createViewModel() = MainViewModel(
        settingsRepository = settingsRepository,
        configRepository = fakeServerConfigRepository,
    )
}
