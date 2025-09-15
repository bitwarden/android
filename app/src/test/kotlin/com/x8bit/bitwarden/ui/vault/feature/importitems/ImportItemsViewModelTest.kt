package com.x8bit.bitwarden.ui.vault.feature.importitems

import androidx.credentials.providerevents.exception.ImportCredentialsInvalidJsonException
import androidx.credentials.providerevents.transfer.CredentialTypes
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.cxf.importer.model.ImportCredentialsSelectionResult
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.ImportCredentialsResult
import io.mockk.awaits
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ImportItemsViewModelTest : BaseViewModelTest() {

    private val vaultRepository = mockk<VaultRepository>()

    @Test
    fun `NavigateBack sends NavigateBack event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ImportItemsAction.NavigateBack)
            assertEquals(ImportItemsEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `GetStartedClick updates state and sends ShowRegisteredImportSources event`() {
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                assertEquals(
                    ImportItemsState.ViewState.NotStarted,
                    viewModel.stateFlow.value.viewState,
                )
                viewModel.trySendAction(ImportItemsAction.GetStartedClick)
                assertEquals(
                    ImportItemsState.ViewState.AwaitingSelection,
                    viewModel.stateFlow.value.viewState,
                )
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

        val expectedState = ImportItemsState.ViewState.Completed(
            title = BitwardenString.import_cancelled.asText(),
            message = BitwardenString.credential_import_was_cancelled.asText(),
            iconData = IconData.Local(BitwardenDrawable.ic_warning),
        )
        assertEquals(expectedState, viewModel.stateFlow.value.viewState)
    }

    @Test
    fun `ImportCredentialSelectionReceive and Failure result updates state`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(
            ImportItemsAction.ImportCredentialSelectionReceive(
                selectionResult = ImportCredentialsSelectionResult.Failure(
                    error = ImportCredentialsInvalidJsonException(),
                ),
            ),
        )

        val expectedState = ImportItemsState.ViewState.Completed(
            title = BitwardenString.import_vault_failure.asText(),
            message = BitwardenString.generic_error_message.asText(),
            iconData = IconData.Local(BitwardenDrawable.ic_warning),
        )
        assertEquals(expectedState, viewModel.stateFlow.value.viewState)
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

            // Verify state is updated to ImportingItems
            assertEquals(
                ImportItemsState.ViewState.ImportingItems(progress = 0.5f),
                viewModel.stateFlow.value.viewState,
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

        viewModel.trySendAction(
            ImportItemsAction.Internal.ImportCredentialsResultReceive(
                ImportCredentialsResult.Error(
                    error = RuntimeException("Error"),
                ),
            ),
        )

        val expectedState = ImportItemsState.ViewState.Completed(
            title = BitwardenString.import_error.asText(),
            message = BitwardenString.there_was_a_problem_importing_your_items.asText(),
            iconData = IconData.Local(BitwardenDrawable.ic_warning),
        )
        assertEquals(expectedState, viewModel.stateFlow.value.viewState)
    }

    @Test
    fun `Internal ImportCxfResultReceive and Success result updates state`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(
            ImportItemsAction.Internal.ImportCredentialsResultReceive(
                ImportCredentialsResult.Success,
            ),
        )

        val expectedState = ImportItemsState.ViewState.Completed(
            title = BitwardenString.import_success.asText(),
            message = BitwardenString
                .your_items_have_been_successfully_imported
                .asText(),
            iconData = IconData.Local(BitwardenDrawable.ic_plain_checkmark),
        )
        assertEquals(expectedState, viewModel.stateFlow.value.viewState)
    }

    @Test
    fun `Internal ImportCxfResultReceive and NoItems result updates state`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(
            ImportItemsAction.Internal.ImportCredentialsResultReceive(
                ImportCredentialsResult.NoItems,
            ),
        )

        val expectedState = ImportItemsState.ViewState.Completed(
            title = BitwardenString.no_items_imported.asText(),
            message = BitwardenString
                .no_items_received_from_the_selected_credential_manager
                .asText(),
            iconData = IconData.Local(BitwardenDrawable.ic_plain_checkmark),
        )
        assertEquals(expectedState, viewModel.stateFlow.value.viewState)
    }

    @Test
    fun `Internal ImportCxfResultReceive and SyncFailed result updates state`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(
            ImportItemsAction.Internal.ImportCredentialsResultReceive(
                ImportCredentialsResult.SyncFailed(
                    error = RuntimeException("Error"),
                ),
            ),
        )

        val expectedState = ImportItemsState.ViewState.Completed(
            title = BitwardenString.vault_sync_failed.asText(),
            message = BitwardenString
                .your_items_have_been_successfully_imported_but_could_not_sync_vault
                .asText(),
            iconData = IconData.Local(BitwardenDrawable.ic_warning),
        )
        assertEquals(expectedState, viewModel.stateFlow.value.viewState)
    }

    private fun createViewModel(): ImportItemsViewModel = ImportItemsViewModel(
        vaultRepository = vaultRepository,
        savedStateHandle = SavedStateHandle(),
    )
}
