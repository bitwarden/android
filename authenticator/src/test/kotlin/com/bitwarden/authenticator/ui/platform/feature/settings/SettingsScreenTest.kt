package com.bitwarden.authenticator.ui.platform.feature.settings

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.net.toUri
import com.bitwarden.authenticator.BuildConfig
import com.bitwarden.authenticator.ui.platform.base.AuthenticatorComposeTest
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppLanguage
import com.bitwarden.authenticator.ui.platform.feature.settings.data.model.DefaultSaveOption
import com.bitwarden.authenticator.ui.platform.manager.biometrics.BiometricsManager
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.assertNoDialogExists
import com.bitwarden.ui.util.concat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsScreenTest : AuthenticatorComposeTest() {

    private var onNavigateToTutorialCalled = false
    private var onNaviateToExportCalled = false
    private var onNavigateToImportCalled = false

    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mutableEventFlow = bufferedMutableSharedFlow<SettingsEvent>()

    val viewModel: SettingsViewModel = mockk {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
        every { trySendAction(any()) } just runs
    }

    private val biometricsManager: BiometricsManager = mockk {
        every { isBiometricsSupported } returns true
    }
    private val intentManager: IntentManager = mockk()

    @Before
    fun setup() {
        setContent(
            biometricsManager = biometricsManager,
            intentManager = intentManager,
        ) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToTutorial = { onNavigateToTutorialCalled = true },
                onNavigateToExport = { onNaviateToExportCalled = true },
                onNavigateToImport = { onNavigateToImportCalled = true },
            )
        }
    }

    @Test
    fun `Sync with Bitwarden row should be hidden when showSyncWithBitwarden is false`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            showSyncWithBitwarden = false,
        )
        composeTestRule.onNodeWithText("Sync with Bitwarden app").assertDoesNotExist()
    }

    @Test
    fun `Sync with Bitwarden row click should send SyncWithBitwardenClick action`() {
        composeTestRule
            .onNodeWithText("Sync with Bitwarden app")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(SettingsAction.DataClick.SyncWithBitwardenClick) }
    }

    @Test
    fun `on NavigateToBitwardenApp receive should launch bitwarden account security deep link`() {
        every { intentManager.startActivity(any()) } returns true
        val intentSlot = slot<Intent>()
        val expectedIntent = Intent(
            Intent.ACTION_VIEW,
            "bitwarden://settings/account_security".toUri(),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        mutableEventFlow.tryEmit(SettingsEvent.NavigateToBitwardenApp)
        verify { intentManager.startActivity(capture(intentSlot)) }
        assertEquals(
            expectedIntent.data,
            intentSlot.captured.data,
        )
        assertEquals(
            expectedIntent.flags,
            intentSlot.captured.flags,
        )
    }

    @Test
    fun `on NavigateToBitwardenPlayStoreListing receive launch Bitwarden Play Store URI`() {
        every { intentManager.launchUri(any()) } just runs
        mutableEventFlow.tryEmit(SettingsEvent.NavigateToBitwardenPlayStoreListing)
        verify {
            intentManager.launchUri(
                "https://play.google.com/store/apps/details?id=com.x8bit.bitwarden".toUri(),
            )
        }
    }

    @Test
    fun `on NavigateToSyncInformation receive launch sync totp uri`() {
        every { intentManager.launchUri(uri = any()) } just runs
        mutableEventFlow.tryEmit(SettingsEvent.NavigateToSyncInformation)
        verify(exactly = 1) {
            intentManager.launchUri("https://bitwarden.com/help/totp-sync".toUri())
        }
    }

    @Test
    fun `Default Save Option row should be hidden when showDefaultSaveOptionRow is false`() {
        mutableStateFlow.value = DEFAULT_STATE
        composeTestRule.onNodeWithText("Default save option").assertExists()

        mutableStateFlow.update {
            it.copy(
                showDefaultSaveOptionRow = false,
            )
        }
        composeTestRule.onNodeWithText("Default save option").assertDoesNotExist()
    }

    @Test
    @Suppress("MaxLineLength")
    fun `Default Save Option dialog should send DefaultSaveOptionUpdated when selection is made`() =
        runTest {
            val expectedSaveOption = DefaultSaveOption.BITWARDEN_APP
            mutableStateFlow.value = DEFAULT_STATE
            composeTestRule
                .onNodeWithText("Default save option")
                .performScrollTo()
                .performClick()

            // Make sure the dialog is showing:
            composeTestRule
                .onAllNodesWithText("Default save option")
                .filterToOne(hasAnyAncestor(isDialog()))
                .assertIsDisplayed()

            // Select updated option:
            composeTestRule
                .onNodeWithText("Save to Bitwarden")
                .assertIsDisplayed()
                .performClick()

            verify {
                viewModel.trySendAction(
                    SettingsAction.DataClick.DefaultSaveOptionUpdated(expectedSaveOption),
                )
            }

            // Make sure the dialog is not showing:
            composeTestRule
                .onNode(isDialog())
                .assertDoesNotExist()
        }

    @Test
    fun `on allow screen capture confirm should send AllowScreenCaptureToggle`() {
        composeTestRule.onNodeWithText("Allow screen capture").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Yes").performClick()
        composeTestRule.assertNoDialogExists()

        verify {
            viewModel.trySendAction(
                SettingsAction.SecurityClick.AllowScreenCaptureToggle(true),
            )
        }
    }

    @Test
    fun `on allow screen capture cancel should dismiss dialog`() {
        composeTestRule.onNodeWithText("Allow screen capture").performScrollTo().performClick()
        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `on allow screen capture row click should display confirm enable screen capture dialog`() {
        composeTestRule.onNodeWithText("Allow screen capture").performScrollTo().performClick()
        composeTestRule
            .onAllNodesWithText("Allow screen capture")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `on language row click should send display language selector dialog`() {
        composeTestRule.assertNoDialogExists()
        composeTestRule
            .onNodeWithContentDescription(label = "English. Language")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onAllNodesWithText(text = "Language")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `on language selected should emit LanguageChange event`() {
        composeTestRule.assertNoDialogExists()
        composeTestRule
            .onNodeWithContentDescription(label = "English. Language")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithText(text = "English (United Kingdom)")
            .performScrollTo()
            .performClick()
        composeTestRule.assertNoDialogExists()
        verify(exactly = 1) {
            viewModel.trySendAction(
                action = SettingsAction.AppearanceChange.LanguageChange(
                    language = AppLanguage.ENGLISH_BRITISH,
                ),
            )
        }
    }

    @Test
    fun `on use dynamic colors row click should send DynamicColorChange event`() {
        composeTestRule
            .onNodeWithText(text = "Use dynamic colors")
            .performScrollTo()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(SettingsAction.AppearanceChange.DynamicColorChange(true))
        }
    }

    @Test
    fun `Unlock with biometrics row should be hidden when hasBiometricsSupport is false`() {
        mutableStateFlow.value = DEFAULT_STATE
        composeTestRule
            .onNodeWithText("Use your device’s lock method to unlock the app")
            .assertExists()

        mutableStateFlow.update {
            it.copy(
                hasBiometricsSupport = false,
            )
        }
        composeTestRule
            .onNodeWithText("Use your device’s lock method to unlock the app")
            .assertDoesNotExist()
    }
}

private val APP_LANGUAGE = AppLanguage.ENGLISH
private val APP_THEME = AppTheme.DEFAULT
private val DEFAULT_SAVE_OPTION = DefaultSaveOption.NONE
private val DEFAULT_STATE = SettingsState(
    appearance = SettingsState.Appearance(
        language = APP_LANGUAGE,
        theme = APP_THEME,
        isDynamicColorsSupported = true,
        isDynamicColorsEnabled = false,
    ),
    isSubmitCrashLogsEnabled = true,
    isUnlockWithBiometricsEnabled = true,
    showSyncWithBitwarden = true,
    showDefaultSaveOptionRow = true,
    defaultSaveOption = DEFAULT_SAVE_OPTION,
    dialog = null,
    version = BitwardenString.version.asText()
        .concat(": ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})".asText()),
    copyrightInfo = "© Bitwarden Inc. 2015-2024".asText(),
    allowScreenCapture = false,
    hasBiometricsSupport = true,
)
