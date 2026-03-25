package com.x8bit.bitwarden.ui.platform.feature.premium.plan

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.manager.intent.model.AuthTabData
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
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
    fun `upgrade now button should be enabled when dialogState is null`() {
        composeTestRule
            .onNodeWithTag("UpgradeNowButton")
            .assertIsEnabled()
    }

    @Test
    fun `upgrade now button should be disabled when dialogState is Loading`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = PlanState.DialogState.Loading(
                    message = BitwardenString.opening_checkout.asText(),
                ),
            )
        }
        composeTestRule
            .onNodeWithTag("UpgradeNowButton")
            .assertIsNotEnabled()
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
}

private val DEFAULT_FREE_STATE = PlanState(
    planMode = PlanMode.Modal,
    viewState = PlanState.ViewState.Free(
        rate = "$1.65",
    ),
    dialogState = null,
)
