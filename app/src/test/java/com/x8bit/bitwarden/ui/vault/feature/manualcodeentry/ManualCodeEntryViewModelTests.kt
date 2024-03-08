package com.x8bit.bitwarden.ui.vault.feature.manualcodeentry

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.TotpCodeResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
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
        val viewModel = createViewModel(initialState = ManualCodeEntryState(""))

        viewModel.eventFlow.test {
            viewModel.trySendAction(ManualCodeEntryAction.CloseClick)
            assertEquals(ManualCodeEntryEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `CodeSubmit should emit new code and NavigateBack`() = runTest {
        val viewModel =
            createViewModel(initialState = ManualCodeEntryState("TestCode"))

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
        val viewModel =
            createViewModel(initialState = ManualCodeEntryState("TestCode"))

        val expectedState = ManualCodeEntryState("NewCode")

        viewModel.trySendAction(ManualCodeEntryAction.CodeTextChange("NewCode"))
        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    fun `SettingsClick should emit NavigateToAppSettings and update state`() = runTest {
        val viewModel = createViewModel(initialState = ManualCodeEntryState(""))

        val expectedState = ManualCodeEntryState("")

        viewModel.eventFlow.test {
            viewModel.trySendAction(ManualCodeEntryAction.SettingsClick)

            assertEquals(ManualCodeEntryEvent.NavigateToAppSettings, awaitItem())
            assertEquals(expectedState, viewModel.stateFlow.value)
        }
    }

    @Test
    fun `ScanQrTextCodeClick should emit NavigateToQrCodeScreen`() = runTest {
        val viewModel = createViewModel(initialState = ManualCodeEntryState(""))

        viewModel.eventFlow.test {
            viewModel.trySendAction(ManualCodeEntryAction.ScanQrCodeTextClick)

            assertEquals(ManualCodeEntryEvent.NavigateToQrCodeScreen, awaitItem())
        }
    }

    private fun createViewModel(initialState: ManualCodeEntryState): ManualCodeEntryViewModel =
        ManualCodeEntryViewModel(
            vaultRepository = vaultRepository,
            savedStateHandle = SavedStateHandle(
                initialState = mapOf("state" to initialState),
            ),
        )
}
