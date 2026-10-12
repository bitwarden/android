package com.x8bit.bitwarden.ui.tools.feature.send.viewsend

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.network.model.SendTypeJson
import com.bitwarden.send.SendView
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.model.EffectiveSendPolicy
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DeleteSendResult
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType
import com.x8bit.bitwarden.ui.tools.feature.send.viewsend.model.SendPolicyRestriction
import com.x8bit.bitwarden.ui.tools.feature.send.viewsend.util.toViewSendViewStateContent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ViewSendViewModelTest : BaseViewModelTest() {

    private val clipboardManager = mockk<BitwardenClipboardManager> {
        every { setText(text = any<String>()) } just runs
    }
    private val mutableSendStateFlow = MutableStateFlow<DataState<SendView?>>(DataState.Loading)
    private val vaultRepository = mockk<VaultRepository> {
        every { getSendStateFlow(sendId = any()) } returns mutableSendStateFlow
    }
    private val environmentRepository = mockk<EnvironmentRepository> {
        every { environment } returns Environment.Prod.Us
    }
    private val mutableEffectiveSendPolicyFlow = MutableStateFlow(DEFAULT_EFFECTIVE_SEND_POLICY)
    private val policyManager = mockk<PolicyManager> {
        every { getEffectiveSendPolicyFlow() } returns mutableEffectiveSendPolicyFlow
    }
    private val mutableSendControlsFlagFlow = MutableStateFlow(false)
    private val mutableExistingSendsFlagFlow = MutableStateFlow(false)
    private val featureFlagManager = mockk<FeatureFlagManager> {
        every {
            getFeatureFlagFlow(key = FlagKey.SendControls)
        } returns mutableSendControlsFlagFlow
        every {
            getFeatureFlagFlow(key = FlagKey.SendControlsExistingSends)
        } returns mutableExistingSendsFlagFlow
    }
    private val mutableSnackbarDataFlow: MutableSharedFlow<BitwardenSnackbarData> =
        bufferedMutableSharedFlow()
    private val snackbarRelayManager: SnackbarRelayManager<SnackbarRelay> = mockk {
        every { sendSnackbarData(data = any(), relay = any()) } just runs
        every {
            getSnackbarDataFlow(relay = any(), relays = anyVararg())
        } returns mutableSnackbarDataFlow
    }

    @BeforeEach
    fun setup() {
        mockkStatic(
            SavedStateHandle::toViewSendArgs,
            SendView::toViewSendViewStateContent,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            SavedStateHandle::toViewSendArgs,
            SendView::toViewSendViewStateContent,
        )
    }

    @Test
    fun `initial state should be correct`() {
        val viewModel = createViewModel()
        assertEquals(
            DEFAULT_STATE.copy(viewState = ViewSendState.ViewState.Loading),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `policy restriction should require a copy for a disabled text send`() {
        val state = restrictedState()

        assertEquals(SendPolicyRestriction.CopyRequired, state.policyRestriction)
        assertFalse(state.isFabVisible)
    }

    @Test
    fun `policy restriction should report a file send as not compliant`() {
        val state = restrictedState(sendType = SendItemType.FILE)

        assertEquals(SendPolicyRestriction.FileNotCompliant, state.policyRestriction)
        assertFalse(state.isFabVisible)
    }

    @Test
    fun `policy restriction should report a text send whose type is not allowed`() {
        val state = restrictedState(allowedSendTypes = listOf(SendTypeJson.FILE))

        assertEquals(SendPolicyRestriction.TypeNotAllowed, state.policyRestriction)
        assertFalse(state.isFabVisible)
    }

    @Test
    fun `policy restriction should report a file send as not compliant before type`() {
        val state = restrictedState(
            sendType = SendItemType.FILE,
            allowedSendTypes = listOf(SendTypeJson.TEXT),
        )

        // A file send cannot be copied either way, so the type restriction is not reported.
        assertEquals(SendPolicyRestriction.FileNotCompliant, state.policyRestriction)
    }

    @Test
    fun `policy restriction should require a copy when the send type is allowed`() {
        val state = restrictedState(allowedSendTypes = listOf(SendTypeJson.TEXT))

        assertEquals(SendPolicyRestriction.CopyRequired, state.policyRestriction)
    }

    @Test
    fun `policy restriction should be absent when the send is not disabled`() {
        val state = restrictedState(isSendDisabled = false)

        assertNull(state.policyRestriction)
        assertTrue(state.isFabVisible)
    }

    @Test
    fun `policy restriction should be absent when send controls is disabled`() {
        val state = restrictedState(isSendControlsEnabled = false)

        assertNull(state.policyRestriction)
        assertTrue(state.isFabVisible)
    }

    @Test
    fun `policy restriction should be absent when existing sends enforcement is disabled`() {
        val state = restrictedState(isSendControlsExistingSendsEnabled = false)

        assertNull(state.policyRestriction)
        assertTrue(state.isFabVisible)
    }

    @Test
    fun `state should update when the enforced send types change`() = runTest {
        mutableSendStateFlow.value = DataState.Loaded(createDisabledSendView())
        mutableSendControlsFlagFlow.value = true
        mutableExistingSendsFlagFlow.value = true
        val viewModel = createViewModel()
        val enforcedState = DEFAULT_STATE.copy(
            isSendDisabled = true,
            isSendControlsEnabled = true,
            isSendControlsExistingSendsEnabled = true,
            viewState = DEFAULT_CONTENT_VIEW_STATE,
        )

        viewModel.stateFlow.test {
            val initialState = awaitItem()
            assertEquals(enforcedState, initialState)
            assertEquals(SendPolicyRestriction.CopyRequired, initialState.policyRestriction)

            mutableEffectiveSendPolicyFlow.value = DEFAULT_EFFECTIVE_SEND_POLICY
                .copy(allowedSendTypes = listOf(SendTypeJson.FILE))

            val state = awaitItem()
            assertEquals(enforcedState.copy(allowedSendTypes = listOf(SendTypeJson.FILE)), state)
            assertEquals(SendPolicyRestriction.TypeNotAllowed, state.policyRestriction)
        }
    }

    @Test
    fun `state should update when the existing sends flag is turned off`() = runTest {
        mutableSendStateFlow.value = DataState.Loaded(createDisabledSendView())
        mutableSendControlsFlagFlow.value = true
        mutableExistingSendsFlagFlow.value = true
        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            assertEquals(SendPolicyRestriction.CopyRequired, awaitItem().policyRestriction)

            mutableExistingSendsFlagFlow.value = false

            val state = awaitItem()
            assertEquals(
                DEFAULT_STATE.copy(
                    isSendDisabled = true,
                    isSendControlsEnabled = true,
                    viewState = DEFAULT_CONTENT_VIEW_STATE,
                ),
                state,
            )
            assertNull(state.policyRestriction)
        }
    }

    @Test
    fun `fab should remain hidden while the view state is loading`() {
        val state = restrictedState(isSendDisabled = false)
            .copy(viewState = ViewSendState.ViewState.Loading)

        assertFalse(state.isFabVisible)
    }

    @Test
    fun `on MakeACopyClick should send NavigateToCopy`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ViewSendAction.MakeACopyClick)
            assertEquals(
                ViewSendEvent.NavigateToCopy(sendType = SendItemType.TEXT, sendId = "send_id"),
                awaitItem(),
            )
        }
    }

    @Test
    fun `on CloseClick should send NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ViewSendAction.CloseClick)
            assertEquals(ViewSendEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on CopyClick should call setText on ClipboardManger`() {
        val viewModel = createViewModel()
        val sendView = createMockSendView(number = 1)
        every {
            sendView.toViewSendViewStateContent(baseWebSendUrl = any(), clock = FIXED_CLOCK)
        } returns DEFAULT_CONTENT_VIEW_STATE
        mutableSendStateFlow.value = DataState.Loaded(data = sendView)
        viewModel.trySendAction(ViewSendAction.CopyClick)
        verify(exactly = 1) {
            clipboardManager.setText(text = "share_link")
        }
    }

    @Test
    fun `on CopyNotesClick should call setText on ClipboardManger`() {
        val viewModel = createViewModel()
        val sendView = createMockSendView(number = 1)
        every {
            sendView.toViewSendViewStateContent(baseWebSendUrl = any(), clock = FIXED_CLOCK)
        } returns DEFAULT_CONTENT_VIEW_STATE
        mutableSendStateFlow.value = DataState.Loaded(data = sendView)
        viewModel.trySendAction(ViewSendAction.CopyNotesClick)
        verify(exactly = 1) {
            clipboardManager.setText(text = "notes")
        }
    }

    @Test
    fun `on DeleteClick with failure should display error dialog`() = runTest {
        val initialState = DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT_VIEW_STATE)
        val sendView = createMockSendView(number = 1)
        every {
            sendView.toViewSendViewStateContent(baseWebSendUrl = any(), clock = FIXED_CLOCK)
        } returns DEFAULT_CONTENT_VIEW_STATE
        val throwable = Throwable("Fail!")
        coEvery {
            vaultRepository.deleteSend(sendId = "send_id")
        } returns DeleteSendResult.Error(error = throwable)
        mutableSendStateFlow.value = DataState.Loaded(data = sendView)

        val viewModel = createViewModel(state = initialState)

        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            viewModel.trySendAction(ViewSendAction.DeleteClick)
            assertEquals(
                initialState.copy(
                    dialogState = ViewSendState.DialogState.Loading(
                        message = BitwardenString.deleting.asText(),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                initialState.copy(
                    dialogState = ViewSendState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.generic_error_message.asText(),
                        throwable = throwable,
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `on DeleteClick with error message should display error message`() = runTest {
        val initialState = DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT_VIEW_STATE)
        val sendView = createMockSendView(number = 1)
        every {
            sendView.toViewSendViewStateContent(baseWebSendUrl = any(), clock = FIXED_CLOCK)
        } returns DEFAULT_CONTENT_VIEW_STATE
        val errorMessage = "User-friendly error"
        val throwable = Throwable("Fail!")
        coEvery {
            vaultRepository.deleteSend(sendId = "send_id")
        } returns DeleteSendResult.Error(errorMessage = errorMessage, error = throwable)
        mutableSendStateFlow.value = DataState.Loaded(data = sendView)

        val viewModel = createViewModel(state = initialState)

        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            viewModel.trySendAction(ViewSendAction.DeleteClick)
            assertEquals(
                initialState.copy(
                    dialogState = ViewSendState.DialogState.Loading(
                        message = BitwardenString.deleting.asText(),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                initialState.copy(
                    dialogState = ViewSendState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = errorMessage.asText(),
                        throwable = throwable,
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `on DeleteClick with success should navigate back`() = runTest {
        val initialState = DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT_VIEW_STATE)
        val sendView = createMockSendView(number = 1)
        every {
            sendView.toViewSendViewStateContent(baseWebSendUrl = any(), clock = FIXED_CLOCK)
        } returns DEFAULT_CONTENT_VIEW_STATE
        coEvery { vaultRepository.deleteSend(sendId = "send_id") } returns DeleteSendResult.Success
        mutableSendStateFlow.value = DataState.Loaded(data = sendView)

        val viewModel = createViewModel(state = initialState)

        viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFLow ->
            assertEquals(initialState, stateFlow.awaitItem())
            viewModel.trySendAction(ViewSendAction.DeleteClick)
            assertEquals(
                initialState.copy(
                    dialogState = ViewSendState.DialogState.Loading(
                        message = BitwardenString.deleting.asText(),
                    ),
                ),
                stateFlow.awaitItem(),
            )
            assertEquals(
                initialState.copy(dialogState = null),
                stateFlow.awaitItem(),
            )
            assertEquals(
                ViewSendEvent.NavigateBack,
                eventFLow.awaitItem(),
            )
        }
        verify(exactly = 1) {
            snackbarRelayManager.sendSnackbarData(
                data = BitwardenSnackbarData(message = BitwardenString.send_deleted.asText()),
                relay = SnackbarRelay.SEND_DELETED,
            )
        }
    }

    @Test
    fun `on DialogDismiss should send clear the dialogState`() = runTest {
        val initialState = DEFAULT_STATE.copy(
            dialogState = ViewSendState.DialogState.Loading(message = "Loading".asText()),
        )
        val viewModel = createViewModel(state = initialState)

        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            viewModel.trySendAction(ViewSendAction.DialogDismiss)
            assertEquals(initialState.copy(dialogState = null), awaitItem())
        }
    }

    @Test
    fun `on EditClick should send NavigateToEdit`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ViewSendAction.EditClick)
            assertEquals(
                ViewSendEvent.NavigateToEdit(
                    sendType = DEFAULT_STATE.sendType,
                    sendId = DEFAULT_STATE.sendId,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `on ShareClick should send ShareText`() = runTest {
        val viewModel = createViewModel()
        val sendView = createMockSendView(number = 1)
        every {
            sendView.toViewSendViewStateContent(baseWebSendUrl = any(), clock = FIXED_CLOCK)
        } returns DEFAULT_CONTENT_VIEW_STATE
        mutableSendStateFlow.value = DataState.Loaded(data = sendView)
        viewModel.eventFlow.test {
            viewModel.trySendAction(ViewSendAction.ShareClick)
            assertEquals(
                ViewSendEvent.ShareText(text = "share_link".asText()),
                awaitItem(),
            )
        }
    }

    @Test
    fun `on SendDataReceive with loading should set viewState to loading`() = runTest {
        val viewModel = createViewModel()
        val sendView = createMockSendView(number = 1)
        every {
            sendView.toViewSendViewStateContent(baseWebSendUrl = any(), clock = FIXED_CLOCK)
        } returns DEFAULT_CONTENT_VIEW_STATE
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            mutableSendStateFlow.value = DataState.Loading
            expectNoEvents()
        }
    }

    @Test
    fun `on SendDataReceive with error and no data should set viewState to error`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            mutableSendStateFlow.value = DataState.Error(error = Throwable("Fail!"))
            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = ViewSendState.ViewState.Error(
                        message = BitwardenString.generic_error_message.asText(),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `on SendDataReceive with error and data should set viewState to content`() = runTest {
        val viewModel = createViewModel()
        val sendView = createMockSendView(number = 1)
        every {
            sendView.toViewSendViewStateContent(baseWebSendUrl = any(), clock = FIXED_CLOCK)
        } returns DEFAULT_CONTENT_VIEW_STATE
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            mutableSendStateFlow.value = DataState.Error(
                error = Throwable("Fail!"),
                data = sendView,
            )
            assertEquals(
                DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT_VIEW_STATE),
                awaitItem(),
            )
        }
    }

    @Test
    fun `on SendDataReceive with no network and no data should set viewState to error`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            mutableSendStateFlow.value = DataState.NoNetwork()
            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = ViewSendState.ViewState.Error(
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
    fun `on SendDataReceive with no network and data should set viewState to content`() = runTest {
        val viewModel = createViewModel()
        val sendView = createMockSendView(number = 1)
        every {
            sendView.toViewSendViewStateContent(baseWebSendUrl = any(), clock = FIXED_CLOCK)
        } returns DEFAULT_CONTENT_VIEW_STATE
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            mutableSendStateFlow.value = DataState.NoNetwork(data = sendView)
            assertEquals(
                DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT_VIEW_STATE),
                awaitItem(),
            )
        }
    }

    @Test
    fun `on SendDataReceive with pending and no data should set viewState to error`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            mutableSendStateFlow.value = DataState.Pending(data = null)
            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = ViewSendState.ViewState.Error(
                        message = BitwardenString.generic_error_message.asText(),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `on SendDataReceive with pending and data should set viewState to content`() = runTest {
        val viewModel = createViewModel()
        val sendView = createMockSendView(number = 1)
        every {
            sendView.toViewSendViewStateContent(baseWebSendUrl = any(), clock = FIXED_CLOCK)
        } returns DEFAULT_CONTENT_VIEW_STATE
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            mutableSendStateFlow.value = DataState.Pending(data = sendView)
            assertEquals(
                DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT_VIEW_STATE),
                awaitItem(),
            )
        }
    }

    @Test
    fun `on SendDataReceive with loaded and no data should set viewState to error`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            mutableSendStateFlow.value = DataState.Loaded(data = null)
            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = ViewSendState.ViewState.Error(
                        message = BitwardenString.missing_send_resync_your_vault.asText(),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `on SendDataReceive with loaded and data should set viewState to content`() = runTest {
        val viewModel = createViewModel()
        val sendView = createMockSendView(number = 1)
        every {
            sendView.toViewSendViewStateContent(baseWebSendUrl = any(), clock = FIXED_CLOCK)
        } returns DEFAULT_CONTENT_VIEW_STATE
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            mutableSendStateFlow.value = DataState.Loaded(data = sendView)
            assertEquals(
                DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT_VIEW_STATE),
                awaitItem(),
            )
        }
    }

    @Test
    fun `SnackbarDataReceive should update emit ShowSnackbar`() = runTest {
        val viewModel = createViewModel()
        val snackbarData = BitwardenSnackbarData(message = "Test".asText())
        viewModel.eventFlow.test {
            mutableSnackbarDataFlow.tryEmit(snackbarData)
            assertEquals(ViewSendEvent.ShowSnackbar(data = snackbarData), awaitItem())
        }
    }

    private fun createViewModel(
        state: ViewSendState? = null,
    ): ViewSendViewModel = ViewSendViewModel(
        clipboardManager = clipboardManager,
        clock = FIXED_CLOCK,
        vaultRepository = vaultRepository,
        environmentRepository = environmentRepository,
        featureFlagManager = featureFlagManager,
        policyManager = policyManager,
        snackbarRelayManager = snackbarRelayManager,
        savedStateHandle = SavedStateHandle().apply {
            set(key = "state", value = state)
            every { toViewSendArgs() } returns ViewSendArgs(
                sendId = (state ?: DEFAULT_STATE).sendId,
                sendType = (state ?: DEFAULT_STATE).sendType,
            )
        },
    )
}

private val DEFAULT_CONTENT_VIEW_STATE = ViewSendState.ViewState.Content(
    sendType = ViewSendState.ViewState.Content.SendType.TextType(
        textToShare = "text_to_share",
    ),
    shareLink = "share_link",
    sendName = "send_name",
    deletionDate = "deletion_date",
    maxAccessCount = 1,
    currentAccessCount = 1,
    notes = "notes",
)

private val DEFAULT_STATE = ViewSendState(
    sendType = SendItemType.TEXT,
    sendId = "send_id",
    viewState = ViewSendState.ViewState.Loading,
    dialogState = null,
    baseWebSendUrl = "https://send.bitwarden.com/#",
    allowedSendTypes = null,
    isSendDisabled = false,
    isSendControlsEnabled = false,
    isSendControlsExistingSendsEnabled = false,
)

/**
 * Builds a state that is policy-restricted by default, so each test only names the field it varies.
 */
private fun restrictedState(
    sendType: SendItemType = SendItemType.TEXT,
    allowedSendTypes: List<SendTypeJson>? = null,
    isSendDisabled: Boolean = true,
    isSendControlsEnabled: Boolean = true,
    isSendControlsExistingSendsEnabled: Boolean = true,
): ViewSendState = DEFAULT_STATE.copy(
    sendType = sendType,
    allowedSendTypes = allowedSendTypes,
    isSendDisabled = isSendDisabled,
    isSendControlsEnabled = isSendControlsEnabled,
    isSendControlsExistingSendsEnabled = isSendControlsExistingSendsEnabled,
    viewState = DEFAULT_CONTENT_VIEW_STATE,
)

/**
 * Builds a [SendView] the server has marked disabled, mapped to [DEFAULT_CONTENT_VIEW_STATE].
 */
private fun createDisabledSendView(): SendView = mockk<SendView> {
    every { disabled } returns true
    every {
        toViewSendViewStateContent(baseWebSendUrl = any(), clock = any())
    } returns DEFAULT_CONTENT_VIEW_STATE
}

private val DEFAULT_EFFECTIVE_SEND_POLICY = EffectiveSendPolicy(
    allowedDomains = null,
    allowedSendTypes = null,
    deletionHours = null,
    disableHideEmail = false,
    disableSend = false,
    whoCanAccess = null,
)

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)
