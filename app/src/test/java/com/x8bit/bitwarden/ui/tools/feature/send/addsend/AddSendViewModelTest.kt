package com.x8bit.bitwarden.ui.tools.feature.send.addsend

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.send.SendView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.NetworkConnectionManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockPolicy
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.CreateSendResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteSendResult
import com.x8bit.bitwarden.data.vault.repository.model.RemovePasswordSendResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateSendResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.model.AddSendType
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.util.toSendView
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.util.toViewState
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
class AddSendViewModelTest : BaseViewModelTest() {

    private val clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )
    private val clipboardManager: BitwardenClipboardManager = mockk {
        every { setText(any<String>()) } just runs
    }
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val authRepository: AuthRepository = mockk {
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

    @BeforeEach
    fun setup() {
        mockkStatic(
            AddSendState.ViewState.Content::toSendView,
            SendView::toSendUrl,
            SendView::toViewState,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            AddSendState.ViewState.Content::toSendView,
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
            SyncResponseJson.Policy(
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
            dialogState = AddSendState.DialogState.Loading("Loading".asText()),
        )
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

    @Suppress("MaxLineLength")
    @Test
    fun `SaveClick with createSend success should emit NavigateBack and ShowShareSheet when not an external shared`() =
        runTest {
            val viewState = DEFAULT_VIEW_STATE.copy(
                common = DEFAULT_COMMON_STATE.copy(name = "input"),
            )
            val initialState = DEFAULT_STATE.copy(viewState = viewState)
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
                viewModel.trySendAction(AddSendAction.SaveClick)
                assertEquals(AddSendEvent.NavigateBack, awaitItem())
                assertEquals(AddSendEvent.ShowShareSheet(sendUrl), awaitItem())
            }
            assertEquals(initialState, viewModel.stateFlow.value)
            coVerify(exactly = 1) {
                vaultRepository.createSend(sendView = mockSendView, fileUri = null)
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
                viewModel.trySendAction(AddSendAction.SaveClick)
                assertEquals(AddSendEvent.NavigateBack, awaitItem())
            }
            assertEquals(initialState, viewModel.stateFlow.value)
            coVerify(exactly = 1) {
                vaultRepository.createSend(sendView = mockSendView, fileUri = null)
                specialCircumstanceManager.specialCircumstance = null
                clipboardManager.setText(sendUrl)
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
                viewModel.trySendAction(AddSendAction.SaveClick)
                assertEquals(AddSendEvent.ExitApp, awaitItem())
            }
            assertEquals(initialState, viewModel.stateFlow.value)
            coVerify(exactly = 1) {
                vaultRepository.createSend(sendView = mockSendView, fileUri = null)
                specialCircumstanceManager.specialCircumstance = null
                clipboardManager.setText(sendUrl)
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
        } returns CreateSendResult.Error("Fail")
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
                addSendType = AddSendType.EditItem(sendId),
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
            val viewModel = createViewModel(initialState, AddSendType.EditItem(sendId))

            viewModel.eventFlow.test {
                viewModel.trySendAction(AddSendAction.SaveClick)
                assertEquals(AddSendEvent.NavigateBack, awaitItem())
                assertEquals(AddSendEvent.ShowShareSheet(sendUrl), awaitItem())
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
            addSendType = AddSendType.EditItem(sendId),
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
        } returns UpdateSendResult.Error(errorMessage = errorMessage)
        mutableSendDataStateFlow.value = DataState.Loaded(mockSendView)
        val viewModel = createViewModel(initialState, AddSendType.EditItem(sendId))

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
    fun `SaveClick with file missing should show error dialog`() {
        val initialState = DEFAULT_STATE.copy(
            viewState = DEFAULT_VIEW_STATE.copy(
                common = DEFAULT_COMMON_STATE.copy(name = "test"),
                selectedType = AddSendState.ViewState.Content.SendType.File(
                    uri = null,
                    name = null,
                    displaySize = null,
                    sizeBytes = null,
                ),
            ),
        )
        val viewModel = createViewModel(initialState)

        viewModel.trySendAction(AddSendAction.SaveClick)

        assertEquals(
            initialState.copy(
                dialogState = AddSendState.DialogState.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.validation_field_required.asText(R.string.file.asText()),
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
                selectedType = AddSendState.ViewState.Content.SendType.File(
                    uri = mockk(),
                    name = "test.png",
                    displaySize = null,
                    // Max size is 104857600
                    sizeBytes = 104857601,
                ),
            ),
        )
        val viewModel = createViewModel(initialState)

        viewModel.trySendAction(AddSendAction.SaveClick)

        assertEquals(
            initialState.copy(
                dialogState = AddSendState.DialogState.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.max_file_size.asText(),
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
                addSendType = AddSendType.EditItem("sendId"),
                viewState = viewState,
            ),
            addSendType = AddSendType.EditItem("sendId"),
        )

        viewModel.trySendAction(AddSendAction.CopyLinkClick)

        verify(exactly = 1) {
            clipboardManager.setText(sendUrl)
        }
    }

    @Test
    fun `in add item state, RemovePasswordClick should do nothing`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(AddSendAction.RemovePasswordClick)
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
            } returns RemovePasswordSendResult.Error(errorMessage = null)
            val initialState = DEFAULT_STATE.copy(
                addSendType = AddSendType.EditItem(sendItemId = sendId),
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
                addSendType = AddSendType.EditItem(sendItemId = sendId),
            )

            viewModel.stateFlow.test {
                assertEquals(initialState, awaitItem())
                viewModel.trySendAction(AddSendAction.RemovePasswordClick)
                assertEquals(
                    initialState.copy(
                        dialogState = AddSendState.DialogState.Loading(
                            message = R.string.removing_send_password.asText(),
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
        }

    @Suppress("MaxLineLength")
    @Test
    fun `in edit item state, RemovePasswordClick vaultRepository removePasswordSend Error with message should show error dialog with message`() =
        runTest {
            val sendId = "mockId-1"
            val errorMessage = "Fail"
            coEvery {
                vaultRepository.removePasswordSend(sendId)
            } returns RemovePasswordSendResult.Error(errorMessage = errorMessage)
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
                addSendType = AddSendType.EditItem(sendItemId = sendId),
            )
            val viewModel = createViewModel(
                state = initialState,
                addSendType = AddSendType.EditItem(sendItemId = sendId),
            )

            viewModel.stateFlow.test {
                assertEquals(initialState, awaitItem())
                viewModel.trySendAction(AddSendAction.RemovePasswordClick)
                assertEquals(
                    initialState.copy(
                        dialogState = AddSendState.DialogState.Loading(
                            message = R.string.removing_send_password.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    initialState.copy(
                        dialogState = AddSendState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
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
                state = DEFAULT_STATE.copy(addSendType = AddSendType.EditItem(sendItemId = sendId)),
                addSendType = AddSendType.EditItem(sendItemId = sendId),
            )

            viewModel.eventFlow.test {
                viewModel.trySendAction(AddSendAction.RemovePasswordClick)
                assertEquals(
                    AddSendEvent.ShowToast(R.string.send_password_removed.asText()),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `DeleteClick vaultRepository deleteSend Error should show error dialog`() = runTest {
        val sendId = "mockId-1"
        coEvery { vaultRepository.deleteSend(sendId) } returns DeleteSendResult.Error
        val initialState = DEFAULT_STATE.copy(
            addSendType = AddSendType.EditItem(sendItemId = sendId),
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
            addSendType = AddSendType.EditItem(sendItemId = sendId),
        )

        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            viewModel.trySendAction(AddSendAction.DeleteClick)
            assertEquals(
                initialState.copy(
                    dialogState = AddSendState.DialogState.Loading(
                        message = R.string.deleting.asText(),
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
    }

    @Test
    fun `DeleteClick vaultRepository deleteSend Success should show toast`() = runTest {
        val sendId = "mockId-1"
        coEvery { vaultRepository.deleteSend(sendId) } returns DeleteSendResult.Success
        val viewModel = createViewModel(
            state = DEFAULT_STATE.copy(addSendType = AddSendType.EditItem(sendItemId = sendId)),
            addSendType = AddSendType.EditItem(sendItemId = sendId),
        )

        viewModel.eventFlow.test {
            viewModel.trySendAction(AddSendAction.DeleteClick)
            assertEquals(AddSendEvent.NavigateBack, awaitItem())
            assertEquals(AddSendEvent.ShowToast(R.string.send_deleted.asText()), awaitItem())
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
                addSendType = AddSendType.EditItem("sendId"),
                viewState = viewState,
            ),
            addSendType = AddSendType.EditItem("sendId"),
        )

        viewModel.eventFlow.test {
            viewModel.trySendAction(AddSendAction.ShareLinkClick)
            assertEquals(AddSendEvent.ShowShareSheet(sendUrl), awaitItem())
        }
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
    fun `DeletionDateChange should store the new deletion date`() {
        val viewModel = createViewModel()
        val newDeletionDate = ZonedDateTime.parse("2024-09-13T00:00Z")
        // DEFAULT deletion date is "2023-11-03T00:00Z"
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)

        viewModel.trySendAction(AddSendAction.DeletionDateChange(newDeletionDate))

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
    fun `ExpirationDateChange should store the new expiration date`() {
        val viewModel = createViewModel()
        val newDeletionDate = ZonedDateTime.parse("2024-09-13T00:00Z")
        // DEFAULT expiration date is null
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)

        viewModel.trySendAction(AddSendAction.ExpirationDateChange(newDeletionDate))

        assertEquals(
            DEFAULT_STATE.copy(
                viewState = DEFAULT_VIEW_STATE.copy(
                    common = DEFAULT_COMMON_STATE.copy(
                        expirationDate = newDeletionDate,
                    ),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `ClearExpirationDate should clear the expiration date`() {
        val initialState = DEFAULT_STATE.copy(
            viewState = DEFAULT_VIEW_STATE.copy(
                DEFAULT_COMMON_STATE.copy(
                    expirationDate = ZonedDateTime.parse("2024-09-13T00:00Z"),
                ),
            ),
        )
        val viewModel = createViewModel(initialState)

        viewModel.trySendAction(AddSendAction.ClearExpirationDate)

        assertEquals(
            // DEFAULT expiration date is null
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `FileChose should emit ShowToast`() = runTest {
        val initialState = DEFAULT_STATE.copy(
            viewState = DEFAULT_VIEW_STATE.copy(
                selectedType = AddSendState.ViewState.Content.SendType.File(
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
        val fileData = IntentManager.FileData(
            fileName = fileName,
            uri = uri,
            sizeBytes = size,
        )
        val viewModel = createViewModel(initialState)

        viewModel.trySendAction(AddSendAction.FileChoose(fileData = fileData))

        assertEquals(
            initialState.copy(
                viewState = DEFAULT_VIEW_STATE.copy(
                    selectedType = AddSendState.ViewState.Content.SendType.File(
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
    fun `ChooseFileClick should emit ShowToast`() = runTest {
        val arePermissionsGranted = true
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AddSendAction.ChooseFileClick(arePermissionsGranted))
            assertEquals(AddSendEvent.ShowChooserSheet(arePermissionsGranted), awaitItem())
        }
    }

    @Test
    fun `FileTypeClick and TextTypeClick should toggle sendType when user is premium`() = runTest {
        val viewModel = createViewModel()
        val premiumUserState = DEFAULT_STATE.copy(isPremiumUser = true)
        val expectedViewState = DEFAULT_VIEW_STATE.copy(
            selectedType = AddSendState.ViewState.Content.SendType.File(
                name = null,
                displaySize = null,
                sizeBytes = null,
                uri = null,
            ),
        )
        // Make sure we are a premium user
        mutableUserStateFlow.tryEmit(
            DEFAULT_USER_STATE.copy(
                accounts = listOf(DEFAULT_USER_ACCOUNT_STATE.copy(isPremium = true)),
            ),
        )

        viewModel.stateFlow.test {
            assertEquals(premiumUserState, awaitItem())
            viewModel.trySendAction(AddSendAction.FileTypeClick)
            assertEquals(premiumUserState.copy(viewState = expectedViewState), awaitItem())
            viewModel.trySendAction(AddSendAction.TextTypeClick)
            assertEquals(premiumUserState, awaitItem())
        }
    }

    @Test
    fun `FileTypeClick should display error dialog when account is not premium`() {
        val viewModel = createViewModel()

        viewModel.trySendAction(AddSendAction.FileTypeClick)

        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = AddSendState.DialogState.Error(
                    title = R.string.send.asText(),
                    message = R.string.send_file_premium_required.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `FileTypeClick should display error dialog when policy disables send`() {
        every {
            policyManager.getActivePolicies(type = PolicyTypeJson.DISABLE_SEND)
        } returns listOf(createMockPolicy())
        val viewModel = createViewModel()

        viewModel.trySendAction(AddSendAction.FileTypeClick)

        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = AddSendState.DialogState.Error(
                    title = null,
                    message = R.string.send_disabled_warning.asText(),
                ),
                policyDisablesSend = true,
            ),
            viewModel.stateFlow.value,
        )
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
    fun `MaxAccessCountChange should update maxAccessCount to value when non-zero`() = runTest {
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
            viewModel.trySendAction(AddSendAction.MaxAccessCountChange(0))
            assertEquals(initialState.copy(viewState = expectedViewState), awaitItem())
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

        viewModel.trySendAction(AddSendAction.SaveClick)
        assertEquals(
            initialState.copy(
                dialogState = AddSendState.DialogState.Error(
                    title = R.string.internet_connection_required_title.asText(),
                    message = R.string.internet_connection_required_message.asText(),
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
        state: AddSendState? = null,
        addSendType: AddSendType = AddSendType.AddItem,
        activityToken: String? = null,
    ): AddSendViewModel = AddSendViewModel(
        savedStateHandle = SavedStateHandle().apply {
            set("state", state?.copy(addSendType = addSendType))
            set(
                "add_send_item_type",
                when (addSendType) {
                    AddSendType.AddItem -> "add"
                    is AddSendType.EditItem -> "edit"
                },
            )
            set("edit_send_id", (addSendType as? AddSendType.EditItem)?.sendItemId)
            set("activityToken", activityToken)
        },
        authRepo = authRepository,
        environmentRepo = environmentRepository,
        specialCircumstanceManager = specialCircumstanceManager,
        clock = clock,
        clipboardManager = clipboardManager,
        vaultRepo = vaultRepository,
        policyManager = policyManager,
        networkConnectionManager = networkConnectionManager,
    )

    companion object {
        private val DEFAULT_COMMON_STATE = AddSendState.ViewState.Content.Common(
            name = "",
            currentAccessCount = null,
            maxAccessCount = null,
            passwordInput = "",
            noteInput = "",
            isHideEmailChecked = false,
            isDeactivateChecked = false,
            deletionDate = ZonedDateTime.parse("2023-11-03T00:00Z"),
            expirationDate = null,
            sendUrl = null,
            hasPassword = false,
            isHideEmailAddressEnabled = true,
        )

        private val DEFAULT_SELECTED_TYPE_STATE = AddSendState.ViewState.Content.SendType.Text(
            input = "",
            isHideByDefaultChecked = false,
        )

        private val DEFAULT_VIEW_STATE = AddSendState.ViewState.Content(
            common = DEFAULT_COMMON_STATE,
            selectedType = DEFAULT_SELECTED_TYPE_STATE,
        )

        private const val DEFAULT_ENVIRONMENT_URL = "https://vault.bitwarden.com/#/send/"

        private val DEFAULT_STATE = AddSendState(
            addSendType = AddSendType.AddItem,
            viewState = DEFAULT_VIEW_STATE,
            dialogState = null,
            shouldFinishOnComplete = false,
            isShared = false,
            isPremiumUser = false,
            baseWebSendUrl = DEFAULT_ENVIRONMENT_URL,
            policyDisablesSend = false,
        )

        private val DEFAULT_USER_ACCOUNT_STATE = UserState.Account(
            userId = "user_id_1",
            name = "Bit",
            email = "bitwarden@gmail.com",
            avatarColorHex = "#ff00ff",
            environment = Environment.Us,
            isPremium = false,
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
        )

        private val DEFAULT_USER_STATE = UserState(
            activeUserId = "user_id_1",
            accounts = listOf(DEFAULT_USER_ACCOUNT_STATE),
        )
    }
}
