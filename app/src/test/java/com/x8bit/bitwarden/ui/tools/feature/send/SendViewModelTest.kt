package com.x8bit.bitwarden.ui.tools.feature.send

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.baseWebSendUrl
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.tools.feature.send.util.toViewState
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

class SendViewModelTest : BaseViewModelTest() {

    private val mutableSendDataFlow = MutableStateFlow<DataState<SendData>>(DataState.Loading)

    private val clipboardManager: BitwardenClipboardManager = mockk()
    private val environmentRepo: EnvironmentRepository = mockk {
        every { environment } returns Environment.Us
    }
    private val vaultRepo: VaultRepository = mockk {
        every { sendDataStateFlow } returns mutableSendDataFlow
    }

    @BeforeEach
    fun setup() {
        mockkStatic(SEND_DATA_EXTENSIONS_PATH)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(SEND_DATA_EXTENSIONS_PATH)
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
        every { vaultRepo.lockVaultForCurrentUser() } just runs

        viewModel.trySendAction(SendAction.LockClick)

        verify {
            vaultRepo.lockVaultForCurrentUser()
        }
    }

    @Test
    fun `RefreshClick should call sync`() {
        val viewModel = createViewModel()
        every { vaultRepo.sync() } just runs

        viewModel.trySendAction(SendAction.RefreshClick)

        verify {
            vaultRepo.sync()
        }
    }

    @Test
    fun `SearchClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SendAction.SearchClick)
            assertEquals(SendEvent.ShowToast("Search Not Implemented".asText()), awaitItem())
        }
    }

    @Test
    fun `SyncClick should call sync`() {
        val viewModel = createViewModel()
        every { vaultRepo.sync() } just runs

        viewModel.trySendAction(SendAction.SyncClick)

        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = SendState.DialogState.Loading(R.string.syncing.asText()),
            ),
            viewModel.stateFlow.value,
        )
        verify {
            vaultRepo.sync()
        }
    }

    @Test
    fun `CopyClick should call setText on the ClipboardManager`() {
        val viewModel = createViewModel()
        val testUrl = "www.test.com/"
        val sendItem = mockk<SendState.ViewState.Content.SendItem> {
            every { shareUrl } returns testUrl
        }
        every { clipboardManager.setText(testUrl) } just runs

        viewModel.trySendAction(SendAction.CopyClick(sendItem))

        verify(exactly = 1) {
            clipboardManager.setText(testUrl)
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
    fun `FileTypeClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SendAction.FileTypeClick)
            assertEquals(SendEvent.ShowToast("Not yet implemented".asText()), awaitItem())
        }
    }

    @Test
    fun `TextTypeClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SendAction.TextTypeClick)
            assertEquals(SendEvent.ShowToast("Not yet implemented".asText()), awaitItem())
        }
    }

    @Test
    fun `VaultRepository SendData Error should update view state to Error`() {
        val dialogState = SendState.DialogState.Loading(R.string.syncing.asText())
        val viewModel = createViewModel(state = DEFAULT_STATE.copy(dialogState = dialogState))

        mutableSendDataFlow.value = DataState.Error(Throwable("Fail"))

        assertEquals(
            SendState(
                viewState = SendState.ViewState.Error(
                    message = R.string.generic_error_message.asText(),
                ),
                dialogState = null,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `VaultRepository SendData Loaded should update view state`() {
        val dialogState = SendState.DialogState.Loading(R.string.syncing.asText())
        val viewModel = createViewModel(state = DEFAULT_STATE.copy(dialogState = dialogState))
        val viewState = mockk<SendState.ViewState.Content>()
        val sendData = mockk<SendData> {
            every {
                toViewState(Environment.Us.environmentUrlData.baseWebSendUrl)
            } returns viewState
        }

        mutableSendDataFlow.value = DataState.Loaded(sendData)

        assertEquals(
            SendState(viewState = viewState, dialogState = null),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `VaultRepository SendData Loading should update view state to Loading`() {
        val dialogState = SendState.DialogState.Loading(R.string.syncing.asText())
        val viewModel = createViewModel(state = DEFAULT_STATE.copy(dialogState = dialogState))

        mutableSendDataFlow.value = DataState.Loading

        assertEquals(
            SendState(viewState = SendState.ViewState.Loading, dialogState = dialogState),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `VaultRepository SendData NoNetwork should update view state to Error`() {
        val dialogState = SendState.DialogState.Loading(R.string.syncing.asText())
        val viewModel = createViewModel(state = DEFAULT_STATE.copy(dialogState = dialogState))

        mutableSendDataFlow.value = DataState.NoNetwork()

        assertEquals(
            SendState(
                viewState = SendState.ViewState.Error(
                    message = R.string.internet_connection_required_title
                        .asText()
                        .concat(R.string.internet_connection_required_message.asText()),
                ),
                dialogState = null,
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
            SendState(viewState = viewState, dialogState = dialogState),
            viewModel.stateFlow.value,
        )
    }

    private fun createViewModel(
        state: SendState? = null,
        bitwardenClipboardManager: BitwardenClipboardManager = clipboardManager,
        environmentRepository: EnvironmentRepository = environmentRepo,
        vaultRepository: VaultRepository = vaultRepo,
    ): SendViewModel = SendViewModel(
        savedStateHandle = SavedStateHandle().apply {
            set("state", state)
        },
        clipboardManager = bitwardenClipboardManager,
        environmentRepo = environmentRepository,
        vaultRepo = vaultRepository,
    )
}

private const val SEND_DATA_EXTENSIONS_PATH: String =
    "com.x8bit.bitwarden.ui.tools.feature.send.util.SendDataExtensionsKt"

private val DEFAULT_STATE: SendState = SendState(
    viewState = SendState.ViewState.Loading,
    dialogState = null,
)
