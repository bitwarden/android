package com.bitwarden.authenticator.ui.platform.feature.settings

import android.os.Build
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.authenticator.BuildConfig
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.authenticator.repository.model.SharedVerificationCodesState
import com.bitwarden.authenticator.data.authenticator.repository.util.isSyncWithBitwardenEnabled
import com.bitwarden.authenticator.data.platform.manager.clipboard.BitwardenClipboardManager
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppLanguage
import com.bitwarden.authenticator.ui.platform.feature.settings.data.model.DefaultSaveOption
import com.bitwarden.authenticator.ui.platform.model.SnackbarRelay
import com.bitwarden.authenticatorbridge.manager.AuthenticatorBridgeManager
import com.bitwarden.authenticatorbridge.manager.model.AccountSyncState
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SettingsViewModelTest : BaseViewModelTest() {

    private val authenticatorBridgeManager: AuthenticatorBridgeManager = mockk {
        every { accountSyncStateFlow } returns MutableStateFlow(AccountSyncState.Loading)
    }

    private val mutableSharedCodesFlow = MutableStateFlow(MOCK_SHARED_CODES_STATE)
    private val authenticatorRepository: AuthenticatorRepository = mockk {
        every { sharedCodesStateFlow } returns mutableSharedCodesFlow
    }
    private val mutableDefaultSaveOptionFlow = bufferedMutableSharedFlow<DefaultSaveOption>()
    private val mutableScreenCaptureAllowedStateFlow = MutableStateFlow(false)
    private val mutableIsDynamicColorsEnabledFlow = MutableStateFlow(false)
    private val mutableIsUnlockWithBiometricsEnabledFlow = MutableStateFlow(true)
    private val settingsRepository: SettingsRepository = mockk {
        every { appLanguage } returns APP_LANGUAGE
        every { appTheme } returns APP_THEME
        every { defaultSaveOption } returns DEFAULT_SAVE_OPTION
        every { defaultSaveOptionFlow } returns mutableDefaultSaveOptionFlow
        every { isUnlockWithBiometricsEnabled } returns true
        every { isCrashLoggingEnabled } returns true
        every { isScreenCaptureAllowedStateFlow } returns mutableScreenCaptureAllowedStateFlow
        every { isScreenCaptureAllowed } answers { mutableScreenCaptureAllowedStateFlow.value }
        every { isScreenCaptureAllowed = any() } just runs
        every { isDynamicColorsEnabled } answers { mutableIsDynamicColorsEnabledFlow.value }
        every { isDynamicColorsEnabled = any() } just runs
        every { isDynamicColorsEnabledFlow } returns mutableIsDynamicColorsEnabledFlow
        every { isUnlockWithBiometricsEnabledFlow } returns mutableIsUnlockWithBiometricsEnabledFlow
    }
    private val clipboardManager: BitwardenClipboardManager = mockk()
    private val mutableSnackbarFlow = bufferedMutableSharedFlow<BitwardenSnackbarData>()
    private val snackbarRelayManager = mockk<SnackbarRelayManager<SnackbarRelay>> {
        every {
            getSnackbarDataFlow(relay = any(), relays = anyVararg())
        } returns mutableSnackbarFlow
    }

    @BeforeEach
    fun setup() {
        mockkStatic(SharedVerificationCodesState::isSyncWithBitwardenEnabled)
        every { MOCK_SHARED_CODES_STATE.isSyncWithBitwardenEnabled } returns false
        mockkStatic(::isBuildVersionAtLeast)
        every { isBuildVersionAtLeast(Build.VERSION_CODES.S) } returns true
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(SharedVerificationCodesState::isSyncWithBitwardenEnabled)
        unmockkStatic(::isBuildVersionAtLeast)
    }

    @Test
    fun `when SnackbarRelay flow updates, snackbar is shown`() = runTest {
        val viewModel = createViewModel()
        val expectedSnackbarData = BitwardenSnackbarData(message = "test message".asText())
        viewModel.eventFlow.test {
            mutableSnackbarFlow.tryEmit(expectedSnackbarData)
            assertEquals(SettingsEvent.ShowSnackbar(expectedSnackbarData), awaitItem())
        }
    }

    @Test
    fun `initialState should be correct when saved state is null but OS version is too low`() {
        every {
            authenticatorBridgeManager.accountSyncStateFlow
        } returns MutableStateFlow(AccountSyncState.OsVersionNotSupported)
        val viewModel = createViewModel(savedState = null)
        val expectedState = DEFAULT_STATE.copy(
            showSyncWithBitwarden = false,
            showDefaultSaveOptionRow = false,
        )
        assertEquals(
            expectedState,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `initialState should be correct when saved state is null and OS version is supported`() {
        every {
            authenticatorBridgeManager.accountSyncStateFlow
        } returns MutableStateFlow(AccountSyncState.Loading)
        val viewModel = createViewModel(savedState = null)
        val expectedState = DEFAULT_STATE.copy(
            showSyncWithBitwarden = true,
        )
        assertEquals(
            expectedState,
            viewModel.stateFlow.value,
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `on SyncWithBitwardenClick receive with AccountSyncState AppNotInstalled should emit NavigateToBitwardenPlayStoreListing`() =
        runTest {
            every {
                authenticatorBridgeManager.accountSyncStateFlow
            } returns MutableStateFlow(AccountSyncState.AppNotInstalled)
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(SettingsAction.DataClick.SyncWithBitwardenClick)
                assertEquals(
                    SettingsEvent.NavigateToBitwardenPlayStoreListing,
                    awaitItem(),
                )
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `on SyncWithBitwardenClick receive with AccountSyncState not AppNotInstalled should emit NavigateToBitwardenApp`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(SettingsAction.DataClick.SyncWithBitwardenClick)
                assertEquals(
                    SettingsEvent.NavigateToBitwardenApp,
                    awaitItem(),
                )
            }
        }

    @Test
    fun `on SyncLearnMoreClick should emit NavigateToSyncInformation`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsAction.DataClick.SyncLearnMoreClick)
            assertEquals(SettingsEvent.NavigateToSyncInformation, awaitItem())
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `Default save option row should only show when shared codes state shows syncing as enabled`() =
        runTest {
            val viewModel = createViewModel()
            val enabledState: SharedVerificationCodesState = mockk {
                every { isSyncWithBitwardenEnabled } returns true
            }
            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_STATE,
                    awaitItem(),
                )
                mutableSharedCodesFlow.update { enabledState }
                assertEquals(
                    DEFAULT_STATE.copy(
                        showDefaultSaveOptionRow = true,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `on DefaultSaveOptionUpdated should update SettingsRepository`() {
        val expectedOption = DefaultSaveOption.BITWARDEN_APP
        every { settingsRepository.defaultSaveOption = expectedOption } just runs
        val viewModel = createViewModel()
        viewModel.trySendAction(SettingsAction.DataClick.DefaultSaveOptionUpdated(expectedOption))
        verify { settingsRepository.defaultSaveOption = expectedOption }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `Default save option should update when repository emits`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())

            mutableDefaultSaveOptionFlow.emit(DefaultSaveOption.LOCAL)
            assertEquals(
                DEFAULT_STATE.copy(
                    defaultSaveOption = DefaultSaveOption.LOCAL,
                ),
                awaitItem(),
            )

            mutableDefaultSaveOptionFlow.emit(DefaultSaveOption.BITWARDEN_APP)
            assertEquals(
                DEFAULT_STATE.copy(
                    defaultSaveOption = DefaultSaveOption.BITWARDEN_APP,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `on AllowScreenCaptureToggled should update value in state and SettingsRepository`() =
        runTest {
            val viewModel = createViewModel()
            val newScreenCaptureAllowedValue = true

            viewModel.trySendAction(
                SettingsAction.SecurityClick.AllowScreenCaptureToggle(
                    newScreenCaptureAllowedValue,
                ),
            )

            verify(exactly = 1) {
                settingsRepository.isScreenCaptureAllowed = newScreenCaptureAllowedValue
            }

            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_STATE.copy(allowScreenCapture = true),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `on DynamicColorChange should update value in state and SettingsRepository`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.trySendAction(
                SettingsAction.AppearanceChange.DynamicColorChange(isEnabled = true),
            )

            verify(exactly = 1) {
                settingsRepository.isDynamicColorsEnabled = true
            }
        }

    @Test
    fun `on DynamicColorsUpdated should update value in state and SettingsRepository`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.trySendAction(SettingsAction.Internal.DynamicColorsUpdated(isEnabled = true))

            assertEquals(
                DEFAULT_STATE.copy(
                    appearance = DEFAULT_APPEARANCE_STATE.copy(isDynamicColorsEnabled = true),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `on BiometricSupportChanged should update value in state`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.trySendAction(
                SettingsAction.BiometricSupportChanged(isBiometricsSupported = false),
            )

            assertEquals(
                DEFAULT_STATE.copy(
                    hasBiometricsSupport = false,
                ),
                viewModel.stateFlow.value,
            )
        }

    private fun createViewModel(
        savedState: SettingsState? = DEFAULT_STATE,
    ) = SettingsViewModel(
        savedStateHandle = SavedStateHandle().apply { this["state"] = savedState },
        clock = CLOCK,
        authenticatorBridgeManager = authenticatorBridgeManager,
        authenticatorRepository = authenticatorRepository,
        settingsRepository = settingsRepository,
        clipboardManager = clipboardManager,
        snackbarRelayManager = snackbarRelayManager,
    )
}

private val MOCK_SHARED_CODES_STATE: SharedVerificationCodesState = mockk()
private val APP_LANGUAGE = AppLanguage.ENGLISH
private val APP_THEME = AppTheme.DEFAULT
private val CLOCK = Clock.fixed(
    Instant.parse("2024-10-12T12:00:00Z"),
    ZoneOffset.UTC,
)
private val DEFAULT_SAVE_OPTION = DefaultSaveOption.NONE
private val DEFAULT_APPEARANCE_STATE = SettingsState.Appearance(
    language = APP_LANGUAGE,
    theme = APP_THEME,
    isDynamicColorsSupported = true,
    isDynamicColorsEnabled = false,
)
private val DEFAULT_STATE = SettingsState(
    appearance = DEFAULT_APPEARANCE_STATE,
    isSubmitCrashLogsEnabled = true,
    isUnlockWithBiometricsEnabled = true,
    showSyncWithBitwarden = true,
    showDefaultSaveOptionRow = false,
    defaultSaveOption = DEFAULT_SAVE_OPTION,
    dialog = null,
    version = BitwardenString.version.asText()
        .concat(": ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})".asText()),
    copyrightInfo = "© Bitwarden Inc. 2015-2024".asText(),
    allowScreenCapture = false,
    hasBiometricsSupport = true,
)
