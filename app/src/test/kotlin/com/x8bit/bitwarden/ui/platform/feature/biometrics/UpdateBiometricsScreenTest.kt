package com.x8bit.bitwarden.ui.platform.feature.biometrics

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.assertNoDialogExists
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricsManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test
import javax.crypto.Cipher

class UpdateBiometricsScreenTest : BitwardenComposeTest() {
    private var onDismissCalled = false
    private val captureBiometricsSuccess = slot<(cipher: Cipher?) -> Unit>()
    private val captureBiometricsCancel = slot<() -> Unit>()
    private val captureBiometricsLockOut = slot<() -> Unit>()
    private val captureBiometricsError = slot<() -> Unit>()
    private val biometricsManager: BiometricsManager = mockk {
        every {
            promptBiometrics(
                onSuccess = capture(captureBiometricsSuccess),
                onCancel = capture(captureBiometricsCancel),
                onLockOut = capture(captureBiometricsLockOut),
                onError = capture(captureBiometricsError),
                cipher = CIPHER,
            )
        } just runs
    }

    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mutableEventFlow = bufferedMutableSharedFlow<UpdateBiometricsEvent>()
    private val viewModel = mockk<UpdateBiometricsViewModel> {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
        every { trySendAction(action = any()) } just runs
    }

    @Before
    fun setup() {
        setContent(biometricsManager = biometricsManager) {
            UpdateBiometricsScreen(
                onDismiss = { onDismissCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `content should be displayed`() {
        composeTestRule
            .onNodeWithText(text = "Keep unlocking with biometrics")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                text = "We’ve made a security update. " +
                    "Confirm your biometrics again to keep using them to unlock Bitwarden.",
            )
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "Confirm")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "Turn off")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `on Confirm biometrics click should send UpdateBiometricsClick action`() {
        composeTestRule
            .onNodeWithText(text = "Confirm")
            .performScrollTo()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(UpdateBiometricsAction.ConfirmBiometricsClick)
        }
    }

    @Test
    fun `on Turn off click should send TurnOffBiometricsClick action`() {
        composeTestRule
            .onNodeWithText(text = "Turn off")
            .performScrollTo()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(UpdateBiometricsAction.TurnOffBiometricsClick)
        }
    }

    @Test
    fun `on system back should send TurnOffBiometricsClick action`() {
        backDispatcher?.onBackPressed()

        verify(exactly = 1) {
            viewModel.trySendAction(UpdateBiometricsAction.TurnOffBiometricsClick)
        }
    }

    @Test
    fun `on NavigateBack event should invoke onDismiss`() {
        mutableEventFlow.tryEmit(UpdateBiometricsEvent.NavigateBack)
        assertTrue(onDismissCalled)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ShowBiometricsPrompt event should prompt for biometrics and send BiometricCipherReceived on success`() {
        mutableEventFlow.tryEmit(UpdateBiometricsEvent.ShowBiometricsPrompt(cipher = CIPHER))

        captureBiometricsSuccess.captured(CIPHER)

        verify(exactly = 1) {
            biometricsManager.promptBiometrics(
                onSuccess = any(),
                onCancel = any(),
                onLockOut = any(),
                onError = any(),
                cipher = CIPHER,
            )
            viewModel.trySendAction(UpdateBiometricsAction.BiometricCipherReceived(cipher = CIPHER))
        }
    }

    @Test
    fun `on ShowBiometricsPrompt event should not send any action on cancel, lock out, or error`() {
        mutableEventFlow.tryEmit(UpdateBiometricsEvent.ShowBiometricsPrompt(cipher = CIPHER))

        captureBiometricsCancel.captured()
        captureBiometricsLockOut.captured()
        captureBiometricsError.captured()

        verify(exactly = 0) {
            viewModel.trySendAction(action = any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `DisableBiometricsAndClose dialog should be displayed according to state and send ConfirmTurnOffClick on confirm`() {
        composeTestRule.assertNoDialogExists()

        mutableStateFlow.update {
            it.copy(dialogState = UpdateBiometricsState.DialogState.DisableBiometricsAndClose)
        }

        composeTestRule
            .onAllNodesWithText(text = "Turn off biometrics?")
            .filterToOne(matcher = hasAnyAncestor(matcher = isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(
                text = "Unlocking will require your master password. " +
                    "You can turn biometrics back on in Settings.",
            )
            .filterToOne(matcher = hasAnyAncestor(matcher = isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText(text = "Turn off")
            .filterToOne(matcher = hasAnyAncestor(matcher = isDialog()))
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(UpdateBiometricsAction.ConfirmTurnOffClick)
        }
    }

    @Test
    fun `DisableBiometricsAndClose dialog cancel click should send DismissDialog action`() {
        mutableStateFlow.update {
            it.copy(dialogState = UpdateBiometricsState.DialogState.DisableBiometricsAndClose)
        }

        composeTestRule
            .onAllNodesWithText(text = "Cancel")
            .filterToOne(matcher = hasAnyAncestor(matcher = isDialog()))
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(UpdateBiometricsAction.DismissDialog)
        }
    }

    @Test
    fun `Loading dialog should be displayed according to state`() {
        val message = "message"
        composeTestRule.assertNoDialogExists()

        mutableStateFlow.update {
            it.copy(
                dialogState = UpdateBiometricsState.DialogState.Loading(message = message.asText()),
            )
        }

        composeTestRule
            .onAllNodesWithText(text = message)
            .filterToOne(matcher = hasAnyAncestor(matcher = isDialog()))
            .assertIsDisplayed()

        mutableStateFlow.update { it.copy(dialogState = null) }
        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Error dialog should be displayed according to state and send DismissDialog action on click`() {
        val title = "title"
        val message = "message"
        composeTestRule.assertNoDialogExists()

        mutableStateFlow.update {
            it.copy(
                dialogState = UpdateBiometricsState.DialogState.Error(
                    title = title.asText(),
                    message = message.asText(),
                ),
            )
        }

        composeTestRule
            .onAllNodesWithText(text = title)
            .filterToOne(matcher = hasAnyAncestor(matcher = isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(text = message)
            .filterToOne(matcher = hasAnyAncestor(matcher = isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText(text = "Okay")
            .filterToOne(matcher = hasAnyAncestor(matcher = isDialog()))
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(UpdateBiometricsAction.DismissDialog)
        }

        mutableStateFlow.update { it.copy(dialogState = null) }
        composeTestRule.assertNoDialogExists()
    }
}

private val CIPHER: Cipher = mockk()
private val DEFAULT_STATE: UpdateBiometricsState = UpdateBiometricsState(
    dialogState = null,
)
