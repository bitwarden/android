package com.x8bit.bitwarden.ui.platform.feature.settings.appearance

import android.os.Build
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AppearanceViewModelTest : BaseViewModelTest() {
    private val mutableAppLanguageStateFlow = MutableStateFlow(AppLanguage.DEFAULT)
    private val mutableIsDynamicColorsEnabledFlow = MutableStateFlow(false)
    private val mockSettingsRepository = mockk<SettingsRepository> {
        every { appLanguage } returns AppLanguage.DEFAULT
        every { appTheme } returns AppTheme.DEFAULT
        every { appLanguage = AppLanguage.ENGLISH } just runs
        every { isIconLoadingDisabled } returns false
        every { isIconLoadingDisabled = true } just runs
        every { appTheme = AppTheme.DARK } just runs
        every { appLanguageStateFlow } returns mutableAppLanguageStateFlow
        every { isDynamicColorsEnabled } returns false
        every { isDynamicColorsEnabled = any() } just runs
        every { isDynamicColorsEnabledFlow } returns mutableIsDynamicColorsEnabledFlow
    }

    @BeforeEach
    fun setUp() {
        mockkStatic(::isBuildVersionAtLeast)
        every { isBuildVersionAtLeast(any()) } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::isBuildVersionAtLeast)
    }

    @Test
    fun `initial state should be correct when not set`() {
        val viewModel = createViewModel(state = null)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct when set`() {
        val state = DEFAULT_STATE.copy(theme = AppTheme.DARK)
        val viewModel = createViewModel(state = state)
        assertEquals(state, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct when build version is below 31`() {
        every { isBuildVersionAtLeast(Build.VERSION_CODES.S) } returns false
        val viewModel = createViewModel(state = null)
        assertEquals(
            DEFAULT_STATE.copy(isDynamicColorsSupported = false),
            viewModel.stateFlow.value,
        )

        verify(exactly = 0) {
            mockSettingsRepository.isDynamicColorsEnabledFlow
        }
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
    fun `on LanguageChange should store updated language in repository`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(AppearanceAction.LanguageChange(AppLanguage.ENGLISH))

        verify { mockSettingsRepository.appLanguage = AppLanguage.ENGLISH }
    }

    @Test
    fun `on AppLanguageStateFlow value updated, view model language state should change`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_STATE,
                    awaitItem(),
                )
                mutableAppLanguageStateFlow.update { AppLanguage.AFRIKAANS }
                assertEquals(
                    DEFAULT_STATE.copy(
                        language = AppLanguage.AFRIKAANS,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `on ShowWebsiteIconsToggle should update state and store the value`() = runTest {
        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE,
                awaitItem(),
            )

            viewModel.trySendAction(AppearanceAction.ShowWebsiteIconsToggle(false))
            assertEquals(
                DEFAULT_STATE.copy(showWebsiteIcons = false),
                awaitItem(),
            )

            // Since we negate the boolean in the ViewModel it should be true
            verify {
                mockSettingsRepository.isIconLoadingDisabled = true
            }
        }
    }

    @Test
    fun `on ShowWebsiteIconsTooltipClick should emit NavigateToWebsiteIconsHelp`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AppearanceAction.ShowWebsiteIconsTooltipClick)
            assertEquals(AppearanceEvent.NavigateToWebsiteIconsHelp, awaitItem())
        }
    }

    @Test
    fun `on ThemeChange should update state and set theme in SettingsRepository`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE,
                awaitItem(),
            )

            viewModel.trySendAction(AppearanceAction.ThemeChange(AppTheme.DARK))
            assertEquals(
                DEFAULT_STATE.copy(theme = AppTheme.DARK),
                awaitItem(),
            )
        }

        verify(exactly = 1) {
            mockSettingsRepository.appTheme
            mockSettingsRepository.appTheme = AppTheme.DARK
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on DynamicColorsStateFlow value updated, view model isDynamicColorsEnabled state should change`() =
        runTest {
            val viewModel = createViewModel(settingsRepository = mockSettingsRepository)
            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_STATE,
                    awaitItem(),
                )
                mutableIsDynamicColorsEnabledFlow.update { true }
                assertEquals(
                    DEFAULT_STATE.copy(isDynamicColorsEnabled = true),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `DynamicColorsToggle should update state and set isDynamicColorsEnabled in SettingsRepository when disabled`() =
        runTest {
            val viewModel = createViewModel()
                .also { it.trySendAction(AppearanceAction.DynamicColorsToggle(false)) }
            assertEquals(
                DEFAULT_STATE.copy(isDynamicColorsEnabled = false),
                viewModel.stateFlow.value,
            )
            verify { mockSettingsRepository.isDynamicColorsEnabled = false }
        }

    @Test
    fun `DynamicColorsToggle should update state to show dialog when enabled`() = runTest {
        val viewModel = createViewModel()
            .also { it.trySendAction(AppearanceAction.DynamicColorsToggle(true)) }
        assertEquals(
            DEFAULT_STATE.copy(dialogState = AppearanceState.DialogState.EnableDynamicColors),
            viewModel.stateFlow.value,
        )
        verify(exactly = 0) { mockSettingsRepository.isDynamicColorsEnabled = any() }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ConfirmEnableDynamicColorsClick should update state and set isDynamicColorsEnabled in SettingsRepository`() =
        runTest {
            val viewModel = createViewModel(
                DEFAULT_STATE.copy(dialogState = AppearanceState.DialogState.EnableDynamicColors),
            )
                .also { it.trySendAction(AppearanceAction.ConfirmEnableDynamicColorsClick) }
            assertEquals(
                DEFAULT_STATE.copy(isDynamicColorsEnabled = true),
                viewModel.stateFlow.value,
            )
            verify { mockSettingsRepository.isDynamicColorsEnabled = true }
        }

    @Test
    fun `DismissDialog should update state to hide dialog`() = runTest {
        val viewModel = createViewModel(
            DEFAULT_STATE.copy(dialogState = AppearanceState.DialogState.EnableDynamicColors),
        )
            .also { it.trySendAction(AppearanceAction.DismissDialog) }
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        // Verify that isDynamicColorsEnabled was not changed
        verify(exactly = 0) { mockSettingsRepository.isDynamicColorsEnabled = any() }
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
            showWebsiteIcons = true,
            theme = AppTheme.DEFAULT,
            isDynamicColorsSupported = true,
            isDynamicColorsEnabled = false,
            dialogState = null,
        )
    }
}
