package com.x8bit.bitwarden.ui.platform.feature.settings.exportvault

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockPolicy
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.feature.settings.exportvault.model.ExportVaultFormat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExportVaultViewModelTest : BaseViewModelTest() {
    private val authRepository: AuthRepository = mockk()

    private val policyManager: PolicyManager = mockk {
        every {
            getActivePolicies(type = PolicyTypeJson.DISABLE_PERSONAL_VAULT_EXPORT)
        } returns emptyList()
    }

    private val savedStateHandle = SavedStateHandle()

    @Test
    fun `initial state should be correct`() = runTest {
        every {
            policyManager.getActivePolicies(type = PolicyTypeJson.DISABLE_PERSONAL_VAULT_EXPORT)
        } returns listOf(createMockPolicy())

        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(
                    policyPreventsExport = true,
                ),
                awaitItem(),
            )
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
            authRepository.validatePassword(
                password = password,
            )
        } returns ValidatePasswordResult.Success(isValid = true)

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
            authRepository.validatePassword(
                password = password,
            )
        } returns ValidatePasswordResult.Success(isValid = false)

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
    fun `ConfirmExportVaultClicked error checking password should show an error`() = runTest {
        val password = "password"
        coEvery {
            authRepository.validatePassword(
                password = password,
            )
        } returns ValidatePasswordResult.Error

        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ExportVaultAction.PasswordInputChanged(password))

            viewModel.trySendAction(ExportVaultAction.ConfirmExportVaultClicked)
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = ExportVaultState.DialogState.Error(
                        title = null,
                        message = R.string.generic_error_message.asText(),
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
            authRepository = authRepository,
            policyManager = policyManager,
            savedStateHandle = savedStateHandle,
        )
}

private val DEFAULT_STATE = ExportVaultState(
    dialogState = null,
    exportFormat = ExportVaultFormat.JSON,
    passwordInput = "",
    policyPreventsExport = false,
)
