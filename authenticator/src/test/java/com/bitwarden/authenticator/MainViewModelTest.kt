package com.bitwarden.authenticator

import app.cash.turbine.test
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.authenticator.data.platform.repository.util.FakeServerConfigRepository
import com.bitwarden.authenticator.ui.platform.base.BaseViewModelTest
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MainViewModelTest : BaseViewModelTest() {

    private val mutableAppThemeFlow = MutableStateFlow(AppTheme.DEFAULT)
    private val mutableScreenCaptureAllowedFlow = MutableStateFlow(false)
    private val settingsRepository = mockk<SettingsRepository> {
        every { appTheme } returns AppTheme.DEFAULT
        every { appThemeStateFlow } returns mutableAppThemeFlow
        every { isScreenCaptureAllowedStateFlow } returns mutableScreenCaptureAllowedFlow
    }
    private val fakeServerConfigRepository = FakeServerConfigRepository()
    private lateinit var mainViewModel: MainViewModel

    @BeforeEach
    fun setUp() {
        mainViewModel = MainViewModel(
            settingsRepository,
            fakeServerConfigRepository,
        )
    }

    @Test
    fun `on AppThemeChanged should update state`() {
        assertEquals(
            MainState(
                theme = AppTheme.DEFAULT,
            ),
            mainViewModel.stateFlow.value,
        )
        mainViewModel.trySendAction(
            MainAction.Internal.ThemeUpdate(
                theme = AppTheme.DARK,
            ),
        )
        assertEquals(
            MainState(
                theme = AppTheme.DARK,
            ),
            mainViewModel.stateFlow.value,
        )

        verify {
            settingsRepository.appTheme
            settingsRepository.appThemeStateFlow
        }
    }

    @Test
    fun `send NavigateToDebugMenu action when OpenDebugMenu action is sent`() = runTest {
        mainViewModel.trySendAction(MainAction.OpenDebugMenu)

        mainViewModel.eventFlow.test {
            awaitItem() // ignore first event
            assertEquals(MainEvent.NavigateToDebugMenu, awaitItem())
        }
    }
}
