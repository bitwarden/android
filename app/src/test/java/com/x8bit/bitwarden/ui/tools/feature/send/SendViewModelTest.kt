package com.x8bit.bitwarden.ui.tools.feature.send

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.data.repository.util.baseWebSendUrl
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.network.NetworkConnectionManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DeleteSendResult
import com.x8bit.bitwarden.data.vault.repository.model.RemovePasswordSendResult
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.ui.tools.feature.send.util.toViewState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SendViewModelTest : BaseViewModelTest() {

    private val mutablePullToRefreshEnabledFlow = MutableStateFlow(false)
    private val mutableSendDataFlow = MutableStateFlow<DataState<SendData>>(DataState.Loading)

    private val clipboardManager: BitwardenClipboardManager = mockk {
        every { setText(text = any<String>(), toastDescriptorOverride = any<Text>()) } just runs
    }
    private val environmentRepo: EnvironmentRepository = mockk {
        every { environment } returns Environment.Us
    }

    private val settingsRepo: SettingsRepository = mockk {
        every { getPullToRefreshEnabledFlow() } returns mutablePullToRefreshEnabledFlow
    }
    private val vaultRepo: VaultRepository = mockk {
        every { sendDataStateFlow } returns mutableSendDataFlow
    }
    private val policyManager: PolicyManager = mockk {
        every { getActivePolicies(type = PolicyTypeJson.DISABLE_SEND) } returns emptyList()
        every { getActivePoliciesFlow(type = PolicyTypeJson.DISABLE_SEND) } returns emptyFlow()
    }

    private val networkConnectionManager: NetworkConnectionManager = mockk {
        every { isNetworkConnected } returns true
    }

    @BeforeEach
    fun setup() {
        mockkStatic(SendData::toViewState)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(SendData::toViewState)
    }

    @Test
    fun `initial state should be Empty`() {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `AboutSendClick should emit NavigateToAboutSend`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SendAction.AboutSendClick)
            assertEquals(SendEvent.NavigateToAboutSend, awaitItem())
        }
    }

    @Test
    fun `AddSendClick should emit NavigateNewSend`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SendAction.AddSendClick)
            assertEquals(SendEvent.NavigateNewSend, awaitItem())
        }
    }

    @Test
    fun `LockClick should lock the vault`() {
        val viewModel = createViewModel()
        every { vaultRepo.lockVaultForCurrentUser(any()) } just runs

        viewModel.trySendAction(SendAction.LockClick)

        verify {
            vaultRepo.lockVaultForCurrentUser(isUserInitiated = true)
        }
    }

    @Test
    fun `RefreshClick should call sync`() {
        val viewModel = createViewModel()
        every { vaultRepo.sync(forced = true) } just runs

        viewModel.trySendAction(SendAction.RefreshClick)

        verify {
            vaultRepo.sync(forced = true)
        }
    }

    @Test
    fun `SearchClick should emit NavigateToSearch`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SendAction.SearchClick)
            assertEquals(SendEvent.NavigateToSearch, awaitItem())
        }
    }

    @Test
    fun `DeleteSendClick with deleteSend error should display error dialog`() = runTest {
        val sendId = "sendId1234"
        val sendItem = mockk<SendState.ViewState.Content.SendItem> {
            every { id } returns sendId
        }
        val error = Throwable("Oops")
        coEvery { vaultRepo.deleteSend(sendId) } returns DeleteSendResult.Error(error = error)

        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(SendAction.DeleteSendClick(sendItem))
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = SendState.DialogState.Loading(R.string.deleting.asText()),
                ),
                awaitItem(),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = SendState.DialogState.Error(
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.generic_error_message.asText(),
                        throwable = error,
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `DeleteSendClick with deleteSend success should emit ShowToast`() = runTest {
        val sendId = "sendId1234"
        val sendItem = mockk<SendState.ViewState.Content.SendItem> {
            every { id } returns sendId
        }
        coEvery { vaultRepo.deleteSend(sendId) } returns DeleteSendResult.Success

        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SendAction.DeleteSendClick(sendItem))
            assertEquals(SendEvent.ShowToast(R.string.send_deleted.asText()), awaitItem())
        }
    }

    @Test
    fun `RemovePasswordClick with removePasswordSend error should display error dialog`() =
        runTest {
            val sendId = "sendId1234"
            val sendItem = mockk<SendState.ViewState.Content.SendItem> {
                every { id } returns sendId
            }
            coEvery {
                vaultRepo.removePasswordSend(sendId)
            } returns RemovePasswordSendResult.Error(errorMessage = null, error = null)

            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(SendAction.RemovePasswordClick(sendItem))
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = SendState.DialogState.Loading(
                            message = R.string.removing_send_password.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialogState = SendState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `RemovePasswordClick with removePasswordSend success should emit ShowToast`() = runTest {
        val sendId = "sendId1234"
        val sendItem = mockk<SendState.ViewState.Content.SendItem> {
            every { id } returns sendId
        }
        coEvery {
            vaultRepo.removePasswordSend(sendId)
        } returns RemovePasswordSendResult.Success(mockk())

        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SendAction.RemovePasswordClick(sendItem))
            assertEquals(SendEvent.ShowToast(R.string.send_password_removed.asText()), awaitItem())
        }
    }

    @Test
    fun `SyncClick should call sync`() {
        val viewModel = createViewModel()
        every { vaultRepo.sync(forced = true) } just runs

        viewModel.trySendAction(SendAction.SyncClick)

        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = SendState.DialogState.Loading(R.string.syncing.asText()),
            ),
            viewModel.stateFlow.value,
        )
        verify {
            vaultRepo.sync(forced = true)
        }
    }

    @Test
    fun `on SyncClick should show the no network dialog if no connection is available`() {
        val viewModel = createViewModel()
        every {
            networkConnectionManager.isNetworkConnected
        } returns false
        viewModel.trySendAction(SendAction.SyncClick)
        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = SendState.DialogState.Error(
                    R.string.internet_connection_required_title.asText(),
                    R.string.internet_connection_required_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
        verify(exactly = 0) {
            vaultRepo.sync(forced = true)
        }
    }

    @Test
    fun `CopyClick should call setText on the ClipboardManager`() = runTest {
        val viewModel = createViewModel()
        val testUrl = "www.test.com/"
        val sendItem = mockk<SendState.ViewState.Content.SendItem> {
            every { shareUrl } returns testUrl
        }
        viewModel.trySendAction(SendAction.CopyClick(sendItem))
        verify(exactly = 1) {
            clipboardManager.setText(
                text = testUrl,
                toastDescriptorOverride = R.string.send_link.asText(),
            )
        }
    }

    @Test
    fun `SendClick should emit NavigateToEditSend`() = runTest {
        val sendId = "sendId1234"
        val sendItem = mockk<SendState.ViewState.Content.SendItem> {
            every { id } returns sendId
        }
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(SendAction.SendClick(sendItem))
            assertEquals(SendEvent.NavigateToEditSend(sendId), awaitItem())
        }
    }

    @Test
    fun `EditClick should emit NavigateToEditSend`() = runTest {
        val sendId = "sendId1234"
        val sendItem = mockk<SendState.ViewState.Content.SendItem> {
            every { id } returns sendId
        }
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(SendAction.EditClick(sendItem = sendItem))
            assertEquals(SendEvent.NavigateToEditSend(sendId = sendId), awaitItem())
        }
    }

    @Test
    fun `ShareClick should emit ShowShareSheet`() = runTest {
        val viewModel = createViewModel()
        val testUrl = "www.test.com"
        val sendItem = mockk<SendState.ViewState.Content.SendItem> {
            every { shareUrl } returns testUrl
        }
        viewModel.eventFlow.test {
            viewModel.trySendAction(SendAction.ShareClick(sendItem))
            assertEquals(SendEvent.ShowShareSheet(testUrl), awaitItem())
        }
    }

    @Test
    fun `FileTypeClick should emit NavigateToFileSends`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SendAction.FileTypeClick)
            assertEquals(SendEvent.NavigateToFileSends, awaitItem())
        }
    }

    @Test
    fun `TextTypeClick should emit NavigateToTextSends`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SendAction.TextTypeClick)
            assertEquals(SendEvent.NavigateToTextSends, awaitItem())
        }
    }

    @Test
    fun `DismissDialog should clear the dialogState`() = runTest {
        val initialState = DEFAULT_STATE.copy(
            dialogState = SendState.DialogState.Error(
                title = null,
                message = "Test".asText(),
            ),
        )
        val viewModel = createViewModel(initialState)

        viewModel.trySendAction(SendAction.DismissDialog)
        assertEquals(initialState.copy(dialogState = null), viewModel.stateFlow.value)
    }

    @Test
    fun `VaultRepository SendData Error should update view state to Error`() = runTest {
        val dialogState = SendState.DialogState.Loading(R.string.syncing.asText())
        val viewModel = createViewModel(state = DEFAULT_STATE.copy(dialogState = dialogState))

        viewModel.eventFlow.test {
            mutableSendDataFlow.value = DataState.Error(Throwable("Fail"))
        }

        assertEquals(
            DEFAULT_STATE.copy(
                viewState = SendState.ViewState.Error(
                    message = R.string.generic_error_message.asText(),
                ),
                dialogState = null,
                isRefreshing = false,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `VaultRepository SendData Loaded should update view state`() = runTest {
        val dialogState = SendState.DialogState.Loading(R.string.syncing.asText())
        val viewModel = createViewModel(state = DEFAULT_STATE.copy(dialogState = dialogState))
        val viewState = mockk<SendState.ViewState.Content>()
        val sendData = mockk<SendData> {
            every {
                toViewState(Environment.Us.environmentUrlData.baseWebSendUrl)
            } returns viewState
        }

        viewModel.eventFlow.test {
            mutableSendDataFlow.value = DataState.Loaded(sendData)
        }

        assertEquals(
            DEFAULT_STATE.copy(
                viewState = viewState,
                dialogState = null,
                isRefreshing = false,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `VaultRepository SendData Loading should update view state to Loading`() {
        val dialogState = SendState.DialogState.Loading(R.string.syncing.asText())
        val viewModel = createViewModel(state = DEFAULT_STATE.copy(dialogState = dialogState))

        mutableSendDataFlow.value = DataState.Loading

        assertEquals(
            DEFAULT_STATE.copy(viewState = SendState.ViewState.Loading, dialogState = dialogState),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `VaultRepository SendData NoNetwork should update view state to Empty when there is no data`() =
        runTest {
            val dialogState = SendState.DialogState.Loading(R.string.syncing.asText())
            val viewModel = createViewModel(state = DEFAULT_STATE.copy(dialogState = dialogState))

            viewModel.eventFlow.test {
                mutableSendDataFlow.value = DataState.NoNetwork()
            }

            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = SendState.ViewState.Empty,
                    dialogState = null,
                    isRefreshing = false,
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `VaultRepository SendData Pending should update view state`() {
        val dialogState = SendState.DialogState.Loading(R.string.syncing.asText())
        val viewModel = createViewModel(state = DEFAULT_STATE.copy(dialogState = dialogState))
        val viewState = mockk<SendState.ViewState.Content>()
        val sendData = mockk<SendData> {
            every {
                toViewState(Environment.Us.environmentUrlData.baseWebSendUrl)
            } returns viewState
        }

        mutableSendDataFlow.value = DataState.Pending(sendData)

        assertEquals(
            DEFAULT_STATE.copy(viewState = viewState, dialogState = dialogState),
            viewModel.stateFlow.value,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `RefreshPull should call vault repository sync`() = runTest {
        every { vaultRepo.sync(forced = false) } just runs
        val viewModel = createViewModel()

        viewModel.trySendAction(SendAction.RefreshPull)
        advanceTimeBy(300)
        verify(exactly = 1) {
            vaultRepo.sync(forced = false)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `RefreshPull should show network error if no internet connection`() = runTest {
        val viewModel = createViewModel()
        every {
            networkConnectionManager.isNetworkConnected
        } returns false

        viewModel.trySendAction(SendAction.RefreshPull)
        advanceTimeBy(300)
        assertEquals(
            DEFAULT_STATE.copy(
                isRefreshing = false,
                dialogState = SendState.DialogState.Error(
                    R.string.internet_connection_required_title.asText(),
                    R.string.internet_connection_required_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
        verify(exactly = 0) {
            vaultRepo.sync(forced = false)
        }
    }

    @Test
    fun `PullToRefreshEnableReceive should update isPullToRefreshEnabled`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(
            SendAction.Internal.PullToRefreshEnableReceive(isPullToRefreshEnabled = true),
        )

        assertEquals(
            DEFAULT_STATE.copy(isPullToRefreshSettingEnabled = true),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("LongParameterList")
    private fun createViewModel(
        state: SendState? = null,
        bitwardenClipboardManager: BitwardenClipboardManager = clipboardManager,
        environmentRepository: EnvironmentRepository = environmentRepo,
        settingsRepository: SettingsRepository = settingsRepo,
        vaultRepository: VaultRepository = vaultRepo,
        policyManager: PolicyManager = this.policyManager,
    ): SendViewModel = SendViewModel(
        savedStateHandle = SavedStateHandle().apply {
            set("state", state)
        },
        clipboardManager = bitwardenClipboardManager,
        environmentRepo = environmentRepository,
        settingsRepo = settingsRepository,
        vaultRepo = vaultRepository,
        policyManager = policyManager,
        networkConnectionManager = networkConnectionManager,
    )
}

private val DEFAULT_STATE: SendState = SendState(
    viewState = SendState.ViewState.Loading,
    dialogState = null,
    isPullToRefreshSettingEnabled = false,
    policyDisablesSend = false,
    isRefreshing = false,
)
