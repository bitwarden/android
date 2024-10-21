package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.room.util.copy
import com.bitwarden.authenticator.data.platform.repository.util.bufferedMutableSharedFlow
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model.SharedCodesDisplayState
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model.VerificationCodeDisplayItem
import com.bitwarden.authenticator.ui.platform.base.BaseComposeTest
import com.bitwarden.authenticator.ui.platform.base.util.asText
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.authenticator.ui.platform.manager.intent.IntentManager
import com.bitwarden.authenticator.ui.platform.manager.permissions.FakePermissionManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val intentManager: IntentManager = mockk()
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
    fun `on sync with bitwarden action card click in empty state should send SyncWithBitwardenClick`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.NoItems(
                actionCard = ItemListingState.ActionCardState.SyncWithBitwarden,
            ),
        )
        composeTestRule
            .onNodeWithText("Sync with Bitwarden app")
            .performClick()
        verify { viewModel.trySendAction(ItemListingAction.SyncWithBitwardenClick) }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `on sync with bitwarden action card click in full state should send SyncWithBitwardenClick`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = ItemListingState.ViewState.Content(
                favoriteItems = emptyList(),
                itemList = emptyList(),
                sharedItems = SharedCodesDisplayState.Codes(emptyList()),
                actionCard = ItemListingState.ActionCardState.SyncWithBitwarden,
            ),
        )
        composeTestRule
            .onNodeWithText("Sync with Bitwarden app")
            .performClick()
        verify { viewModel.trySendAction(ItemListingAction.SyncWithBitwardenClick) }
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
}

private val APP_THEME = AppTheme.DEFAULT
private const val ALERT_THRESHOLD = 7

private val SHARED_ACCOUNTS_SECTION = SharedCodesDisplayState.SharedCodesAccountSection(
    label = "test@test.com".asText(),
    codes = listOf(
        VerificationCodeDisplayItem(
            id = "1",
            issuer = "bitwarden.com",
            label = "joe+shared_code_1@test.com",
            timeLeftSeconds = 10,
            periodSeconds = 30,
            alertThresholdSeconds = ALERT_THRESHOLD,
            authCode = "123456",
            favorite = false,
            allowLongPressActions = false,
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
