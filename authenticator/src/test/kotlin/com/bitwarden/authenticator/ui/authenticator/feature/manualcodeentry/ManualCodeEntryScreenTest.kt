package com.bitwarden.authenticator.ui.authenticator.feature.manualcodeentry

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.bitwarden.authenticator.ui.platform.base.AuthenticatorComposeTest
import com.bitwarden.authenticator.ui.platform.manager.permissions.FakePermissionManager
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.manager.util.startAppSettingsActivity
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.assertNoDialogExists
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ManualCodeEntryScreenTest : AuthenticatorComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToQrCodeScreenCalled = false

    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mutableEventFlow = bufferedMutableSharedFlow<ManualCodeEntryEvent>()

    private val viewModel: ManualCodeEntryViewModel = mockk {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
        every { trySendAction(any()) } just runs
    }

    private val intentManager: IntentManager = mockk {
        every { startActivity(intent = any()) } returns true
    }
    private val permissionsManager = FakePermissionManager()

    @Before
    fun setup() {
        setContent(
            intentManager = intentManager,
            permissionsManager = permissionsManager,
        ) {
            ManualCodeEntryScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToQrCodeScreen = { onNavigateToQrCodeScreenCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `on NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(ManualCodeEntryEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `on NavigateToQrCodeScreen should call onNavigateToQrCodeScreen`() {
        mutableEventFlow.tryEmit(ManualCodeEntryEvent.NavigateToQrCodeScreen)
        assertTrue(onNavigateToQrCodeScreenCalled)
    }

    @Test
    fun `on NavigateToAppSettings should call intentManager`() {
        mockkStatic(IntentManager::startAppSettingsActivity) {
            every { intentManager.startAppSettingsActivity() } returns true
            mutableEventFlow.tryEmit(ManualCodeEntryEvent.NavigateToAppSettings)
            verify(exactly = 1) { intentManager.startAppSettingsActivity() }
        }
    }

    @Test
    fun `on Close click should emit CloseClick`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Close")
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(ManualCodeEntryAction.CloseClick)
        }
    }

    @Test
    fun `on Add code click should emit SaveLocallyClick`() {
        composeTestRule
            .onNodeWithText("Add code")
            .performScrollTo()
            .performClick()

        // Make sure save to bitwarden isn't showing:
        composeTestRule
            .onNodeWithText("Add code to Bitwarden")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(text = "Save here")
            .assertDoesNotExist()

        verify { viewModel.trySendAction(ManualCodeEntryAction.SaveLocallyClick) }
    }

    @Test
    fun `on Save here click should emit SaveToBitwardenClick`() {
        mutableStateFlow.update {
            it.copy(buttonState = ManualCodeEntryState.ButtonState.SaveToBitwardenPrimary)
        }
        composeTestRule
            .onNodeWithText("Save to Bitwarden")
            .performScrollTo()
            .performClick()

        // Make sure locally only save isn't showing:
        composeTestRule
            .onNodeWithText("Add code")
            .assertDoesNotExist()

        // Make sure locally option is showing:
        composeTestRule
            .onNodeWithText("Save here")
            .assertIsDisplayed()

        verify { viewModel.trySendAction(ManualCodeEntryAction.SaveToBitwardenClick) }
    }

    @Test
    fun `on Save here click should emit SaveLocallyClick`() {
        mutableStateFlow.update {
            it.copy(buttonState = ManualCodeEntryState.ButtonState.SaveLocallyPrimary)
        }
        composeTestRule
            .onNodeWithText("Save here")
            .performClick()

        // Make sure locally only save isn't showing:
        composeTestRule
            .onNodeWithText("Add code")
            .assertDoesNotExist()

        // Make sure save to bitwarden option is showing:
        composeTestRule
            .onNodeWithText("Save to Bitwarden")
            .assertIsDisplayed()

        verify { viewModel.trySendAction(ManualCodeEntryAction.SaveLocallyClick) }
    }

    @Test
    fun `on Scan QR code click with permission should emit ScanQrCodeTextClick`() {
        permissionsManager.checkPermissionResult = true
        composeTestRule
            .onNodeWithText(text = "Scan QR code")
            .performScrollTo()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(ManualCodeEntryAction.ScanQrCodeTextClick)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on Scan QR code click without permission and permission is not granted should display dialog`() {
        permissionsManager.checkPermissionResult = false
        permissionsManager.getPermissionsResult = false
        composeTestRule
            .onNodeWithText(text = "Scan QR code")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNodeWithText(text = "Enable camera permission to use the scanner")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(text = "No thanks")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()

        verify(exactly = 0) {
            viewModel.trySendAction(any())
        }
    }

    @Test
    fun `on permission dialog Settings click should emit SettingsClick`() {
        permissionsManager.checkPermissionResult = false
        permissionsManager.getPermissionsResult = false
        composeTestRule
            .onNodeWithText(text = "Scan QR code")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNodeWithText(text = "Settings")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(ManualCodeEntryAction.SettingsClick)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on Scan QR code click without permission and permission is granted should emit ScanQrCodeTextClick`() {
        permissionsManager.checkPermissionResult = false
        permissionsManager.getPermissionsResult = true
        composeTestRule
            .onNodeWithText(text = "Scan QR code")
            .performScrollTo()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(ManualCodeEntryAction.ScanQrCodeTextClick)
        }
    }

    @Test
    fun `on dialog should updates according to state`() {
        composeTestRule.assertNoDialogExists()
        val loadingMessage = "Loading!"
        mutableStateFlow.update {
            it.copy(
                dialog = ManualCodeEntryState.DialogState.Loading(
                    message = loadingMessage.asText(),
                ),
            )
        }
        composeTestRule.onNodeWithText(text = loadingMessage).assert(hasAnyAncestor(isDialog()))

        val errorMessage = "Error!"
        mutableStateFlow.update {
            it.copy(
                dialog = ManualCodeEntryState.DialogState.Error(message = errorMessage.asText()),
            )
        }
        composeTestRule.onNodeWithText(text = errorMessage).assert(hasAnyAncestor(isDialog()))

        mutableStateFlow.update { it.copy(dialog = null) }
        composeTestRule.assertNoDialogExists()
    }
}

private val DEFAULT_STATE = ManualCodeEntryState(
    code = "",
    issuer = "",
    dialog = null,
    buttonState = ManualCodeEntryState.ButtonState.LocalOnly,
)
