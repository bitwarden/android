package com.x8bit.bitwarden.ui.vault.feature.importitems

import androidx.credentials.providerevents.exception.ImportCredentialsInvalidJsonException
import androidx.credentials.providerevents.transfer.CredentialTypes
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.cxf.importer.model.ImportCredentialsSelectionResult
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.resource.BitwardenPlurals
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asPluralsText
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.ImportCredentialsResult
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelay
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import io.mockk.awaits
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ImportItemsViewModelTest : BaseViewModelTest() {

    private val vaultRepository = mockk<VaultRepository>()
    private val snackbarRelayManager = mockk<SnackbarRelayManager> {
        every { sendSnackbarData(any(), any()) } just runs
    }

    @Test
    fun `BackClick sends NavigateBack event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ImportItemsAction.BackClick)
            assertEquals(ImportItemsEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `ImportFromComputerClick sends NavigateToImportFromComputer event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ImportItemsAction.ImportFromComputerClick)
            assertEquals(ImportItemsEvent.NavigateToImportFromComputer, awaitItem())
        }
    }

    @Test
    fun `DismissDialog updates state`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(ImportItemsAction.DismissDialog)
        assertEquals(ImportItemsState(dialog = null), viewModel.stateFlow.value)
    }

    @Test
    fun `ImportFromAnotherAppClick sends ShowRegisteredImportSources event`() {
        runTest {
            val viewModel = createViewModel()
            viewModel.trySendAction(ImportItemsAction.ImportFromAnotherAppClick)
            viewModel.eventFlow.test {
                assertEquals(
                    ImportItemsEvent.ShowRegisteredImportSources(
                        listOf(
                            CredentialTypes.BASIC_AUTH,
                            CredentialTypes.PUBLIC_KEY,
                            CredentialTypes.TOTP,
                            CredentialTypes.CREDIT_CARD,
                            CredentialTypes.SSH_KEY,
                            CredentialTypes.ADDRESS,
                        ),
                    ),
                    awaitItem(),
                )
            }
        }
    }

    @Test
    fun `ImportCredentialSelectionReceive and Cancelled result updates state`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(
            ImportItemsAction.ImportCredentialSelectionReceive(
                selectionResult = ImportCredentialsSelectionResult.Cancelled,
            ),
        )

        val expectedState = ImportItemsState(
            dialog = ImportItemsState.DialogState.General(
                title = BitwardenString.import_cancelled.asText(),
                message = BitwardenString.import_was_cancelled_in_the_selected_app.asText(),
                throwable = null,
            ),
        )
        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    fun `ImportCredentialSelectionReceive and Failure result updates state`() = runTest {
        val viewModel = createViewModel()
        val exception = ImportCredentialsInvalidJsonException("Error")

        viewModel.trySendAction(
            ImportItemsAction.ImportCredentialSelectionReceive(
                selectionResult = ImportCredentialsSelectionResult.Failure(
                    error = exception,
                ),
            ),
        )

        val expectedState = ImportItemsState(
            dialog = ImportItemsState.DialogState.General(
                title = BitwardenString.unable_to_import_your_items.asText(),
                message = BitwardenString.there_was_a_problem_importing_your_items.asText(),
                throwable = exception,
            ),
        )
        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    fun `ImportCredentialSelectionReceive and Success result updates state and triggers import`() =
        runTest {
            val cxfPayload = "{\"credentials\":[]}"
            val selectionResult = ImportCredentialsSelectionResult.Success(
                response = cxfPayload,
                callingAppInfo = mockk(),
            )
            coEvery {
                vaultRepository.importCxfPayload(cxfPayload)
            } just awaits

            val viewModel = createViewModel()

            viewModel.trySendAction(
                ImportItemsAction.ImportCredentialSelectionReceive(
                    selectionResult,
                ),
            )

            assertEquals(
                ImportItemsState(
                    dialog = ImportItemsState.DialogState.Loading(
                        message = BitwardenString.saving_items.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )

            // Verify that the repository method was called
            coVerify { vaultRepository.importCxfPayload(cxfPayload) }
        }

    @Test
    fun `ReturnToVaultClick sends NavigateToVault event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ImportItemsAction.ReturnToVaultClick)
            assertEquals(
                ImportItemsEvent.NavigateToVault,
                awaitItem(),
            )
        }
    }

    @Test
    fun `Internal ImportCxfResultReceive and Error result updates state`() = runTest {
        val viewModel = createViewModel()
        val exception = ImportCredentialsInvalidJsonException("Error")
        viewModel.trySendAction(
            ImportItemsAction.Internal.ImportCredentialsResultReceive(
                ImportCredentialsResult.Error(
                    error = exception,
                ),
            ),
        )

        val expectedState = ImportItemsState(
            dialog = ImportItemsState.DialogState.General(
                title = BitwardenString.unable_to_import_your_items.asText(),
                message = BitwardenString.there_was_a_problem_importing_your_items.asText(),
                throwable = exception,
            ),
        )
        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Internal ImportCredentialsResultReceive with Success result should clear dialogs, show snackbar, and navigate to vault`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.trySendAction(
                ImportItemsAction.Internal.ImportCredentialsResultReceive(
                    ImportCredentialsResult.Success(itemCount = 2),
                ),
            )

            val expectedState = ImportItemsState(dialog = null)
            assertEquals(expectedState, viewModel.stateFlow.value)
            coVerify(exactly = 1) {
                snackbarRelayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(
                        messageHeader = BitwardenString.import_successful.asText(),
                        message = BitwardenPlurals
                            .x_items_have_been_imported_to_your_vault
                            .asPluralsText(
                                quantity = 2,
                                args = arrayOf(2),
                            ),
                    ),
                    relay = SnackbarRelay.LOGINS_IMPORTED,
                )
            }
            viewModel.eventFlow.test {
                assertEquals(
                    ImportItemsEvent.NavigateToVault,
                    awaitItem(),
                )
            }
        }

    @Test
    fun `Internal ImportCxfResultReceive and NoItems result updates state`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(
            ImportItemsAction.Internal.ImportCredentialsResultReceive(
                ImportCredentialsResult.NoItems,
            ),
        )

        val expectedState = ImportItemsState(
            dialog = ImportItemsState.DialogState.General(
                title = BitwardenString.no_items_imported.asText(),
                message = BitwardenString.no_items_received_from_the_selected_app.asText(),
                throwable = null,
            ),
        )
        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Internal ImportCxfResultReceive and SyncFailed should clear dialogs, show snackbar, and navigate to vault`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.trySendAction(
                ImportItemsAction.Internal.ImportCredentialsResultReceive(
                    ImportCredentialsResult.SyncFailed(
                        error = RuntimeException("Error"),
                    ),
                ),
            )

            val expectedState = ImportItemsState(dialog = null)
            assertEquals(expectedState, viewModel.stateFlow.value)
            coVerify(exactly = 1) {
                snackbarRelayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(
                        messageHeader = BitwardenString.vault_sync_failed.asText(),
                        message = BitwardenString
                            .your_items_have_been_successfully_imported_but_could_not_sync_vault
                            .asText(),
                        actionLabel = BitwardenString.try_again.asText(),
                    ),
                    relay = SnackbarRelay.VAULT_SYNC_FAILED,
                )
            }
        }

    private fun createViewModel(): ImportItemsViewModel = ImportItemsViewModel(
        vaultRepository = vaultRepository,
        savedStateHandle = SavedStateHandle(),
        snackbarRelayManager = snackbarRelayManager,
    )
}
