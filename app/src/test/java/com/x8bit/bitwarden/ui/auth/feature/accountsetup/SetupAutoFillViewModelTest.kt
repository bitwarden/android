package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import app.cash.turbine.test
import com.google.common.base.Verify.verify
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SetupAutoFillViewModelTest : BaseViewModelTest() {

    private val mutableAutoFillEnabledStateFlow = MutableStateFlow(false)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true) {
        every { isAutofillEnabledStateFlow } returns mutableAutoFillEnabledStateFlow
        every { disableAutofill() } answers {
            mutableAutoFillEnabledStateFlow.value = false
        }
    }

    private lateinit var viewModel: SetupAutoFillViewModel

    @BeforeEach
    fun setup() {
        viewModel = SetupAutoFillViewModel(settingsRepository)
    }

    @Test
    fun `handleAutofillEnabledUpdateReceive updates autofillEnabled state`() {
        assertFalse(viewModel.stateFlow.value.autofillEnabled)
        mutableAutoFillEnabledStateFlow.value = true

        assertTrue(viewModel.stateFlow.value.autofillEnabled)
    }

    @Test
    fun `handleAutofillServiceChanged with autofillEnabled true navigates to autofill settings`() =
        runTest {
            viewModel.eventFlow.test {
                viewModel.trySendAction(SetupAutoFillAction.AutofillServiceChanged(true))
                assertEquals(
                    SetupAutoFillEvent.NavigateToAutofillSettings,
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `handleAutofillServiceChanged with autofillEnabled false disables autofill and cause state update`() =
        runTest {
            mutableAutoFillEnabledStateFlow.value = true
            assertTrue(viewModel.stateFlow.value.autofillEnabled)
            viewModel.eventFlow.test {
                viewModel.trySendAction(SetupAutoFillAction.AutofillServiceChanged(false))
                expectNoEvents()
            }
            verify { settingsRepository.disableAutofill() }
            assertFalse(viewModel.stateFlow.value.autofillEnabled)
        }

    @Test
    fun `handleContinueClick sends NavigateToCompleteSetup event`() = runTest {
        viewModel.eventFlow.test {
            viewModel.trySendAction(SetupAutoFillAction.ContinueClick)
            assertEquals(SetupAutoFillEvent.NavigateToCompleteSetup, awaitItem())
        }
    }

    @Test
    fun `handleTurnOnLater click sets dialogState to TurnOnLaterDialog`() {
        viewModel.trySendAction(SetupAutoFillAction.TurnOnLaterClick)
        assertEquals(
            SetupAutoFillDialogState.TurnOnLaterDialog,
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Test
    fun `handleTurnOnLaterConfirmClick sends NavigateToCompleteSetup event`() = runTest {
        viewModel.eventFlow.test {
            viewModel.trySendAction(SetupAutoFillAction.TurnOnLaterConfirmClick)
            assertEquals(SetupAutoFillEvent.NavigateToCompleteSetup, awaitItem())
        }
    }

    @Test
    fun `handleDismissDialog sets dialogState to null`() {
        viewModel.trySendAction(SetupAutoFillAction.TurnOnLaterClick)
        assertEquals(
            SetupAutoFillDialogState.TurnOnLaterDialog,
            viewModel.stateFlow.value.dialogState,
        )
        viewModel.trySendAction(SetupAutoFillAction.DismissDialog)
        assertNull(viewModel.stateFlow.value.dialogState)
    }

    @Test
    fun `handleAutoFillServiceFallback sets dialogState to AutoFillFallbackDialog`() {
        viewModel.trySendAction(SetupAutoFillAction.AutoFillServiceFallback)
        assertEquals(
            SetupAutoFillDialogState.AutoFillFallbackDialog,
            viewModel.stateFlow.value.dialogState,
        )
    }
}
