package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SetupAutoFillViewModelTest : BaseViewModelTest() {

    private val mutableAutoFillEnabledStateFlow = MutableStateFlow(false)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true) {
        every { isAutofillEnabledStateFlow } returns mutableAutoFillEnabledStateFlow
        every { disableAutofill() } just runs
    }

    @Test
    fun `handleAutofillEnabledUpdateReceive updates autofillEnabled state`() {
        val viewModel = createViewModel()
        assertFalse(viewModel.stateFlow.value.autofillEnabled)
        mutableAutoFillEnabledStateFlow.value = true

        assertTrue(viewModel.stateFlow.value.autofillEnabled)
    }

    @Test
    fun `handleAutofillServiceChanged with autofillEnabled true navigates to autofill settings`() =
        runTest {
            val viewModel = createViewModel()
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
    fun `handleAutofillServiceChanged with autofillEnabled false disables autofill`() =
        runTest {
            val viewModel = createViewModel()
            mutableAutoFillEnabledStateFlow.value = true
            assertTrue(viewModel.stateFlow.value.autofillEnabled)
            viewModel.eventFlow.test {
                viewModel.trySendAction(SetupAutoFillAction.AutofillServiceChanged(false))
                expectNoEvents()
            }
            verify { settingsRepository.disableAutofill() }
        }

    @Test
    fun `handleTurnOnLater click sets dialogState to TurnOnLaterDialog`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(SetupAutoFillAction.TurnOnLaterClick)
        assertEquals(
            SetupAutoFillDialogState.TurnOnLaterDialog,
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Test
    fun `handleDismissDialog sets dialogState to null`() {
        val viewModel = createViewModel()
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
        val viewModel = createViewModel()
        viewModel.trySendAction(SetupAutoFillAction.AutoFillServiceFallback)
        assertEquals(
            SetupAutoFillDialogState.AutoFillFallbackDialog,
            viewModel.stateFlow.value.dialogState,
        )
    }

    private fun createViewModel() = SetupAutoFillViewModel(settingsRepository)
}
