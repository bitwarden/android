package com.x8bit.bitwarden.ui.platform.feature.settings.exportvault

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockPolicy
import com.x8bit.bitwarden.data.vault.manager.FileManager
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.ExportVaultDataResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.feature.settings.exportvault.model.ExportVaultFormat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ExportVaultViewModelTest : BaseViewModelTest() {
    private val authRepository: AuthRepository = mockk()

    private val policyManager: PolicyManager = mockk {
        every {
            getActivePolicies(type = PolicyTypeJson.DISABLE_PERSONAL_VAULT_EXPORT)
        } returns emptyList()
    }

    private val clock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )

    private val vaultRepository: VaultRepository = mockk {
        coEvery { exportVaultDataToString(any()) } returns mockk()
    }
    private val fileManager: FileManager = mockk()

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
            viewModel.trySendAction(ExportVaultAction.CloseButtonClick)
            assertEquals(
                ExportVaultEvent.NavigateBack,
                awaitItem(),
            )
        }
    }

    @Test
    fun `ConfirmExportVaultClicked correct password should call exportVaultDataToString`() =
        runTest {
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

                coVerify {
                    vaultRepository.exportVaultDataToString(any())
                }
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

    @Test
    fun `ReceiveExportVaultDataToStringResult should update state to error if result is error`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.trySendAction(
                ExportVaultAction.Internal.ReceiveExportVaultDataToStringResult(
                    result = ExportVaultDataResult.Error,
                ),
            )

            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = ExportVaultState.DialogState.Error(
                        title = null,
                        message = R.string.export_vault_failure.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ReceiveExportVaultDataToStringResult should emit NavigateToSelectExportDataLocation on result success`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    ExportVaultAction.Internal.ReceiveExportVaultDataToStringResult(
                        result = ExportVaultDataResult.Success(vaultData = "TestVaultData"),
                    ),
                )

                assertEquals(
                    ExportVaultEvent.NavigateToSelectExportDataLocation(
                        fileName = "bitwarden_export_20231027120000.json",
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `ExportLocationReceive should update state to error if exportData is null`() {
        val viewModel = createViewModel()
        val uri = mockk<Uri>()

        viewModel.trySendAction(ExportVaultAction.ExportLocationReceive(fileUri = uri))

        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = ExportVaultState.DialogState.Error(
                    title = null,
                    message = R.string.export_vault_failure.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `ExportLocationReceive should update state to error if saving the data fails`() =
        runTest {
            val exportData = "TestExportVaultData"
            val viewModel = createViewModel(
                DEFAULT_STATE.copy(
                    exportData = exportData,
                ),
            )
            val uri = mockk<Uri>()
            coEvery {
                fileManager.stringToUri(fileUri = any(), dataString = exportData)
            } returns false

            viewModel.trySendAction(ExportVaultAction.ExportLocationReceive(fileUri = uri))

            coVerify {
                fileManager.stringToUri(fileUri = any(), dataString = exportData)
            }

            assertEquals(
                DEFAULT_STATE.copy(
                    exportData = exportData,
                    dialogState = ExportVaultState.DialogState.Error(
                        title = null,
                        message = R.string.export_vault_failure.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `ExportLocationReceive should emit ShowToast on success`() = runTest {
        val exportData = "TestExportVaultData"
        val viewModel = createViewModel(
            DEFAULT_STATE.copy(
                exportData = exportData,
            ),
        )
        val uri = mockk<Uri>()
        coEvery { fileManager.stringToUri(fileUri = any(), dataString = exportData) } returns true

        viewModel.eventFlow.test {
            viewModel.trySendAction(ExportVaultAction.ExportLocationReceive(uri))

            coVerify { fileManager.stringToUri(fileUri = any(), dataString = exportData) }

            assertEquals(
                ExportVaultEvent.ShowToast(R.string.export_vault_success.asText()),
                awaitItem(),
            )
        }
    }

    private fun createViewModel(
        initialState: ExportVaultState? = null,
    ): ExportVaultViewModel =
        ExportVaultViewModel(
            authRepository = authRepository,
            policyManager = policyManager,
            savedStateHandle = SavedStateHandle(
                initialState = mapOf("state" to initialState),
            ),
            fileManager = fileManager,
            vaultRepository = vaultRepository,
            clock = clock,
        )
}

private val DEFAULT_STATE = ExportVaultState(
    dialogState = null,
    exportFormat = ExportVaultFormat.JSON,
    passwordInput = "",
    exportData = null,
    policyPreventsExport = false,
)
