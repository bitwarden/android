package com.x8bit.bitwarden.ui.platform.feature.settings.exportvault

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.feature.settings.exportvault.model.ExportVaultFormat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExportVaultViewModelTest : BaseViewModelTest() {
    private val settingsRepository: SettingsRepository = mockk()

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
    fun `ConfirmExportVaultClicked correct password should emit ShowToast`() = runTest {
        val password = "password"
        coEvery {
            settingsRepository.validatePassword(
                password = password,
            )
        } returns true

        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ExportVaultAction.PasswordInputChanged(password))

            viewModel.trySendAction(ExportVaultAction.ConfirmExportVaultClicked)
            assertEquals(
                ExportVaultEvent.ShowToast("Not yet implemented".asText()),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ConfirmExportVaultClicked blank password should show an error`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ExportVaultAction.ConfirmExportVaultClicked)
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = ExportVaultState.DialogState.Error(
                        title = null,
                        message = R.string.validation_field_required.asText(
                            R.string.master_password.asText(),
                        ),
                    ),
                ),
                viewModel.stateFlow.value,
            )

            viewModel.trySendAction(ExportVaultAction.DialogDismiss)
            assertEquals(
                DEFAULT_STATE,
                viewModel.stateFlow.value,
            )
        }
    }

    @Test
    fun `ConfirmExportVaultClicked invalid password should show an error`() = runTest {
        val password = "password"
        coEvery {
            settingsRepository.validatePassword(
                password = password,
            )
        } returns false

        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ExportVaultAction.PasswordInputChanged(password))

            viewModel.trySendAction(ExportVaultAction.ConfirmExportVaultClicked)
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = ExportVaultState.DialogState.Error(
                        title = null,
                        message = R.string.invalid_master_password.asText(),
                    ),
                    passwordInput = password,
                ),
                viewModel.stateFlow.value,
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
            settingsRepository = settingsRepository,
            savedStateHandle = savedStateHandle,
        )
}

private val DEFAULT_STATE = ExportVaultState(
    dialogState = null,
    exportFormat = ExportVaultFormat.JSON,
    passwordInput = "",
)
