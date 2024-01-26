package com.x8bit.bitwarden.ui.platform.feature.settings.folders.addedit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.DateTime
import com.bitwarden.core.FolderView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.model.FolderAddEditType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FolderAddEditViewModelTest : BaseViewModelTest() {

    private val mutableFoldersStateFlow =
        MutableStateFlow<DataState<FolderView?>>(DataState.Loading)

    private val vaultRepository: VaultRepository = mockk {
        every { getVaultFolderStateFlow(DEFAULT_EDIT_ITEM_ID) } returns mutableFoldersStateFlow
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
    fun `DeleteClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(FolderAddEditAction.DeleteClick)
            assertEquals(FolderAddEditEvent.ShowToast("Not yet implemented.".asText()), awaitItem())
        }
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
    fun `SaveClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(FolderAddEditAction.SaveClick)
            assertEquals(FolderAddEditEvent.ShowToast("Not yet implemented.".asText()), awaitItem())
        }
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
                    R.string.generic_error_message.asText(),
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
                    DEFAULT_EDIT_ITEM_ID,
                    DEFAULT_FOLDER_NAME,
                    revisionDate = DateTime.now(),
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
                    message = R.string.generic_error_message.asText(),
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
                    R.string.internet_connection_required_title
                        .asText()
                        .concat(R.string.internet_connection_required_message.asText()),
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
                    DEFAULT_EDIT_ITEM_ID,
                    DEFAULT_FOLDER_NAME,
                    revisionDate = DateTime.now(),
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
                    message = R.string.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    private fun createSavedStateHandleWithState(
        state: FolderAddEditState? = DEFAULT_STATE,
    ) = SavedStateHandle().apply {
        val folderAddEditType = state?.folderAddEditType
            ?: FolderAddEditType.AddItem

        set("state", state)
        set(
            "folder_add_edit_type",
            when (folderAddEditType) {
                FolderAddEditType.AddItem -> "add"
                is FolderAddEditType.EditItem -> "edit"
            },
        )
        set("folder_edit_id", (folderAddEditType as? FolderAddEditType.EditItem)?.folderId)
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = createSavedStateHandleWithState(),
    ): FolderAddEditViewModel = FolderAddEditViewModel(
        savedStateHandle = savedStateHandle,
        vaultRepository = vaultRepository,
    )
}

private val DEFAULT_STATE = FolderAddEditState(
    viewState = FolderAddEditState.ViewState.Loading,
    dialog = FolderAddEditState.DialogState.Loading("Loading".asText()),
    folderAddEditType = FolderAddEditType.AddItem,
)

private const val DEFAULT_EDIT_ITEM_ID = "edit_id"
private const val DEFAULT_FOLDER_NAME = "test_name"
