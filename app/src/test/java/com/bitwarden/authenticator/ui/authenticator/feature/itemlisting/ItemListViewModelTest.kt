package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting

import app.cash.turbine.test
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.authenticator.repository.model.AuthenticatorItem
import com.bitwarden.authenticator.data.authenticator.repository.model.SharedVerificationCodesState
import com.bitwarden.authenticator.data.platform.manager.BitwardenEncodingManager
import com.bitwarden.authenticator.data.platform.manager.clipboard.BitwardenClipboardManager
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.authenticator.data.platform.repository.model.DataState
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model.SharedCodesDisplayState
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.util.toDisplayItem
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.util.toSharedCodesDisplayState
import com.bitwarden.authenticator.ui.platform.base.BaseViewModelTest
import com.bitwarden.authenticator.ui.platform.base.util.asText
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.authenticatorbridge.manager.AuthenticatorBridgeManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ItemListViewModelTest : BaseViewModelTest() {

    private val mutableAuthenticatorAlertThresholdFlow =
        MutableStateFlow(AUTHENTICATOR_ALERT_SECONDS)
    private val mutableAppThemeFlow = MutableStateFlow(APP_THEME)
    private val mutableVerificationCodesFlow =
        MutableStateFlow<DataState<List<VerificationCodeItem>>>(DataState.Loading)
    private val mutableSharedCodesFlow =
        MutableStateFlow<SharedVerificationCodesState>(SharedVerificationCodesState.Loading)

    private val authenticatorRepository: AuthenticatorRepository = mockk {
        every { totpCodeFlow } returns emptyFlow()
        every { getLocalVerificationCodesFlow() } returns mutableVerificationCodesFlow
        every { sharedCodesStateFlow } returns mutableSharedCodesFlow
    }
    private val authenticatorBridgeManager: AuthenticatorBridgeManager = mockk()
    private val clipboardManager: BitwardenClipboardManager = mockk()
    private val encodingManager: BitwardenEncodingManager = mockk()
    private val settingsRepository: SettingsRepository = mockk {
        every { appTheme } returns mutableAppThemeFlow.value
        every {
            authenticatorAlertThresholdSeconds
        } returns mutableAuthenticatorAlertThresholdFlow.value
        every {
            authenticatorAlertThresholdSecondsFlow
        } returns mutableAuthenticatorAlertThresholdFlow
        every { appThemeStateFlow } returns mutableAppThemeFlow
        every { hasUserDismissedDownloadBitwardenCard } returns false
    }

    @Test
    fun `initial state should be correct`() {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
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
                actionCard = ItemListingState.ActionCardState.None,
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
                sharedItems = SharedCodesDisplayState.Codes(emptyList()),
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
                actionCard = ItemListingState.ActionCardState.None,
                favoriteItems = LOCAL_FAVORITE_ITEMS,
                itemList = LOCAL_NON_FAVORITE_ITEMS,
                sharedItems = SharedCodesDisplayState.Codes(emptyList()),
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
                actionCard = ItemListingState.ActionCardState.None,
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
                actionCard = ItemListingState.ActionCardState.None,
                favoriteItems = LOCAL_FAVORITE_ITEMS.map { it.copy(showMoveToBitwarden = true) },
                itemList = LOCAL_NON_FAVORITE_ITEMS.map { it.copy(showMoveToBitwarden = true) },
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
                actionCard = ItemListingState.ActionCardState.None,
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
                actionCard = ItemListingState.ActionCardState.None,
                favoriteItems = emptyList(),
                itemList = emptyList(),
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
                    actionCard = ItemListingState.ActionCardState.None,
                    favoriteItems = LOCAL_FAVORITE_ITEMS,
                    itemList = LOCAL_NON_FAVORITE_ITEMS,
                    sharedItems = SharedCodesDisplayState.Codes(emptyList()),
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
                    actionCard = ItemListingState.ActionCardState.None,
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
                    actionCard = ItemListingState.ActionCardState.None,
                    favoriteItems = LOCAL_FAVORITE_ITEMS,
                    itemList = LOCAL_NON_FAVORITE_ITEMS,
                    sharedItems = SharedCodesDisplayState.Codes(emptyList()),
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
                    actionCard = ItemListingState.ActionCardState.None,
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
                actionCard = ItemListingState.ActionCardState.None,
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
                sharedItems = SharedCodesDisplayState.Codes(emptyList()),
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
                actionCard = ItemListingState.ActionCardState.None,
                favoriteItems = LOCAL_FAVORITE_ITEMS,
                itemList = LOCAL_NON_FAVORITE_ITEMS,
                sharedItems = SharedCodesDisplayState.Codes(emptyList()),
            ),
        )
        every { settingsRepository.hasUserDismissedSyncWithBitwardenCard } returns true
        mutableVerificationCodesFlow.value = DataState.Loaded(LOCAL_VERIFICATION_ITEMS)
        mutableSharedCodesFlow.value = SharedVerificationCodesState.SyncNotEnabled
        val viewModel = createViewModel()
        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    fun `on MoveToBitwardenClick receive should call startAddTotpLoginItemFlow`() {
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

        viewModel.trySendAction(ItemListingAction.MoveToBitwardenClick(entityId = "1"))
        verify { authenticatorBridgeManager.startAddTotpLoginItemFlow(expectedUriString) }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `on MoveToBitwardenClick should show error dialog when startAddTotpLoginItemFlow returns false`() {
        val expectedState = DEFAULT_STATE.copy(
            dialog = ItemListingState.DialogState.Error(
                title = R.string.something_went_wrong.asText(),
                message = R.string.please_try_again.asText(),
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
        viewModel.trySendAction(ItemListingAction.MoveToBitwardenClick(entityId = "1"))
        assertEquals(
            expectedState,
            viewModel.stateFlow.value,
        )
        verify { authenticatorBridgeManager.startAddTotpLoginItemFlow(expectedUriString) }
    }

    private fun createViewModel() = ItemListingViewModel(
        authenticatorRepository = authenticatorRepository,
        authenticatorBridgeManager = authenticatorBridgeManager,
        clipboardManager = clipboardManager,
        encodingManager = encodingManager,
        settingsRepository = settingsRepository,
    )
}

private val APP_THEME: AppTheme = mockk()
private const val AUTHENTICATOR_ALERT_SECONDS = 7
private val DEFAULT_STATE = ItemListingState(
    appTheme = APP_THEME,
    alertThresholdSeconds = AUTHENTICATOR_ALERT_SECONDS,
    viewState = ItemListingState.ViewState.Loading,
    dialog = null,
)

private val LOCAL_VERIFICATION_ITEMS = listOf(
    VerificationCodeItem(
        code = "123456",
        periodSeconds = 60,
        timeLeftSeconds = 430,
        issueTime = 35L,
        id = "1",
        issuer = "issuer",
        accountName = "accountName",
        source = AuthenticatorItem.Source.Local("1", isFavorite = false),
    ),
    VerificationCodeItem(
        code = "123456",
        periodSeconds = 60,
        timeLeftSeconds = 430,
        issueTime = 35L,
        id = "1",
        issuer = "issuer",
        accountName = "accountName",
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
        accountName = "sharedAccountName",
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
        AUTHENTICATOR_ALERT_SECONDS,
        SharedVerificationCodesState.AppNotInstalled,
    )
}

private val SHARED_DISPLAY_ITEMS = SharedVerificationCodesState.Success(SHARED_VERIFICATION_ITEMS)
    .toSharedCodesDisplayState(AUTHENTICATOR_ALERT_SECONDS)

private val LOCAL_FAVORITE_ITEMS = LOCAL_DISPLAY_ITEMS.filter { it.favorite }
private val LOCAL_NON_FAVORITE_ITEMS = LOCAL_DISPLAY_ITEMS.filterNot { it.favorite }
