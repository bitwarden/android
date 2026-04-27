package com.x8bit.bitwarden.ui.vault.feature.attachments

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.model.FileData
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.CreateAttachmentResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteAttachmentResult
import com.x8bit.bitwarden.ui.vault.feature.attachments.util.toViewState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Suppress("LargeClass")
class AttachmentsViewModelTest : BaseViewModelTest() {
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val authRepository: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
    }
    private val environmentRepository: EnvironmentRepository = mockk {
        every { environment } returns Environment.Us
    }
    private val mutableVaultItemStateFlow =
        MutableStateFlow<DataState<CipherView?>>(DataState.Loading)
    private val vaultRepository: VaultRepository = mockk {
        every { getVaultItemStateFlow(any()) } returns mutableVaultItemStateFlow
    }
    private val mutableAttachmentUpdatesFlow = MutableStateFlow(true)
    private val featureFlagManager: FeatureFlagManager = mockk {
        every {
            getFeatureFlag(FlagKey.AttachmentUpdates)
        } answers { mutableAttachmentUpdatesFlow.value }
        every { getFeatureFlagFlow(FlagKey.AttachmentUpdates) } returns mutableAttachmentUpdatesFlow
    }

    @BeforeEach
    fun setup() {
        mockkStatic(
            SavedStateHandle::toAttachmentsArgs,
            CipherView::toViewState,
        )
        mockkStatic(Uri::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            SavedStateHandle::toAttachmentsArgs,
            CipherView::toViewState,
        )
        unmockkStatic(Uri::class)
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
    fun `ItemClick should display RequiresPremium dialog when user is not Premium`() = runTest {
        mutableUserStateFlow.update {
            DEFAULT_USER_STATE.copy(
                accounts = listOf(DEFAULT_ACCOUNT.copy(isPremium = false)),
            )
        }
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                AttachmentsAction.ItemClick(attachment = DEFAULT_ATTACHMENT_ITEM),
            )
            expectNoEvents()
        }
        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = AttachmentsState.DialogState.RequiresPremium,
                isPremiumUser = false,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `ItemClick should emit NavigateToPreview when user is Premium`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                AttachmentsAction.ItemClick(attachment = DEFAULT_ATTACHMENT_ITEM),
            )
            assertEquals(
                AttachmentsEvent.NavigateToPreview(
                    cipherId = DEFAULT_STATE.cipherId,
                    attachmentId = DEFAULT_ATTACHMENT_ITEM.id,
                    fileName = DEFAULT_ATTACHMENT_ITEM.title,
                    displaySize = DEFAULT_ATTACHMENT_ITEM.displaySize,
                    isLargeFile = false,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `UpgradeToPremiumClick should emit NavigateToUri`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AttachmentsAction.UpgradeToPremiumClick)
            assertEquals(
                AttachmentsEvent.NavigateToUri(
                    uri = "https://vault.bitwarden.com/#/settings/subscription" +
                        "/premium?callToAction=upgradeToPremium",
                ),
                awaitItem(),
            )
        }
        verify(exactly = 1) {
            environmentRepository.environment
        }
    }

    @Test
    fun `FileNameChange should update the newAttachment state`() = runTest {
        val cipherView = createMockCipherView(number = 1)
        mutableVaultItemStateFlow.value = DataState.Loaded(cipherView)
        mutableUserStateFlow.value = DEFAULT_USER_STATE
        val uri = mockk<Uri>()
        val newAttachment = AttachmentsState.NewAttachment(
            uri = uri,
            extension = "png",
            displayName = "cool_file",
            sizeBytes = 100L,
        )
        val initialState = DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT_WITH_ATTACHMENTS)

        val fileData = FileData(
            fileName = "cool_file.png",
            uri = uri,
            sizeBytes = 100L,
        )
        mutableVaultItemStateFlow.value = DataState.Loaded(cipherView)
        mutableUserStateFlow.value = DEFAULT_USER_STATE

        val viewModel = createViewModel()
        // Need to populate the VM with a file
        viewModel.trySendAction(AttachmentsAction.FileChoose(fileData))

        // Then alter the name
        viewModel.trySendAction(AttachmentsAction.FileNameChange(fileName = "cool_file5"))

        assertEquals(
            initialState.copy(
                viewState = DEFAULT_CONTENT_WITH_ATTACHMENTS.copy(
                    newAttachment = newAttachment.copy(displayName = "cool_file5"),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `SaveClick should display error dialog when user is not Premium`() = runTest {
        val cipherView = createMockCipherView(number = 1)
        mutableVaultItemStateFlow.value = DataState.Loaded(cipherView)
        mutableUserStateFlow.value = null
        val viewModel = createViewModel()

        viewModel.trySendAction(AttachmentsAction.SaveClick)
        assertEquals(
            DEFAULT_STATE.copy(
                viewState = DEFAULT_CONTENT_WITH_ATTACHMENTS,
                dialogState = AttachmentsState.DialogState.RequiresPremium,
                isPremiumUser = false,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `SaveClick should display error dialog when no file is selected`() = runTest {
        val cipherView = createMockCipherView(number = 1)
        val state = DEFAULT_STATE.copy(
            viewState = DEFAULT_CONTENT_WITH_ATTACHMENTS,
            isPremiumUser = true,
        )
        mutableVaultItemStateFlow.value = DataState.Loaded(cipherView)
        mutableUserStateFlow.value = DEFAULT_USER_STATE
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(state, awaitItem())
            viewModel.trySendAction(AttachmentsAction.SaveClick)
            assertEquals(
                state.copy(
                    dialogState = AttachmentsState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.validation_field_required
                            .asText(BitwardenString.file.asText()),
                        throwable = null,
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `SaveClick should display error dialog when file is too large`() = runTest {
        val cipherView = createMockCipherView(number = 1)
        val fileName = "test.png"
        val uri = mockk<Uri>()
        val sizeToBig = 104_857_601L
        val state = DEFAULT_STATE.copy(
            viewState = DEFAULT_CONTENT_WITH_ATTACHMENTS.copy(
                newAttachment = AttachmentsState.NewAttachment(
                    extension = "png",
                    displayName = "test",
                    uri = uri,
                    sizeBytes = sizeToBig,
                ),
            ),
            isPremiumUser = true,
        )
        val fileData = FileData(
            fileName = fileName,
            uri = uri,
            sizeBytes = sizeToBig,
        )
        mutableVaultItemStateFlow.value = DataState.Loaded(cipherView)
        mutableUserStateFlow.value = DEFAULT_USER_STATE

        val viewModel = createViewModel()
        // Need to populate the VM with a file
        viewModel.trySendAction(AttachmentsAction.FileChoose(fileData))

        viewModel.stateFlow.test {
            assertEquals(state, awaitItem())
            viewModel.trySendAction(AttachmentsAction.SaveClick)
            assertEquals(
                state.copy(
                    dialogState = AttachmentsState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.max_file_size.asText(),
                        throwable = null,
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `SaveClick should display loading dialog and error dialog when createAttachment fails`() =
        runTest {
            val cipherView = createMockCipherView(number = 1)
            val fileName = "test.png"
            val uri = mockk<Uri>()
            val sizeJustRight = 104_857_600L
            val state = DEFAULT_STATE.copy(
                viewState = DEFAULT_CONTENT_WITH_ATTACHMENTS.copy(
                    newAttachment = AttachmentsState.NewAttachment(
                        extension = "png",
                        displayName = "test",
                        uri = uri,
                        sizeBytes = sizeJustRight,
                    ),
                ),
                isPremiumUser = true,
            )
            val fileData = FileData(
                fileName = fileName,
                uri = uri,
                sizeBytes = sizeJustRight,
            )
            mutableVaultItemStateFlow.value = DataState.Loaded(cipherView)
            mutableUserStateFlow.value = DEFAULT_USER_STATE
            val error = IllegalStateException("No permissions.")
            coEvery {
                vaultRepository.createAttachment(
                    cipherId = state.cipherId,
                    cipherView = cipherView,
                    fileSizeBytes = sizeJustRight.toString(),
                    fileName = fileName,
                    fileUri = uri,
                )
            } returns CreateAttachmentResult.Error(
                error = error,
                message = "No permissions.",
            )

            val viewModel = createViewModel()
            // Need to populate the VM with a file
            viewModel.trySendAction(AttachmentsAction.FileChoose(fileData))

            viewModel.stateFlow.test {
                assertEquals(state, awaitItem())
                viewModel.trySendAction(AttachmentsAction.SaveClick)
                assertEquals(
                    state.copy(
                        dialogState = AttachmentsState.DialogState.Loading(
                            message = BitwardenString.saving.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    state.copy(
                        dialogState = AttachmentsState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = error.message!!.asText(),
                            throwable = error,
                        ),
                    ),
                    awaitItem(),
                )
            }
            coVerify(exactly = 1) {
                vaultRepository.createAttachment(
                    cipherId = state.cipherId,
                    cipherView = cipherView,
                    fileSizeBytes = sizeJustRight.toString(),
                    fileName = fileName,
                    fileUri = uri,
                )
            }
        }

    @Test
    fun `SaveClick should display generic error message dialog when createAttachment fails`() =
        runTest {
            val cipherView = createMockCipherView(number = 1)
            val fileName = "test.png"
            val uri = mockk<Uri>()
            val sizeJustRight = 104_857_600L
            val state = DEFAULT_STATE.copy(
                viewState = DEFAULT_CONTENT_WITH_ATTACHMENTS.copy(
                    newAttachment = AttachmentsState.NewAttachment(
                        extension = "png",
                        displayName = "test",
                        uri = uri,
                        sizeBytes = sizeJustRight,
                    ),
                ),
                isPremiumUser = true,
            )
            val fileData = FileData(
                fileName = fileName,
                uri = uri,
                sizeBytes = sizeJustRight,
            )
            mutableVaultItemStateFlow.value = DataState.Loaded(cipherView)
            mutableUserStateFlow.value = DEFAULT_USER_STATE
            val error = Exception()
            coEvery {
                vaultRepository.createAttachment(
                    cipherId = state.cipherId,
                    cipherView = cipherView,
                    fileSizeBytes = sizeJustRight.toString(),
                    fileName = fileName,
                    fileUri = uri,
                )
            } returns CreateAttachmentResult.Error(error = error)

            val viewModel = createViewModel()
            // Need to populate the VM with a file
            viewModel.trySendAction(AttachmentsAction.FileChoose(fileData))

            viewModel.stateFlow.test {
                assertEquals(state, awaitItem())
                viewModel.trySendAction(AttachmentsAction.SaveClick)
                assertEquals(
                    state.copy(
                        dialogState = AttachmentsState.DialogState.Loading(
                            message = BitwardenString.saving.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    state.copy(
                        dialogState = AttachmentsState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                            throwable = error,
                        ),
                    ),
                    awaitItem(),
                )
            }
            coVerify(exactly = 1) {
                vaultRepository.createAttachment(
                    cipherId = state.cipherId,
                    cipherView = cipherView,
                    fileSizeBytes = sizeJustRight.toString(),
                    fileName = fileName,
                    fileUri = uri,
                )
            }
        }

    @Test
    fun `SaveClick should send ShowSnackbar when createAttachment succeeds`() = runTest {
        val cipherView = createMockCipherView(number = 1)
        val fileName = "test.png"
        val uri = mockk<Uri>()
        val sizeJustRight = 104_857_600L
        val state = DEFAULT_STATE.copy(
            viewState = DEFAULT_CONTENT_WITH_ATTACHMENTS.copy(
                newAttachment = AttachmentsState.NewAttachment(
                    extension = "png",
                    displayName = "test",
                    uri = uri,
                    sizeBytes = sizeJustRight,
                ),
            ),
            isPremiumUser = true,
        )
        val fileData = FileData(
            fileName = fileName,
            uri = uri,
            sizeBytes = sizeJustRight,
        )
        mutableVaultItemStateFlow.value = DataState.Loaded(cipherView)
        mutableUserStateFlow.value = DEFAULT_USER_STATE
        coEvery {
            vaultRepository.createAttachment(
                cipherId = state.cipherId,
                cipherView = cipherView,
                fileSizeBytes = sizeJustRight.toString(),
                fileName = fileName,
                fileUri = uri,
            )
        } returns CreateAttachmentResult.Success(cipherView)

        val viewModel = createViewModel()
        // Need to populate the VM with a file
        viewModel.trySendAction(AttachmentsAction.FileChoose(fileData))

        viewModel.eventFlow.test {
            viewModel.trySendAction(AttachmentsAction.SaveClick)
            assertEquals(
                AttachmentsEvent.ShowSnackbar(BitwardenString.save_attachment_success.asText()),
                awaitItem(),
            )
        }
        coVerify(exactly = 1) {
            vaultRepository.createAttachment(
                cipherId = state.cipherId,
                cipherView = cipherView,
                fileSizeBytes = sizeJustRight.toString(),
                fileName = fileName,
                fileUri = uri,
            )
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
    fun `ChooseFileClick should emit ShowChooserSheet`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AttachmentsAction.ChooseFileClick)
            assertEquals(AttachmentsEvent.ShowChooserSheet, awaitItem())
        }
    }

    @Test
    fun `ChooseFile should update state with new file data`() = runTest {
        val uri = createMockUri()
        val fileData = FileData(
            fileName = "filename-1.png",
            uri = uri,
            sizeBytes = 100L,
        )
        val cipherView = createMockCipherView(number = 1)
        val initialState = DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT_WITH_ATTACHMENTS)
        mutableVaultItemStateFlow.value = DataState.Loaded(cipherView)
        val viewModel = createViewModel()
        viewModel.trySendAction(AttachmentsAction.FileChoose(fileData))

        assertEquals(
            initialState.copy(
                viewState = DEFAULT_CONTENT_WITH_ATTACHMENTS.copy(
                    newAttachment = AttachmentsState.NewAttachment(
                        extension = "png",
                        displayName = "filename-1",
                        uri = uri,
                        sizeBytes = 100L,
                    ),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `DeleteClick with deleteCipherAttachment error should display error dialog`() = runTest {
        val cipherId = "mockId-1"
        val attachmentId = "mockId-1"
        val cipherView = createMockCipherView(number = 1)
        val initialState = DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT_WITH_ATTACHMENTS)
        val error = NoActiveUserException()
        coEvery {
            vaultRepository.deleteCipherAttachment(
                cipherId = cipherId,
                attachmentId = attachmentId,
                cipherView = cipherView,
            )
        } returns DeleteAttachmentResult.Error(error = error)
        mutableVaultItemStateFlow.value = DataState.Loaded(cipherView)

        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            viewModel.trySendAction(AttachmentsAction.DeleteClick(attachmentId))
            assertEquals(
                initialState.copy(
                    dialogState = AttachmentsState.DialogState.Loading(
                        message = BitwardenString.deleting.asText(),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                initialState.copy(
                    dialogState = AttachmentsState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.generic_error_message.asText(),
                        throwable = error,
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `DeleteClick with deleteCipherAttachment error with errorMessage should display that message`() =
        runTest {
            val cipherId = "mockId-1"
            val attachmentId = "mockId-1"
            val cipherView = createMockCipherView(number = 1)
            val initialState = DEFAULT_STATE.copy(
                viewState = DEFAULT_CONTENT_WITH_ATTACHMENTS,
            )
            val errorMessage = "You do not have permission to edit this."
            val error = Throwable("Fail")
            coEvery {
                vaultRepository.deleteCipherAttachment(
                    cipherId = cipherId,
                    attachmentId = attachmentId,
                    cipherView = cipherView,
                )
            } returns DeleteAttachmentResult.Error(
                errorMessage = errorMessage,
                error = error,
            )
            mutableVaultItemStateFlow.value = DataState.Loaded(cipherView)

            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(initialState, awaitItem())
                viewModel.trySendAction(AttachmentsAction.DeleteClick(attachmentId))
                assertEquals(
                    initialState.copy(
                        dialogState = AttachmentsState.DialogState.Loading(
                            message = BitwardenString.deleting.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    initialState.copy(
                        dialogState = AttachmentsState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = errorMessage.asText(),
                            throwable = error,
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `DeleteClick with deleteCipherAttachment success should emit ShowSnackbar`() = runTest {
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
        mutableVaultItemStateFlow.value = DataState.Loaded(cipherView)

        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AttachmentsAction.DeleteClick(cipherId))
            assertEquals(
                AttachmentsEvent.ShowSnackbar(BitwardenString.attachment_deleted.asText()),
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
                    message = BitwardenString.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `vaultItemStateFlow Loaded with data should update state to Content`() = runTest {
        val cipherView = createMockCipherView(number = 1)
        every { cipherView.toViewState() } returns DEFAULT_CONTENT_WITH_ATTACHMENTS
        mutableVaultItemStateFlow.value = DataState.Loaded(cipherView)

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
                    message = BitwardenString.generic_error_message.asText(),
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
                    message = BitwardenString.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `userStateFlow should update isPremiumUser state`() = runTest {
        val viewModel = createViewModel()

        mutableUserStateFlow.value = DEFAULT_USER_STATE
        assertEquals(
            DEFAULT_STATE.copy(isPremiumUser = true),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `AttachmentUpdatesFlow should update isAttachmentUpdatesEnabled state`() = runTest {
        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())

            mutableAttachmentUpdatesFlow.update { false }
            assertEquals(DEFAULT_STATE.copy(isAttachmentUpdatesEnabled = false), awaitItem())

            mutableAttachmentUpdatesFlow.update { true }
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    private fun createViewModel(
        initialState: AttachmentsState? = null,
    ): AttachmentsViewModel = AttachmentsViewModel(
        authRepo = authRepository,
        environmentRepo = environmentRepository,
        vaultRepo = vaultRepository,
        featureFlagManager = featureFlagManager,
        savedStateHandle = SavedStateHandle().apply {
            set("state", initialState)
            every {
                toAttachmentsArgs()
            } returns AttachmentsArgs(cipherId = initialState?.cipherId ?: "mockId-1")
        },
    )
}

private val DEFAULT_ACCOUNT = UserState.Account(
    userId = "mockUserId-1",
    name = "Active User",
    email = "active@bitwarden.com",
    environment = Environment.Us,
    avatarColorHex = "#aa00aa",
    isPremium = true,
    isLoggedIn = true,
    isVaultUnlocked = true,
    needsPasswordReset = false,
    isBiometricsEnabled = false,
    organizations = emptyList(),
    needsMasterPassword = false,
    trustedDevice = null,
    hasMasterPassword = true,
    isUsingKeyConnector = false,
    onboardingStatus = OnboardingStatus.COMPLETE,
    firstTimeState = FirstTimeState(showImportLoginsCard = true),
    isExportable = true,
    creationDate = null,
)
private val DEFAULT_USER_STATE = UserState(
    activeUserId = "mockUserId-1",
    accounts = listOf(DEFAULT_ACCOUNT),
)

private val DEFAULT_STATE: AttachmentsState = AttachmentsState(
    cipherId = "mockId-1",
    viewState = AttachmentsState.ViewState.Loading,
    dialogState = null,
    isPremiumUser = true,
    isAttachmentUpdatesEnabled = true,
)

private val DEFAULT_ATTACHMENT_ITEM: AttachmentsState.AttachmentItem =
    AttachmentsState.AttachmentItem(
        id = "mockId-1",
        title = "mockFileName-1",
        displaySize = "mockSizeName-1",
        isLargeFile = false,
    )

private val DEFAULT_CONTENT_WITH_ATTACHMENTS: AttachmentsState.ViewState.Content =
    AttachmentsState.ViewState.Content(
        originalCipher = createMockCipherView(number = 1),
        attachments = persistentListOf(DEFAULT_ATTACHMENT_ITEM),
        newAttachment = null,
    )

private fun createMockUri(): Uri {
    val uriMock = mockk<Uri>()
    every { Uri.parse(any()) } returns uriMock
    return uriMock
}
