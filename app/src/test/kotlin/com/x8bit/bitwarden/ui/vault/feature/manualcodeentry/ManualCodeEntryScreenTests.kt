package com.x8bit.bitwarden.ui.vault.feature.manualcodeentry

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.manager.util.startAppSettingsActivity
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.assertNoDialogExists
import com.bitwarden.ui.util.performCustomAccessibilityAction
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.manager.permissions.FakePermissionManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test

class ManualCodeEntryScreenTests : BitwardenComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToScanQrCodeCalled = false

    private val mutableEventFlow = bufferedMutableSharedFlow<ManualCodeEntryEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)

    private val fakePermissionManager: FakePermissionManager = FakePermissionManager()
    private val intentManager = mockk<IntentManager>(relaxed = true)

    private val viewModel = mockk<ManualCodeEntryViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        setContent(
            permissionsManager = fakePermissionManager,
            intentManager = intentManager,
        ) {
            ManualCodeEntryScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToQrCodeScreen = { onNavigateToScanQrCodeCalled = true },
            )
        }
    }

    @Test
    fun `on NavigateBack event should invoke onNavigateBack`() {
        mutableEventFlow.tryEmit(ManualCodeEntryEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `on NavigateToScanQrCode event should invoke NavigateToScanQrCode`() {
        mutableEventFlow.tryEmit(ManualCodeEntryEvent.NavigateToQrCodeScreen)
        assertTrue(onNavigateToScanQrCodeCalled)
    }

    @Test
    fun `on NavigateToAppSettings event should invoke intent handler`() {
        mockkStatic(IntentManager::startAppSettingsActivity) {
            every { intentManager.startAppSettingsActivity() } returns true
            mutableEventFlow.tryEmit(ManualCodeEntryEvent.NavigateToAppSettings)
            verify(exactly = 1) { intentManager.startAppSettingsActivity() }
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking on manual text should send ScanQrCodeTextClick if camera permission is granted`() {
        fakePermissionManager.checkPermissionResult = true

        composeTestRule
            .onNodeWithText(text = "Cannot add authenticator key? Scan QR Code")
            .performCustomAccessibilityAction(label = "Scan QR code")

        verify {
            viewModel.trySendAction(ManualCodeEntryAction.ScanQrCodeTextClick)
        }
    }

    @Test
    fun `dialog should be dismissed on dismiss click in settings dialog`() {
        fakePermissionManager.checkPermissionResult = false

        composeTestRule
            .onNodeWithText(text = "Cannot add authenticator key? Scan QR Code")
            .performCustomAccessibilityAction(label = "Scan QR code")

        composeTestRule
            .onAllNodesWithText("Enable camera permission to use the scanner")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("No thanks")
            .performClick()

        composeTestRule
            .onAllNodesWithText("Enable camera permission to use the scanner")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsNotDisplayed()
    }

    @Test
    fun `error dialog should be updated according to state`() {
        composeTestRule.assertNoDialogExists()

        mutableStateFlow.update {
            it.copy(
                dialog = ManualCodeEntryState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.authenticator_key_read_error.asText(),
                ),
            )
        }

        composeTestRule
            .onAllNodesWithText(text = "An error has occurred")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(text = "Cannot read authenticator key.")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(dialog = null)
        }

        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `error dialog Ok click should emit DialogDismiss`() {
        composeTestRule.assertNoDialogExists()

        mutableStateFlow.update {
            it.copy(
                dialog = ManualCodeEntryState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.authenticator_key_read_error.asText(),
                ),
            )
        }

        composeTestRule
            .onAllNodesWithText(text = "Okay")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(ManualCodeEntryAction.DialogDismiss)
        }
    }

    @Test
    fun `settings dialog should call SettingsClick action on confirm click`() {
        fakePermissionManager.checkPermissionResult = false

        composeTestRule
            .onNodeWithText(text = "Cannot add authenticator key? Scan QR Code")
            .performCustomAccessibilityAction(label = "Scan QR code")

        composeTestRule
            .onAllNodesWithText("Enable camera permission to use the scanner")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Settings")
            .performClick()

        verify {
            viewModel.trySendAction(ManualCodeEntryAction.SettingsClick)
        }
    }

    @Test
    fun `CodeTextChanged will be sent when text for code is updated`() {
        composeTestRule
            .onAllNodesWithText(text = "Authenticator key")
            .onFirst()
            .assertTextEquals("Authenticator key", "")

        composeTestRule
            .onAllNodesWithText(text = "Authenticator key")
            .onFirst()
            .performTextInput(text = "TestCode")

        verify {
            viewModel.trySendAction(
                ManualCodeEntryAction.CodeTextChange("TestCode"),
            )
        }
    }

    @Test
    fun `Authenticator key text should display the text provided by the state`() {
        composeTestRule
            .onAllNodesWithText(text = "Authenticator key")
            .onFirst()
            .assertTextEquals("Authenticator key", "")

        mutableStateFlow.update {
            it.copy(code = "TestCode")
        }

        composeTestRule
            .onAllNodesWithText(text = "Authenticator key")
            .onFirst()
            .assertTextEquals("Authenticator key", "TestCode")
    }

    @Test
    fun `clicking Add TOTP button should send CodeSubmit action`() {
        composeTestRule
            .onNodeWithText(text = "Add TOTP")
            .performClick()

        verify {
            viewModel.trySendAction(
                ManualCodeEntryAction.CodeSubmit,
            )
        }
    }
}

private val DEFAULT_STATE: ManualCodeEntryState = ManualCodeEntryState(
    code = "",
    dialog = null,
)
