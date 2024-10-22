package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.loginapproval

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.manager.exit.ExitManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue

class LoginApprovalScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false

    private val exitManager: ExitManager = mockk {
        every { exitApplication() } just runs
    }
    private val mutableEventFlow = bufferedMutableSharedFlow<LoginApprovalEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<LoginApprovalViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        setContentWithBackDispatcher {
            LoginApprovalScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                viewModel = viewModel,
                exitManager = exitManager,
            )
        }
    }

    @Test
    fun `on NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(LoginApprovalEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `system back should send CloseClick`() {
        backDispatcher?.onBackPressed()
        verify {
            viewModel.trySendAction(LoginApprovalAction.CloseClick)
        }
    }

    @Test
    fun `on ExitApp should call exit appliction`() {
        mutableEventFlow.tryEmit(LoginApprovalEvent.ExitApp)
        verify(exactly = 1) {
            exitManager.exitApplication()
        }
    }

    @Test
    fun `on Confirm login should send ApproveRequestClick`() = runTest {
        composeTestRule
            .onNodeWithText("Confirm login")
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(LoginApprovalAction.ApproveRequestClick)
        }
    }

    @Test
    fun `on Deny login should send DeclineRequestClick`() = runTest {
        composeTestRule
            .onNodeWithText("Deny login")
            .performScrollTo()
            .performClick()
        verify {
            viewModel.trySendAction(LoginApprovalAction.DeclineRequestClick)
        }
    }

    @Test
    fun `on error dialog dismiss click should send ErrorDialogDismiss`() = runTest {
        val title = "An error has occurred."
        val message = "We were unable to process your request."
        mutableStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                dialogState = LoginApprovalState.DialogState.Error(
                    title = title.asText(),
                    message = message.asText(),
                ),
            ),
        )

        composeTestRule
            .onNodeWithText(text = title)
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = message)
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Ok")
            .performClick()

        verify {
            viewModel.trySendAction(LoginApprovalAction.ErrorDialogDismiss)
        }
    }

    @Test
    fun `on change account dialog confirm click should send ApproveAccountChangeClick`() = runTest {
        val message = "We were unable to process your request."
        mutableStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                dialogState = LoginApprovalState.DialogState.ChangeAccount(
                    message = message.asText(),
                ),
            ),
        )

        composeTestRule
            .onAllNodesWithText(text = "Login requested")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = message)
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(text = "Ok")
            .performClick()

        verify {
            viewModel.trySendAction(LoginApprovalAction.ApproveAccountChangeClick)
        }
    }

    @Test
    fun `on change account dialog dismiss click should send CancelAccountChangeClick`() = runTest {
        val message = "We were unable to process your request."
        mutableStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                dialogState = LoginApprovalState.DialogState.ChangeAccount(
                    message = message.asText(),
                ),
            ),
        )

        composeTestRule
            .onAllNodesWithText(text = "Login requested")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = message)
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(text = "Cancel")
            .performClick()

        verify {
            viewModel.trySendAction(LoginApprovalAction.CancelAccountChangeClick)
        }
    }
}

private const val FINGERPRINT = "fingerprint"
private val DEFAULT_STATE: LoginApprovalState = LoginApprovalState(
    specialCircumstance = null,
    fingerprint = FINGERPRINT,
    masterPasswordHash = null,
    publicKey = "publicKey",
    requestId = "",
    dialogState = null,
    viewState = LoginApprovalState.ViewState.Content(
        deviceType = "Android",
        domainUrl = "bitwarden.com",
        email = "test@bitwarden.com",
        fingerprint = FINGERPRINT,
        ipAddress = "1.0.0.1",
        time = "now",
    ),
)
