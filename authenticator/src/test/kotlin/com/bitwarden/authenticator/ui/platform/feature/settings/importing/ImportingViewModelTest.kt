package com.bitwarden.authenticator.ui.platform.feature.settings.importing

import android.net.Uri
import app.cash.turbine.test
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportDataResult
import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportFileFormat
import com.bitwarden.authenticator.ui.platform.model.SnackbarRelay
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.model.FileData
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ImportingViewModelTest : BaseViewModelTest() {

    private val authenticatorRepository: AuthenticatorRepository = mockk()
    private val snackbarRelayManager: SnackbarRelayManager<SnackbarRelay> = mockk {
        every { sendSnackbarData(data = any(), relay = any()) } just runs
    }

    @Test
    fun `initial state should default to Bitwarden JSON with no dialog`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    @Test
    fun `CloseButtonClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            expectNoEvents()
            viewModel.trySendAction(ImportAction.CloseButtonClick)
            assertEquals(ImportEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `ImportClick should emit NavigateToSelectImportFile with the selected format`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(
            ImportAction.ImportFormatOptionSelect(ImportFileFormat.AEGIS),
        )
        viewModel.eventFlow.test {
            expectNoEvents()
            viewModel.trySendAction(ImportAction.ImportClick)
            assertEquals(
                ImportEvent.NavigateToSelectImportFile(ImportFileFormat.AEGIS),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ImportLocationReceive with a plaintext file should import without a password`() = runTest {
        coEvery {
            authenticatorRepository.importVaultData(
                format = ImportFileFormat.BITWARDEN_JSON,
                fileData = FILE_DATA,
                password = null,
            )
        } returns ImportDataResult.Success

        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ImportAction.ImportLocationReceive(FILE_DATA))
            assertEquals(ImportEvent.NavigateBack, awaitItem())
        }

        coVerify {
            authenticatorRepository.importVaultData(
                format = ImportFileFormat.BITWARDEN_JSON,
                fileData = FILE_DATA,
                password = null,
            )
        }
    }

    @Test
    fun `PasswordRequired should stash the file and show the password prompt`() = runTest {
        coEvery {
            authenticatorRepository.importVaultData(any(), any(), null)
        } returns ImportDataResult.PasswordRequired

        val viewModel = createViewModel()
        viewModel.trySendAction(ImportAction.ImportLocationReceive(FILE_DATA))

        assertEquals(
            DEFAULT_STATE.copy(
                fileData = FILE_DATA,
                dialogState = ImportState.DialogState.PasswordPrompt(),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `PasswordSubmit should retry the import with the entered password`() = runTest {
        coEvery {
            authenticatorRepository.importVaultData(any(), any(), null)
        } returns ImportDataResult.PasswordRequired
        coEvery {
            authenticatorRepository.importVaultData(
                format = ImportFileFormat.BITWARDEN_JSON,
                fileData = FILE_DATA,
                password = "pass",
            )
        } returns ImportDataResult.Success

        val viewModel = createViewModel()
        viewModel.trySendAction(ImportAction.ImportLocationReceive(FILE_DATA))

        viewModel.eventFlow.test {
            viewModel.trySendAction(ImportAction.PasswordSubmit("pass"))
            assertEquals(ImportEvent.NavigateBack, awaitItem())
        }

        coVerify {
            authenticatorRepository.importVaultData(
                format = ImportFileFormat.BITWARDEN_JSON,
                fileData = FILE_DATA,
                password = "pass",
            )
        }
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `IncorrectPassword should keep the prompt open with an inline error`() = runTest {
        coEvery {
            authenticatorRepository.importVaultData(any(), any(), null)
        } returns ImportDataResult.PasswordRequired
        coEvery {
            authenticatorRepository.importVaultData(any(), any(), "wrong")
        } returns ImportDataResult.IncorrectPassword

        val viewModel = createViewModel()
        viewModel.trySendAction(ImportAction.ImportLocationReceive(FILE_DATA))
        viewModel.trySendAction(ImportAction.PasswordSubmit("wrong"))

        assertEquals(
            DEFAULT_STATE.copy(
                fileData = FILE_DATA,
                dialogState = ImportState.DialogState.PasswordPrompt(
                    errorMessage = BitwardenString.the_password_you_entered_is_incorrect.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `PasswordPromptDismiss should drop the stashed file and close the prompt`() = runTest {
        coEvery {
            authenticatorRepository.importVaultData(any(), any(), null)
        } returns ImportDataResult.PasswordRequired

        val viewModel = createViewModel()
        viewModel.trySendAction(ImportAction.ImportLocationReceive(FILE_DATA))
        viewModel.trySendAction(ImportAction.PasswordPromptDismiss)

        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `PasswordSubmit with no stashed file should not call the repository`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(ImportAction.PasswordSubmit("pass"))

        coVerify(exactly = 0) {
            authenticatorRepository.importVaultData(any(), any(), any())
        }
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `Error should drop the stashed file and show the error dialog`() = runTest {
        coEvery {
            authenticatorRepository.importVaultData(any(), any(), null)
        } returns ImportDataResult.Error()

        val viewModel = createViewModel()
        viewModel.trySendAction(ImportAction.ImportLocationReceive(FILE_DATA))

        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = ImportState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.import_vault_failure.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `Success should relay a snackbar and navigate back`() = runTest {
        coEvery {
            authenticatorRepository.importVaultData(any(), any(), null)
        } returns ImportDataResult.Success

        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ImportAction.ImportLocationReceive(FILE_DATA))
            assertEquals(ImportEvent.NavigateBack, awaitItem())
        }

        coVerify {
            snackbarRelayManager.sendSnackbarData(
                data = any(),
                relay = SnackbarRelay.IMPORT_SUCCESS,
            )
        }
    }

    private fun createViewModel(): ImportingViewModel = ImportingViewModel(
        authenticatorRepository = authenticatorRepository,
        snackbarRelayManager = snackbarRelayManager,
    )
}

private val FILE_DATA: FileData = FileData(
    fileName = "authenticator_export.json",
    uri = mockk<Uri>(),
    sizeBytes = 0L,
)

private val DEFAULT_STATE = ImportState(
    fileData = null,
    dialogState = null,
    importFileFormat = ImportFileFormat.BITWARDEN_JSON,
)
