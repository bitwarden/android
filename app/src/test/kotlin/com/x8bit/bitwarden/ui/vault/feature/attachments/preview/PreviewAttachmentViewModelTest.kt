package com.x8bit.bitwarden.ui.vault.feature.attachments.preview

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.data.manager.file.FileManager
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DownloadAttachmentResult
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

@Suppress("LargeClass")
class PreviewAttachmentViewModelTest : BaseViewModelTest() {
    private val mutableVaultItemStateFlow =
        MutableStateFlow<DataState<CipherView?>>(DataState.Loading)
    private val vaultRepository: VaultRepository = mockk {
        every { getVaultItemStateFlow(any()) } returns mutableVaultItemStateFlow
    }
    private val fileManager: FileManager = mockk {
        coJustRun { delete(*anyVararg()) }
    }
    private val mockFile: File = mockk(relaxed = true)

    @BeforeEach
    fun setup() {
        mockkStatic(SavedStateHandle::toPreviewAttachmentArgs)
        coEvery {
            vaultRepository.downloadAttachment(cipherView = any(), attachmentId = any())
        } returns DownloadAttachmentResult.Success(mockFile)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(SavedStateHandle::toPreviewAttachmentArgs)
    }

    @Test
    fun `initial state should be Loading when file is previewable`() = runTest {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be Error when file is not previewable`() = runTest {
        val viewModel = createViewModel(initialState = NON_PREVIEWABLE_STATE)
        assertEquals(NON_PREVIEWABLE_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be loaded from savedStateHandle when state is set`() = runTest {
        // Use a non-previewable state to prevent the init block from overwriting the viewState
        val savedState = NON_PREVIEWABLE_STATE.copy(
            dialogState = PreviewAttachmentState.DialogState.Loading(
                message = "saved-dialog".asText(),
            ),
        )
        val viewModel = createViewModel(initialState = savedState)
        assertEquals(savedState, viewModel.stateFlow.value)
    }

    @Test
    fun `BackClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(PreviewAttachmentAction.BackClick)
            assertEquals(PreviewAttachmentEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `CloseClick should clear dialogState and emit NavigateBack`() = runTest {
        val initialState = DEFAULT_STATE.copy(
            dialogState = PreviewAttachmentState.DialogState.PreviewUnavailable,
        )
        val viewModel = createViewModel(initialState = initialState)
        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            viewModel.trySendAction(PreviewAttachmentAction.CloseClick)
            assertEquals(initialState.copy(dialogState = null), awaitItem())
        }
        // NavigateBack was buffered in the event channel by the CloseClick above
        viewModel.eventFlow.test {
            assertEquals(PreviewAttachmentEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `DismissDialog should clear dialogState`() = runTest {
        val initialState = DEFAULT_STATE.copy(
            dialogState = PreviewAttachmentState.DialogState.Loading(
                message = "Loading…".asText(),
            ),
        )
        val viewModel = createViewModel(initialState = initialState)
        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            viewModel.trySendAction(PreviewAttachmentAction.DismissDialog)
            assertEquals(initialState.copy(dialogState = null), awaitItem())
        }
    }

    @Test
    fun `BitmapRenderComplete with Content state should delete the content file`() = runTest {
        val contentFile = mockk<File>(relaxed = true)
        // Use isPreviewable = false to prevent the init block from
        // overwriting the Content viewState
        val initialState = DEFAULT_STATE.copy(
            isPreviewable = false,
            viewState = PreviewAttachmentState.ViewState.Content(file = contentFile),
        )
        val viewModel = createViewModel(initialState = initialState)
        viewModel.trySendAction(PreviewAttachmentAction.BitmapRenderComplete)
        coVerify(exactly = 1) { fileManager.delete(contentFile) }
    }

    @Test
    fun `BitmapRenderComplete with non-Content state should not delete any files`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(PreviewAttachmentAction.BitmapRenderComplete)
        coVerify(exactly = 0) { fileManager.delete(any()) }
    }

    @Test
    fun `BitmapRenderError should update viewState to Error`() = runTest {
        val contentFile = mockk<File>(relaxed = true)
        // Use isPreviewable = false to prevent the init block from
        // overwriting the Content viewState
        val initialState = DEFAULT_STATE.copy(
            isPreviewable = false,
            viewState = PreviewAttachmentState.ViewState.Content(file = contentFile),
        )
        val viewModel = createViewModel(initialState = initialState)
        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            viewModel.trySendAction(PreviewAttachmentAction.BitmapRenderError)
            assertEquals(
                initialState.copy(
                    viewState = PreviewAttachmentState.ViewState.Error(
                        message = BitwardenString.preview_unavailable_for_this_file.asText(),
                        illustrationRes = BitwardenDrawable.ill_file_error,
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `FileMissing with Content state should delete content file and reset to Loading`() =
        runTest {
            val contentFile = mockk<File>(relaxed = true)
            // Use isPreviewable = false to prevent the init block from
            // overwriting the Content viewState
            val initialState = DEFAULT_STATE.copy(
                isPreviewable = false,
                viewState = PreviewAttachmentState.ViewState.Content(file = contentFile),
            )
            val viewModel = createViewModel(initialState = initialState)
            viewModel.stateFlow.test {
                assertEquals(initialState, awaitItem())
                viewModel.trySendAction(PreviewAttachmentAction.FileMissing)
                assertEquals(
                    initialState.copy(
                        viewState = PreviewAttachmentState.ViewState.Loading(),
                    ),
                    awaitItem(),
                )
            }
            coVerify(exactly = 1) { fileManager.delete(contentFile) }
        }

    @Test
    fun `FileMissing with non-Content state should not emit a new state`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(PreviewAttachmentAction.FileMissing)
            expectNoEvents()
        }
    }

    @Test
    fun `NoAttachmentFileLocationReceive should show error dialog and delete temp file`() =
        runTest {
            val viewModel = createViewModel(tempFile = mockFile)
            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(PreviewAttachmentAction.NoAttachmentFileLocationReceive)
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = PreviewAttachmentState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.unable_to_save_attachment.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
            coVerify(exactly = 1) { fileManager.delete(mockFile) }
        }

    @Test
    fun `NoAttachmentFileLocationReceive without temp file should still show error dialog`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(PreviewAttachmentAction.NoAttachmentFileLocationReceive)
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = PreviewAttachmentState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.unable_to_save_attachment.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
            coVerify(exactly = 0) { fileManager.delete(any()) }
        }

    @Test
    fun `AttachmentFileLocationReceive with no temp file should do nothing`() = runTest {
        val uri = mockk<Uri>()
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(
                PreviewAttachmentAction.AttachmentFileLocationReceive(uri = uri),
            )
            expectNoEvents()
        }
    }

    @Test
    fun `AttachmentFileLocationReceive success should emit ShowSnackbar and delete temp file`() =
        runTest {
            val uri = mockk<Uri>()
            coEvery { fileManager.fileToUri(fileUri = uri, file = mockFile) } returns true
            val viewModel = createViewModel(tempFile = mockFile)
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    PreviewAttachmentAction.AttachmentFileLocationReceive(uri = uri),
                )
                assertEquals(
                    PreviewAttachmentEvent.ShowSnackbar(
                        message = BitwardenString.save_attachment_success.asText(),
                    ),
                    awaitItem(),
                )
            }
            coVerify(exactly = 1) { fileManager.delete(mockFile) }
        }

    @Test
    fun `AttachmentFileLocationReceive failure should show loading then error dialog`() = runTest {
        val uri = mockk<Uri>()
        coEvery { fileManager.fileToUri(fileUri = uri, file = mockFile) } returns false
        val viewModel = createViewModel(tempFile = mockFile)
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(
                PreviewAttachmentAction.AttachmentFileLocationReceive(uri = uri),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = PreviewAttachmentState.DialogState.Loading(
                        message = BitwardenString.saving.asText(),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = PreviewAttachmentState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.unable_to_save_attachment.asText(),
                    ),
                ),
                awaitItem(),
            )
        }
        coVerify(exactly = 1) { fileManager.delete(mockFile) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `DownloadClick should show loading dialog and emit NavigateToSelectAttachmentSaveLocation`() =
        runTest {
            val cipherView = createMockCipherView(number = 1)
            mutableVaultItemStateFlow.value = DataState.Loaded(cipherView)
            coEvery {
                vaultRepository.downloadAttachment(
                    cipherView = cipherView,
                    attachmentId = DEFAULT_ATTACHMENT_ID,
                )
            } returns DownloadAttachmentResult.Success(mockFile)
            val viewModel = createViewModel(initialState = NON_PREVIEWABLE_STATE)
            viewModel.eventFlow.test {
                viewModel.trySendAction(PreviewAttachmentAction.DownloadClick)
                assertEquals(
                    PreviewAttachmentEvent.NavigateToSelectAttachmentSaveLocation(
                        fileName = NON_PREVIEWABLE_STATE.fileName,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `DownloadClick should show loading dialog while downloading`() = runTest {
        val cipherView = createMockCipherView(number = 1)
        mutableVaultItemStateFlow.value = DataState.Loaded(cipherView)
        coEvery {
            vaultRepository.downloadAttachment(
                cipherView = cipherView,
                attachmentId = DEFAULT_ATTACHMENT_ID,
            )
        } returns DownloadAttachmentResult.Success(mockFile)
        val viewModel = createViewModel(initialState = NON_PREVIEWABLE_STATE)
        viewModel.stateFlow.test {
            assertEquals(NON_PREVIEWABLE_STATE, awaitItem())
            viewModel.trySendAction(PreviewAttachmentAction.DownloadClick)
            assertEquals(
                NON_PREVIEWABLE_STATE.copy(
                    dialogState = PreviewAttachmentState.DialogState.Loading(
                        message = BitwardenString.downloading.asText(),
                    ),
                ),
                awaitItem(),
            )
            // Dialog is cleared after successful download
            assertEquals(NON_PREVIEWABLE_STATE, awaitItem())
        }
    }

    @Test
    fun `DownloadClick should show loading then error dialog on failure`() = runTest {
        val cipherView = createMockCipherView(number = 1)
        val error = Exception("Download failed")
        mutableVaultItemStateFlow.value = DataState.Loaded(cipherView)
        coEvery {
            vaultRepository.downloadAttachment(
                cipherView = cipherView,
                attachmentId = DEFAULT_ATTACHMENT_ID,
            )
        } returns DownloadAttachmentResult.Failure(error = error)
        val viewModel = createViewModel(initialState = NON_PREVIEWABLE_STATE)
        viewModel.stateFlow.test {
            assertEquals(NON_PREVIEWABLE_STATE, awaitItem())
            viewModel.trySendAction(PreviewAttachmentAction.DownloadClick)
            assertEquals(
                NON_PREVIEWABLE_STATE.copy(
                    dialogState = PreviewAttachmentState.DialogState.Loading(
                        message = BitwardenString.downloading.asText(),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                NON_PREVIEWABLE_STATE.copy(
                    dialogState = PreviewAttachmentState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.unable_to_download_file.asText(),
                        throwable = error,
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `vaultItemStateFlow Loading should update viewState to Loading`() = runTest {
        mutableVaultItemStateFlow.value = DataState.Loading
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultItemStateFlow Error with null data should update viewState to Error and show PreviewUnavailable dialog`() =
        runTest {
            mutableVaultItemStateFlow.value =
                DataState.Error(data = null, error = Throwable("Fail"))
            val viewModel = createViewModel()
            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = PreviewAttachmentState.ViewState.Error(
                        message = BitwardenString.generic_error_message.asText(),
                    ),
                    dialogState = PreviewAttachmentState.DialogState.PreviewUnavailable,
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `vaultItemStateFlow Loaded with null data should update viewState to Error`() = runTest {
        mutableVaultItemStateFlow.value = DataState.Loaded(null)
        val viewModel = createViewModel()
        assertEquals(
            DEFAULT_STATE.copy(
                viewState = PreviewAttachmentState.ViewState.Error(
                    message = BitwardenString.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultItemStateFlow Loaded with data should update viewState to Content`() = runTest {
        val cipherView = createMockCipherView(number = 1)
        mutableVaultItemStateFlow.value = DataState.Loaded(cipherView)
        val viewModel = createViewModel()
        assertEquals(
            DEFAULT_STATE.copy(
                viewState = PreviewAttachmentState.ViewState.Content(file = mockFile),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultItemStateFlow NoNetwork with null data should update viewState to Error`() = runTest {
        mutableVaultItemStateFlow.value = DataState.NoNetwork(null)
        val viewModel = createViewModel()
        assertEquals(
            DEFAULT_STATE.copy(
                viewState = PreviewAttachmentState.ViewState.Error(
                    message = BitwardenString.internet_connection_required_title
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
    fun `vaultItemStateFlow Pending with null data should update viewState to Error`() = runTest {
        mutableVaultItemStateFlow.value = DataState.Pending(null)
        val viewModel = createViewModel()
        assertEquals(
            DEFAULT_STATE.copy(
                viewState = PreviewAttachmentState.ViewState.Error(
                    message = BitwardenString.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vault item flow should not be observed when file is not previewable`() = runTest {
        val viewModel = createViewModel(initialState = NON_PREVIEWABLE_STATE)
        mutableVaultItemStateFlow.value = DataState.Error(data = null, error = Throwable())
        assertEquals(NON_PREVIEWABLE_STATE, viewModel.stateFlow.value)
        verify(exactly = 0) { vaultRepository.getVaultItemStateFlow(any()) }
    }

    @Test
    fun `CipherReceive Loading should not change viewState`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(
                PreviewAttachmentAction.Internal.CipherReceive(DataState.Loading),
            )
            expectNoEvents()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `CipherReceive Error with null data should show PreviewUnavailable dialog and Error viewState`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(
                    PreviewAttachmentAction.Internal.CipherReceive(
                        DataState.Error(data = null, error = Throwable("Fail")),
                    ),
                )
                // Both state updates (dialogState and viewState) are conflated into one emission
                assertEquals(
                    DEFAULT_STATE.copy(
                        viewState = PreviewAttachmentState.ViewState.Error(
                            message = BitwardenString.generic_error_message.asText(),
                        ),
                        dialogState = PreviewAttachmentState.DialogState.PreviewUnavailable,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `CipherReceive Loaded with null data should update viewState to Error`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(
                PreviewAttachmentAction.Internal.CipherReceive(DataState.Loaded(null)),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = PreviewAttachmentState.ViewState.Error(
                        message = BitwardenString.generic_error_message.asText(),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `CipherReceive Loaded with data should download and update viewState to Content`() =
        runTest {
            val cipherView = createMockCipherView(number = 1)
            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(
                    PreviewAttachmentAction.Internal.CipherReceive(DataState.Loaded(cipherView)),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        viewState = PreviewAttachmentState.ViewState.Content(file = mockFile),
                    ),
                    awaitItem(),
                )
            }
            coVerify(exactly = 1) {
                vaultRepository.downloadAttachment(
                    cipherView = cipherView,
                    attachmentId = DEFAULT_ATTACHMENT_ID,
                )
            }
        }

    @Test
    fun `CipherReceive Error with non-null data should download and update viewState to Content`() =
        runTest {
            val cipherView = createMockCipherView(number = 1)
            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(
                    PreviewAttachmentAction.Internal.CipherReceive(
                        DataState.Error(data = cipherView, error = Throwable("Fail")),
                    ),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        viewState = PreviewAttachmentState.ViewState.Content(file = mockFile),
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `CipherReceive NoNetwork with non-null data should download and update viewState to Content`() =
        runTest {
            val cipherView = createMockCipherView(number = 1)
            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(
                    PreviewAttachmentAction.Internal.CipherReceive(
                        DataState.NoNetwork(cipherView),
                    ),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        viewState = PreviewAttachmentState.ViewState.Content(file = mockFile),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `CipherReceive NoNetwork with null data should update viewState to Error`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(
                PreviewAttachmentAction.Internal.CipherReceive(DataState.NoNetwork(null)),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = PreviewAttachmentState.ViewState.Error(
                        message = BitwardenString.internet_connection_required_title
                            .asText()
                            .concat(
                                " ".asText(),
                                BitwardenString.internet_connection_required_message.asText(),
                            ),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `CipherReceive Pending with null data should update viewState to Error`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(
                PreviewAttachmentAction.Internal.CipherReceive(DataState.Pending(null)),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = PreviewAttachmentState.ViewState.Error(
                        message = BitwardenString.generic_error_message.asText(),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `CipherReceive Pending with data should download and update viewState to Content`() =
        runTest {
            val cipherView = createMockCipherView(number = 1)
            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(
                    PreviewAttachmentAction.Internal.CipherReceive(DataState.Pending(cipherView)),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        viewState = PreviewAttachmentState.ViewState.Content(file = mockFile),
                    ),
                    awaitItem(),
                )
            }
            coVerify(exactly = 1) {
                vaultRepository.downloadAttachment(
                    cipherView = cipherView,
                    attachmentId = DEFAULT_ATTACHMENT_ID,
                )
            }
        }

    @Test
    fun `AttachmentFileReceive Success should update viewState to Content`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(
                PreviewAttachmentAction.Internal.AttachmentFileReceive(
                    DownloadAttachmentResult.Success(mockFile),
                ),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = PreviewAttachmentState.ViewState.Content(file = mockFile),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `AttachmentFileReceive Failure should update viewState to Error`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(
                PreviewAttachmentAction.Internal.AttachmentFileReceive(
                    DownloadAttachmentResult.Failure(error = Throwable("Fail")),
                ),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = PreviewAttachmentState.ViewState.Error(
                        message = BitwardenString.generic_error_message.asText(),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `DownloadAttachmentReceive Success should clear dialog and emit NavigateToSelectAttachmentSaveLocation`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    PreviewAttachmentAction.Internal.DownloadAttachmentReceive(
                        DownloadAttachmentResult.Success(mockFile),
                    ),
                )
                assertEquals(
                    PreviewAttachmentEvent.NavigateToSelectAttachmentSaveLocation(
                        fileName = DEFAULT_FILE_NAME,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `DownloadAttachmentReceive Success should clear loading dialog`() = runTest {
        val initialState = DEFAULT_STATE.copy(
            dialogState = PreviewAttachmentState.DialogState.Loading(
                message = BitwardenString.downloading.asText(),
            ),
        )
        val viewModel = createViewModel(initialState = initialState)
        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            viewModel.trySendAction(
                PreviewAttachmentAction.Internal.DownloadAttachmentReceive(
                    DownloadAttachmentResult.Success(mockFile),
                ),
            )
            assertEquals(initialState.copy(dialogState = null), awaitItem())
        }
    }

    @Test
    fun `DownloadAttachmentReceive Failure should show error dialog`() = runTest {
        val error = Throwable("Download failed")
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(
                PreviewAttachmentAction.Internal.DownloadAttachmentReceive(
                    DownloadAttachmentResult.Failure(error = error),
                ),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = PreviewAttachmentState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.unable_to_download_file.asText(),
                        throwable = error,
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `AttachmentFinishedSavingToDisk success should clear dialog and emit ShowSnackbar`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    PreviewAttachmentAction.Internal.AttachmentFinishedSavingToDisk(
                        isSaved = true,
                        file = mockFile,
                    ),
                )
                assertEquals(
                    PreviewAttachmentEvent.ShowSnackbar(
                        message = BitwardenString.save_attachment_success.asText(),
                    ),
                    awaitItem(),
                )
            }
            coVerify(exactly = 1) { fileManager.delete(mockFile) }
        }

    @Test
    fun `AttachmentFinishedSavingToDisk success should clear loading dialog`() = runTest {
        val initialState = DEFAULT_STATE.copy(
            dialogState = PreviewAttachmentState.DialogState.Loading(
                message = BitwardenString.saving.asText(),
            ),
        )
        val viewModel = createViewModel(initialState = initialState)
        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            viewModel.trySendAction(
                PreviewAttachmentAction.Internal.AttachmentFinishedSavingToDisk(
                    isSaved = true,
                    file = mockFile,
                ),
            )
            assertEquals(initialState.copy(dialogState = null), awaitItem())
        }
    }

    @Test
    fun `AttachmentFinishedSavingToDisk failure should show error dialog`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(
                PreviewAttachmentAction.Internal.AttachmentFinishedSavingToDisk(
                    isSaved = false,
                    file = mockFile,
                ),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = PreviewAttachmentState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.unable_to_save_attachment.asText(),
                    ),
                ),
                awaitItem(),
            )
        }
        coVerify(exactly = 1) { fileManager.delete(mockFile) }
    }

    private fun createViewModel(
        initialState: PreviewAttachmentState? = null,
        tempFile: File? = null,
    ): PreviewAttachmentViewModel = PreviewAttachmentViewModel(
        fileManager = fileManager,
        vaultRepository = vaultRepository,
        savedStateHandle = SavedStateHandle().apply {
            set("state", initialState)
            set("tempAttachmentFile", tempFile)
            every { toPreviewAttachmentArgs() } returns PreviewAttachmentArgs(
                cipherId = initialState?.cipherId ?: DEFAULT_CIPHER_ID,
                attachmentId = initialState?.attachmentId ?: DEFAULT_ATTACHMENT_ID,
                fileName = initialState?.fileName ?: DEFAULT_FILE_NAME,
            )
        },
    )
}

private const val DEFAULT_CIPHER_ID = "mockCipherId"
private const val DEFAULT_ATTACHMENT_ID = "mockAttachmentId"
private const val DEFAULT_FILE_NAME = "test.png"

private val DEFAULT_STATE = PreviewAttachmentState(
    cipherId = DEFAULT_CIPHER_ID,
    attachmentId = DEFAULT_ATTACHMENT_ID,
    fileName = DEFAULT_FILE_NAME,
    isPreviewable = true,
    viewState = PreviewAttachmentState.ViewState.Loading(),
    dialogState = null,
)

private val NON_PREVIEWABLE_STATE = PreviewAttachmentState(
    cipherId = DEFAULT_CIPHER_ID,
    attachmentId = DEFAULT_ATTACHMENT_ID,
    fileName = "test.pdf",
    isPreviewable = false,
    viewState = PreviewAttachmentState.ViewState.Error(
        message = BitwardenString.preview_not_available_for_files.asText("PDF"),
    ),
    dialogState = null,
)
