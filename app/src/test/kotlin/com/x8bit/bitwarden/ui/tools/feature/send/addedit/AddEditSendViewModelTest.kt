package com.x8bit.bitwarden.ui.tools.feature.send.addedit

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.createMockPolicy
import com.bitwarden.send.SendView
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.model.FileData
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.manager.network.NetworkConnectionManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.CreateSendResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteSendResult
import com.x8bit.bitwarden.data.vault.repository.model.RemovePasswordSendResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateSendResult
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.model.AddEditSendType
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.util.toSendView
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.util.toViewState
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType
import com.x8bit.bitwarden.ui.tools.feature.send.util.toSendUrl
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Suppress("LargeClass")
class AddEditSendViewModelTest : BaseViewModelTest() {

    private val clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )
    private val clipboardManager: BitwardenClipboardManager = mockk {
        every { setText(any<String>(), toastDescriptorOverride = any<Text>()) } just runs
    }
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val authRepository = mockk<AuthRepository> {
        every { userStateFlow } returns mutableUserStateFlow
    }
    private val environmentRepository: EnvironmentRepository = mockk {
        every { environment } returns Environment.Us
    }
    private val specialCircumstanceManager: SpecialCircumstanceManager = mockk {
        every { specialCircumstance } returns null
        every { specialCircumstance = any() } just runs
    }
    private val mutableSendDataStateFlow = MutableStateFlow<DataState<SendView>>(DataState.Loading)
    private val vaultRepository: VaultRepository = mockk {
        every { getSendStateFlow(any()) } returns mutableSendDataStateFlow
    }
    private val policyManager: PolicyManager = mockk {
        every { getActivePolicies(type = PolicyTypeJson.DISABLE_SEND) } returns emptyList()
        every { getActivePolicies(type = PolicyTypeJson.SEND_OPTIONS) } returns emptyList()
    }
    private val networkConnectionManager = mockk<NetworkConnectionManager> {
        every { isNetworkConnected } returns true
    }
    private val snackbarRelayManager: SnackbarRelayManager<SnackbarRelay> = mockk {
        every { sendSnackbarData(data = any(), relay = any()) } just runs
    }

    @BeforeEach
    fun setup() {
        mockkStatic(
            SavedStateHandle::toAddEditSendArgs,
            AddEditSendState.ViewState.Content::toSendView,
            SendView::toSendUrl,
            SendView::toViewState,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            SavedStateHandle::toAddEditSendArgs,
            AddEditSendState.ViewState.Content::toSendView,
            SendView::toSendUrl,
            SendView::toViewState,
        )
    }

    @Test
    fun `initial state should be correct`() {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct when a sendOption includes shouldDisableHideEmail`() {
        every {
            policyManager.getActivePolicies(type = PolicyTypeJson.SEND_OPTIONS)
        } returns listOf(
            createMockPolicy(
                id = "123",
                type = PolicyTypeJson.SEND_OPTIONS,
                isEnabled = true,
                data = Json.encodeToJsonElement(
                    PolicyInformation.SendOptions(shouldDisableHideEmail = true),
                ).jsonObject,
                organizationId = "id2",
            ),
        )
        val viewModel = createViewModel()
        val viewState = DEFAULT_VIEW_STATE.copy(
            common = DEFAULT_COMMON_STATE.copy(
                isHideEmailAddressEnabled = false,
            ),
        )
        assertEquals(DEFAULT_STATE.copy(viewState = viewState), viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should read from saved state when present`() {
        val savedState = DEFAULT_STATE.copy(
            dialogState = AddEditSendState.DialogState.Loading("Loading".asText()),
        )
        val viewModel = createViewModel(savedState)
        assertEquals(savedState, viewModel.stateFlow.value)
    }

    @Test
    fun `CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AddEditSendAction.CloseClick)
            assertEquals(AddEditSendEvent.NavigateBack, awaitItem())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `SaveClick with createSend success should copy the send URL to the clipboard and emit NavigateBack`() =
        runTest {
            val viewState = DEFAULT_VIEW_STATE.copy(
                common = DEFAULT_COMMON_STATE.copy(name = "input"),
            )
            val initialState = DEFAULT_STATE.copy(
                shouldFinishOnComplete = false,
                isShared = true,
                viewState = viewState,
            )
            val mockSendView = mockk<SendView>()
            every { viewState.toSendView(clock) } returns mockSendView
            val sendUrl = "www.test.com/send/test"
            val resultSendView = mockk<SendView> {
                every { toSendUrl(DEFAULT_ENVIRONMENT_URL) } returns sendUrl
            }
            coEvery {
                vaultRepository.createSend(sendView = mockSendView, fileUri = null)
            } returns CreateSendResult.Success(sendView = resultSendView)
            val viewModel = createViewModel(initialState)

            viewModel.eventFlow.test {
                viewModel.trySendAction(AddEditSendAction.SaveClick)
                assertEquals(AddEditSendEvent.NavigateBack, awaitItem())
                assertEquals(
                    AddEditSendEvent.ShowShareSheet(message = "www.test.com/send/test"),
                    awaitItem(),
                )
            }
            assertEquals(initialState, viewModel.stateFlow.value)
            coVerify(exactly = 1) {
                vaultRepository.createSend(sendView = mockSendView, fileUri = null)
                specialCircumstanceManager.specialCircumstance = null
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `SaveClick with createSend success should copy the send URL to the clipboard and emit ExitApp`() =
        runTest {
            val viewState = DEFAULT_VIEW_STATE.copy(
                common = DEFAULT_COMMON_STATE.copy(name = "input"),
            )
            val initialState = DEFAULT_STATE.copy(
                shouldFinishOnComplete = true,
                isShared = true,
                viewState = viewState,
            )
            val mockSendView = mockk<SendView>()
            every { viewState.toSendView(clock) } returns mockSendView
            val sendUrl = "www.test.com/send/test"
            val resultSendView = mockk<SendView> {
                every { toSendUrl(DEFAULT_ENVIRONMENT_URL) } returns sendUrl
            }
            coEvery {
                vaultRepository.createSend(sendView = mockSendView, fileUri = null)
            } returns CreateSendResult.Success(sendView = resultSendView)
            val viewModel = createViewModel(initialState)

            viewModel.eventFlow.test {
                viewModel.trySendAction(AddEditSendAction.SaveClick)
                assertEquals(AddEditSendEvent.ExitApp, awaitItem())
                assertEquals(
                    AddEditSendEvent.ShowShareSheet(message = "www.test.com/send/test"),
                    awaitItem(),
                )
            }
            assertEquals(initialState, viewModel.stateFlow.value)
            coVerify(exactly = 1) {
                vaultRepository.createSend(sendView = mockSendView, fileUri = null)
                specialCircumstanceManager.specialCircumstance = null
            }
        }

    @Test
    fun `SaveClick with createSend failure should show error dialog`() = runTest {
        val viewState = DEFAULT_VIEW_STATE.copy(
            common = DEFAULT_COMMON_STATE.copy(name = "input"),
        )
        val initialState = DEFAULT_STATE.copy(viewState = viewState)
        val mockSendView = mockk<SendView>()
        every { viewState.toSendView(clock) } returns mockSendView
        coEvery {
            vaultRepository.createSend(sendView = mockSendView, fileUri = null)
        } returns CreateSendResult.Error(message = "Fail", error = null)
        val viewModel = createViewModel(initialState)

        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            viewModel.trySendAction(AddEditSendAction.SaveClick)
            assertEquals(
                initialState.copy(
                    dialogState = AddEditSendState.DialogState.Loading(
                        message = BitwardenString.saving.asText(),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                initialState.copy(
                    dialogState = AddEditSendState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = "Fail".asText(),
                    ),
                ),
                awaitItem(),
            )
        }
        coVerify(exactly = 1) {
            vaultRepository.createSend(sendView = mockSendView, fileUri = null)
        }
    }

    @Test
    fun `SaveClick with updateSend success should emit NavigateBack and ShowShareSheet`() =
        runTest {
            val sendId = "sendId-1"
            val viewState = DEFAULT_VIEW_STATE.copy(
                common = DEFAULT_COMMON_STATE.copy(name = "input"),
            )
            val initialState = DEFAULT_STATE.copy(
                addEditSendType = AddEditSendType.EditItem(sendId),
                viewState = viewState,
            )
            val mockSendView = createMockSendView(number = 1)
            every {
                mockSendView.toViewState(
                    clock = clock,
                    baseWebSendUrl = DEFAULT_ENVIRONMENT_URL,
                    isHideEmailAddressEnabled = true,
                )
            } returns viewState
            every { viewState.toSendView(clock) } returns mockSendView
            val sendUrl = "www.test.com/send/test"
            val resultSendView = mockk<SendView> {
                every { toSendUrl(DEFAULT_ENVIRONMENT_URL) } returns sendUrl
                every { id } returns sendId
            }
            coEvery {
                vaultRepository.updateSend(sendId = sendId, sendView = mockSendView)
            } returns UpdateSendResult.Success(sendView = resultSendView)
            mutableSendDataStateFlow.value = DataState.Loaded(mockSendView)
            val viewModel = createViewModel(initialState, AddEditSendType.EditItem(sendId))

            viewModel.eventFlow.test {
                viewModel.trySendAction(AddEditSendAction.SaveClick)
                assertEquals(AddEditSendEvent.NavigateBack, awaitItem())
                assertEquals(AddEditSendEvent.ShowShareSheet(sendUrl), awaitItem())
            }
            assertEquals(initialState, viewModel.stateFlow.value)
            coVerify(exactly = 1) {
                vaultRepository.updateSend(sendId = sendId, sendView = mockSendView)
            }
        }

    @Test
    fun `SaveClick with updateSend failure should show error dialog`() = runTest {
        val sendId = "sendId-1"
        val viewState = DEFAULT_VIEW_STATE.copy(
            common = DEFAULT_COMMON_STATE.copy(name = "input"),
        )
        val initialState = DEFAULT_STATE.copy(
            addEditSendType = AddEditSendType.EditItem(sendId),
            viewState = viewState,
        )
        val mockSendView = mockk<SendView> {
            every { id } returns sendId
        }
        val errorMessage = "Failure"
        every {
            mockSendView.toViewState(
                clock = clock,
                baseWebSendUrl = DEFAULT_ENVIRONMENT_URL,
                isHideEmailAddressEnabled = true,
            )
        } returns viewState
        every { viewState.toSendView(clock) } returns mockSendView
        coEvery {
            vaultRepository.updateSend(sendId = sendId, sendView = mockSendView)
        } returns UpdateSendResult.Error(errorMessage = errorMessage, error = null)
        mutableSendDataStateFlow.value = DataState.Loaded(mockSendView)
        val viewModel = createViewModel(initialState, AddEditSendType.EditItem(sendId))

        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            viewModel.trySendAction(AddEditSendAction.SaveClick)
            assertEquals(
                initialState.copy(
                    dialogState = AddEditSendState.DialogState.Loading(
                        message = BitwardenString.saving.asText(),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                initialState.copy(
                    dialogState = AddEditSendState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = errorMessage.asText(),
                    ),
                ),
                awaitItem(),
            )
        }
        coVerify(exactly = 1) {
            vaultRepository.updateSend(sendId = sendId, sendView = mockSendView)
        }
    }

    @Test
    fun `SaveClick with blank name should show error dialog`() {
        val viewModel = createViewModel(DEFAULT_STATE)

        viewModel.trySendAction(AddEditSendAction.SaveClick)

        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = AddEditSendState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.validation_field_required.asText(
                        BitwardenString.name.asText(),
                    ),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `SaveClick for file without premium show error dialog`() {
        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
            accounts = listOf(DEFAULT_ACCOUNT.copy(isPremium = false)),
        )
        val initialState = DEFAULT_STATE.copy(
            isPremium = false,
            viewState = DEFAULT_VIEW_STATE.copy(
                common = DEFAULT_COMMON_STATE.copy(name = "test"),
                selectedType = AddEditSendState.ViewState.Content.SendType.File(
                    uri = null,
                    name = null,
                    displaySize = null,
                    sizeBytes = null,
                ),
            ),
        )
        val viewModel = createViewModel(initialState)

        viewModel.trySendAction(AddEditSendAction.SaveClick)

        assertEquals(
            initialState.copy(
                dialogState = AddEditSendState.DialogState.Error(
                    title = BitwardenString.send.asText(),
                    message = BitwardenString.send_file_premium_required.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `SaveClick with file missing should show error dialog`() {
        val initialState = DEFAULT_STATE.copy(
            viewState = DEFAULT_VIEW_STATE.copy(
                common = DEFAULT_COMMON_STATE.copy(name = "test"),
                selectedType = AddEditSendState.ViewState.Content.SendType.File(
                    uri = null,
                    name = null,
                    displaySize = null,
                    sizeBytes = null,
                ),
            ),
        )
        val viewModel = createViewModel(initialState)

        viewModel.trySendAction(AddEditSendAction.SaveClick)

        assertEquals(
            initialState.copy(
                dialogState = AddEditSendState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.you_must_attach_a_file_to_save_this_send.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `SaveClick with file too large should show error dialog`() {
        val initialState = DEFAULT_STATE.copy(
            viewState = DEFAULT_VIEW_STATE.copy(
                common = DEFAULT_COMMON_STATE.copy(name = "test"),
                selectedType = AddEditSendState.ViewState.Content.SendType.File(
                    uri = mockk(),
                    name = "test.png",
                    displaySize = null,
                    // Max size is 104857600
                    sizeBytes = 104857601,
                ),
            ),
        )
        val viewModel = createViewModel(initialState)

        viewModel.trySendAction(AddEditSendAction.SaveClick)

        assertEquals(
            initialState.copy(
                dialogState = AddEditSendState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.max_file_size.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `CopyLinkClick with nonnull sendUrl should copy to clipboard`() {
        val sendUrl = "www.test.com/send-stuff"
        val viewState = DEFAULT_VIEW_STATE.copy(
            common = DEFAULT_COMMON_STATE.copy(sendUrl = sendUrl),
        )
        val mockSendView = createMockSendView(number = 1)
        every {
            mockSendView.toViewState(
                clock = clock,
                baseWebSendUrl = DEFAULT_ENVIRONMENT_URL,
                isHideEmailAddressEnabled = true,
            )
        } returns viewState
        mutableSendDataStateFlow.value = DataState.Loaded(mockSendView)
        val viewModel = createViewModel(
            state = DEFAULT_STATE.copy(
                addEditSendType = AddEditSendType.EditItem("sendId"),
                viewState = viewState,
            ),
            addEditSendType = AddEditSendType.EditItem("sendId"),
        )

        viewModel.trySendAction(AddEditSendAction.CopyLinkClick)

        verify(exactly = 1) {
            clipboardManager.setText(
                text = sendUrl,
                toastDescriptorOverride = BitwardenString.send_link.asText(),
            )
        }
    }

    @Test
    fun `in add item state, RemovePasswordClick should do nothing`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(AddEditSendAction.RemovePasswordClick)
            expectNoEvents()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in edit item state, RemovePasswordClick vaultRepository removePasswordSend Error without message should show default error dialog`() =
        runTest {
            val sendId = "mockId-1"
            coEvery {
                vaultRepository.removePasswordSend(sendId)
            } returns RemovePasswordSendResult.Error(errorMessage = null, error = null)
            val initialState = DEFAULT_STATE.copy(
                addEditSendType = AddEditSendType.EditItem(sendItemId = sendId),
            )
            val mockSendView = createMockSendView(number = 1)
            every {
                mockSendView.toViewState(
                    clock = clock,
                    baseWebSendUrl = DEFAULT_ENVIRONMENT_URL,
                    isHideEmailAddressEnabled = true,
                )
            } returns DEFAULT_VIEW_STATE
            mutableSendDataStateFlow.value = DataState.Loaded(mockSendView)
            val viewModel = createViewModel(
                state = initialState,
                addEditSendType = AddEditSendType.EditItem(sendItemId = sendId),
            )

            viewModel.stateFlow.test {
                assertEquals(initialState, awaitItem())
                viewModel.trySendAction(AddEditSendAction.RemovePasswordClick)
                assertEquals(
                    initialState.copy(
                        dialogState = AddEditSendState.DialogState.Loading(
                            message = BitwardenString.removing_send_password.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    initialState.copy(
                        dialogState = AddEditSendState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in edit item state, RemovePasswordClick vaultRepository removePasswordSend Error with message should show error dialog with message`() =
        runTest {
            val sendId = "mockId-1"
            val errorMessage = "Fail"
            coEvery {
                vaultRepository.removePasswordSend(sendId)
            } returns RemovePasswordSendResult.Error(errorMessage = errorMessage, error = null)
            val mockSendView = createMockSendView(number = 1)
            every {
                mockSendView.toViewState(
                    clock = clock,
                    baseWebSendUrl = DEFAULT_ENVIRONMENT_URL,
                    isHideEmailAddressEnabled = true,
                )
            } returns DEFAULT_VIEW_STATE
            mutableSendDataStateFlow.value = DataState.Loaded(mockSendView)
            val initialState = DEFAULT_STATE.copy(
                addEditSendType = AddEditSendType.EditItem(sendItemId = sendId),
            )
            val viewModel = createViewModel(
                state = initialState,
                addEditSendType = AddEditSendType.EditItem(sendItemId = sendId),
            )

            viewModel.stateFlow.test {
                assertEquals(initialState, awaitItem())
                viewModel.trySendAction(AddEditSendAction.RemovePasswordClick)
                assertEquals(
                    initialState.copy(
                        dialogState = AddEditSendState.DialogState.Loading(
                            message = BitwardenString.removing_send_password.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    initialState.copy(
                        dialogState = AddEditSendState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = errorMessage.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in edit item state, RemovePasswordClick vaultRepository removePasswordSend Success should show toast`() =
        runTest {
            val sendId = "mockId-1"
            val mockSendView = createMockSendView(number = 1)
            coEvery {
                vaultRepository.removePasswordSend(sendId)
            } returns RemovePasswordSendResult.Success(mockSendView)
            val viewModel = createViewModel(
                state = DEFAULT_STATE.copy(addEditSendType = AddEditSendType.EditItem(sendItemId = sendId)),
                addEditSendType = AddEditSendType.EditItem(sendItemId = sendId),
            )

            viewModel.eventFlow.test {
                viewModel.trySendAction(AddEditSendAction.RemovePasswordClick)
                assertEquals(
                    AddEditSendEvent.ShowSnackbar(
                        data = BitwardenSnackbarData(message = BitwardenString.password_removed.asText()),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `DeleteClick vaultRepository deleteSend Error should show error dialog`() = runTest {
        val error = Throwable("Ooops")
        val sendId = "mockId-1"
        coEvery { vaultRepository.deleteSend(sendId) } returns DeleteSendResult.Error(
            error = error,
        )
        val initialState = DEFAULT_STATE.copy(
            addEditSendType = AddEditSendType.EditItem(sendItemId = sendId),
        )
        val mockSendView = createMockSendView(number = 1)
        every {
            mockSendView.toViewState(
                clock = clock,
                baseWebSendUrl = DEFAULT_ENVIRONMENT_URL,
                isHideEmailAddressEnabled = true,
            )
        } returns DEFAULT_VIEW_STATE
        mutableSendDataStateFlow.value = DataState.Loaded(mockSendView)
        val viewModel = createViewModel(
            state = initialState,
            addEditSendType = AddEditSendType.EditItem(sendItemId = sendId),
        )

        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            viewModel.trySendAction(AddEditSendAction.DeleteClick)
            assertEquals(
                initialState.copy(
                    dialogState = AddEditSendState.DialogState.Loading(
                        message = BitwardenString.deleting.asText(),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                initialState.copy(
                    dialogState = AddEditSendState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.generic_error_message.asText(),
                        throwable = error,
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `DeleteClick vaultRepository deleteSend Success should emit NavigateUpToSearchOrRoot`() =
        runTest {
            val sendId = "mockId-1"
            coEvery { vaultRepository.deleteSend(sendId) } returns DeleteSendResult.Success
            val viewModel = createViewModel(
                state = DEFAULT_STATE.copy(
                    addEditSendType = AddEditSendType.EditItem(sendItemId = sendId),
                ),
                addEditSendType = AddEditSendType.EditItem(sendItemId = sendId),
            )

            viewModel.eventFlow.test {
                viewModel.trySendAction(AddEditSendAction.DeleteClick)
                assertEquals(AddEditSendEvent.NavigateUpToSearchOrRoot, awaitItem())
            }
            verify(exactly = 1) {
                snackbarRelayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(message = BitwardenString.send_deleted.asText()),
                    relay = SnackbarRelay.SEND_DELETED,
                )
            }
        }

    @Test
    fun `ShareLinkClick with nonnull sendUrl should launch share sheet`() = runTest {
        val sendUrl = "www.test.com/send-stuff"
        val viewState = DEFAULT_VIEW_STATE.copy(
            common = DEFAULT_COMMON_STATE.copy(sendUrl = sendUrl),
        )
        val mockSendView = createMockSendView(number = 1)
        every {
            mockSendView.toViewState(
                clock = clock,
                baseWebSendUrl = DEFAULT_ENVIRONMENT_URL,
                isHideEmailAddressEnabled = true,
            )
        } returns viewState
        mutableSendDataStateFlow.value = DataState.Loaded(mockSendView)
        val viewModel = createViewModel(
            state = DEFAULT_STATE.copy(
                addEditSendType = AddEditSendType.EditItem("sendId"),
                viewState = viewState,
            ),
            addEditSendType = AddEditSendType.EditItem("sendId"),
        )

        viewModel.eventFlow.test {
            viewModel.trySendAction(AddEditSendAction.ShareLinkClick)
            assertEquals(AddEditSendEvent.ShowShareSheet(sendUrl), awaitItem())
        }
    }

    @Test
    fun `DismissDialogClick should clear the dialog state`() {
        val viewModel = createViewModel(
            DEFAULT_STATE.copy(
                dialogState = AddEditSendState.DialogState.Error(
                    title = "Fail Title".asText(),
                    message = "Fail Message".asText(),
                ),
            ),
        )
        viewModel.trySendAction(AddEditSendAction.DismissDialogClick)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `DeletionDateChange should store the new deletion date`() {
        val viewModel = createViewModel()
        val newDeletionDate = ZonedDateTime.parse("2024-09-13T00:00Z")
        // DEFAULT deletion date is "2023-11-03T00:00Z"
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)

        viewModel.trySendAction(AddEditSendAction.DeletionDateChange(newDeletionDate))

        assertEquals(
            DEFAULT_STATE.copy(
                viewState = DEFAULT_VIEW_STATE.copy(
                    common = DEFAULT_COMMON_STATE.copy(
                        deletionDate = newDeletionDate,
                    ),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `FileChose should update the state accordingly`() = runTest {
        val initialState = DEFAULT_STATE.copy(
            viewState = DEFAULT_VIEW_STATE.copy(
                selectedType = AddEditSendState.ViewState.Content.SendType.File(
                    uri = null,
                    name = null,
                    displaySize = null,
                    sizeBytes = null,
                ),
            ),
        )
        val fileName = "test.png"
        val uri = mockk<Uri>()
        val size = 50L
        val fileData = FileData(
            fileName = fileName,
            uri = uri,
            sizeBytes = size,
        )
        val viewModel = createViewModel(initialState)

        viewModel.trySendAction(AddEditSendAction.FileChoose(fileData = fileData))

        assertEquals(
            initialState.copy(
                viewState = DEFAULT_VIEW_STATE.copy(
                    selectedType = AddEditSendState.ViewState.Content.SendType.File(
                        uri = uri,
                        name = fileName,
                        displaySize = null,
                        sizeBytes = size,
                    ),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `ChooseFileClick should emit ShowChooserSheet`() = runTest {
        val arePermissionsGranted = true
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AddEditSendAction.ChooseFileClick(arePermissionsGranted))
            assertEquals(AddEditSendEvent.ShowChooserSheet(arePermissionsGranted), awaitItem())
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
            viewModel.trySendAction(AddEditSendAction.NameChange("input"))
            assertEquals(DEFAULT_STATE.copy(viewState = expectedViewState), awaitItem())
        }
    }

    @Test
    fun `MaxAccessCountChange should update maxAccessCount to value when non-zero`() = runTest {
        val viewModel = createViewModel()
        val expectedViewState = DEFAULT_VIEW_STATE.copy(
            common = DEFAULT_COMMON_STATE.copy(maxAccessCount = 5),
        )

        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(AddEditSendAction.MaxAccessCountChange(5))
            assertEquals(DEFAULT_STATE.copy(viewState = expectedViewState), awaitItem())
        }
    }

    @Test
    fun `MaxAccessCountChange should update maxAccessCount to null when zero`() = runTest {
        val initialState = DEFAULT_STATE.copy(
            viewState = DEFAULT_VIEW_STATE.copy(
                common = DEFAULT_COMMON_STATE.copy(maxAccessCount = 5),
            ),
        )
        val expectedViewState = DEFAULT_VIEW_STATE.copy(
            common = DEFAULT_COMMON_STATE.copy(maxAccessCount = null),
        )
        val viewModel = createViewModel(initialState)

        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            viewModel.trySendAction(AddEditSendAction.MaxAccessCountChange(0))
            assertEquals(initialState.copy(viewState = expectedViewState), awaitItem())
        }
    }

    @Test
    fun `TextChange should update text input`() = runTest {
        val viewModel = createViewModel()
        val expectedViewState = DEFAULT_VIEW_STATE.copy(
            selectedType = AddEditSendState.ViewState.Content.SendType.Text(
                input = "input",
                isHideByDefaultChecked = false,
            ),
        )

        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(AddEditSendAction.TextChange("input"))
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
            viewModel.trySendAction(AddEditSendAction.NoteChange("input"))
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
            viewModel.trySendAction(AddEditSendAction.PasswordChange("input"))
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
            viewModel.trySendAction(AddEditSendAction.DeactivateThisSendToggle(true))
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
            viewModel.trySendAction(AddEditSendAction.HideMyEmailToggle(isChecked = true))
            assertEquals(DEFAULT_STATE.copy(viewState = expectedViewState), awaitItem())
        }
    }

    @Test
    fun `If there is no network connection, show dialog and do not attempt add send`() = runTest {
        val viewState = DEFAULT_VIEW_STATE.copy(
            common = DEFAULT_COMMON_STATE.copy(name = "input"),
        )
        val initialState = DEFAULT_STATE.copy(
            shouldFinishOnComplete = true,
            isShared = true,
            viewState = viewState,
        )
        val mockSendView = mockk<SendView>()
        every { viewState.toSendView(clock) } returns mockSendView
        val sendUrl = "www.test.com/send/test"
        val resultSendView = mockk<SendView> {
            every { toSendUrl(DEFAULT_ENVIRONMENT_URL) } returns sendUrl
        }
        coEvery {
            vaultRepository.createSend(sendView = mockSendView, fileUri = null)
        } returns CreateSendResult.Success(sendView = resultSendView)
        val viewModel = createViewModel(initialState)
        every { networkConnectionManager.isNetworkConnected } returns false

        viewModel.trySendAction(AddEditSendAction.SaveClick)
        assertEquals(
            initialState.copy(
                dialogState = AddEditSendState.DialogState.Error(
                    title = BitwardenString.internet_connection_required_title.asText(),
                    message = BitwardenString.internet_connection_required_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )

        coVerify(
            exactly = 0,
        ) {
            vaultRepository.createSend(sendView = mockSendView, fileUri = null)
        }
    }

    private fun createViewModel(
        state: AddEditSendState? = null,
        addEditSendType: AddEditSendType = AddEditSendType.AddItem,
        sendType: SendItemType = SendItemType.TEXT,
        activityToken: String? = null,
    ): AddEditSendViewModel = AddEditSendViewModel(
        savedStateHandle = SavedStateHandle().apply {
            set("state", state?.copy(addEditSendType = addEditSendType))
            set("activityToken", activityToken)
            every {
                toAddEditSendArgs()
            } returns AddEditSendArgs(sendType = sendType, addEditSendType = addEditSendType)
        },
        authRepo = authRepository,
        environmentRepo = environmentRepository,
        specialCircumstanceManager = specialCircumstanceManager,
        clock = clock,
        clipboardManager = clipboardManager,
        vaultRepo = vaultRepository,
        policyManager = policyManager,
        networkConnectionManager = networkConnectionManager,
        snackbarRelayManager = snackbarRelayManager,
    )
}

private val DEFAULT_COMMON_STATE = AddEditSendState.ViewState.Content.Common(
    name = "",
    currentAccessCount = null,
    maxAccessCount = null,
    passwordInput = "",
    noteInput = "",
    isHideEmailChecked = false,
    isDeactivateChecked = false,
    deletionDate = ZonedDateTime.parse("2023-11-03T12:00Z"),
    expirationDate = null,
    sendUrl = null,
    hasPassword = false,
    isHideEmailAddressEnabled = true,
)

private val DEFAULT_SELECTED_TYPE_STATE = AddEditSendState.ViewState.Content.SendType.Text(
    input = "",
    isHideByDefaultChecked = false,
)

private val DEFAULT_VIEW_STATE = AddEditSendState.ViewState.Content(
    common = DEFAULT_COMMON_STATE,
    selectedType = DEFAULT_SELECTED_TYPE_STATE,
)

private const val DEFAULT_ENVIRONMENT_URL = "https://send.bitwarden.com/#"

private val DEFAULT_STATE = AddEditSendState(
    addEditSendType = AddEditSendType.AddItem,
    viewState = DEFAULT_VIEW_STATE,
    dialogState = null,
    shouldFinishOnComplete = false,
    isShared = false,
    baseWebSendUrl = DEFAULT_ENVIRONMENT_URL,
    policyDisablesSend = false,
    sendType = SendItemType.TEXT,
    isPremium = true,
)

private val DEFAULT_ACCOUNT = UserState.Account(
    userId = "activeUserId",
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
)

private val DEFAULT_USER_STATE = UserState(
    activeUserId = "activeUserId",
    accounts = listOf(DEFAULT_ACCOUNT),
)
