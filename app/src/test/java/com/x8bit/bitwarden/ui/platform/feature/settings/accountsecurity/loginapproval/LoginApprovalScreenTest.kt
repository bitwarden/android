package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.loginapproval

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue

class LoginApprovalScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false

    private val mutableEventFlow = bufferedMutableSharedFlow<LoginApprovalEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<LoginApprovalViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            LoginApprovalScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `on NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(LoginApprovalEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
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
        mutableStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                shouldShowErrorDialog = true,
            ),
        )

        composeTestRule
            .onNodeWithText("An error has occurred.")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Ok")
            .performClick()

        verify {
            viewModel.trySendAction(LoginApprovalAction.ErrorDialogDismiss)
        }
    }
}

private const val FINGERPRINT = "fingerprint"
private val DEFAULT_STATE: LoginApprovalState = LoginApprovalState(
    fingerprint = FINGERPRINT,
    masterPasswordHash = null,
    publicKey = "publicKey",
    requestId = "",
    shouldShowErrorDialog = false,
    viewState = LoginApprovalState.ViewState.Content(
        deviceType = "Android",
        domainUrl = "bitwarden.com",
        email = "test@bitwarden.com",
        fingerprint = FINGERPRINT,
        ipAddress = "1.0.0.1",
        time = "now",
    ),
)
