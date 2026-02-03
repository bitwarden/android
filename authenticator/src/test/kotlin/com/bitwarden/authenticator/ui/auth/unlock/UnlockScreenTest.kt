package com.bitwarden.authenticator.ui.auth.unlock

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bitwarden.authenticator.ui.platform.base.AuthenticatorComposeTest
import com.bitwarden.authenticator.ui.platform.manager.biometrics.BiometricsManager
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.crypto.Cipher

class UnlockScreenTest : AuthenticatorComposeTest() {

    private var onUnlockedCalled = false

    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mutableEventFlow = bufferedMutableSharedFlow<UnlockEvent>()

    private val mockViewModel: UnlockViewModel = mockk(relaxed = true) {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
        every { trySendAction(any()) } just runs
    }

    private val mockBiometricsManager: BiometricsManager = mockk(relaxed = true)
    private val mockCipher: Cipher = mockk()

    @Before
    fun setUp() {
        setContent(
            biometricsManager = mockBiometricsManager,
        ) {
            UnlockScreen(
                viewModel = mockViewModel,
                onUnlocked = { onUnlockedCalled = true },
            )
        }
    }

    @Test
    fun `unlock button should be displayed`() {
        composeTestRule
            .onNodeWithText("Unlock")
            .assertIsDisplayed()
    }

    @Test
    fun `logo should be displayed`() {
        composeTestRule
            .onNodeWithContentDescription("Bitwarden Authenticator")
            .assertIsDisplayed()
    }

    @Test
    fun `unlock button click should send BiometricsUnlockClick action`() {
        composeTestRule
            .onNodeWithText("Unlock")
            .performClick()

        verify {
            mockViewModel.trySendAction(UnlockAction.BiometricsUnlockClick)
        }
    }

    @Test
    fun `NavigateToItemListing event should call onUnlocked callback`() {
        mutableEventFlow.tryEmit(UnlockEvent.NavigateToItemListing)

        assertTrue(onUnlockedCalled)
    }

    @Test
    fun `PromptForBiometrics event should call biometricsManager`() {
        val onSuccessSlot = slot<(Cipher) -> Unit>()
        val onCancelSlot = slot<() -> Unit>()
        val onErrorSlot = slot<() -> Unit>()
        val onLockOutSlot = slot<() -> Unit>()

        every {
            mockBiometricsManager.promptBiometrics(
                onSuccess = capture(onSuccessSlot),
                onCancel = capture(onCancelSlot),
                onError = capture(onErrorSlot),
                onLockOut = capture(onLockOutSlot),
                cipher = mockCipher,
            )
        } just runs

        mutableEventFlow.tryEmit(UnlockEvent.PromptForBiometrics(mockCipher))

        verify {
            mockBiometricsManager.promptBiometrics(
                onSuccess = any(),
                onCancel = any(),
                onError = any(),
                onLockOut = any(),
                cipher = mockCipher,
            )
        }
    }

    @Test
    fun `biometric success callback should send BiometricsUnlockSuccess action`() {
        val onSuccessSlot = slot<(Cipher) -> Unit>()

        every {
            mockBiometricsManager.promptBiometrics(
                onSuccess = capture(onSuccessSlot),
                onCancel = any(),
                onError = any(),
                onLockOut = any(),
                cipher = mockCipher,
            )
        } just runs

        mutableEventFlow.tryEmit(UnlockEvent.PromptForBiometrics(mockCipher))

        // Invoke the captured success callback
        onSuccessSlot.captured.invoke(mockCipher)

        verify {
            mockViewModel.trySendAction(UnlockAction.BiometricsUnlockSuccess(mockCipher))
        }
    }

    @Test
    fun `biometric lockout callback should send BiometricsLockout action`() {
        val onLockOutSlot = slot<() -> Unit>()

        every {
            mockBiometricsManager.promptBiometrics(
                onSuccess = any(),
                onCancel = any(),
                onError = any(),
                onLockOut = capture(onLockOutSlot),
                cipher = mockCipher,
            )
        } just runs

        mutableEventFlow.tryEmit(UnlockEvent.PromptForBiometrics(mockCipher))

        // Invoke the captured lockout callback
        onLockOutSlot.captured.invoke()

        verify {
            mockViewModel.trySendAction(UnlockAction.BiometricsLockout)
        }
    }

    @Test
    fun `error dialog should display when state has Error dialog`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            dialog = UnlockState.Dialog.Error(
                title = "Error Title".asText(),
                message = "Error Message".asText(),
            ),
        )

        composeTestRule
            .onNodeWithText("Error Title")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Error Message")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `error dialog dismiss should send DismissDialog action`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            dialog = UnlockState.Dialog.Error(
                title = BitwardenString.an_error_has_occurred.asText(),
                message = BitwardenString.generic_error_message.asText(),
            ),
        )

        composeTestRule
            .onNodeWithTag("AcceptAlertButton")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            mockViewModel.trySendAction(UnlockAction.DismissDialog)
        }
    }

    @Test
    fun `loading dialog should display when state has Loading dialog`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            dialog = UnlockState.Dialog.Loading,
        )

        composeTestRule
            .onNodeWithText("Loading")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `no dialog should be displayed when state dialog is null`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(dialog = null)

        composeTestRule
            .onNodeWithText("Loading")
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithText("Ok")
            .assertDoesNotExist()
    }

    @Test
    fun `biometric cancel callback should not crash`() {
        val onCancelSlot = slot<() -> Unit>()

        every {
            mockBiometricsManager.promptBiometrics(
                onSuccess = any(),
                onCancel = capture(onCancelSlot),
                onError = any(),
                onLockOut = any(),
                cipher = mockCipher,
            )
        } just runs

        mutableEventFlow.tryEmit(UnlockEvent.PromptForBiometrics(mockCipher))

        // Invoke the captured cancel callback - should not crash
        onCancelSlot.captured.invoke()

        // Verify no action was sent (it's a no-op)
        verify(exactly = 0) {
            mockViewModel.trySendAction(any())
        }
    }

    @Test
    fun `biometric error callback should not crash`() {
        val onErrorSlot = slot<() -> Unit>()

        every {
            mockBiometricsManager.promptBiometrics(
                onSuccess = any(),
                onCancel = any(),
                onError = capture(onErrorSlot),
                onLockOut = any(),
                cipher = mockCipher,
            )
        } just runs

        mutableEventFlow.tryEmit(UnlockEvent.PromptForBiometrics(mockCipher))

        // Invoke the captured error callback - should not crash
        onErrorSlot.captured.invoke()

        // Verify no action was sent (it's a no-op)
        verify(exactly = 0) {
            mockViewModel.trySendAction(any())
        }
    }
}

private val DEFAULT_STATE = UnlockState(
    isBiometricsEnabled = true,
    isBiometricsValid = true,
    showBiometricInvalidatedMessage = false,
    dialog = null,
)
