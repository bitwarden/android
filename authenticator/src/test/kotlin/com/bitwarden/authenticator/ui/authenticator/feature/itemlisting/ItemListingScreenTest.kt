package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.core.net.toUri
import com.bitwarden.authenticator.ui.platform.base.AuthenticatorComposeTest
import com.bitwarden.authenticator.ui.platform.components.listitem.model.SharedCodesDisplayState
import com.bitwarden.authenticator.ui.platform.components.listitem.model.VaultDropdownMenuAction
import com.bitwarden.authenticator.ui.platform.components.listitem.model.VerificationCodeDisplayItem
import com.bitwarden.authenticator.ui.platform.manager.permissions.FakePermissionManager
import com.bitwarden.authenticator.ui.platform.util.startBitwardenAccountSettings
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.onNodeWithContentDescriptionAfterScroll
import com.bitwarden.ui.util.onNodeWithTextAfterScroll
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test

class ItemListingScreenTest : AuthenticatorComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToSearchCalled = false
    private var onNavigateToQrCodeScannerCalled = false
    private var onNavigateToManualKeyEntryCalled = false
    private var onNavigateToEditItemScreenCalled = false

    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mutableEventFlow = bufferedMutableSharedFlow<ItemListingEvent>()

    private val viewModel: ItemListingViewModel = mockk {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
        every { trySendAction(any()) } just runs
    }

    private val intentManager: IntentManager = mockk {
        every { launchUri(uri = any()) } just runs
    }
    private val permissionsManager = FakePermissionManager()

    @Before
    fun setup() {
        setContent(
            intentManager = intentManager,
            permissionsManager = permissionsManager,
        ) {
            ItemListingScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToSearch = { onNavigateToSearchCalled = true },
                onNavigateToQrCodeScanner = { onNavigateToQrCodeScannerCalled = true },
                onNavigateToManualKeyEntry = { onNavigateToManualKeyEntryCalled = true },
                onNavigateToEditItemScreen = { onNavigateToEditItemScreenCalled = true },
            )
        }
    }

    @Test
    fun `on NavigateToSyncInformation should launch sync uri`() {
        mutableEventFlow.tryEmit(ItemListingEvent.NavigateToSyncInformation)
        verify(exactly = 1) {
            intentManager.launchUri(uri = "https://bitwarden.com/help/totp-sync".toUri())
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `when denying camera permissions and attempting to add a code we should be shown the manual entry screen`() {
        permissionsManager.getPermissionsResult = false

        composeTestRule
            .onNodeWithText("Add code")
            .performClick()

        verify {
            viewModel.trySendAction(
                ItemListingAction.EnterSetupKeyClick,
            )
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `when allowing camera permissions and attempting to add a code we should be shown the scan QR code screen`() {
        permissionsManager.getPermissionsResult = true

        composeTestRule
            .onNodeWithText("Add code")
            .performClick()

        verify {
            viewModel.trySendAction(
                ItemListingAction.ScanQrCodeClick,
            )
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `shared accounts error message should show when view is Content with SharedCodesDisplayState Error`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.Content(
                actionCard = null,
                favoriteItems = persistentListOf(),
                itemList = persistentListOf(),
                sharedItems = SharedCodesDisplayState.Error,
            ),
        )

        composeTestRule
            .onNodeWithText("Unable to sync codes from the Bitwarden app. Make sure both apps are up-to-date. You can still access your existing codes in the Bitwarden app.")
            .assertIsDisplayed()

        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.Content(
                actionCard = null,
                favoriteItems = persistentListOf(),
                itemList = persistentListOf(),
                sharedItems = SharedCodesDisplayState.Codes(persistentListOf()),
            ),
        )

        composeTestRule
            .onNodeWithText("Unable to sync codes from the Bitwarden app. Make sure both apps are up-to-date. You can still access your existing codes in the Bitwarden app.")
            .assertDoesNotExist()
    }

    @Test
    fun `clicking shared accounts verification code item should send ItemClick action`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.Content(
                actionCard = null,
                favoriteItems = persistentListOf(),
                itemList = persistentListOf(),
                sharedItems = SharedCodesDisplayState.Codes(
                    sections = persistentListOf(SHARED_ACCOUNTS_SECTION),
                ),
            ),
        )

        composeTestRule
            .onNodeWithText("joe+shared_code_1@test.com")
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                ItemListingAction.ItemClick(SHARED_ACCOUNTS_SECTION.codes[0].authCode),
            )
        }

        // Make sure long press sends action as well, since local items have long press options
        // but shared items do not:
        composeTestRule
            .onNodeWithText("joe+shared_code_1@test.com")
            .performTouchInput { longClick() }

        verify {
            viewModel.trySendAction(
                ItemListingAction.ItemClick(SHARED_ACCOUNTS_SECTION.codes[0].authCode),
            )
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `on NavigateToBitwardenSettings receive should launch bitwarden account security deep link`() {
        mockkStatic(IntentManager::startBitwardenAccountSettings) {
            every { intentManager.startBitwardenAccountSettings() } just runs
            mutableEventFlow.tryEmit(ItemListingEvent.NavigateToBitwardenSettings)
            verify { intentManager.startBitwardenAccountSettings() }
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `on sync with bitwarden app settings click in empty state should send SyncWithBitwardenClick`() {
        mutableStateFlow.update {
            it.copy(
                viewState = ItemListingState.ViewState.NoItems(
                    actionCard = ItemListingState.ActionCardState.SyncWithBitwarden,
                ),
            )
        }

        composeTestRule
            .onNodeWithText(text = "Take me to the app settings")
            .performClick()
        verify { viewModel.trySendAction(ItemListingAction.SyncWithBitwardenClick) }
    }

    @Test
    fun `on sync with bitwarden learn more click in empty state should send SyncLearnMoreClick`() {
        mutableStateFlow.update {
            it.copy(
                viewState = ItemListingState.ViewState.NoItems(
                    actionCard = ItemListingState.ActionCardState.SyncWithBitwarden,
                ),
            )
        }

        composeTestRule
            .onNodeWithText(text = "Learn more")
            .performClick()
        verify { viewModel.trySendAction(ItemListingAction.SyncLearnMoreClick) }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `on sync with bitwarden app settings click in full state should send SyncWithBitwardenClick`() {
        mutableStateFlow.update {
            it.copy(
                viewState = ItemListingState.ViewState.Content(
                    favoriteItems = persistentListOf(),
                    itemList = persistentListOf(),
                    sharedItems = SharedCodesDisplayState.Codes(persistentListOf()),
                    actionCard = ItemListingState.ActionCardState.SyncWithBitwarden,
                ),
            )
        }
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Take me to the app settings")
            .performClick()
        verify { viewModel.trySendAction(ItemListingAction.SyncWithBitwardenClick) }
    }

    @Test
    fun `on sync with bitwarden learn more click in full state should send SyncLearnMoreClick`() {
        mutableStateFlow.update {
            it.copy(
                viewState = ItemListingState.ViewState.Content(
                    favoriteItems = persistentListOf(),
                    itemList = persistentListOf(),
                    sharedItems = SharedCodesDisplayState.Codes(persistentListOf()),
                    actionCard = ItemListingState.ActionCardState.SyncWithBitwarden,
                ),
            )
        }
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Learn more")
            .performClick()
        verify { viewModel.trySendAction(ItemListingAction.SyncLearnMoreClick) }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `on sync with bitwarden action card dismiss in empty state should send SyncWithBitwardenDismiss`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.NoItems(
                actionCard = ItemListingState.ActionCardState.SyncWithBitwarden,
            ),
        )
        composeTestRule
            .onNodeWithContentDescription("Close")
            .performClick()
        verify { viewModel.trySendAction(ItemListingAction.SyncWithBitwardenDismiss) }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `on sync with bitwarden action card dismiss in full state should send SyncWithBitwardenDismiss`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.Content(
                favoriteItems = persistentListOf(),
                itemList = persistentListOf(),
                sharedItems = SharedCodesDisplayState.Codes(persistentListOf()),
                actionCard = ItemListingState.ActionCardState.SyncWithBitwarden,
            ),
        )
        composeTestRule
            .onNodeWithContentDescription("Close")
            .performClick()
        verify { viewModel.trySendAction(ItemListingAction.SyncWithBitwardenDismiss) }
    }

    @Test
    fun `on download bitwarden click in empty state should send DownloadBitwardenClick`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.NoItems(
                actionCard = ItemListingState.ActionCardState.DownloadBitwardenApp,
            ),
        )
        composeTestRule
            .onNodeWithText(text = "Download now")
            .performClick()
        verify { viewModel.trySendAction(ItemListingAction.DownloadBitwardenClick) }
    }

    @Test
    fun `on download bitwarden click in full state should send DownloadBitwardenClick`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.Content(
                favoriteItems = persistentListOf(),
                itemList = persistentListOf(),
                sharedItems = SharedCodesDisplayState.Codes(persistentListOf()),
                actionCard = ItemListingState.ActionCardState.DownloadBitwardenApp,
            ),
        )
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Download now")
            .performClick()
        verify { viewModel.trySendAction(ItemListingAction.DownloadBitwardenClick) }
    }

    @Test
    fun `on download bitwarden dismiss in empty state should send DownloadBitwardenDismiss`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.NoItems(
                actionCard = ItemListingState.ActionCardState.DownloadBitwardenApp,
            ),
        )
        composeTestRule
            .onNodeWithContentDescription(label = "Close")
            .performClick()
        verify { viewModel.trySendAction(ItemListingAction.DownloadBitwardenDismiss) }
    }

    @Test
    fun `on download bitwarden dismiss in full state should send DownloadBitwardenDismiss`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.Content(
                favoriteItems = persistentListOf(),
                itemList = persistentListOf(),
                sharedItems = SharedCodesDisplayState.Codes(persistentListOf()),
                actionCard = ItemListingState.ActionCardState.DownloadBitwardenApp,
            ),
        )
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Close")
            .performClick()
        verify { viewModel.trySendAction(ItemListingAction.DownloadBitwardenDismiss) }
    }

    @Test
    fun `clicking Copy to Bitwarden vault should send DropdownMenuClick with COPY_TO_BITWARDEN`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.Content(
                actionCard = null,
                favoriteItems = persistentListOf(),
                itemList = persistentListOf(LOCAL_CODE),
                sharedItems = SharedCodesDisplayState.Error,
            ),
        )
        composeTestRule
            .onNodeWithText(text = "issuer")
            .onChildren()
            .filterToOne(hasContentDescription(value = "More"))
            .performClick()

        composeTestRule
            .onNodeWithText(text = "Copy to Bitwarden vault")
            .performClick()

        verify {
            viewModel.trySendAction(
                ItemListingAction.DropdownMenuClick(
                    menuAction = VaultDropdownMenuAction.COPY_TO_BITWARDEN,
                    item = LOCAL_CODE,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Copy to Bitwarden vault long press action should not show when showMoveToBitwarden is false`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.Content(
                actionCard = null,
                favoriteItems = persistentListOf(),
                itemList = persistentListOf(LOCAL_CODE.copy(showMoveToBitwarden = false)),
                sharedItems = SharedCodesDisplayState.Error,
            ),
        )
        composeTestRule
            .onNodeWithText("issuer")
            .performTouchInput { longClick() }

        composeTestRule
            .onNodeWithText(text = "Copy to Bitwarden vault")
            .assertDoesNotExist()
    }

    @Test
    fun `on ShowFirstTimeSyncSnackbar receive should show snackbar`() {
        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                viewState = ItemListingState.ViewState.Content(
                    actionCard = null,
                    favoriteItems = persistentListOf(),
                    itemList = persistentListOf(),
                    sharedItems = SharedCodesDisplayState.Codes(persistentListOf()),
                ),
            )
        }
        // Make sure the snackbar isn't showing:
        composeTestRule
            .onNodeWithText("Account synced from Bitwarden app")
            .assertIsNotDisplayed()

        // Send ShowSnackbar event
        mutableEventFlow.tryEmit(
            ItemListingEvent.ShowSnackbar(message = "Account synced from Bitwarden app".asText()),
        )

        // Make sure the snackbar is showing:
        composeTestRule
            .onNodeWithText("Account synced from Bitwarden app")
            .assertIsDisplayed()
    }

    @Test
    fun `local codes header should be displayed and expanded when syncing is enabled`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.Content(
                actionCard = null,
                favoriteItems = persistentListOf(),
                itemList = persistentListOf(LOCAL_CODE),
                sharedItems = SharedCodesDisplayState.Codes(
                    sections = persistentListOf(SHARED_ACCOUNTS_SECTION),
                ),
            ),
        )

        composeTestRule
            .onNodeWithText(text = "LOCAL CODES (1)")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(LOCAL_CODE.title)
            .assertIsDisplayed()
    }

    @Test
    fun `shared codes header click should emit SectionExpandedClick`() {
        val sharedItems = SharedCodesDisplayState.Codes(
            sections = persistentListOf(SHARED_ACCOUNTS_SECTION),
        )
        val viewState = ItemListingState.ViewState.Content(
            actionCard = null,
            favoriteItems = persistentListOf(),
            itemList = persistentListOf(LOCAL_CODE),
            sharedItems = sharedItems,
        )
        mutableStateFlow.value = DEFAULT_STATE.copy(viewState = viewState)

        composeTestRule
            .onNodeWithTextAfterScroll(text = "TEST@TEST.COM | BITWARDEN.COM (1)")
            .performClick()

        verify {
            viewModel.trySendAction(ItemListingAction.SectionExpandedClick(SHARED_ACCOUNTS_SECTION))
        }
    }

    @Test
    fun `shared codes header should be displayed and collapsed when syncing is enabled`() {
        val sharedItems = SharedCodesDisplayState.Codes(
            sections = persistentListOf(SHARED_ACCOUNTS_SECTION),
        )
        val viewState = ItemListingState.ViewState.Content(
            actionCard = null,
            favoriteItems = persistentListOf(),
            itemList = persistentListOf(LOCAL_CODE),
            sharedItems = sharedItems,
        )
        mutableStateFlow.value = DEFAULT_STATE.copy(viewState = viewState)

        composeTestRule
            .onNodeWithTextAfterScroll(text = "TEST@TEST.COM | BITWARDEN.COM (1)")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTextAfterScroll(SHARED_ACCOUNTS_SECTION.codes[0].title)
            .assertIsDisplayed()

        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = viewState.copy(
                sharedItems = sharedItems.copy(
                    sections = persistentListOf(SHARED_ACCOUNTS_SECTION.copy(isExpanded = false)),
                ),
            ),
        )

        composeTestRule
            .onNodeWithTextAfterScroll(text = "TEST@TEST.COM | BITWARDEN.COM (1)")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(SHARED_ACCOUNTS_SECTION.codes[0].title)
            .assertIsNotDisplayed()
    }

    @Test
    fun `local codes should be displayed based on expanding header state`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.Content(
                actionCard = null,
                favoriteItems = persistentListOf(),
                itemList = persistentListOf(LOCAL_CODE),
                sharedItems = SharedCodesDisplayState.Codes(
                    sections = persistentListOf(SHARED_ACCOUNTS_SECTION),
                ),
            ),
        )

        composeTestRule
            .onNodeWithText(LOCAL_CODE.title)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(text = "LOCAL CODES (1)")
            .performClick()

        composeTestRule
            .onNodeWithText(LOCAL_CODE.title)
            .assertIsNotDisplayed()
    }
}

private const val ALERT_THRESHOLD = 7

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

private val SHARED_ACCOUNTS_SECTION = SharedCodesDisplayState.SharedCodesAccountSection(
    id = "id",
    label = "test@test.com | bitwarden.com (1)".asText(),
    codes = persistentListOf(
        VerificationCodeDisplayItem(
            id = "1",
            title = "bitwarden.com",
            subtitle = "joe+shared_code_1@test.com",
            timeLeftSeconds = 10,
            periodSeconds = 30,
            alertThresholdSeconds = ALERT_THRESHOLD,
            authCode = "123456",
            favorite = false,
            showOverflow = false,
            showMoveToBitwarden = false,
        ),
    ),
    isExpanded = true,
)

private val DEFAULT_STATE = ItemListingState(
    alertThresholdSeconds = ALERT_THRESHOLD,
    viewState = ItemListingState.ViewState.NoItems(
        actionCard = null,
    ),
    dialog = null,
)
