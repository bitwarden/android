package com.x8bit.bitwarden.ui.tools.feature.send.addsend

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.SendView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.CreateSendResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateSendResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.model.AddSendType
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.util.toSendView
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.util.toViewState
import com.x8bit.bitwarden.ui.tools.feature.send.util.toSendUrl
import io.mockk.coEvery
import io.mockk.coVerify
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

class AddSendViewModelTest : BaseViewModelTest() {

    private val clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val authRepository: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
    }
    private val environmentRepository: EnvironmentRepository = mockk {
        every { environment } returns Environment.Us
    }
    private val mutableSendDataStateFlow = MutableStateFlow<DataState<SendView>>(DataState.Loading)
    private val vaultRepository: VaultRepository = mockk {
        every { getSendStateFlow(any()) } returns mutableSendDataStateFlow
    }

    @BeforeEach
    fun setup() {
        mockkStatic(
            ADD_SEND_STATE_EXTENSIONS_PATH,
            ADD_SEND_VIEW_EXTENSIONS_PATH,
            SEND_VIEW_EXTENSIONS_PATH,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            ADD_SEND_STATE_EXTENSIONS_PATH,
            ADD_SEND_VIEW_EXTENSIONS_PATH,
            SEND_VIEW_EXTENSIONS_PATH,
        )
    }

    @Test
    fun `initial state should be correct`() {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
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

    @Test
    fun `SaveClick with createSend success should emit NavigateBack and ShowShareSheet`() =
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
                vaultRepository.createSend(mockSendView)
            } returns CreateSendResult.Success(sendView = resultSendView)
            val viewModel = createViewModel(initialState)

            viewModel.eventFlow.test {
                viewModel.trySendAction(AddSendAction.SaveClick)
                assertEquals(AddSendEvent.NavigateBack, awaitItem())
                assertEquals(AddSendEvent.ShowShareSheet(sendUrl), awaitItem())
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
        every { viewState.toSendView(clock) } returns mockSendView
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
            every { mockSendView.toViewState(clock, DEFAULT_ENVIRONMENT_URL) } returns viewState
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
        every { mockSendView.toViewState(clock, DEFAULT_ENVIRONMENT_URL) } returns viewState
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
    fun `CopyLinkClick should send ShowToast`() = runTest {
        val viewModel = createViewModel(
            state = DEFAULT_STATE.copy(addSendType = AddSendType.EditItem("sendId")),
            addSendType = AddSendType.EditItem("sendId"),
        )

        viewModel.eventFlow.test {
            viewModel.trySendAction(AddSendAction.CopyLinkClick)
            assertEquals(AddSendEvent.ShowToast("Not yet implemented".asText()), awaitItem())
        }
    }

    @Test
    fun `DeleteClick should send ShowToast`() = runTest {
        val viewModel = createViewModel(
            state = DEFAULT_STATE.copy(addSendType = AddSendType.EditItem("sendId")),
            addSendType = AddSendType.EditItem("sendId"),
        )

        viewModel.eventFlow.test {
            viewModel.trySendAction(AddSendAction.DeleteClick)
            assertEquals(AddSendEvent.ShowToast("Not yet implemented".asText()), awaitItem())
        }
    }

    @Test
    fun `RemovePasswordClick should send ShowToast`() = runTest {
        val viewModel = createViewModel(
            state = DEFAULT_STATE.copy(addSendType = AddSendType.EditItem("sendId")),
            addSendType = AddSendType.EditItem("sendId"),
        )

        viewModel.eventFlow.test {
            viewModel.trySendAction(AddSendAction.RemovePasswordClick)
            assertEquals(AddSendEvent.ShowToast("Not yet implemented".asText()), awaitItem())
        }
    }

    @Test
    fun `ShareLinkClick should send ShowToast`() = runTest {
        val viewModel = createViewModel(
            state = DEFAULT_STATE.copy(addSendType = AddSendType.EditItem("sendId")),
            addSendType = AddSendType.EditItem("sendId"),
        )

        viewModel.eventFlow.test {
            viewModel.trySendAction(AddSendAction.ShareLinkClick)
            assertEquals(AddSendEvent.ShowToast("Not yet implemented".asText()), awaitItem())
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
    fun `ChooseFileClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AddSendAction.ChooseFileClick)
            assertEquals(
                AddSendEvent.ShowToast("Not Implemented: File Upload".asText()),
                awaitItem(),
            )
        }
    }

    @Test
    fun `FileTypeClick and TextTypeClick should toggle sendType when user is premium`() = runTest {
        val viewModel = createViewModel()
        val premiumUserState = DEFAULT_STATE.copy(isPremiumUser = true)
        val expectedViewState = DEFAULT_VIEW_STATE.copy(
            selectedType = AddSendState.ViewState.Content.SendType.File,
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

    private fun createViewModel(
        state: AddSendState? = null,
        addSendType: AddSendType = AddSendType.AddItem,
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
        },
        authRepo = authRepository,
        environmentRepo = environmentRepository,
        clock = clock,
        vaultRepo = vaultRepository,
    )

    companion object {
        private const val ADD_SEND_STATE_EXTENSIONS_PATH: String =
            "com.x8bit.bitwarden.ui.tools.feature.send.addsend.util.AddSendStateExtensionsKt"
        private const val ADD_SEND_VIEW_EXTENSIONS_PATH: String =
            "com.x8bit.bitwarden.ui.tools.feature.send.addsend.util.SendViewExtensionsKt"
        private const val SEND_VIEW_EXTENSIONS_PATH: String =
            "com.x8bit.bitwarden.ui.tools.feature.send.util.SendViewExtensionsKt"

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
            isPremiumUser = false,
            baseWebSendUrl = DEFAULT_ENVIRONMENT_URL,
        )

        private val DEFAULT_USER_ACCOUNT_STATE = UserState.Account(
            userId = "user_id_1",
            name = "Bit",
            email = "bitwarden@gmail.com",
            avatarColorHex = "#ff00ff",
            environment = Environment.Us,
            isPremium = false,
            isVaultUnlocked = true,
            organizations = emptyList(),
        )

        private val DEFAULT_USER_STATE = UserState(
            activeUserId = "user_id_1",
            accounts = listOf(DEFAULT_USER_ACCOUNT_STATE),
        )
    }
}
