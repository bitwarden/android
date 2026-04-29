package com.x8bit.bitwarden.ui.platform.feature.premium.plan

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.net.toUri
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.manager.intent.model.AuthTabData
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.billing.repository.model.PremiumSubscriptionStatus
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.model.AuthTabLaunchers
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@Suppress("LargeClass")
class PlanScreenTest : BitwardenComposeTest() {

    private var onNavigateBackCalled = false
    private val premiumCheckoutLauncher: ActivityResultLauncher<Intent> = mockk()

    private val mutableEventFlow = bufferedMutableSharedFlow<PlanEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_FREE_STATE)
    private val viewModel = mockk<PlanViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }
    private val intentManager = mockk<IntentManager> {
        every {
            startAuthTab(uri = any(), authTabData = any(), launcher = any())
        } just runs
        every { launchUri(any()) } just runs
    }

    @Before
    fun setUp() {
        setContent(
            authTabLaunchers = AuthTabLaunchers(
                duo = mockk(),
                sso = mockk(),
                webAuthn = mockk(),
                cookie = mockk(),
                premiumCheckout = premiumCheckoutLauncher,
            ),
            intentManager = intentManager,
        ) {
            PlanScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                viewModel = viewModel,
            )
        }
    }

    // region Events

    @Test
    fun `NavigateBack event should call onNavigateBack`() {
        mutableEventFlow.tryEmit(PlanEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `LaunchBrowser event should call startAuthTab`() {
        val url = "https://checkout.stripe.com/session123"
        val authTabData = AuthTabData.CustomScheme(
            callbackUrl = PREMIUM_CHECKOUT_CALLBACK_URL,
        )
        mutableEventFlow.tryEmit(
            PlanEvent.LaunchBrowser(
                url = url,
                authTabData = authTabData,
            ),
        )
        verify {
            intentManager.startAuthTab(
                uri = url.toUri(),
                authTabData = authTabData,
                launcher = premiumCheckoutLauncher,
            )
        }
    }

    @Test
    fun `ShowSnackbar event should display snackbar`() {
        val data = BitwardenSnackbarData("Upgraded to premium".asText())
        mutableEventFlow.tryEmit(PlanEvent.ShowSnackbar(data))
        composeTestRule
            .onNodeWithText("Upgraded to premium")
            .assertIsDisplayed()
    }

    // endregion Events

    // region Free content tests

    @Test
    fun `close button click should send BackClick action for Modal mode`() {
        composeTestRule
            .onNodeWithContentDescription("Close")
            .performClick()
        verify { viewModel.trySendAction(PlanAction.BackClick) }
    }

    @Test
    fun `back button click should send BackClick action for Standard mode`() {
        mutableStateFlow.update {
            it.copy(planMode = PlanMode.Standard)
        }
        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()
        verify { viewModel.trySendAction(PlanAction.BackClick) }
    }

    @Test
    fun `upgrade now button click should send UpgradeNowClick action`() {
        composeTestRule
            .onNodeWithTag("UpgradeNowButton")
            .performScrollTo()
            .performClick()
        verify {
            viewModel.trySendAction(PlanAction.UpgradeNowClick)
        }
    }

    @Test
    fun `price and subtitle should render from state`() {
        composeTestRule
            .onNodeWithText("\$1.65")
            .assertExists()
        composeTestRule
            .onNodeWithText("/ month")
            .assertExists()
        composeTestRule
            .onNodeWithText(
                "Unlock more advanced features with a Premium plan.",
            )
            .assertExists()
    }

    @Test
    fun `feature list items should render`() {
        composeTestRule
            .onNodeWithText("Built-in authenticator")
            .assertExists()
        composeTestRule
            .onNodeWithText("Emergency access")
            .assertExists()
        composeTestRule
            .onNodeWithText("Secure file storage")
            .assertExists()
        composeTestRule
            .onNodeWithText("Breach monitoring")
            .assertExists()
    }

    @Test
    fun `stripe footer should render`() {
        composeTestRule
            .onNodeWithTag("StripeFooterText")
            .assertExists()
    }

    @Test
    fun `loading dialog should render when dialogState is Loading`() {
        composeTestRule
            .onAllNodesWithText("Opening checkout\u2026")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = PlanState.DialogState.Loading(
                    message = BitwardenString.opening_checkout.asText(),
                ),
            )
        }

        composeTestRule
            .onAllNodesWithText("Opening checkout\u2026")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
    }

    @Test
    fun `error dialog should render when dialogState is Error`() {
        composeTestRule
            .onAllNodesWithText("Secure checkout didn\u2019t load")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = PlanState.DialogState.CheckoutError,
            )
        }

        composeTestRule
            .onAllNodesWithText("Secure checkout didn\u2019t load")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
    }

    @Test
    fun `error dialog try again click should send RetryClick action`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = PlanState.DialogState.CheckoutError,
            )
        }
        composeTestRule
            .onAllNodesWithText("Try again")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(PlanAction.RetryClick) }
    }

    @Test
    fun `error dialog close click should send DismissError action`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = PlanState.DialogState.CheckoutError,
            )
        }
        composeTestRule
            .onAllNodesWithText("Close")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify {
            viewModel.trySendAction(PlanAction.DismissError)
        }
    }

    @Test
    fun `waiting for payment dialog should render when dialogState is WaitingForPayment`() {
        composeTestRule
            .onAllNodesWithText("Payment not received yet")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState =
                    PlanState.DialogState.WaitingForPayment,
            )
        }

        composeTestRule
            .onAllNodesWithText("Payment not received yet")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
    }

    @Test
    fun `waiting for payment dialog go back click should send GoBackClick action`() {
        mutableStateFlow.update {
            it.copy(
                dialogState =
                    PlanState.DialogState.WaitingForPayment,
            )
        }
        composeTestRule
            .onAllNodesWithText("Go back")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify {
            viewModel.trySendAction(PlanAction.GoBackClick)
        }
    }

    @Test
    fun `waiting for payment dialog close click should send CancelWaiting action`() {
        mutableStateFlow.update {
            it.copy(
                dialogState =
                    PlanState.DialogState.WaitingForPayment,
            )
        }
        composeTestRule
            .onAllNodesWithText("Close")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify {
            viewModel.trySendAction(PlanAction.CancelWaiting)
        }
    }

    // endregion Free content tests

    // region PendingUpgrade dialog tests

    @Test
    fun `pending upgrade dialog should render when dialogState is PendingUpgrade`() {
        composeTestRule
            .onAllNodesWithText("Upgrade pending")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = PlanState.DialogState.PendingUpgrade,
            )
        }

        composeTestRule
            .onAllNodesWithText("Upgrade pending")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
    }

    @Test
    fun `pending upgrade dialog should display sync now button`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = PlanState.DialogState.PendingUpgrade,
            )
        }
        composeTestRule
            .onAllNodesWithText("Sync now")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
    }

    @Test
    fun `pending upgrade dialog should display continue button`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = PlanState.DialogState.PendingUpgrade,
            )
        }
        composeTestRule
            .onAllNodesWithText("Continue")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
    }

    @Test
    fun `pending upgrade dialog sync now click should send SyncClick action`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = PlanState.DialogState.PendingUpgrade,
            )
        }
        composeTestRule
            .onAllNodesWithText("Sync now")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(PlanAction.SyncClick) }
    }

    @Test
    fun `pending upgrade dialog continue click should send ContinueClick action`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = PlanState.DialogState.PendingUpgrade,
            )
        }
        composeTestRule
            .onAllNodesWithText("Continue")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(PlanAction.ContinueClick) }
    }

    // endregion PendingUpgrade dialog tests

    // region GetPricingError dialog tests

    @Test
    fun `get pricing error dialog should render when dialogState is GetPricingError`() {
        val title = "An error has occurred".asText()
        val message = "Unable to retrieve pricing.".asText()

        composeTestRule
            .onAllNodesWithText("An error has occurred")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = PlanState.DialogState.GetPricingError(
                    title = title,
                    message = message,
                ),
            )
        }

        composeTestRule
            .onAllNodesWithText("An error has occurred")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
        composeTestRule
            .onAllNodesWithText("Unable to retrieve pricing.")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
    }

    @Test
    fun `get pricing error dialog try again click should send RetryPricingClick action`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = PlanState.DialogState.GetPricingError(
                    title = "An error has occurred".asText(),
                    message = "Unable to retrieve pricing.".asText(),
                ),
            )
        }
        composeTestRule
            .onAllNodesWithText("Try again")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(PlanAction.RetryPricingClick) }
    }

    @Test
    fun `get pricing error dialog close click should send ClosePricingErrorClick action`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = PlanState.DialogState.GetPricingError(
                    title = "An error has occurred".asText(),
                    message = "Unable to retrieve pricing.".asText(),
                ),
            )
        }
        composeTestRule
            .onAllNodesWithText("Close")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify {
            viewModel.trySendAction(PlanAction.ClosePricingErrorClick)
        }
    }

    // endregion GetPricingError dialog tests

    // region Premium content rendering

    @Test
    fun `premium content should render subscription card when viewState is Premium`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_PREMIUM_VIEW_STATE) }
        composeTestRule
            .onNodeWithTag("BillingAmountRow")
            .assertExists()
    }

    @Test
    fun `premium plan name should render`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_PREMIUM_VIEW_STATE) }
        composeTestRule
            .onNodeWithText("Premium")
            .assertIsDisplayed()
    }

    @Test
    fun `status badge should render with Active label for ACTIVE status`() {
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_PREMIUM_VIEW_STATE.copy(
                    status = PremiumSubscriptionStatus.ACTIVE,
                ),
            )
        }
        composeTestRule
            .onNodeWithText("Active")
            .assertIsDisplayed()
    }

    @Test
    fun `status badge should render with Canceled label for CANCELED status`() {
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_PREMIUM_VIEW_STATE.copy(
                    status = PremiumSubscriptionStatus.CANCELED,
                ),
            )
        }
        composeTestRule
            .onNodeWithText("Canceled")
            .assertIsDisplayed()
    }

    @Test
    fun `status badge should render with Overdue payment label for OVERDUE_PAYMENT status`() {
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_PREMIUM_VIEW_STATE.copy(
                    status = PremiumSubscriptionStatus.OVERDUE_PAYMENT,
                ),
            )
        }
        composeTestRule
            .onNodeWithText("Overdue payment")
            .assertIsDisplayed()
    }

    @Test
    fun `status badge should render with Past due label for PAST_DUE status`() {
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_PREMIUM_VIEW_STATE.copy(
                    status = PremiumSubscriptionStatus.PAST_DUE,
                ),
            )
        }
        composeTestRule
            .onNodeWithText("Past due")
            .assertIsDisplayed()
    }

    @Test
    fun `status badge should render with Paused label for PAUSED status`() {
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_PREMIUM_VIEW_STATE.copy(
                    status = PremiumSubscriptionStatus.PAUSED,
                ),
            )
        }
        composeTestRule
            .onNodeWithText("Paused")
            .assertIsDisplayed()
    }

    @Test
    fun `status badge should not render when status is null`() {
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_PREMIUM_VIEW_STATE.copy(status = null),
            )
        }
        composeTestRule.onNodeWithText("Active").assertDoesNotExist()
        composeTestRule.onNodeWithText("Canceled").assertDoesNotExist()
        composeTestRule.onNodeWithText("Overdue payment").assertDoesNotExist()
        composeTestRule.onNodeWithText("Past due").assertDoesNotExist()
        composeTestRule.onNodeWithText("Paused").assertDoesNotExist()
    }

    @Test
    fun `description text should render when descriptionText is present`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_PREMIUM_VIEW_STATE) }
        composeTestRule
            .onNodeWithText("Your next charge is for $45.55 USD due on April 2, 2026.")
            .assertIsDisplayed()
    }

    @Test
    fun `description text should not render when descriptionText is null`() {
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_PREMIUM_VIEW_STATE.copy(descriptionText = null),
            )
        }
        composeTestRule
            .onNodeWithText("Your next charge is for $45.55 USD due on April 2, 2026.")
            .assertDoesNotExist()
    }

    // endregion Premium content rendering

    // region Line items

    @Test
    fun `billing amount row should display billingAmountText value`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_PREMIUM_VIEW_STATE) }
        composeTestRule
            .onNodeWithTag("BillingAmountRow")
            .assertExists()
        composeTestRule
            .onNodeWithText("$19.80 / year")
            .assertIsDisplayed()
    }

    @Test
    fun `storage cost row should display storageCostText value`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_PREMIUM_VIEW_STATE) }
        composeTestRule
            .onNodeWithTag("StorageCostRow")
            .assertExists()
        composeTestRule
            .onNodeWithText("$24.00")
            .assertIsDisplayed()
    }

    @Test
    fun `discount row should display discountAmountText value`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_PREMIUM_VIEW_STATE) }
        composeTestRule
            .onNodeWithTag("DiscountRow")
            .assertExists()
        composeTestRule
            .onNodeWithText("-$2.10")
            .assertIsDisplayed()
    }

    @Test
    fun `estimated tax row should display estimatedTaxText value`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_PREMIUM_VIEW_STATE) }
        composeTestRule
            .onNodeWithTag("EstimatedTaxRow")
            .assertExists()
        composeTestRule
            .onNodeWithText("$3.85")
            .assertIsDisplayed()
    }

    @Test
    fun `line items should display -- placeholder when values are defaults`() {
        mutableStateFlow.update {
            it.copy(viewState = PlanState.ViewState.Premium())
        }
        // Four rows, each displaying the default placeholder value "--".
        composeTestRule
            .onAllNodesWithText("--")
            .assertCountEquals(4)
    }

    // endregion Line items

    // region Action buttons

    @Test
    fun `manage plan button click should send ManagePlanClick action`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_PREMIUM_VIEW_STATE) }
        composeTestRule
            .onNodeWithTag("ManagePlanButton")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(PlanAction.ManagePlanClick) }
    }

    @Test
    fun `cancel premium button should render when showCancelButton is true`() {
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_PREMIUM_VIEW_STATE.copy(showCancelButton = true),
            )
        }
        composeTestRule
            .onNodeWithTag("CancelPremiumButton")
            .assertExists()
    }

    @Test
    fun `cancel premium button should not render when showCancelButton is false`() {
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_PREMIUM_VIEW_STATE.copy(showCancelButton = false),
            )
        }
        composeTestRule
            .onNodeWithTag("CancelPremiumButton")
            .assertDoesNotExist()
    }

    @Test
    fun `cancel premium button click should send CancelPremiumClick action`() {
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_PREMIUM_VIEW_STATE.copy(showCancelButton = true),
            )
        }
        composeTestRule
            .onNodeWithTag("CancelPremiumButton")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(PlanAction.CancelPremiumClick) }
    }

    // endregion Action buttons

    // region Premium-flow dialogs

    @Test
    fun `subscription error dialog should render when dialogState is SubscriptionError`() {
        val title = "An error has occurred".asText()
        val message = "Unable to load subscription.".asText()

        composeTestRule
            .onAllNodesWithText("An error has occurred")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = PlanState.ViewState.Premium(),
                dialogState = PlanState.DialogState.SubscriptionError(
                    title = title,
                    message = message,
                ),
            )
        }

        composeTestRule
            .onAllNodesWithText("An error has occurred")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
        composeTestRule
            .onAllNodesWithText("Unable to load subscription.")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
        composeTestRule
            .onAllNodesWithText("Try again")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
        composeTestRule
            .onAllNodesWithText("Close")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
    }

    @Test
    fun `subscription error dialog try again click should send RetrySubscriptionClick action`() {
        mutableStateFlow.update {
            it.copy(
                viewState = PlanState.ViewState.Premium(),
                dialogState = PlanState.DialogState.SubscriptionError(
                    title = "An error has occurred".asText(),
                    message = "Unable to load subscription.".asText(),
                ),
            )
        }
        composeTestRule
            .onAllNodesWithText("Try again")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(PlanAction.RetrySubscriptionClick) }
    }

    @Test
    fun `subscription error dialog close click should send BackClick action`() {
        mutableStateFlow.update {
            it.copy(
                viewState = PlanState.ViewState.Premium(),
                dialogState = PlanState.DialogState.SubscriptionError(
                    title = "An error has occurred".asText(),
                    message = "Unable to load subscription.".asText(),
                ),
            )
        }
        composeTestRule
            .onAllNodesWithText("Close")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(PlanAction.BackClick) }
    }

    @Test
    fun `loading portal dialog should render when dialogState is LoadingPortal`() {
        composeTestRule
            .onAllNodesWithText("Loading portal…")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_PREMIUM_VIEW_STATE,
                dialogState = PlanState.DialogState.LoadingPortal,
            )
        }

        composeTestRule
            .onAllNodesWithText("Loading portal…")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
    }

    @Test
    fun `portal error dialog should render when dialogState is PortalError`() {
        composeTestRule
            .onAllNodesWithText("Something went wrong")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_PREMIUM_VIEW_STATE,
                dialogState = PlanState.DialogState.PortalError,
            )
        }

        composeTestRule
            .onAllNodesWithText("Something went wrong")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
        composeTestRule
            .onAllNodesWithText(
                "We had trouble loading the management portal, so try again.",
            )
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
        composeTestRule
            .onAllNodesWithText("Try again")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
        composeTestRule
            .onAllNodesWithText("Close")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
    }

    @Test
    fun `portal error dialog dismiss click should send DismissPortalError action`() {
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_PREMIUM_VIEW_STATE,
                dialogState = PlanState.DialogState.PortalError,
            )
        }
        composeTestRule
            .onAllNodesWithText("Close")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(PlanAction.DismissPortalError) }
    }

    @Test
    fun `cancel confirmation dialog should render when dialogState is CancelConfirmation`() {
        composeTestRule
            .onAllNodesWithText("Cancel Premium")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_PREMIUM_VIEW_STATE,
                dialogState = PlanState.DialogState.CancelConfirmation(
                    nextRenewalDate = "April 2, 2026",
                ),
            )
        }

        composeTestRule
            .onAllNodesWithText("Cancel Premium")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
        composeTestRule
            .onAllNodesWithText(
                "You’ll continue to have Premium access until April 2, 2026.",
            )
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
    }

    @Test
    fun `cancel confirmation dialog confirm click should send ConfirmCancelClick action`() {
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_PREMIUM_VIEW_STATE,
                dialogState = PlanState.DialogState.CancelConfirmation(
                    nextRenewalDate = "April 2, 2026",
                ),
            )
        }
        composeTestRule
            .onAllNodesWithText("Cancel now")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(PlanAction.ConfirmCancelClick) }
    }

    @Test
    fun `cancel confirmation dialog dismiss click should send DismissCancelConfirmation action`() {
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_PREMIUM_VIEW_STATE,
                dialogState = PlanState.DialogState.CancelConfirmation(
                    nextRenewalDate = "April 2, 2026",
                ),
            )
        }
        composeTestRule
            .onAllNodesWithText("Close")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(PlanAction.DismissCancelConfirmation) }
    }

    // endregion Premium-flow dialogs

    // region LaunchPortal event

    @Test
    fun `LaunchPortal event should call intentManager launchUri`() {
        val url = "https://portal"
        mutableEventFlow.tryEmit(PlanEvent.LaunchPortal(url = url))
        verify { intentManager.launchUri(url.toUri()) }
    }

    // endregion LaunchPortal event
}

private val DEFAULT_FREE_STATE = PlanState(
    planMode = PlanMode.Modal,
    viewState = PlanState.ViewState.Free(
        rate = "$1.65",
        checkoutUrl = null,
        isAwaitingPremiumStatus = false,
    ),
    dialogState = null,
)

private val DEFAULT_PREMIUM_VIEW_STATE = PlanState.ViewState.Premium(
    status = PremiumSubscriptionStatus.ACTIVE,
    descriptionText = BitwardenString.premium_next_charge_summary.asText(
        "$45.55",
        "April 2, 2026",
    ),
    billingAmountText = BitwardenString.billing_rate_per_year.asText("$19.80"),
    storageCostText = "$24.00",
    discountAmountText = "-$2.10",
    estimatedTaxText = "$3.85",
    nextChargeDateText = "April 2, 2026",
    showCancelButton = true,
)
