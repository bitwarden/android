package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.core.net.toUri
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model.SharedCodesDisplayState
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model.VaultDropdownMenuAction
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model.VerificationCodeDisplayItem
import com.bitwarden.authenticator.ui.platform.base.BaseComposeTest
import com.bitwarden.authenticator.ui.platform.manager.intent.IntentManager
import com.bitwarden.authenticator.ui.platform.manager.permissions.FakePermissionManager
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.ui.util.asText
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test

class ItemListingScreenTest : BaseComposeTest() {

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
        composeTestRule.setContent {
            ItemListingScreen(
                viewModel = viewModel,
                intentManager = intentManager,
                permissionsManager = permissionsManager,
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
                actionCard = ItemListingState.ActionCardState.None,
                favoriteItems = emptyList(),
                itemList = emptyList(),
                sharedItems = SharedCodesDisplayState.Error,
            ),
        )

        composeTestRule
            .onNodeWithText("Unable to sync codes from the Bitwarden app. Make sure both apps are up-to-date. You can still access your existing codes in the Bitwarden app.")
            .assertIsDisplayed()

        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.Content(
                actionCard = ItemListingState.ActionCardState.None,
                favoriteItems = emptyList(),
                itemList = emptyList(),
                sharedItems = SharedCodesDisplayState.Codes(emptyList()),
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
                actionCard = ItemListingState.ActionCardState.None,
                favoriteItems = emptyList(),
                itemList = emptyList(),
                sharedItems = SharedCodesDisplayState.Codes(
                    sections = listOf(
                        SHARED_ACCOUNTS_SECTION,
                    ),
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
        every { intentManager.startMainBitwardenAppAccountSettings() } just runs
        mutableEventFlow.tryEmit(ItemListingEvent.NavigateToBitwardenSettings)
        verify { intentManager.startMainBitwardenAppAccountSettings() }
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
                    favoriteItems = emptyList(),
                    itemList = emptyList(),
                    sharedItems = SharedCodesDisplayState.Codes(emptyList()),
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
                    favoriteItems = emptyList(),
                    itemList = emptyList(),
                    sharedItems = SharedCodesDisplayState.Codes(emptyList()),
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
                favoriteItems = emptyList(),
                itemList = emptyList(),
                sharedItems = SharedCodesDisplayState.Codes(emptyList()),
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
                favoriteItems = emptyList(),
                itemList = emptyList(),
                sharedItems = SharedCodesDisplayState.Codes(emptyList()),
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
                favoriteItems = emptyList(),
                itemList = emptyList(),
                sharedItems = SharedCodesDisplayState.Codes(emptyList()),
                actionCard = ItemListingState.ActionCardState.DownloadBitwardenApp,
            ),
        )
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Close")
            .performClick()
        verify { viewModel.trySendAction(ItemListingAction.DownloadBitwardenDismiss) }
    }

    @Test
    fun `clicking Move to Bitwarden should send MoveToBitwardenClick`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.Content(
                actionCard = ItemListingState.ActionCardState.None,
                favoriteItems = emptyList(),
                itemList = listOf(LOCAL_CODE),
                sharedItems = SharedCodesDisplayState.Error,
            ),
        )
        composeTestRule
            .onNodeWithText("issuer")
            .performTouchInput { longClick() }

        composeTestRule
            .onNodeWithText("Move to Bitwarden")
            .performClick()

        verify {
            viewModel.trySendAction(
                ItemListingAction.DropdownMenuClick(
                    menuAction = VaultDropdownMenuAction.MOVE,
                    item = LOCAL_CODE,
                ),
            )
        }
    }

    @Test
    fun `Move to Bitwarden long press action should not show when showMoveToBitwarden is false`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.Content(
                actionCard = ItemListingState.ActionCardState.None,
                favoriteItems = emptyList(),
                itemList = listOf(LOCAL_CODE.copy(showMoveToBitwarden = false)),
                sharedItems = SharedCodesDisplayState.Error,
            ),
        )
        composeTestRule
            .onNodeWithText("issuer")
            .performTouchInput { longClick() }

        composeTestRule
            .onNodeWithText("Move to Bitwarden")
            .assertDoesNotExist()
    }

    @Test
    fun `on ShowFirstTimeSyncSnackbar receive should show snackbar`() {
        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                viewState = ItemListingState.ViewState.Content(
                    actionCard = ItemListingState.ActionCardState.None,
                    favoriteItems = emptyList(),
                    itemList = emptyList(),
                    sharedItems = SharedCodesDisplayState.Codes(emptyList()),
                ),
            )
        }
        // Make sure the snackbar isn't showing:
        composeTestRule
            .onNodeWithText("Account synced from Bitwarden app")
            .assertIsNotDisplayed()

        // Send ShowFirstTimeSyncSnackbar event
        mutableEventFlow.tryEmit(ItemListingEvent.ShowFirstTimeSyncSnackbar)

        // Make sure the snackbar is showing:
        composeTestRule
            .onNodeWithText("Account synced from Bitwarden app")
            .assertIsDisplayed()
    }
}

private val APP_THEME = AppTheme.DEFAULT
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
    allowLongPressActions = true,
    showMoveToBitwarden = true,
)

private val SHARED_ACCOUNTS_SECTION = SharedCodesDisplayState.SharedCodesAccountSection(
    label = "test@test.com".asText(),
    codes = listOf(
        VerificationCodeDisplayItem(
            id = "1",
            title = "bitwarden.com",
            subtitle = "joe+shared_code_1@test.com",
            timeLeftSeconds = 10,
            periodSeconds = 30,
            alertThresholdSeconds = ALERT_THRESHOLD,
            authCode = "123456",
            favorite = false,
            allowLongPressActions = false,
            showMoveToBitwarden = false,
        ),
    ),
)

private val DEFAULT_STATE = ItemListingState(
    appTheme = APP_THEME,
    alertThresholdSeconds = ALERT_THRESHOLD,
    viewState = ItemListingState.ViewState.NoItems(
        actionCard = ItemListingState.ActionCardState.None,
    ),
    dialog = null,
)

/**
 * A helper used to scroll to and get the matching node in a scrollable list. This is intended to
 * be used with lazy lists that would otherwise fail when calling [performScrollToNode].
 */
fun ComposeContentTestRule.onNodeWithContentDescriptionAfterScroll(
    label: String,
): SemanticsNodeInteraction {
    onNode(hasScrollToNodeAction()).performScrollToNode(hasContentDescription(label))
    return onNodeWithContentDescription(label)
}

/**
 * A helper used to scroll to and get the matching node in a scrollable list. This is intended to
 * be used with lazy lists that would otherwise fail when calling [performScrollToNode].
 */
fun ComposeContentTestRule.onNodeWithTextAfterScroll(
    text: String,
    substring: Boolean = false,
): SemanticsNodeInteraction {
    onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text, substring))
    return onNodeWithText(text, substring)
}
