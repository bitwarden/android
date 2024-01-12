package com.x8bit.bitwarden.ui.platform.feature.settings.appearance

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AppearanceViewModelTest : BaseViewModelTest() {
    private val mockSettingsRepository = mockk<SettingsRepository> {
        every { appLanguage } returns AppLanguage.DEFAULT
        every { appLanguage = AppLanguage.ENGLISH } just runs
    }

    @BeforeEach
    fun setup() {
        mockkStatic(AppCompatDelegate::setApplicationLocales)
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(AppCompatDelegate::setApplicationLocales)
    }

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
    fun `on LanguageChange should update state and store language`() = runTest {
        val viewModel = createViewModel(
            settingsRepository = mockSettingsRepository,
        )
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE,
                awaitItem(),
            )
            viewModel.trySendAction(
                AppearanceAction.LanguageChange(AppLanguage.ENGLISH),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    language = AppLanguage.ENGLISH,
                ),
                awaitItem(),
            )
        }
        verify {
            AppCompatDelegate.setApplicationLocales(any())
            mockSettingsRepository.appLanguage
            mockSettingsRepository.appLanguage = AppLanguage.ENGLISH
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
        settingsRepository: SettingsRepository = mockSettingsRepository,
    ) = AppearanceViewModel(
        savedStateHandle = SavedStateHandle().apply {
            set("state", state)
        },
        settingsRepository = settingsRepository,
    )

    companion object {
        private val DEFAULT_STATE = AppearanceState(
            language = AppLanguage.DEFAULT,
            showWebsiteIcons = false,
            theme = AppearanceState.Theme.DEFAULT,
        )
    }
}
