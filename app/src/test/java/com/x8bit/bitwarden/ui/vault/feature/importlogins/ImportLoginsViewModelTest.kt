package com.x8bit.bitwarden.ui.vault.feature.importlogins

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.SyncVaultDataResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarData
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelay
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManagerImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ImportLoginsViewModelTest : BaseViewModelTest() {

    private val vaultRepository: VaultRepository = mockk {
        coEvery { syncForResult() } returns SyncVaultDataResult.Success(itemsAvailable = true)
    }

    private val firstTimeActionManager: FirstTimeActionManager = mockk {
        every { storeShowImportLogins(any()) } just runs
        every { storeShowImportLoginsSettingsBadge(any()) } just runs
    }

    private val environmentRepository: EnvironmentRepository = mockk {
        every { environment } returns Environment.Us
    }

    @BeforeEach
    fun setUp() {
        mockkStatic(Uri::parse)
        every { Uri.parse(Environment.Us.environmentUrlData.base) } returns mockk {
            every { host } returns DEFAULT_VAULT_URL
        }
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Uri::parse)
    }

    private val snackbarRelayManager: SnackbarRelayManagerImpl = mockk {
        coEvery { sendSnackbarData(any(), any()) } just runs
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
                currentWebVaultUrl = DEFAULT_VAULT_URL,
                snackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
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
                currentWebVaultUrl = DEFAULT_VAULT_URL,
                snackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
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
                    currentWebVaultUrl = DEFAULT_VAULT_URL,
                    snackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
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
                    currentWebVaultUrl = DEFAULT_VAULT_URL,
                    snackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
                ),
                awaitItem(),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ConfirmImportLater sets dialog state to null, sends NavigateBack event, and stores first time values`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
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
                        currentWebVaultUrl = DEFAULT_VAULT_URL,
                        snackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
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
                        currentWebVaultUrl = DEFAULT_VAULT_URL,
                        snackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
                    ),
                    stateFlow.awaitItem(),
                )
                assertEquals(
                    ImportLoginsEvent.NavigateBack,
                    eventFlow.awaitItem(),
                )
            }
            verify(exactly = 1) {
                firstTimeActionManager.storeShowImportLogins(showImportLogins = false)
                firstTimeActionManager.storeShowImportLoginsSettingsBadge(showBadge = true)
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
                    currentWebVaultUrl = DEFAULT_VAULT_URL,
                    snackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
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
                    currentWebVaultUrl = DEFAULT_VAULT_URL,
                    snackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
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
                currentWebVaultUrl = DEFAULT_VAULT_URL,
                snackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
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
                currentWebVaultUrl = DEFAULT_VAULT_URL,
                snackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
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
                currentWebVaultUrl = DEFAULT_VAULT_URL,
                snackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
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
                currentWebVaultUrl = DEFAULT_VAULT_URL,
                snackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `MoveToSyncInProgress sets isVaultSyncing to true and calls syncForResult`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE,
                awaitItem(),
            )
            viewModel.trySendAction(ImportLoginsAction.MoveToSyncInProgress)
            assertEquals(
                ImportLoginsState(
                    dialogState = null,
                    viewState = ImportLoginsState.ViewState.InitialContent,
                    isVaultSyncing = true,
                    showBottomSheet = false,
                    currentWebVaultUrl = DEFAULT_VAULT_URL,
                    snackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
                ),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
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
                coEvery { vaultRepository.syncForResult() } returns SyncVaultDataResult.Success(
                    itemsAvailable = true,
                )
                viewModel.trySendAction(ImportLoginsAction.RetryVaultSync)
                assertEquals(
                    ImportLoginsState(
                        dialogState = null,
                        viewState = ImportLoginsState.ViewState.InitialContent,
                        isVaultSyncing = true,
                        showBottomSheet = false,
                        currentWebVaultUrl = DEFAULT_VAULT_URL,
                        snackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
            coVerify { vaultRepository.syncForResult() }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `SyncVaultDataResult success should update state to show bottom sheet and set first time values to false`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(ImportLoginsAction.MoveToSyncInProgress)
        assertEquals(
            ImportLoginsState(
                dialogState = null,
                viewState = ImportLoginsState.ViewState.InitialContent,
                isVaultSyncing = false,
                showBottomSheet = true,
                currentWebVaultUrl = DEFAULT_VAULT_URL,
                snackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
            ),
            viewModel.stateFlow.value,
        )
        verify {
            firstTimeActionManager.storeShowImportLogins(showImportLogins = false)
            firstTimeActionManager.storeShowImportLoginsSettingsBadge(showBadge = false)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `MoveToSyncInProgress should set no items imported error dialog state when sync succeeds but no items are available`() =
        runTest {
            coEvery {
                vaultRepository.syncForResult()
            } returns SyncVaultDataResult.Success(itemsAvailable = false)
            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_STATE,
                    awaitItem(),
                )
                viewModel.trySendAction(ImportLoginsAction.MoveToSyncInProgress)
                assertEquals(
                    ImportLoginsState(
                        dialogState = null,
                        viewState = ImportLoginsState.ViewState.InitialContent,
                        isVaultSyncing = true,
                        showBottomSheet = false,
                        currentWebVaultUrl = DEFAULT_VAULT_URL,
                        snackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
                    ),
                    awaitItem(),
                )
                assertEquals(
                    ImportLoginsState(
                        dialogState = ImportLoginsState.DialogState.Error(
                            message = R.string.no_logins_were_imported.asText(),
                            title = R.string.import_error.asText(),
                        ),
                        viewState = ImportLoginsState.ViewState.InitialContent,
                        isVaultSyncing = false,
                        showBottomSheet = false,
                        currentWebVaultUrl = DEFAULT_VAULT_URL,
                        snackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `SyncVaultDataResult Error should remove loading state and show error dialog`() = runTest {
        coEvery { vaultRepository.syncForResult() } returns SyncVaultDataResult.Error(Exception())
        val viewModel = createViewModel()
        viewModel.trySendAction(ImportLoginsAction.MoveToSyncInProgress)
        assertEquals(
            ImportLoginsState(
                dialogState = ImportLoginsState.DialogState.Error(),
                viewState = ImportLoginsState.ViewState.InitialContent,
                isVaultSyncing = false,
                showBottomSheet = false,
                currentWebVaultUrl = DEFAULT_VAULT_URL,
                snackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
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
                        currentWebVaultUrl = DEFAULT_VAULT_URL,
                        snackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
                    ),
                    viewModel.stateFlow.value,
                )
                assertEquals(
                    ImportLoginsEvent.NavigateBack,
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `SuccessfulSyncAcknowledged should hide bottom sheet and send NavigateBack event and send Snackbar data through snackbar manager`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.stateEventFlow(backgroundScope = backgroundScope) { stateFlow, eventFlow ->
                // Initial state
                assertEquals(DEFAULT_STATE, stateFlow.awaitItem())
                viewModel.trySendAction(ImportLoginsAction.MoveToSyncInProgress)
                assertEquals(
                    ImportLoginsState(
                        dialogState = null,
                        viewState = ImportLoginsState.ViewState.InitialContent,
                        isVaultSyncing = true,
                        showBottomSheet = false,
                        currentWebVaultUrl = DEFAULT_VAULT_URL,
                        snackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
                    ),
                    stateFlow.awaitItem(),
                )
                assertEquals(
                    ImportLoginsState(
                        dialogState = null,
                        viewState = ImportLoginsState.ViewState.InitialContent,
                        isVaultSyncing = false,
                        showBottomSheet = true,
                        currentWebVaultUrl = DEFAULT_VAULT_URL,
                        snackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
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
                        currentWebVaultUrl = DEFAULT_VAULT_URL,
                        snackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
                    ),
                    stateFlow.awaitItem(),
                )
                assertEquals(ImportLoginsEvent.NavigateBack, eventFlow.awaitItem())
            }
            val expectedSnackbarData = BitwardenSnackbarData(
                messageHeader = R.string.logins_imported.asText(),
                message = R.string.remember_to_delete_your_imported_password_file_from_your_computer
                    .asText(),
            )
            verify {
                snackbarRelayManager.sendSnackbarData(
                    data = expectedSnackbarData,
                    relay = SnackbarRelay.MY_VAULT_RELAY,
                )
            }
        }

    private fun createViewModel(
        snackbarRelay: SnackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
    ): ImportLoginsViewModel = ImportLoginsViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(
                "snackbarRelay" to snackbarRelay.name,
            ),
        ),
        vaultRepository = vaultRepository,
        firstTimeActionManager = firstTimeActionManager,
        environmentRepository = environmentRepository,
        snackbarRelayManager = snackbarRelayManager,
    )
}

private const val DEFAULT_VAULT_URL = "vault.bitwarden.com"

private val DEFAULT_STATE = ImportLoginsState(
    dialogState = null,
    viewState = ImportLoginsState.ViewState.InitialContent,
    isVaultSyncing = false,
    showBottomSheet = false,
    currentWebVaultUrl = DEFAULT_VAULT_URL,
    snackbarRelay = SnackbarRelay.MY_VAULT_RELAY,
)
