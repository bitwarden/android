package com.x8bit.bitwarden.ui.vault.feature.manualcodeentry

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.TotpCodeResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ManualCodeEntryViewModelTests : BaseViewModelTest() {

    private val totpTestCodeFlow: Flow<TotpCodeResult> = bufferedMutableSharedFlow()
    private val vaultRepository: VaultRepository = mockk {
        every { totpCodeFlow } returns totpTestCodeFlow
        every { emitTotpCodeResult(any()) } just runs
    }

    @Test
    fun `CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(ManualCodeEntryAction.CloseClick)
            assertEquals(ManualCodeEntryEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `CodeSubmit wihh blank code should display error dialog`() {
        val initialState = DEFAULT_STATE.copy(code = "   ")
        val viewModel = createViewModel(initialState = initialState)

        viewModel.trySendAction(ManualCodeEntryAction.CodeSubmit)

        verify(exactly = 0) {
            vaultRepository.emitTotpCodeResult(TotpCodeResult.Success("TestCode"))
        }
        assertEquals(
            initialState.copy(
                dialog = ManualCodeEntryState.DialogState.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.authenticator_key_read_error.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `CodeSubmit should emit new code and NavigateBack`() = runTest {
        val viewModel = createViewModel(initialState = DEFAULT_STATE.copy(code = "   TestCode   "))

        viewModel.eventFlow.test {
            viewModel.trySendAction(ManualCodeEntryAction.CodeSubmit)

            verify(exactly = 1) {
                vaultRepository.emitTotpCodeResult(TotpCodeResult.Success("TestCode"))
            }
            assertEquals(ManualCodeEntryEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `CodeTextChange should update state with new value`() = runTest {
        val viewModel = createViewModel(initialState = DEFAULT_STATE.copy(code = "TestCode"))

        val expectedState = DEFAULT_STATE.copy(code = "NewCode")

        viewModel.trySendAction(ManualCodeEntryAction.CodeTextChange("NewCode"))
        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    fun `DialogDismiss should clear the dialog state`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(ManualCodeEntryAction.CloseClick)
            assertEquals(ManualCodeEntryEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `SettingsClick should emit NavigateToAppSettings and update state`() = runTest {
        val viewModel = createViewModel()

        val expectedState = DEFAULT_STATE

        viewModel.eventFlow.test {
            viewModel.trySendAction(ManualCodeEntryAction.SettingsClick)

            assertEquals(ManualCodeEntryEvent.NavigateToAppSettings, awaitItem())
            assertEquals(expectedState, viewModel.stateFlow.value)
        }
    }

    @Test
    fun `ScanQrTextCodeClick should emit NavigateToQrCodeScreen`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(ManualCodeEntryAction.ScanQrCodeTextClick)

            assertEquals(ManualCodeEntryEvent.NavigateToQrCodeScreen, awaitItem())
        }
    }

    private fun createViewModel(
        initialState: ManualCodeEntryState? = null,
    ): ManualCodeEntryViewModel =
        ManualCodeEntryViewModel(
            vaultRepository = vaultRepository,
            savedStateHandle = SavedStateHandle(
                initialState = mapOf("state" to initialState),
            ),
        )
}

private val DEFAULT_STATE: ManualCodeEntryState = ManualCodeEntryState(
    code = "",
    dialog = null,
)
