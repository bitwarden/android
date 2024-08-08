package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.components.toggle.UnlockWithPinState
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricsManager
import com.x8bit.bitwarden.ui.util.assertNoDialogExists
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config

class SetupUnlockScreenTest : BaseComposeTest() {

    private var onNavigateToSetupAutofillCalled = false

    private val biometricsManager: BiometricsManager = mockk {
        every { isBiometricsSupported } returns true
    }

    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mutableEventFlow = bufferedMutableSharedFlow<SetupUnlockEvent>()
    private val viewModel = mockk<SetupUnlockViewModel> {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
        every { trySendAction(action = any()) } just runs
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            SetupUnlockScreen(
                onNavigateToSetupAutofill = { onNavigateToSetupAutofillCalled = true },
                viewModel = viewModel,
                biometricsManager = biometricsManager,
            )
        }
    }

    @Config(qualifiers = "land")
    @Test
    fun `header should display in landscape mode`() {
        composeTestRule
            .onNodeWithText(text = "Set up unlock")
            .performScrollTo()
            .assertExists()
            .assertIsDisplayed()

        @Suppress("MaxLineLength")
        composeTestRule
            .onNodeWithText(
                text = "Set up biometrics or choose a PIN code to quickly access your vault and AutoFill your logins.",
            )
            .performScrollTo()
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `NavigateToSetupAutofill event should invoke the navigate to autofill lambda`() {
        mutableEventFlow.tryEmit(SetupUnlockEvent.NavigateToSetupAutofill)
        assertTrue(onNavigateToSetupAutofillCalled)
    }

    @Test
    fun `on unlock with biometrics should be toggled on or off according to state`() {
        composeTestRule.onNodeWithText(text = "Unlock with Biometrics").assertIsOff()
        mutableStateFlow.update { it.copy(isUnlockWithBiometricsEnabled = true) }
        composeTestRule.onNodeWithText(text = "Unlock with Biometrics").assertIsOn()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on unlock with biometrics toggle should send EnableBiometricsClick when isUnlockWithBiometricsEnabled is false`() {
        composeTestRule
            .onNodeWithText(text = "Unlock with Biometrics")
            .performScrollTo()
            .assertIsOff()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(SetupUnlockAction.EnableBiometricsClick)
        }
    }

    @Test
    fun `on unlock with biometrics toggle should send UnlockWithBiometricToggle`() {
        mutableStateFlow.update { it.copy(isUnlockWithBiometricsEnabled = true) }
        composeTestRule
            .onNodeWithText(text = "Unlock with Biometrics")
            .performScrollTo()
            .assertIsOn()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(SetupUnlockAction.UnlockWithBiometricToggle(isEnabled = false))
        }
    }

    @Test
    fun `on unlock with pin code should be toggled on or off according to state`() {
        composeTestRule.onNodeWithText(text = "Unlock with PIN code").assertIsOff()
        mutableStateFlow.update { it.copy(isUnlockWithPinEnabled = true) }
        composeTestRule.onNodeWithText(text = "Unlock with PIN code").assertIsOn()
    }

    @Test
    fun `on unlock with pin toggle when enabled should send UnlockWithPinToggle Disabled`() {
        mutableStateFlow.update {
            it.copy(isUnlockWithPinEnabled = true)
        }

        composeTestRule
            .onNodeWithText(text = "Unlock with PIN code")
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                SetupUnlockAction.UnlockWithPinToggle(UnlockWithPinState.Disabled),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on unlock with pin toggle when disabled should show the PIN input dialog and send UnlockWithPinToggle PendingEnabled`() {
        mutableStateFlow.update {
            it.copy(isUnlockWithPinEnabled = false)
        }

        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithText(text = "Unlock with PIN code")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Enter your PIN code.")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(
                text = "Set your PIN code for unlocking Bitwarden. Your PIN settings will be reset if " +
                    "you ever fully log out of the application.",
            )
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(text = "PIN")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(text = "Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(text = "Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        verify {
            viewModel.trySendAction(
                SetupUnlockAction.UnlockWithPinToggle(UnlockWithPinState.PendingEnabled),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PIN input dialog Cancel click should clear the dialog and send UnlockWithPinToggle Disabled`() {
        mutableStateFlow.update {
            it.copy(isUnlockWithPinEnabled = false)
        }
        composeTestRule
            .onNodeWithText(text = "Unlock with PIN code")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                SetupUnlockAction.UnlockWithPinToggle(UnlockWithPinState.Disabled),
            )
        }
        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PIN input dialog Submit click with empty pin should clear the dialog and send UnlockWithPinToggle Disabled`() {
        mutableStateFlow.update {
            it.copy(isUnlockWithPinEnabled = false)
        }
        composeTestRule
            .onNodeWithText(text = "Unlock with PIN code")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                SetupUnlockAction.UnlockWithPinToggle(UnlockWithPinState.Disabled),
            )
        }
        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PIN input dialog Submit click with non-empty pin and isUnlockWithPasswordEnabled true should show a confirmation dialog and send UnlockWithPinToggle PendingEnabled`() {
        mutableStateFlow.update {
            it.copy(isUnlockWithPinEnabled = false)
        }
        composeTestRule
            .onNodeWithText(text = "Unlock with PIN code")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "PIN")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performTextInput(text = "1234")
        composeTestRule
            .onAllNodesWithText(text = "Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Unlock with PIN code")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(
                text = "Do you want to require unlocking with your master password when the application " +
                    "is restarted?",
            )
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(text = "Yes")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(text = "No")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        verify {
            viewModel.trySendAction(
                SetupUnlockAction.UnlockWithPinToggle(UnlockWithPinState.PendingEnabled),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PIN input dialog Submit click with non-empty pin and isUnlockWithPasswordEnabled false should show a confirmation dialog and send UnlockWithPinToggle Enabled`() {
        mutableStateFlow.update {
            it.copy(
                isUnlockWithPinEnabled = false,
                isUnlockWithPasswordEnabled = false,
            )
        }
        composeTestRule
            .onNodeWithText(text = "Unlock with PIN code")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "PIN")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performTextInput(text = "1234")
        composeTestRule
            .onAllNodesWithText(text = "Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()

        verify {
            viewModel.trySendAction(
                SetupUnlockAction.UnlockWithPinToggle(
                    UnlockWithPinState.Enabled(
                        pin = "1234",
                        shouldRequireMasterPasswordOnRestart = false,
                    ),
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PIN confirmation dialog No click should send UnlockWithPinToggle Enabled and close the dialog`() {
        mutableStateFlow.update {
            it.copy(isUnlockWithPinEnabled = false)
        }
        composeTestRule
            .onNodeWithText(text = "Unlock with PIN code")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onAllNodesWithText(text = "PIN")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performTextInput(text = "1234")
        composeTestRule
            .onAllNodesWithText(text = "Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "No")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                SetupUnlockAction.UnlockWithPinToggle(
                    UnlockWithPinState.Enabled(
                        pin = "1234",
                        shouldRequireMasterPasswordOnRestart = false,
                    ),
                ),
            )
        }
        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PIN confirmation dialog Yes click should send UnlockWithPinToggle Enabled and close the dialog`() {
        mutableStateFlow.update {
            it.copy(isUnlockWithPinEnabled = false)
        }
        composeTestRule
            .onNodeWithText(text = "Unlock with PIN code")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onAllNodesWithText(text = "PIN")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performTextInput(text = "1234")
        composeTestRule
            .onAllNodesWithText(text = "Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Yes")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                SetupUnlockAction.UnlockWithPinToggle(
                    UnlockWithPinState.Enabled(
                        pin = "1234",
                        shouldRequireMasterPasswordOnRestart = true,
                    ),
                ),
            )
        }
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `on Continue click should send ContinueClick when disabled`() {
        composeTestRule
            .onNodeWithText(text = "Continue")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        verify(exactly = 0) {
            viewModel.trySendAction(SetupUnlockAction.ContinueClick)
        }
    }

    @Test
    fun `on Continue click should send ContinueClick when enabled`() {
        mutableStateFlow.update {
            it.copy(isUnlockWithPinEnabled = true)
        }
        composeTestRule
            .onNodeWithText(text = "Continue")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(SetupUnlockAction.ContinueClick)
        }
    }

    @Test
    fun `on Set up later click should display confirmation dialog`() {
        composeTestRule.assertNoDialogExists()
        composeTestRule
            .onNodeWithText(text = "Set up later")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Set up unlock later?")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `on Set up later dialog cancel click should dismiss the dialog`() {
        composeTestRule.assertNoDialogExists()
        composeTestRule
            .onNodeWithText(text = "Set up later")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on Set up later dialog confirm click should dismiss the dialog and send SetUpLaterClick`() {
        composeTestRule.assertNoDialogExists()
        composeTestRule
            .onNodeWithText(text = "Set up later")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Confirm")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()

        verify(exactly = 1) {
            viewModel.trySendAction(SetupUnlockAction.SetUpLaterClick)
        }
    }
}

private val DEFAULT_STATE: SetupUnlockState = SetupUnlockState(
    isUnlockWithPinEnabled = false,
    isUnlockWithPasswordEnabled = true,
    isUnlockWithBiometricsEnabled = false,
)
