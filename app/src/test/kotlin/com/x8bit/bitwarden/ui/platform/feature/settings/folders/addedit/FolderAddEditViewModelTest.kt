package com.x8bit.bitwarden.ui.platform.feature.settings.folders.addedit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.CreateFolderResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteFolderResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateFolderResult
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.model.FolderAddEditType
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Suppress("LargeClass")
class FolderAddEditViewModelTest : BaseViewModelTest() {

    private val mutableFoldersStateFlow =
        MutableStateFlow<DataState<FolderView?>>(DataState.Loading)

    private val vaultRepository: VaultRepository = mockk {
        every { getVaultFolderStateFlow(DEFAULT_EDIT_ITEM_ID) } returns mutableFoldersStateFlow
    }
    private val relayManager: SnackbarRelayManager<SnackbarRelay> = mockk {
        every { sendSnackbarData(data = any(), relay = any()) } just runs
    }

    @BeforeEach
    fun setup() {
        mockkStatic(SavedStateHandle::toFolderAddEditArgs)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(SavedStateHandle::toFolderAddEditArgs)
    }

    @Test
    fun `initial add state should be correct`() = runTest {
        val folderAddEditType = FolderAddEditType.AddItem

        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = DEFAULT_STATE.copy(
                    folderAddEditType = folderAddEditType,
                ),
            ),
        )
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        verify(exactly = 0) {
            vaultRepository.getVaultItemStateFlow(DEFAULT_EDIT_ITEM_ID)
        }
    }

    @Test
    fun `initial edit state should be correct`() = runTest {
        val folderAddEditType = FolderAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID)

        val initState = DEFAULT_STATE.copy(
            folderAddEditType = folderAddEditType,
        )
        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = DEFAULT_STATE.copy(
                    folderAddEditType = folderAddEditType,
                ),
            ),
        )
        assertEquals(
            initState.copy(viewState = FolderAddEditState.ViewState.Loading),
            viewModel.stateFlow.value,
        )
        verify(exactly = 1) {
            vaultRepository.getVaultFolderStateFlow(DEFAULT_EDIT_ITEM_ID)
        }
    }

    @Test
    fun `CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(FolderAddEditAction.CloseClick)
            assertEquals(FolderAddEditEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `DeleteClick with DeleteFolderResult Success should emit toast and navigate back`() =
        runTest {
            val viewModel = createViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = DEFAULT_STATE.copy(
                        folderAddEditType = FolderAddEditType.EditItem((DEFAULT_EDIT_ITEM_ID)),
                    ),
                ),
            )

            mutableFoldersStateFlow.value =
                DataState.Loaded(
                    FolderView(
                        id = DEFAULT_EDIT_ITEM_ID,
                        name = DEFAULT_FOLDER_NAME,
                        revisionDate = FIXED_CLOCK.instant(),
                    ),
                )

            coEvery {
                vaultRepository.deleteFolder(folderId = DEFAULT_EDIT_ITEM_ID)
            } returns DeleteFolderResult.Success

            viewModel.trySendAction(FolderAddEditAction.DeleteClick)

            viewModel.eventFlow.test {
                assertEquals(
                    FolderAddEditEvent.NavigateBack,
                    awaitItem(),
                )
            }
            verify(exactly = 1) {
                relayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(BitwardenString.folder_deleted.asText()),
                    relay = SnackbarRelay.FOLDER_DELETED,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `DeleteClick with DeleteFolderResult Success should show dialog, and remove it once an item is deleted`() =
        runTest {
            val stateWithDialog = FolderAddEditState(
                folderAddEditType = FolderAddEditType.EditItem((DEFAULT_EDIT_ITEM_ID)),
                dialog = FolderAddEditState.DialogState.Loading(
                    BitwardenString.deleting.asText(),
                ),
                viewState = FolderAddEditState.ViewState.Content(
                    folderName = DEFAULT_FOLDER_NAME,
                ),
                parentFolderName = null,
            )

            val stateWithoutDialog = stateWithDialog.copy(
                dialog = null,
            )

            val viewModel = createViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = stateWithoutDialog,
                ),
            )

            mutableFoldersStateFlow.value =
                DataState.Loaded(
                    FolderView(
                        id = DEFAULT_EDIT_ITEM_ID,
                        name = DEFAULT_FOLDER_NAME,
                        revisionDate = FIXED_CLOCK.instant(),
                    ),
                )

            coEvery {
                vaultRepository.deleteFolder(folderId = DEFAULT_EDIT_ITEM_ID)
            } returns DeleteFolderResult.Success

            viewModel.stateFlow.test {
                viewModel.trySendAction(FolderAddEditAction.DeleteClick)
                assertEquals(stateWithoutDialog, awaitItem())
                assertEquals(stateWithDialog, awaitItem())
                assertEquals(stateWithoutDialog, awaitItem())
            }
            verify(exactly = 1) {
                relayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(BitwardenString.folder_deleted.asText()),
                    relay = SnackbarRelay.FOLDER_DELETED,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `DeleteClick should not call deleteFolder if no folderId is present`() =
        runTest {
            val state = FolderAddEditState(
                folderAddEditType = FolderAddEditType.AddItem,
                dialog = null,
                viewState = FolderAddEditState.ViewState.Error(
                    BitwardenString.generic_error_message.asText(),
                ),
                parentFolderName = null,
            )

            val viewModel = createViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = state,
                ),
            )

            viewModel.trySendAction(FolderAddEditAction.DeleteClick)

            coVerify(exactly = 0) {
                vaultRepository.deleteFolder(any())
            }
        }

    @Test
    fun `DeleteClick with DeleteFolderResult Failure should show an error dialog`() =
        runTest {
            val error = Throwable("Oops")
            val stateWithDialog = FolderAddEditState(
                folderAddEditType = FolderAddEditType.EditItem((DEFAULT_EDIT_ITEM_ID)),
                dialog = FolderAddEditState.DialogState.Error(
                    BitwardenString.generic_error_message.asText(),
                    throwable = error,
                ),
                viewState = FolderAddEditState.ViewState.Content(
                    folderName = DEFAULT_FOLDER_NAME,
                ),
                parentFolderName = null,
            )

            val stateWithoutDialog = stateWithDialog.copy(
                dialog = null,
            )

            val viewModel = createViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = stateWithoutDialog,
                ),
            )

            mutableFoldersStateFlow.value =
                DataState.Loaded(
                    FolderView(
                        id = DEFAULT_EDIT_ITEM_ID,
                        name = DEFAULT_FOLDER_NAME,
                        revisionDate = FIXED_CLOCK.instant(),
                    ),
                )

            coEvery {
                vaultRepository.deleteFolder(folderId = DEFAULT_EDIT_ITEM_ID)
            } returns DeleteFolderResult.Error(error = error)

            viewModel.trySendAction(FolderAddEditAction.DeleteClick)

            assertEquals(
                stateWithDialog,
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `SaveClick with empty name should show an error dialog`() =
        runTest {
            val stateWithoutName = FolderAddEditState(
                folderAddEditType = FolderAddEditType.AddItem,
                dialog = null,
                viewState = FolderAddEditState.ViewState.Content(
                    folderName = "",
                ),
                parentFolderName = null,
            )

            val stateWithDialog = stateWithoutName.copy(
                dialog = FolderAddEditState.DialogState.Error(
                    BitwardenString.validation_field_required
                        .asText(BitwardenString.name.asText()),
                ),
            )

            val viewModel = createViewModel(
                createSavedStateHandleWithState(
                    state = stateWithoutName,
                ),
            )

            assertEquals(stateWithoutName, viewModel.stateFlow.value)

            viewModel.trySendAction(FolderAddEditAction.SaveClick)

            assertEquals(stateWithDialog, viewModel.stateFlow.value)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in add mode, SaveClick createFolder success should show dialog, and remove it once an item is saved`() =
        runTest {
            val stateWithDialog = FolderAddEditState(
                folderAddEditType = FolderAddEditType.AddItem,
                dialog = FolderAddEditState.DialogState.Loading(
                    BitwardenString.saving.asText(),
                ),
                viewState = FolderAddEditState.ViewState.Content(
                    folderName = DEFAULT_FOLDER_NAME,
                ),
                parentFolderName = null,
            )

            val stateWithoutDialog = stateWithDialog.copy(
                dialog = null,
            )

            val viewModel = createViewModel(
                createSavedStateHandleWithState(
                    state = stateWithoutDialog,
                ),
            )

            coEvery {
                vaultRepository.createFolder(any())
            } returns CreateFolderResult.Success(mockk())

            viewModel.stateFlow.test {
                viewModel.trySendAction(FolderAddEditAction.SaveClick)
                assertEquals(stateWithoutDialog, awaitItem())
                assertEquals(stateWithDialog, awaitItem())
                assertEquals(stateWithoutDialog, awaitItem())
            }
            verify(exactly = 1) {
                relayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(BitwardenString.folder_created.asText()),
                    relay = SnackbarRelay.FOLDER_CREATED,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in add mode, SaveClick createFolder with no parentFolderNamePresent should just create folder with entered name`() =
        runTest {
            val viewModel =
                createViewModel(
                    createSavedStateHandleWithState(
                        FolderAddEditState(
                            folderAddEditType = FolderAddEditType.AddItem,
                            viewState = FolderAddEditState.ViewState.Content(
                                folderName = DEFAULT_FOLDER_NAME,
                            ),
                            dialog = null,
                            parentFolderName = null,
                        ),
                    ),
                )

            coEvery {
                vaultRepository.createFolder(any())
            } returns CreateFolderResult.Success(mockk())
            viewModel.trySendAction(FolderAddEditAction.SaveClick)
            coVerify(exactly = 1) {
                vaultRepository.createFolder(
                    folderView = FolderView(
                        name = DEFAULT_FOLDER_NAME,
                        id = null,
                        revisionDate = FIXED_CLOCK.instant(),
                    ),
                )
            }
            verify(exactly = 1) {
                relayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(BitwardenString.folder_created.asText()),
                    relay = SnackbarRelay.FOLDER_CREATED,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in add mode, SaveClick createFolder with a parentFolderNamePresent should prepend the parent folder to the entered name`() =
        runTest {
            val parentFolderName = "parent/folder"
            val viewModel =
                createViewModel(
                    createSavedStateHandleWithState(
                        FolderAddEditState(
                            folderAddEditType = FolderAddEditType.AddItem,
                            viewState = FolderAddEditState.ViewState.Content(
                                folderName = DEFAULT_FOLDER_NAME,
                            ),
                            dialog = null,
                            parentFolderName = parentFolderName,
                        ),
                    ),
                )

            coEvery {
                vaultRepository.createFolder(any())
            } returns CreateFolderResult.Success(mockk())
            viewModel.trySendAction(FolderAddEditAction.SaveClick)
            coVerify(exactly = 1) {
                vaultRepository.createFolder(
                    folderView = FolderView(
                        name = "$parentFolderName/$DEFAULT_FOLDER_NAME",
                        id = null,
                        revisionDate = FIXED_CLOCK.instant(),
                    ),
                )
            }
            verify(exactly = 1) {
                relayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(BitwardenString.folder_created.asText()),
                    relay = SnackbarRelay.FOLDER_CREATED,
                )
            }
        }

    @Test
    fun `in add mode, SaveClick createFolder error should show an error dialog`() = runTest {
        val state = FolderAddEditState(
            folderAddEditType = FolderAddEditType.AddItem,
            dialog = null,
            viewState = FolderAddEditState.ViewState.Content(
                folderName = DEFAULT_FOLDER_NAME,
            ),
            parentFolderName = null,
        )

        val viewModel = createViewModel(
            createSavedStateHandleWithState(
                state = state,
            ),
        )

        val error = Throwable("Oops")
        coEvery {
            vaultRepository.createFolder(any())
        } returns CreateFolderResult.Error(error = error)

        viewModel.trySendAction(FolderAddEditAction.SaveClick)

        assertEquals(
            state.copy(
                dialog = FolderAddEditState.DialogState.Error(
                    BitwardenString.generic_error_message.asText(),
                    throwable = error,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `in edit mode, SaveClick should show dialog, and remove it once an item is saved`() =
        runTest {
            val stateWithDialog = FolderAddEditState(
                folderAddEditType = FolderAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID),
                dialog = FolderAddEditState.DialogState.Loading(
                    BitwardenString.saving.asText(),
                ),
                viewState = FolderAddEditState.ViewState.Content(
                    folderName = DEFAULT_FOLDER_NAME,
                ),
                parentFolderName = null,
            )

            val stateWithoutDialog = stateWithDialog.copy(
                folderAddEditType = FolderAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID),
                dialog = null,
                viewState = FolderAddEditState.ViewState.Content(
                    folderName = DEFAULT_FOLDER_NAME,
                ),
            )

            val viewModel = createViewModel(
                savedStateHandle = createSavedStateHandleWithState(
                    state = stateWithoutDialog,
                ),
            )

            mutableFoldersStateFlow.value =
                DataState.Loaded(
                    FolderView(
                        id = DEFAULT_EDIT_ITEM_ID,
                        name = DEFAULT_FOLDER_NAME,
                        revisionDate = FIXED_CLOCK.instant(),
                    ),
                )

            coEvery {
                vaultRepository.updateFolder(any(), any())
            } returns UpdateFolderResult.Success(mockk())

            viewModel.stateFlow.test {
                viewModel.trySendAction(FolderAddEditAction.SaveClick)
                assertEquals(stateWithoutDialog, awaitItem())
                assertEquals(stateWithDialog, awaitItem())
                assertEquals(stateWithoutDialog, awaitItem())
            }
            verify(exactly = 1) {
                relayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(BitwardenString.folder_updated.asText()),
                    relay = SnackbarRelay.FOLDER_UPDATED,
                )
            }
        }

    @Test
    fun `in edit mode, SaveClick updateFolder error should show an error dialog`() = runTest {
        val state = FolderAddEditState(
            folderAddEditType = FolderAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID),
            dialog = null,
            viewState = FolderAddEditState.ViewState.Content(
                folderName = DEFAULT_FOLDER_NAME,
            ),
            parentFolderName = null,
        )

        val viewModel = createViewModel(
            createSavedStateHandleWithState(
                state = state,
            ),
        )
        val error = Throwable("Oops")

        mutableFoldersStateFlow.value =
            DataState.Loaded(
                FolderView(
                    id = DEFAULT_EDIT_ITEM_ID,
                    name = DEFAULT_FOLDER_NAME,
                    revisionDate = FIXED_CLOCK.instant(),
                ),
            )

        coEvery {
            vaultRepository.updateFolder(any(), any())
        } returns UpdateFolderResult.Error(errorMessage = null, error = error)

        viewModel.trySendAction(FolderAddEditAction.SaveClick)

        assertEquals(
            state.copy(
                dialog = FolderAddEditState.DialogState.Error(
                    message = BitwardenString.generic_error_message.asText(),
                    throwable = error,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `DismissDialog should emit update dialog state to null`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(FolderAddEditAction.DismissDialog)

        val expectedState = DEFAULT_STATE.copy(
            dialog = null,
        )

        assertEquals(
            expectedState,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `NameTextChange should update name`() = runTest {
        val viewModel = createViewModel()

        val expectedState = DEFAULT_STATE.copy(
            viewState = FolderAddEditState.ViewState.Content(
                folderName = "NewName",
            ),
        )

        viewModel.trySendAction(FolderAddEditAction.NameTextChange("NewName"))

        assertEquals(
            expectedState,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `folderStateFlow Error should update state to error`() {
        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = DEFAULT_STATE.copy(
                    folderAddEditType = FolderAddEditType.EditItem((DEFAULT_EDIT_ITEM_ID)),
                ),
            ),
        )

        mutableFoldersStateFlow.tryEmit(
            value = DataState.Error(
                data = null,
                error = IllegalStateException(),
            ),
        )

        assertEquals(
            DEFAULT_STATE.copy(
                folderAddEditType = FolderAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID),
                viewState = FolderAddEditState.ViewState.Error(
                    BitwardenString.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `folderStateFlow Loaded with data should update state to content`() {
        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = DEFAULT_STATE.copy(
                    folderAddEditType = FolderAddEditType.EditItem((DEFAULT_EDIT_ITEM_ID)),
                ),
            ),
        )

        mutableFoldersStateFlow.tryEmit(
            DataState.Loaded(
                FolderView(
                    id = DEFAULT_EDIT_ITEM_ID,
                    name = DEFAULT_FOLDER_NAME,
                    revisionDate = FIXED_CLOCK.instant(),
                ),
            ),
        )
        assertEquals(
            DEFAULT_STATE.copy(
                folderAddEditType = FolderAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID),
                viewState = FolderAddEditState.ViewState.Content(
                    folderName = DEFAULT_FOLDER_NAME,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `folderStateFlow Loaded with empty data should update state to error`() {
        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = DEFAULT_STATE.copy(
                    folderAddEditType = FolderAddEditType.EditItem((DEFAULT_EDIT_ITEM_ID)),
                ),
            ),
        )

        mutableFoldersStateFlow.tryEmit(DataState.Loaded(null))
        assertEquals(
            DEFAULT_STATE.copy(
                folderAddEditType = FolderAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID),
                viewState = FolderAddEditState.ViewState.Error(
                    message = BitwardenString.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `folderStateFlow Loading should update the state to Loading`() {
        val viewModel = createViewModel()

        mutableFoldersStateFlow.tryEmit(
            DataState.Loading,
        )

        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `folderStateFlow NoNetwork should update the state to Error`() {
        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = DEFAULT_STATE.copy(
                    folderAddEditType = FolderAddEditType.EditItem((DEFAULT_EDIT_ITEM_ID)),
                ),
            ),
        )

        mutableFoldersStateFlow.tryEmit(
            value = DataState.NoNetwork(
                data = null,
            ),
        )

        assertEquals(
            DEFAULT_STATE.copy(
                folderAddEditType = FolderAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID),
                viewState = FolderAddEditState.ViewState.Error(
                    BitwardenString.internet_connection_required_title
                        .asText()
                        .concat(
                            " ".asText(),
                            BitwardenString.internet_connection_required_message.asText(),
                        ),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `folderStateFlow Pending should update the state to Content`() {
        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = DEFAULT_STATE.copy(
                    folderAddEditType = FolderAddEditType.EditItem((DEFAULT_EDIT_ITEM_ID)),
                ),
            ),
        )

        mutableFoldersStateFlow.tryEmit(
            value = DataState.Pending(
                FolderView(
                    id = DEFAULT_EDIT_ITEM_ID,
                    name = DEFAULT_FOLDER_NAME,
                    revisionDate = FIXED_CLOCK.instant(),
                ),
            ),
        )

        assertEquals(
            DEFAULT_STATE.copy(
                folderAddEditType = FolderAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID),
                viewState = FolderAddEditState.ViewState.Content(
                    folderName = DEFAULT_FOLDER_NAME,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `folderStateFlow Pending with empty data should update state to error`() {
        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = DEFAULT_STATE.copy(
                    folderAddEditType = FolderAddEditType.EditItem((DEFAULT_EDIT_ITEM_ID)),
                ),
            ),
        )

        mutableFoldersStateFlow.tryEmit(DataState.Pending(null))
        assertEquals(
            DEFAULT_STATE.copy(
                folderAddEditType = FolderAddEditType.EditItem(DEFAULT_EDIT_ITEM_ID),
                viewState = FolderAddEditState.ViewState.Error(
                    message = BitwardenString.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    private fun createSavedStateHandleWithState(
        state: FolderAddEditState? = DEFAULT_STATE,
    ) = SavedStateHandle().apply {
        val folderAddEditType = state?.folderAddEditType ?: FolderAddEditType.AddItem
        set("state", state)
        every { toFolderAddEditArgs() } returns FolderAddEditArgs(
            folderAddEditType = folderAddEditType,
            parentFolderName = null,
        )
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = createSavedStateHandleWithState(),
    ): FolderAddEditViewModel = FolderAddEditViewModel(
        savedStateHandle = savedStateHandle,
        clock = FIXED_CLOCK,
        vaultRepository = vaultRepository,
        relayManager = relayManager,
    )
}

private val DEFAULT_STATE = FolderAddEditState(
    viewState = FolderAddEditState.ViewState.Loading,
    dialog = FolderAddEditState.DialogState.Loading("Loading".asText()),
    folderAddEditType = FolderAddEditType.AddItem,
    parentFolderName = null,
)

private val FIXED_CLOCK = Clock.fixed(
    Instant.parse("2025-04-11T10:15:30.00Z"),
    ZoneOffset.UTC,
)

private const val DEFAULT_EDIT_ITEM_ID = "edit_id"
private const val DEFAULT_FOLDER_NAME = "test_name"
