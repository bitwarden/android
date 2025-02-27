package com.bitwarden.authenticator.ui.platform.feature.settings

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.net.toUri
import com.bitwarden.authenticator.BuildConfig
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.platform.repository.util.bufferedMutableSharedFlow
import com.bitwarden.authenticator.ui.platform.base.BaseComposeTest
import com.bitwarden.authenticator.ui.platform.base.util.asText
import com.bitwarden.authenticator.ui.platform.base.util.concat
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppLanguage
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.authenticator.ui.platform.feature.settings.data.model.DefaultSaveOption
import com.bitwarden.authenticator.ui.platform.manager.biometrics.BiometricsManager
import com.bitwarden.authenticator.ui.platform.manager.intent.IntentManager
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

class SettingsScreenTest : BaseComposeTest() {

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
        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                biometricsManager = biometricsManager,
                intentManager = intentManager,
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
        every { intentManager.startActivity(any()) } just runs
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
    fun `Default Save Option dialog should send DefaultSaveOptionUpdated when confirm is clicked`() =
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

            // Click confirm:
            composeTestRule
                .onNodeWithText("Confirm")
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
}

private val APP_LANGUAGE = AppLanguage.ENGLISH
private val APP_THEME = AppTheme.DEFAULT
private val DEFAULT_SAVE_OPTION = DefaultSaveOption.NONE
private val DEFAULT_STATE = SettingsState(
    appearance = SettingsState.Appearance(
        APP_LANGUAGE,
        APP_THEME,
    ),
    isSubmitCrashLogsEnabled = true,
    isUnlockWithBiometricsEnabled = true,
    showSyncWithBitwarden = true,
    showDefaultSaveOptionRow = true,
    defaultSaveOption = DEFAULT_SAVE_OPTION,
    dialog = null,
    version = R.string.version.asText()
        .concat(": ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})".asText()),
    copyrightInfo = "© Bitwarden Inc. 2015-2024".asText(),
)
