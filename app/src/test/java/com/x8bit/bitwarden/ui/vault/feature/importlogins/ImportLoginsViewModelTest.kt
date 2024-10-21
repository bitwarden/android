package com.x8bit.bitwarden.ui.vault.feature.importlogins

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.SyncVaultDataResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ImportLoginsViewModelTest : BaseViewModelTest() {

    private val vaultRepository: VaultRepository = mockk() {
        coEvery { syncForResult() } returns SyncVaultDataResult.Success
    }

    @Test
    fun `initial state is correct`() {
        val viewModel = createViewModel()
        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `GetStartedClick sets dialog state to GetStarted`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(ImportLoginsAction.GetStartedClick)
        assertEquals(
            ImportLoginsState(
                dialogState = ImportLoginsState.DialogState.GetStarted,
                viewState = ImportLoginsState.ViewState.InitialContent,
                isVaultSyncing = false,
                showBottomSheet = false,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `ImportLaterClick sets dialog state to ImportLater`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(ImportLoginsAction.ImportLaterClick)
        assertEquals(
            ImportLoginsState(
                dialogState = ImportLoginsState.DialogState.ImportLater,
                viewState = ImportLoginsState.ViewState.InitialContent,
                isVaultSyncing = false,
                showBottomSheet = false,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `DismissDialog sets dialog state to null`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE,
                awaitItem(),
            )
            viewModel.trySendAction(ImportLoginsAction.GetStartedClick)
            assertEquals(
                ImportLoginsState(
                    dialogState = ImportLoginsState.DialogState.GetStarted,
                    viewState = ImportLoginsState.ViewState.InitialContent,
                    isVaultSyncing = false,
                    showBottomSheet = false,
                ),
                awaitItem(),
            )
            viewModel.trySendAction(ImportLoginsAction.DismissDialog)
            assertEquals(
                ImportLoginsState(
                    dialogState = null,
                    viewState = ImportLoginsState.ViewState.InitialContent,
                    isVaultSyncing = false,
                    showBottomSheet = false,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ConfirmImportLater sets dialog state to null and sends NavigateBack event`() = runTest {
        val viewModel = createViewModel()
        turbineScope {
            val stateFlow = viewModel.stateFlow.testIn(backgroundScope)
            val eventFlow = viewModel.eventFlow.testIn(backgroundScope)
            // Initial state
            assertEquals(DEFAULT_STATE, stateFlow.awaitItem())

            // Set the dialog state to ImportLater
            viewModel.trySendAction(ImportLoginsAction.ImportLaterClick)
            assertEquals(
                ImportLoginsState(
                    dialogState = ImportLoginsState.DialogState.ImportLater,
                    viewState = ImportLoginsState.ViewState.InitialContent,
                    isVaultSyncing = false,
                    showBottomSheet = false,
                ),
                stateFlow.awaitItem(),
            )
            viewModel.trySendAction(ImportLoginsAction.ConfirmImportLater)
            assertEquals(
                ImportLoginsState(
                    dialogState = null,
                    viewState = ImportLoginsState.ViewState.InitialContent,
                    isVaultSyncing = false,
                    showBottomSheet = false,
                ),
                stateFlow.awaitItem(),
            )
            assertEquals(
                ImportLoginsEvent.NavigateBack,
                eventFlow.awaitItem(),
            )
        }
    }

    @Test
    fun `ConfirmGetStarted sets dialog state to null and view state to step one`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            // Initial state
            assertEquals(DEFAULT_STATE, awaitItem())

            // Set the dialog state to GetStarted
            viewModel.trySendAction(ImportLoginsAction.GetStartedClick)
            assertEquals(
                ImportLoginsState(
                    dialogState = ImportLoginsState.DialogState.GetStarted,
                    viewState = ImportLoginsState.ViewState.InitialContent,
                    isVaultSyncing = false,
                    showBottomSheet = false,
                ),
                awaitItem(),
            )
            viewModel.trySendAction(ImportLoginsAction.ConfirmGetStarted)
            assertEquals(
                ImportLoginsState(
                    dialogState = null,
                    viewState = ImportLoginsState.ViewState.ImportStepOne,
                    isVaultSyncing = false,
                    showBottomSheet = false,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `CloseClick sends NavigateBack event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ImportLoginsAction.CloseClick)
            assertEquals(
                ImportLoginsEvent.NavigateBack,
                awaitItem(),
            )
        }
    }

    @Test
    fun `HelpClick sends OpenHelpLink event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ImportLoginsAction.HelpClick)
            assertEquals(
                ImportLoginsEvent.OpenHelpLink,
                awaitItem(),
            )
        }
    }

    @Test
    fun `MoveToStepOne sets view state to ImportStepOne`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(ImportLoginsAction.MoveToStepOne)
        assertEquals(
            ImportLoginsState(
                dialogState = null,
                viewState = ImportLoginsState.ViewState.ImportStepOne,
                isVaultSyncing = false,
                showBottomSheet = false,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `MoveToStepTwo sets view state to ImportStepTwo`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(ImportLoginsAction.MoveToStepTwo)
        assertEquals(
            ImportLoginsState(
                dialogState = null,
                viewState = ImportLoginsState.ViewState.ImportStepTwo,
                isVaultSyncing = false,
                showBottomSheet = false,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `MoveToStepThree sets view state to ImportStepThree`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(ImportLoginsAction.MoveToStepThree)
        assertEquals(
            ImportLoginsState(
                dialogState = null,
                viewState = ImportLoginsState.ViewState.ImportStepThree,
                isVaultSyncing = false,
                showBottomSheet = false,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `MoveToInitialContent sets view state to InitialContent`() {
        val viewModel = createViewModel()
        // first set to step one
        viewModel.trySendAction(ImportLoginsAction.MoveToStepOne)
        assertTrue(viewModel.stateFlow.value.viewState is ImportLoginsState.ViewState.ImportStepOne)
        // now move back to intial
        viewModel.trySendAction(ImportLoginsAction.MoveToInitialContent)
        assertEquals(
            ImportLoginsState(
                dialogState = null,
                viewState = ImportLoginsState.ViewState.InitialContent,
                isVaultSyncing = false,
                showBottomSheet = false,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `MoveToSyncInProgress sets isVaultSyncing to true and calls syncForResult`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(ImportLoginsAction.MoveToSyncInProgress)
        assertEquals(
            ImportLoginsState(
                dialogState = null,
                viewState = ImportLoginsState.ViewState.InitialContent,
                isVaultSyncing = true,
                showBottomSheet = false,
            ),
            viewModel.stateFlow.value,
        )
        coVerify { vaultRepository.syncForResult() }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `RetryVaultSync sets isVaultSyncing to true and clears dialog state and calls syncForResult`() =
        runTest {
            coEvery { vaultRepository.syncForResult() } returns SyncVaultDataResult.Error(Exception())
            val viewModel = createViewModel()
            viewModel.trySendAction(ImportLoginsAction.MoveToSyncInProgress)
            viewModel.stateFlow.test {
                assertNotNull(awaitItem().dialogState)
                coEvery { vaultRepository.syncForResult() } returns SyncVaultDataResult.Success
                viewModel.trySendAction(ImportLoginsAction.RetryVaultSync)
                assertEquals(
                    ImportLoginsState(
                        dialogState = null,
                        viewState = ImportLoginsState.ViewState.InitialContent,
                        isVaultSyncing = true,
                        showBottomSheet = false,
                    ),
                    awaitItem(),
                )
            }
            coVerify { vaultRepository.syncForResult() }
        }

    @Test
    fun `SyncVaultDataResult success should update state to show bottom sheet`() {
        val viewModel = createViewModel()
            viewModel.trySendAction(ImportLoginsAction.MoveToSyncInProgress)
        assertEquals(
            ImportLoginsState(
                dialogState = null,
                viewState = ImportLoginsState.ViewState.InitialContent,
                isVaultSyncing = false,
                showBottomSheet = true,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `SyncVaultDataResult Error should remove loading state and show error dialog`() = runTest {
        coEvery { vaultRepository.syncForResult() } returns SyncVaultDataResult.Error(Exception())
        val viewModel = createViewModel()
        viewModel.trySendAction(ImportLoginsAction.MoveToSyncInProgress)
        assertEquals(
            ImportLoginsState(
                dialogState = ImportLoginsState.DialogState.Error,
                viewState = ImportLoginsState.ViewState.InitialContent,
                isVaultSyncing = false,
                showBottomSheet = false,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `FailSyncAcknowledged should remove dialog state and send NavigateBack event`() =
        runTest {
            coEvery {
                vaultRepository.syncForResult()
            } returns SyncVaultDataResult.Error(Exception())
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(ImportLoginsAction.MoveToSyncInProgress)
                assertNotNull(viewModel.stateFlow.value.dialogState)
                viewModel.trySendAction(ImportLoginsAction.FailedSyncAcknowledged)
                assertEquals(
                    ImportLoginsState(
                        dialogState = null,
                        viewState = ImportLoginsState.ViewState.InitialContent,
                        isVaultSyncing = false,
                        showBottomSheet = false,
                    ),
                    viewModel.stateFlow.value,
                )
                assertEquals(
                    ImportLoginsEvent.NavigateBack,
                    awaitItem(),
                )
            }
        }

    @Test
    fun `SuccessfulSyncAcknowledged should hide bottom sheet and send NavigateBack`() = runTest {
        val viewModel = createViewModel()
        turbineScope {
            val stateFlow = viewModel.stateFlow.testIn(backgroundScope)
            val eventFlow = viewModel.eventFlow.testIn(backgroundScope)
            // Initial state
            assertEquals(DEFAULT_STATE, stateFlow.awaitItem())
            viewModel.trySendAction(ImportLoginsAction.MoveToSyncInProgress)
            assertEquals(
                ImportLoginsState(
                    dialogState = null,
                    viewState = ImportLoginsState.ViewState.InitialContent,
                    isVaultSyncing = false,
                    showBottomSheet = true,
                ),
                stateFlow.awaitItem(),
            )
            viewModel.trySendAction(ImportLoginsAction.SuccessfulSyncAcknowledged)
            assertEquals(
                ImportLoginsState(
                    dialogState = null,
                    viewState = ImportLoginsState.ViewState.InitialContent,
                    isVaultSyncing = false,
                    showBottomSheet = false,
                ),
                stateFlow.awaitItem(),
            )
            assertEquals(ImportLoginsEvent.NavigateBack, eventFlow.awaitItem())
        }
    }

    private fun createViewModel(): ImportLoginsViewModel = ImportLoginsViewModel(
        vaultRepository = vaultRepository,
    )
}

private val DEFAULT_STATE = ImportLoginsState(
    dialogState = null,
    viewState = ImportLoginsState.ViewState.InitialContent,
    isVaultSyncing = false,
    showBottomSheet = false,
)
