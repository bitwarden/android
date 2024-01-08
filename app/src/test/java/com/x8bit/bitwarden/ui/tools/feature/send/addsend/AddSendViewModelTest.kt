package com.x8bit.bitwarden.ui.tools.feature.send.addsend

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.SendView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.CreateSendResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.util.toSendView
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AddSendViewModelTest : BaseViewModelTest() {

    private val vaultRepository: VaultRepository = mockk()

    @BeforeEach
    fun setup() {
        mockkStatic(ADD_SEND_STATE_EXTENSIONS_PATH)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(ADD_SEND_STATE_EXTENSIONS_PATH)
    }

    @Test
    fun `initial state should be correct`() {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should read from saved state when present`() {
        val savedState = mockk<AddSendState>()
        val viewModel = createViewModel(savedState)
        assertEquals(savedState, viewModel.stateFlow.value)
    }

    @Test
    fun `CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AddSendAction.CloseClick)
            assertEquals(AddSendEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `SaveClick with createSend success should emit NavigateBack`() = runTest {
        val viewState = DEFAULT_VIEW_STATE.copy(
            common = DEFAULT_COMMON_STATE.copy(name = "input"),
        )
        val initialState = DEFAULT_STATE.copy(viewState = viewState)
        val mockSendView = mockk<SendView>()
        every { viewState.toSendView() } returns mockSendView
        coEvery { vaultRepository.createSend(mockSendView) } returns CreateSendResult.Success
        val viewModel = createViewModel(initialState)

        viewModel.eventFlow.test {
            viewModel.trySendAction(AddSendAction.SaveClick)
            assertEquals(AddSendEvent.NavigateBack, awaitItem())
        }
        assertEquals(initialState, viewModel.stateFlow.value)
        coVerify(exactly = 1) {
            vaultRepository.createSend(mockSendView)
        }
    }

    @Test
    fun `SaveClick with createSend failure should show error dialog`() = runTest {
        val viewState = DEFAULT_VIEW_STATE.copy(
            common = DEFAULT_COMMON_STATE.copy(name = "input"),
        )
        val initialState = DEFAULT_STATE.copy(viewState = viewState)
        val mockSendView = mockk<SendView>()
        every { viewState.toSendView() } returns mockSendView
        coEvery { vaultRepository.createSend(mockSendView) } returns CreateSendResult.Error
        val viewModel = createViewModel(initialState)

        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            viewModel.trySendAction(AddSendAction.SaveClick)
            assertEquals(
                initialState.copy(
                    dialogState = AddSendState.DialogState.Loading(
                        message = R.string.saving.asText(),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                initialState.copy(
                    dialogState = AddSendState.DialogState.Error(
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.generic_error_message.asText(),
                    ),
                ),
                awaitItem(),
            )
        }
        coVerify(exactly = 1) {
            vaultRepository.createSend(mockSendView)
        }
    }

    @Test
    fun `SaveClick with blank name should show error dialog`() {
        val viewModel = createViewModel(DEFAULT_STATE)

        viewModel.trySendAction(AddSendAction.SaveClick)

        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = AddSendState.DialogState.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.validation_field_required.asText(
                        R.string.name.asText(),
                    ),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `DismissDialogClick should clear the dialog state`() {
        val viewModel = createViewModel(
            DEFAULT_STATE.copy(
                dialogState = AddSendState.DialogState.Error(
                    title = "Fail Title".asText(),
                    message = "Fail Message".asText(),
                ),
            ),
        )
        viewModel.trySendAction(AddSendAction.DismissDialogClick)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `ChooseFileClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AddSendAction.ChooseFileClick)
            assertEquals(AddSendEvent.ShowToast("Not Implemented: File Upload"), awaitItem())
        }
    }

    @Test
    fun `FileTypeClick and TextTypeClick should toggle sendType`() = runTest {
        val viewModel = createViewModel()
        val expectedViewState = DEFAULT_VIEW_STATE.copy(
            selectedType = AddSendState.ViewState.Content.SendType.File,
        )

        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(AddSendAction.FileTypeClick)
            assertEquals(DEFAULT_STATE.copy(viewState = expectedViewState), awaitItem())
            viewModel.trySendAction(AddSendAction.TextTypeClick)
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    @Test
    fun `NameChange should update name input`() = runTest {
        val viewModel = createViewModel()
        val expectedViewState = DEFAULT_VIEW_STATE.copy(
            common = DEFAULT_COMMON_STATE.copy(name = "input"),
        )

        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(AddSendAction.NameChange("input"))
            assertEquals(DEFAULT_STATE.copy(viewState = expectedViewState), awaitItem())
        }
    }

    @Test
    fun `MaxAccessCountChange should update maxAccessCount`() = runTest {
        val viewModel = createViewModel()
        val expectedViewState = DEFAULT_VIEW_STATE.copy(
            common = DEFAULT_COMMON_STATE.copy(maxAccessCount = 5),
        )

        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(AddSendAction.MaxAccessCountChange(5))
            assertEquals(DEFAULT_STATE.copy(viewState = expectedViewState), awaitItem())
        }
    }

    @Test
    fun `TextChange should update text input`() = runTest {
        val viewModel = createViewModel()
        val expectedViewState = DEFAULT_VIEW_STATE.copy(
            selectedType = AddSendState.ViewState.Content.SendType.Text(
                input = "input",
                isHideByDefaultChecked = false,
            ),
        )

        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(AddSendAction.TextChange("input"))
            assertEquals(DEFAULT_STATE.copy(viewState = expectedViewState), awaitItem())
        }
    }

    @Test
    fun `NoteChange should update note input`() = runTest {
        val viewModel = createViewModel()
        val expectedViewState = DEFAULT_VIEW_STATE.copy(
            common = DEFAULT_COMMON_STATE.copy(noteInput = "input"),
        )

        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(AddSendAction.NoteChange("input"))
            assertEquals(DEFAULT_STATE.copy(viewState = expectedViewState), awaitItem())
        }
    }

    @Test
    fun `PasswordChange should update note input`() = runTest {
        val viewModel = createViewModel()
        val expectedViewState = DEFAULT_VIEW_STATE.copy(
            common = DEFAULT_COMMON_STATE.copy(passwordInput = "input"),
        )

        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(AddSendAction.PasswordChange("input"))
            assertEquals(DEFAULT_STATE.copy(viewState = expectedViewState), awaitItem())
        }
    }

    @Test
    fun `DeactivateThisSendToggle should update isDeactivateChecked`() = runTest {
        val viewModel = createViewModel()
        val expectedViewState = DEFAULT_VIEW_STATE.copy(
            common = DEFAULT_COMMON_STATE.copy(isDeactivateChecked = true),
        )

        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(AddSendAction.DeactivateThisSendToggle(true))
            assertEquals(DEFAULT_STATE.copy(viewState = expectedViewState), awaitItem())
        }
    }

    @Test
    fun `HideMyEmailToggle should update isHideEmailChecked`() = runTest {
        val viewModel = createViewModel()
        val expectedViewState = DEFAULT_VIEW_STATE.copy(
            common = DEFAULT_COMMON_STATE.copy(isHideEmailChecked = true),
        )

        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(AddSendAction.HideMyEmailToggle(isChecked = true))
            assertEquals(DEFAULT_STATE.copy(viewState = expectedViewState), awaitItem())
        }
    }

    private fun createViewModel(
        state: AddSendState? = null,
    ): AddSendViewModel = AddSendViewModel(
        savedStateHandle = SavedStateHandle().apply { set("state", state) },
        vaultRepo = vaultRepository,
    )

    companion object {
        private const val ADD_SEND_STATE_EXTENSIONS_PATH: String =
            "com.x8bit.bitwarden.ui.tools.feature.send.addsend.util.AddSendStateExtensionsKt"

        private val DEFAULT_COMMON_STATE = AddSendState.ViewState.Content.Common(
            name = "",
            maxAccessCount = null,
            passwordInput = "",
            noteInput = "",
            isHideEmailChecked = false,
            isDeactivateChecked = false,
        )

        private val DEFAULT_SELECTED_TYPE_STATE = AddSendState.ViewState.Content.SendType.Text(
            input = "",
            isHideByDefaultChecked = false,
        )

        private val DEFAULT_VIEW_STATE = AddSendState.ViewState.Content(
            common = DEFAULT_COMMON_STATE,
            selectedType = DEFAULT_SELECTED_TYPE_STATE,
        )

        private val DEFAULT_STATE = AddSendState(
            viewState = DEFAULT_VIEW_STATE,
            dialogState = null,
        )
    }
}
