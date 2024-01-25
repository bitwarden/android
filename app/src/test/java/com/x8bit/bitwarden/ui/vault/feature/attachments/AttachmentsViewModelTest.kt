package com.x8bit.bitwarden.ui.vault.feature.attachments

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.CipherView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DeleteAttachmentResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.vault.feature.attachments.util.toViewState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AttachmentsViewModelTest : BaseViewModelTest() {
    private val mutableVaultItemStateFlow =
        MutableStateFlow<DataState<CipherView?>>(DataState.Loading)
    private val vaultRepository: VaultRepository = mockk {
        every { getVaultItemStateFlow(any()) } returns mutableVaultItemStateFlow
    }

    @BeforeEach
    fun setup() {
        mockkStatic(CipherView::toViewState)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(CipherView::toViewState)
    }

    @Test
    fun `initial state should be correct when state is null`() = runTest {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct when state is set`() = runTest {
        val initialState = DEFAULT_STATE.copy(cipherId = "123456789")
        val viewModel = createViewModel(initialState)
        assertEquals(initialState, viewModel.stateFlow.value)
    }

    @Test
    fun `BackClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AttachmentsAction.BackClick)
            assertEquals(AttachmentsEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `SaveClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AttachmentsAction.SaveClick)
            assertEquals(AttachmentsEvent.ShowToast("Not Yet Implemented".asText()), awaitItem())
        }
    }

    @Test
    fun `DismissDialogClick should emit DismissDialogClick`() = runTest {
        val initialState = DEFAULT_STATE.copy(
            dialogState = AttachmentsState.DialogState.Loading("Loading".asText()),
        )
        val viewModel = createViewModel(initialState)
        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            viewModel.trySendAction(AttachmentsAction.DismissDialogClick)
            assertEquals(initialState.copy(dialogState = null), awaitItem())
        }
    }

    @Test
    fun `ChooseFileClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AttachmentsAction.ChooseFileClick)
            assertEquals(AttachmentsEvent.ShowChooserSheet, awaitItem())
        }
    }

    @Test
    fun `ChooseFile should emit ShowToast`() = runTest {
        val fileData = mockk<IntentManager.FileData>()
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AttachmentsAction.FileChoose(fileData))
            assertEquals(AttachmentsEvent.ShowToast("Not Yet Implemented".asText()), awaitItem())
        }
    }

    @Test
    fun `DeleteClick with deleteCipherAttachment error should display error dialog`() = runTest {
        val cipherId = "mockId-1"
        val attachmentId = "mockId-1"
        val cipherView = createMockCipherView(number = 1)
        val initialState = DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT_WITH_ATTACHMENTS)
        coEvery {
            vaultRepository.deleteCipherAttachment(
                cipherId = cipherId,
                attachmentId = attachmentId,
                cipherView = cipherView,
            )
        } returns DeleteAttachmentResult.Error
        mutableVaultItemStateFlow.tryEmit(DataState.Loaded(cipherView))

        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            viewModel.actionChannel.trySend(AttachmentsAction.DeleteClick(attachmentId))
            assertEquals(
                initialState.copy(
                    dialogState = AttachmentsState.DialogState.Loading(
                        message = R.string.deleting.asText(),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                initialState.copy(
                    dialogState = AttachmentsState.DialogState.Error(
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.generic_error_message.asText(),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `DeleteClick with deleteCipherAttachment success should emit ShowToast`() = runTest {
        val cipherId = "mockId-1"
        val attachmentId = "mockId-1"
        val cipherView = createMockCipherView(number = 1)
        coEvery {
            vaultRepository.deleteCipherAttachment(
                cipherId = cipherId,
                attachmentId = attachmentId,
                cipherView = cipherView,
            )
        } returns DeleteAttachmentResult.Success
        mutableVaultItemStateFlow.tryEmit(DataState.Loaded(cipherView))

        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(AttachmentsAction.DeleteClick(cipherId))
            assertEquals(
                AttachmentsEvent.ShowToast(R.string.attachment_deleted.asText()),
                awaitItem(),
            )
        }
    }

    @Test
    fun `vaultItemStateFlow Error should update state to Error`() = runTest {
        mutableVaultItemStateFlow.tryEmit(value = DataState.Error(Throwable("Fail")))

        val viewModel = createViewModel()

        assertEquals(
            DEFAULT_STATE.copy(
                viewState = AttachmentsState.ViewState.Error(
                    message = R.string.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultItemStateFlow Loaded with data should update state to Content`() = runTest {
        val cipherView = createMockCipherView(number = 1)
        every { cipherView.toViewState() } returns DEFAULT_CONTENT_WITH_ATTACHMENTS
        mutableVaultItemStateFlow.tryEmit(DataState.Loaded(cipherView))

        val viewModel = createViewModel()

        assertEquals(
            DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT_WITH_ATTACHMENTS),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultItemStateFlow Loaded without data should update state to Content`() = runTest {
        mutableVaultItemStateFlow.tryEmit(DataState.Loaded(null))

        val viewModel = createViewModel()

        assertEquals(
            DEFAULT_STATE.copy(
                viewState = AttachmentsState.ViewState.Error(
                    message = R.string.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultItemStateFlow Loading should update state to Loading`() = runTest {
        mutableVaultItemStateFlow.tryEmit(value = DataState.Loading)

        val viewModel = createViewModel()

        assertEquals(
            DEFAULT_STATE.copy(viewState = AttachmentsState.ViewState.Loading),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultItemStateFlow NoNetwork should update state to Error`() = runTest {
        mutableVaultItemStateFlow.tryEmit(value = DataState.NoNetwork(null))

        val viewModel = createViewModel()

        assertEquals(
            DEFAULT_STATE.copy(
                viewState = AttachmentsState.ViewState.Error(
                    message = R.string.internet_connection_required_title
                        .asText()
                        .concat("\n".asText())
                        .concat(R.string.internet_connection_required_message.asText()),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultItemStateFlow Pending with data should update state to Content`() = runTest {
        val cipherView = createMockCipherView(number = 1)
        every { cipherView.toViewState() } returns DEFAULT_CONTENT_WITH_ATTACHMENTS
        mutableVaultItemStateFlow.tryEmit(DataState.Pending(cipherView))

        val viewModel = createViewModel()

        assertEquals(
            DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT_WITH_ATTACHMENTS),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultItemStateFlow Pending without data should update state to Content`() = runTest {
        mutableVaultItemStateFlow.tryEmit(DataState.Pending(null))

        val viewModel = createViewModel()

        assertEquals(
            DEFAULT_STATE.copy(
                viewState = AttachmentsState.ViewState.Error(
                    message = R.string.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    private fun createViewModel(
        initialState: AttachmentsState? = null,
    ): AttachmentsViewModel = AttachmentsViewModel(
        vaultRepo = vaultRepository,
        savedStateHandle = SavedStateHandle().apply {
            set("state", initialState)
            set("cipher_id", initialState?.cipherId ?: "mockId-1")
        },
    )
}

private val DEFAULT_STATE: AttachmentsState = AttachmentsState(
    cipherId = "mockId-1",
    viewState = AttachmentsState.ViewState.Loading,
    dialogState = null,
)

private val DEFAULT_CONTENT_WITH_ATTACHMENTS: AttachmentsState.ViewState.Content =
    AttachmentsState.ViewState.Content(
        originalCipher = createMockCipherView(number = 1),
        attachments = listOf(
            AttachmentsState.AttachmentItem(
                id = "mockId-1",
                title = "mockFileName-1",
                displaySize = "mockSizeName-1",
            ),
        ),
    )
