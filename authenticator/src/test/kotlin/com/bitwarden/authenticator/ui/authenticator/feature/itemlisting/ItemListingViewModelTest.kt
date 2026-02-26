package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting

import app.cash.turbine.test
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.authenticator.repository.model.AuthenticatorItem
import com.bitwarden.authenticator.data.authenticator.repository.model.SharedVerificationCodesState
import com.bitwarden.authenticator.data.platform.manager.BitwardenEncodingManager
import com.bitwarden.authenticator.data.platform.manager.clipboard.BitwardenClipboardManager
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.authenticator.ui.authenticator.feature.util.toDisplayItem
import com.bitwarden.authenticator.ui.authenticator.feature.util.toSharedCodesDisplayState
import com.bitwarden.authenticator.ui.platform.components.listitem.model.SharedCodesDisplayState
import com.bitwarden.authenticator.ui.platform.components.listitem.model.VaultDropdownMenuAction
import com.bitwarden.authenticator.ui.platform.components.listitem.model.VerificationCodeDisplayItem
import com.bitwarden.authenticator.ui.platform.model.SnackbarRelay
import com.bitwarden.authenticatorbridge.manager.AuthenticatorBridgeManager
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ItemListingViewModelTest : BaseViewModelTest() {

    private val mutableAuthenticatorAlertThresholdFlow =
        MutableStateFlow(AUTHENTICATOR_ALERT_SECONDS)
    private val mutableVerificationCodesFlow =
        MutableStateFlow<DataState<List<VerificationCodeItem>>>(DataState.Loading)
    private val mutableSharedCodesFlow =
        MutableStateFlow<SharedVerificationCodesState>(SharedVerificationCodesState.Loading)
    private val firstTimeAccountSyncChannel: Channel<Unit> = Channel(capacity = Channel.UNLIMITED)

    private val authenticatorRepository: AuthenticatorRepository = mockk {
        every { totpCodeFlow } returns emptyFlow()
        every { getLocalVerificationCodesFlow() } returns mutableVerificationCodesFlow
        every { sharedCodesStateFlow } returns mutableSharedCodesFlow
        every { firstTimeAccountSyncFlow } returns firstTimeAccountSyncChannel.receiveAsFlow()
    }
    private val authenticatorBridgeManager: AuthenticatorBridgeManager = mockk()
    private val clipboardManager: BitwardenClipboardManager = mockk()
    private val encodingManager: BitwardenEncodingManager = mockk()
    private val settingsRepository: SettingsRepository = mockk {
        every {
            authenticatorAlertThresholdSeconds
        } returns mutableAuthenticatorAlertThresholdFlow.value
        every {
            authenticatorAlertThresholdSecondsFlow
        } returns mutableAuthenticatorAlertThresholdFlow
        every { hasUserDismissedDownloadBitwardenCard } returns false
    }
    private val mutableSnackbarFlow = bufferedMutableSharedFlow<BitwardenSnackbarData>()
    private val snackbarRelayManager = mockk<SnackbarRelayManager<SnackbarRelay>> {
        every {
            getSnackbarDataFlow(relay = any(), relays = anyVararg())
        } returns mutableSnackbarFlow
    }

    @Test
    fun `initial state should be correct`() {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `when SnackbarRelay flow updates, snackbar is shown`() = runTest {
        val viewModel = createViewModel()
        val expectedSnackbarData = BitwardenSnackbarData(message = "test message".asText())
        viewModel.eventFlow.test {
            mutableSnackbarFlow.tryEmit(expectedSnackbarData)
            assertEquals(ItemListingEvent.ShowSnackbar(expectedSnackbarData), awaitItem())
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `stateFlow value should show download bitwarden action card when local items are empty and shared state is AppNotInstalled`() {
        val expectedState = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.NoItems(
                actionCard = ItemListingState.ActionCardState.DownloadBitwardenApp,
            ),
        )
        mutableSharedCodesFlow.value = SharedVerificationCodesState.AppNotInstalled
        mutableVerificationCodesFlow.value = DataState.Loaded(emptyList())
        val viewModel = createViewModel()
        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `stateFlow value should not show download bitwarden card when local items are empty and shared state is AppNotInstalled but user has dismissed card`() {
        val expectedState = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.NoItems(
                actionCard = null,
            ),
        )
        every { settingsRepository.hasUserDismissedDownloadBitwardenCard } returns true
        mutableSharedCodesFlow.value = SharedVerificationCodesState.AppNotInstalled
        mutableVerificationCodesFlow.value = DataState.Loaded(emptyList())
        val viewModel = createViewModel()
        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `stateFlow value should show download bitwarden card when there are local items and shared state is AppNotInstalled`() {
        val expectedState = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.Content(
                actionCard = ItemListingState.ActionCardState.DownloadBitwardenApp,
                favoriteItems = LOCAL_FAVORITE_ITEMS,
                itemList = LOCAL_NON_FAVORITE_ITEMS,
                sharedItems = SharedCodesDisplayState.Codes(persistentListOf()),
            ),
        )
        every { settingsRepository.hasUserDismissedDownloadBitwardenCard } returns false
        mutableVerificationCodesFlow.value = DataState.Loaded(LOCAL_VERIFICATION_ITEMS)
        mutableSharedCodesFlow.value = SharedVerificationCodesState.AppNotInstalled
        val viewModel = createViewModel()
        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `stateFlow value should not show download bitwarden card when there are local items and shared state is AppNotInstalled but user has dismissed card`() {
        val expectedState = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.Content(
                actionCard = null,
                favoriteItems = LOCAL_FAVORITE_ITEMS,
                itemList = LOCAL_NON_FAVORITE_ITEMS,
                sharedItems = SharedCodesDisplayState.Codes(persistentListOf()),
            ),
        )
        every { settingsRepository.hasUserDismissedDownloadBitwardenCard } returns true
        mutableVerificationCodesFlow.value = DataState.Loaded(LOCAL_VERIFICATION_ITEMS)
        mutableSharedCodesFlow.value = SharedVerificationCodesState.AppNotInstalled
        val viewModel = createViewModel()
        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `stateFlow sharedItems value should be Error when shared state is Error `() {
        val expectedState = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.Content(
                actionCard = null,
                favoriteItems = LOCAL_FAVORITE_ITEMS,
                itemList = LOCAL_NON_FAVORITE_ITEMS,
                sharedItems = SharedCodesDisplayState.Error,
            ),
        )
        mutableVerificationCodesFlow.value = DataState.Loaded(LOCAL_VERIFICATION_ITEMS)
        mutableSharedCodesFlow.value = SharedVerificationCodesState.Error
        val viewModel = createViewModel()
        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `stateFlow sharedItems value should be Codes with empty list when shared state is Success `() {
        val expectedState = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.Content(
                actionCard = null,
                favoriteItems = LOCAL_FAVORITE_ITEMS
                    .map { it.copy(showMoveToBitwarden = true) }
                    .toImmutableList(),
                itemList = LOCAL_NON_FAVORITE_ITEMS
                    .map { it.copy(showMoveToBitwarden = true) }
                    .toImmutableList(),
                sharedItems = SHARED_DISPLAY_ITEMS,
            ),
        )
        mutableVerificationCodesFlow.value = DataState.Loaded(LOCAL_VERIFICATION_ITEMS)
        mutableSharedCodesFlow.value =
            SharedVerificationCodesState.Success(SHARED_VERIFICATION_ITEMS)
        val viewModel = createViewModel()
        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `stateFlow sharedItems value should show items even when local items are empty`() {
        val expectedState = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.NoItems(
                actionCard = null,
            ),
        )
        mutableVerificationCodesFlow.value = DataState.Loaded(emptyList())
        mutableSharedCodesFlow.value =
            SharedVerificationCodesState.Success(emptyList())
        val viewModel = createViewModel()
        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `stateFlow viewState value should be NoItems when both local and shared codes are empty`() {
        val expectedState = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.Content(
                actionCard = null,
                favoriteItems = persistentListOf(),
                itemList = persistentListOf(),
                sharedItems = SHARED_DISPLAY_ITEMS,
            ),
        )
        mutableVerificationCodesFlow.value = DataState.Loaded(emptyList())
        mutableSharedCodesFlow.value =
            SharedVerificationCodesState.Success(SHARED_VERIFICATION_ITEMS)
        val viewModel = createViewModel()
        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    fun `on DownloadBitwardenClick receive should emit NavigateToBitwardenListing`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ItemListingAction.DownloadBitwardenClick)
            assertEquals(ItemListingEvent.NavigateToBitwardenListing, awaitItem())
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `on DownloadBitwardenDismiss receive should dismiss action card and store dismissal in settings`() =
        runTest {
            val expectedState = DEFAULT_STATE.copy(
                viewState = ItemListingState.ViewState.Content(
                    actionCard = null,
                    favoriteItems = LOCAL_FAVORITE_ITEMS,
                    itemList = LOCAL_NON_FAVORITE_ITEMS,
                    sharedItems = SharedCodesDisplayState.Codes(persistentListOf()),
                ),
            )
            every { settingsRepository.hasUserDismissedDownloadBitwardenCard = true } just runs
            every { settingsRepository.hasUserDismissedDownloadBitwardenCard } returns false
            mutableVerificationCodesFlow.value = DataState.Loaded(LOCAL_VERIFICATION_ITEMS)
            val viewModel = createViewModel()
            viewModel.trySendAction(ItemListingAction.DownloadBitwardenDismiss)
            verify { settingsRepository.hasUserDismissedDownloadBitwardenCard = true }
            assertEquals(expectedState, viewModel.stateFlow.value)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `on DownloadBitwardenDismiss receive in empty state should dismiss action card and store dismissal in settings`() =
        runTest {
            val expectedState = DEFAULT_STATE.copy(
                viewState = ItemListingState.ViewState.NoItems(
                    actionCard = null,
                ),
            )
            every { settingsRepository.hasUserDismissedDownloadBitwardenCard = true } just runs
            every { settingsRepository.hasUserDismissedDownloadBitwardenCard } returns false
            mutableVerificationCodesFlow.value = DataState.Loaded(emptyList())
            val viewModel = createViewModel()
            viewModel.trySendAction(ItemListingAction.DownloadBitwardenDismiss)
            verify { settingsRepository.hasUserDismissedDownloadBitwardenCard = true }
            assertEquals(expectedState, viewModel.stateFlow.value)
        }

    @Test
    fun `on SyncWithBitwardenClick receive should emit NavigateToBitwardenSettings`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ItemListingAction.SyncWithBitwardenClick)
            assertEquals(ItemListingEvent.NavigateToBitwardenSettings, awaitItem())
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `on SyncWithBitwardenDismiss receive should dismiss action card and store dismissal in settings`() =
        runTest {
            val expectedState = DEFAULT_STATE.copy(
                viewState = ItemListingState.ViewState.Content(
                    actionCard = null,
                    favoriteItems = LOCAL_FAVORITE_ITEMS,
                    itemList = LOCAL_NON_FAVORITE_ITEMS,
                    sharedItems = SharedCodesDisplayState.Codes(persistentListOf()),
                ),
            )
            mutableSharedCodesFlow.value = SharedVerificationCodesState.SyncNotEnabled
            every { settingsRepository.hasUserDismissedSyncWithBitwardenCard = true } just runs
            every { settingsRepository.hasUserDismissedSyncWithBitwardenCard } returns false
            mutableVerificationCodesFlow.value = DataState.Loaded(LOCAL_VERIFICATION_ITEMS)
            val viewModel = createViewModel()
            viewModel.trySendAction(ItemListingAction.SyncWithBitwardenDismiss)
            verify { settingsRepository.hasUserDismissedSyncWithBitwardenCard = true }
            assertEquals(expectedState, viewModel.stateFlow.value)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `on SyncWithBitwardenDismiss receive in empty state should dismiss action card and store dismissal in settings`() =
        runTest {
            val expectedState = DEFAULT_STATE.copy(
                viewState = ItemListingState.ViewState.NoItems(
                    actionCard = null,
                ),
            )
            every { settingsRepository.hasUserDismissedSyncWithBitwardenCard = true } just runs
            every { settingsRepository.hasUserDismissedSyncWithBitwardenCard } returns false
            mutableVerificationCodesFlow.value = DataState.Loaded(emptyList())
            val viewModel = createViewModel()
            viewModel.trySendAction(ItemListingAction.SyncWithBitwardenDismiss)
            verify { settingsRepository.hasUserDismissedSyncWithBitwardenCard = true }
            assertEquals(expectedState, viewModel.stateFlow.value)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `stateFlow value should show sync with bitwarden action card when local items are empty and shared state is SyncNotEnabled`() {
        val expectedState = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.NoItems(
                actionCard = ItemListingState.ActionCardState.SyncWithBitwarden,
            ),
        )
        every { settingsRepository.hasUserDismissedSyncWithBitwardenCard } returns false
        mutableSharedCodesFlow.value = SharedVerificationCodesState.SyncNotEnabled
        mutableVerificationCodesFlow.value = DataState.Loaded(emptyList())
        val viewModel = createViewModel()
        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `stateFlow value should not show download bitwarden card when local items are empty and shared state is SyncNotEnabled but user has dismissed card`() {
        val expectedState = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.NoItems(
                actionCard = null,
            ),
        )
        every { settingsRepository.hasUserDismissedSyncWithBitwardenCard } returns true
        mutableSharedCodesFlow.value = SharedVerificationCodesState.SyncNotEnabled
        mutableVerificationCodesFlow.value = DataState.Loaded(emptyList())
        val viewModel = createViewModel()
        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `stateFlow value should show sync with bitwarden card when there are local items and shared state is SyncNotEnabled`() {
        val expectedState = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.Content(
                actionCard = ItemListingState.ActionCardState.SyncWithBitwarden,
                favoriteItems = LOCAL_FAVORITE_ITEMS,
                itemList = LOCAL_NON_FAVORITE_ITEMS,
                sharedItems = SharedCodesDisplayState.Codes(persistentListOf()),
            ),
        )
        every { settingsRepository.hasUserDismissedSyncWithBitwardenCard } returns false
        mutableVerificationCodesFlow.value = DataState.Loaded(LOCAL_VERIFICATION_ITEMS)
        mutableSharedCodesFlow.value = SharedVerificationCodesState.SyncNotEnabled
        val viewModel = createViewModel()
        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `stateFlow value should not show sync with bitwarden card when there are local items and shared state is AppNotInstalled but user has dismissed card`() {
        val expectedState = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.Content(
                actionCard = null,
                favoriteItems = LOCAL_FAVORITE_ITEMS,
                itemList = LOCAL_NON_FAVORITE_ITEMS,
                sharedItems = SharedCodesDisplayState.Codes(persistentListOf()),
            ),
        )
        every { settingsRepository.hasUserDismissedSyncWithBitwardenCard } returns true
        mutableVerificationCodesFlow.value = DataState.Loaded(LOCAL_VERIFICATION_ITEMS)
        mutableSharedCodesFlow.value = SharedVerificationCodesState.SyncNotEnabled
        val viewModel = createViewModel()
        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    fun `on SyncLearnMoreClick should send NavigateToSyncInformation`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ItemListingAction.SyncLearnMoreClick)
            assertEquals(ItemListingEvent.NavigateToSyncInformation, awaitItem())
        }
    }

    @Test
    fun `on CopyToBitwardenClick receive should call startAddTotpLoginItemFlow`() {
        val expectedUriString = "expectedUriString"
        val entity: AuthenticatorItemEntity = mockk {
            every { toOtpAuthUriString() } returns expectedUriString
        }
        every {
            authenticatorRepository.getItemStateFlow("1")
        } returns MutableStateFlow(DataState.Loaded(data = entity))
        every {
            authenticatorBridgeManager.startAddTotpLoginItemFlow(expectedUriString)
        } returns true

        val viewModel = createViewModel()

        viewModel.trySendAction(
            ItemListingAction.DropdownMenuClick(
                menuAction = VaultDropdownMenuAction.COPY_TO_BITWARDEN,
                item = LOCAL_CODE,
            ),
        )
        verify { authenticatorBridgeManager.startAddTotpLoginItemFlow(expectedUriString) }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `on CopyToBitwardenClick should show error dialog when startAddTotpLoginItemFlow returns false`() {
        val expectedState = DEFAULT_STATE.copy(
            dialog = ItemListingState.DialogState.Error(
                title = BitwardenString.something_went_wrong.asText(),
                message = BitwardenString.please_try_again.asText(),
            ),
        )
        val expectedUriString = "expectedUriString"
        val entity: AuthenticatorItemEntity = mockk {
            every { toOtpAuthUriString() } returns expectedUriString
        }
        every {
            authenticatorRepository.getItemStateFlow("1")
        } returns MutableStateFlow(DataState.Loaded(data = entity))
        every {
            authenticatorBridgeManager.startAddTotpLoginItemFlow(expectedUriString)
        } returns false

        val viewModel = createViewModel()
        viewModel.trySendAction(
            ItemListingAction.DropdownMenuClick(
                menuAction = VaultDropdownMenuAction.COPY_TO_BITWARDEN,
                item = LOCAL_CODE,
            ),
        )
        assertEquals(
            expectedState,
            viewModel.stateFlow.value,
        )
        verify { authenticatorBridgeManager.startAddTotpLoginItemFlow(expectedUriString) }
    }

    @Test
    fun `on FirstTimeUserSyncReceive should emit ShowSnackbar`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            firstTimeAccountSyncChannel.send(Unit)
            assertEquals(
                ItemListingEvent.ShowSnackbar(
                    message = BitwardenString.account_synced_from_bitwarden_app.asText(),
                    withDismissAction = true,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `should copy text to clipboard when DropdownMenuClick COPY_CODE is triggered`() = runTest {
        val viewModel = createViewModel()

        every { clipboardManager.setText(text = LOCAL_CODE.authCode) } just runs

        viewModel.eventFlow.test {
            viewModel.trySendAction(
                ItemListingAction.DropdownMenuClick(
                    menuAction = VaultDropdownMenuAction.COPY_CODE,
                    item = LOCAL_CODE,
                ),
            )

            verify(exactly = 1) {
                clipboardManager.setText(text = LOCAL_CODE.authCode)
            }
        }
    }

    @Test
    fun `should trigger edit action when DropdownMenuClick EDIT is triggered`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(
                ItemListingAction.DropdownMenuClick(VaultDropdownMenuAction.EDIT, LOCAL_CODE),
            )

            assertEquals(
                ItemListingEvent.NavigateToEditItem(LOCAL_CODE.id),
                awaitItem(),
            )
        }
    }

    @Test
    fun `should trigger delete prompt when DropdownMenuClick DELETE is triggered`() = runTest {
        val viewModel = createViewModel()

        val expectedState = DEFAULT_STATE.copy(
            dialog = ItemListingState.DialogState.DeleteConfirmationPrompt(
                message = BitwardenString
                    .do_you_really_want_to_permanently_delete_this_cannot_be_undone
                    .asText(),
                itemId = LOCAL_CODE.id,
            ),
        )

        viewModel.trySendAction(
            ItemListingAction.DropdownMenuClick(
                menuAction = VaultDropdownMenuAction.DELETE,
                item = LOCAL_CODE,
            ),
        )

        assertEquals(
            expectedState,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on SectionExpandedClick should update expanded state for clicked section`() = runTest {
        val expectedState = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.Content(
                actionCard = null,
                favoriteItems = LOCAL_FAVORITE_ITEMS
                    .map { it.copy(showMoveToBitwarden = true) }
                    .toImmutableList(),
                itemList = LOCAL_NON_FAVORITE_ITEMS
                    .map { it.copy(showMoveToBitwarden = true) }
                    .toImmutableList(),
                sharedItems = SHARED_DISPLAY_ITEMS.copy(
                    sections = SHARED_DISPLAY_ITEMS
                        .sections
                        .map { it.copy(isExpanded = false) }
                        .toImmutableList(),
                ),
            ),
        )
        mutableVerificationCodesFlow.value = DataState.Loaded(LOCAL_VERIFICATION_ITEMS)
        mutableSharedCodesFlow.value =
            SharedVerificationCodesState.Success(SHARED_VERIFICATION_ITEMS)
        val viewModel = createViewModel()
        viewModel.trySendAction(
            ItemListingAction.SectionExpandedClick(section = SHARED_DISPLAY_ITEMS.sections.first()),
        )
        assertEquals(
            expectedState,
            viewModel.stateFlow.value,
        )
    }

    private fun createViewModel(): ItemListingViewModel = ItemListingViewModel(
        authenticatorRepository = authenticatorRepository,
        authenticatorBridgeManager = authenticatorBridgeManager,
        clipboardManager = clipboardManager,
        encodingManager = encodingManager,
        settingsRepository = settingsRepository,
        snackbarRelayManager = snackbarRelayManager,
    )
}

private const val AUTHENTICATOR_ALERT_SECONDS = 7
private val DEFAULT_STATE = ItemListingState(
    alertThresholdSeconds = AUTHENTICATOR_ALERT_SECONDS,
    viewState = ItemListingState.ViewState.Loading,
    dialog = null,
)

private val LOCAL_CODE = VerificationCodeDisplayItem(
    id = "1",
    title = "issuer",
    subtitle = null,
    timeLeftSeconds = 10,
    periodSeconds = 30,
    alertThresholdSeconds = 7,
    authCode = "123456",
    favorite = false,
    showOverflow = true,
    showMoveToBitwarden = true,
)

private val LOCAL_VERIFICATION_ITEMS = listOf(
    VerificationCodeItem(
        code = "123456",
        periodSeconds = 60,
        timeLeftSeconds = 430,
        issueTime = 35L,
        id = "1",
        issuer = "issuer",
        label = "accountName",
        source = AuthenticatorItem.Source.Local("1", isFavorite = false),
    ),
    VerificationCodeItem(
        code = "123456",
        periodSeconds = 60,
        timeLeftSeconds = 430,
        issueTime = 35L,
        id = "1",
        issuer = "issuer",
        label = "accountName",
        source = AuthenticatorItem.Source.Local("1", isFavorite = true),
    ),
)

private val SHARED_VERIFICATION_ITEMS = listOf(
    VerificationCodeItem(
        code = "987654",
        periodSeconds = 60,
        timeLeftSeconds = 430,
        issueTime = 35L,
        id = "1",
        issuer = "sharedIssue",
        label = "sharedAccountName",
        source = AuthenticatorItem.Source.Shared(
            userId = "1",
            nameOfUser = null,
            email = "email",
            environmentLabel = "environmentLabel",
        ),
    ),
)

private val LOCAL_DISPLAY_ITEMS = LOCAL_VERIFICATION_ITEMS.map {
    it.toDisplayItem(
        alertThresholdSeconds = AUTHENTICATOR_ALERT_SECONDS,
        sharedVerificationCodesState = SharedVerificationCodesState.AppNotInstalled,
        showOverflow = true,
    )
}

private val SHARED_DISPLAY_ITEMS = SharedVerificationCodesState.Success(SHARED_VERIFICATION_ITEMS)
    .toSharedCodesDisplayState(AUTHENTICATOR_ALERT_SECONDS)

private val LOCAL_FAVORITE_ITEMS = LOCAL_DISPLAY_ITEMS.filter { it.favorite }.toImmutableList()
private val LOCAL_NON_FAVORITE_ITEMS = LOCAL_DISPLAY_ITEMS
    .filterNot { it.favorite }
    .toImmutableList()
