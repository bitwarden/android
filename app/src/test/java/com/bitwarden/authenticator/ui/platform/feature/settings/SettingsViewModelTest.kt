package com.bitwarden.authenticator.ui.platform.feature.settings

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.authenticator.BuildConfig
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.platform.manager.FeatureFlagManager
import com.bitwarden.authenticator.data.platform.manager.clipboard.BitwardenClipboardManager
import com.bitwarden.authenticator.data.platform.manager.model.LocalFeatureFlag
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.authenticator.ui.platform.base.BaseViewModelTest
import com.bitwarden.authenticator.ui.platform.base.util.asText
import com.bitwarden.authenticator.ui.platform.base.util.concat
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppLanguage
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.authenticatorbridge.manager.AuthenticatorBridgeManager
import com.bitwarden.authenticatorbridge.manager.model.AccountSyncState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SettingsViewModelTest : BaseViewModelTest() {

    private val authenticatorBridgeManager: AuthenticatorBridgeManager = mockk {
        every { accountSyncStateFlow } returns MutableStateFlow(AccountSyncState.Loading)
    }
    private val settingsRepository: SettingsRepository = mockk {
        every { appLanguage } returns APP_LANGUAGE
        every { appTheme } returns APP_THEME
        every { isUnlockWithBiometricsEnabled } returns true
        every { isCrashLoggingEnabled } returns true
    }
    private val clipboardManager: BitwardenClipboardManager = mockk()
    private val featureFlagManager: FeatureFlagManager = mockk {
        every { getFeatureFlag(LocalFeatureFlag.PasswordManagerSync) } returns true
    }

    @Test
    @Suppress("MaxLineLength")
    fun `initialState should be correct when saved state is null and password manager feature flag is off`() {
        every {
            featureFlagManager.getFeatureFlag(LocalFeatureFlag.PasswordManagerSync)
        } returns false
        val viewModel = createViewModel(savedState = null)
        val expectedState = DEFAULT_STATE.copy(
            showSyncWithBitwarden = false,
        )
        assertEquals(
            expectedState,
            viewModel.stateFlow.value,
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `initialState should be correct when saved state is null and password manager feature flag is on but OS version is too low`() {
        every {
            authenticatorBridgeManager.accountSyncStateFlow
        } returns MutableStateFlow(AccountSyncState.OsVersionNotSupported)
        val viewModel = createViewModel(savedState = null)
        val expectedState = DEFAULT_STATE.copy(
            showSyncWithBitwarden = false,
        )
        assertEquals(
            expectedState,
            viewModel.stateFlow.value,
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `initialState should be correct when saved state is null and password manager feature flag is on and OS version is supported`() {
        every {
            authenticatorBridgeManager.accountSyncStateFlow
        } returns MutableStateFlow(AccountSyncState.Loading)
        every {
            featureFlagManager.getFeatureFlag(LocalFeatureFlag.PasswordManagerSync)
        } returns true
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

    private fun createViewModel(
        savedState: SettingsState? = DEFAULT_STATE,
    ) = SettingsViewModel(
        savedStateHandle = SavedStateHandle().apply { this["state"] = savedState },
        clock = CLOCK,
        authenticatorBridgeManager = authenticatorBridgeManager,
        settingsRepository = settingsRepository,
        clipboardManager = clipboardManager,
        featureFlagManager = featureFlagManager,
    )
}

private val APP_LANGUAGE = AppLanguage.ENGLISH
private val APP_THEME = AppTheme.DEFAULT
private val CLOCK = Clock.fixed(
    Instant.parse("2024-10-12T12:00:00Z"),
    ZoneOffset.UTC,
)
private val DEFAULT_STATE = SettingsState(
    appearance = SettingsState.Appearance(
        APP_LANGUAGE,
        APP_THEME,
    ),
    isSubmitCrashLogsEnabled = true,
    isUnlockWithBiometricsEnabled = true,
    showSyncWithBitwarden = true,
    dialog = null,
    version = R.string.version.asText()
        .concat(": ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})".asText()),
    copyrightInfo = "© Bitwarden Inc. 2015-2024".asText(),
)
