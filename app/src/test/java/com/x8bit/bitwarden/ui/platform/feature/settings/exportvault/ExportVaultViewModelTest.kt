package com.x8bit.bitwarden.ui.platform.feature.settings.exportvault

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.feature.settings.exportvault.model.ExportVaultFormat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExportVaultViewModelTest : BaseViewModelTest() {
    private val savedStateHandle = SavedStateHandle()

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    @Test
    fun `CloseButtonClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(ExportVaultAction.CloseButtonClick)
            assertEquals(
                ExportVaultEvent.NavigateBack,
                awaitItem(),
            )
        }
    }

    @Test
    fun `ExportFormatOptionSelect should update the selected export format in the state`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    ExportVaultAction.ExportFormatOptionSelect(ExportVaultFormat.CSV),
                )

                assertEquals(
                    DEFAULT_STATE.copy(
                        exportFormat = ExportVaultFormat.CSV,
                    ),
                    viewModel.stateFlow.value,
                )
            }
        }

    @Test
    fun `PasswordInputChanged should update the password input in the state`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ExportVaultAction.PasswordInputChanged("Test123"))

            assertEquals(
                DEFAULT_STATE.copy(
                    passwordInput = "Test123",
                ),
                viewModel.stateFlow.value,
            )
        }
    }

    private fun createViewModel(): ExportVaultViewModel =
        ExportVaultViewModel(
            savedStateHandle = savedStateHandle,
        )

    companion object {
        private val DEFAULT_STATE = ExportVaultState(
            dialogState = null,
            exportFormat = ExportVaultFormat.JSON,
            passwordInput = "",
        )
    }
}
