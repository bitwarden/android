package com.x8bit.bitwarden.ui.vault.feature.exportitems.verifypassword

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.network.model.OrganizationType
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.vault.feature.exportitems.model.AccountSelectionListItem
import com.x8bit.bitwarden.ui.vault.feature.vault.util.initials
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VerifyPasswordScreenTest : BitwardenComposeTest() {

    private var onNavigateBackClicked: Boolean = false
    private var onPasswordVerifiedClicked: Boolean = false
    private val onPasswordVerifiedArgSlot = mutableListOf<String>()

    private val mockStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mockEventFlow = bufferedMutableSharedFlow<VerifyPasswordEvent>()
    private val viewModel = mockk<VerifyPasswordViewModel> {
        every { stateFlow } returns mockStateFlow
        every { eventFlow } returns mockEventFlow
        every { trySendAction(any()) } just runs
    }

    @Before
    fun verifyPasswordScreen() {
        setContent {
            VerifyPasswordScreen(
                onNavigateBack = { onNavigateBackClicked = true },
                onPasswordVerified = { userId ->
                    onPasswordVerifiedClicked = true
                    onPasswordVerifiedArgSlot.add(userId)
                },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `initial state should be correct`() = runTest {
        composeTestRule
            .onNodeWithText("Verify your master password")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("AU")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("active@bitwarden.com")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Continue")
            .assertIsNotEnabled()
    }

    @Test
    fun `otp state should be correct`() = runTest {
        mockStateFlow.emit(
            DEFAULT_STATE.copy(
                title = BitwardenString.verify_your_account_email_address.asText(),
                subtext = BitwardenString
                    .enter_the_6_digit_code_that_was_emailed_to_the_address_below
                    .asText(),
                showResendCodeButton = true,
            ),
        )

        composeTestRule
            .onNodeWithText("Verify your account email address")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Enter the 6-digit code that was emailed to the address below")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Resend code")
            .assertIsDisplayed()
    }

    @Test
    fun `input change should send PasswordInputChangeReceive action`() = runTest {
        composeTestRule
            .onNodeWithText("Master password")
            .performTextInput("abc123")
        verify {
            viewModel.trySendAction(
                VerifyPasswordAction.PasswordInputChangeReceive("abc123"),
            )
        }
    }

    @Test
    fun `Continue button should should update based on input`() = runTest {
        composeTestRule
            .onNodeWithText("Continue")
            .assertIsNotEnabled()

        mockStateFlow.emit(DEFAULT_STATE.copy(input = "abc123"))

        composeTestRule
            .onNodeWithText("Continue")
            .assertIsEnabled()
    }

    @Test
    fun `Continue button should send ContinueClick action`() = runTest {
        mockStateFlow.emit(DEFAULT_STATE.copy(input = "abc123"))
        composeTestRule
            .onNodeWithText("Continue")
            .performClick()
        verify {
            viewModel.trySendAction(VerifyPasswordAction.ContinueClick)
        }
    }

    @Test
    fun `Resend code button should send SendCodeClick action`() = runTest {
        mockStateFlow.emit(DEFAULT_STATE.copy(showResendCodeButton = true))
        composeTestRule
            .onNodeWithText("Resend code")
            .performClick()
        verify {
            viewModel.trySendAction(VerifyPasswordAction.ResendCodeClick)
        }
    }

    @Test
    fun `back click should send NavigateBackClick action`() = runTest {
        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()
        verify {
            viewModel.trySendAction(VerifyPasswordAction.NavigateBackClick)
        }
    }

    @Test
    fun `NavigateBack event should trigger onNavigateBack`() = runTest {
        mockEventFlow.emit(VerifyPasswordEvent.NavigateBack)
        assertTrue(onNavigateBackClicked)
    }

    @Test
    fun `PasswordVerified event should call onPasswordVerified with userId`() = runTest {
        mockEventFlow.emit(VerifyPasswordEvent.PasswordVerified(DEFAULT_USER_ID))
        assertTrue(onPasswordVerifiedClicked)
        assertEquals(1, onPasswordVerifiedArgSlot.size)
        assertEquals(DEFAULT_USER_ID, onPasswordVerifiedArgSlot.first())
    }

    @Test
    fun `General dialog should display based on state`() = runTest {
        mockStateFlow.emit(
            DEFAULT_STATE.copy(
                dialog = VerifyPasswordState.DialogState.General(
                    title = "title".asText(),
                    message = "message".asText(),
                    error = null,
                ),
            ),
        )
        composeTestRule
            .onAllNodesWithText("title")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `General dialog dismiss should send DismissDialog action`() = runTest {
        mockStateFlow.emit(
            DEFAULT_STATE.copy(
                dialog = VerifyPasswordState.DialogState.General(
                    title = "title".asText(),
                    message = "message".asText(),
                    error = null,
                ),
            ),
        )
        composeTestRule
            .onAllNodesWithText("Okay")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify {
            viewModel.trySendAction(VerifyPasswordAction.DismissDialog)
        }
    }

    @Test
    fun `Loading dialog should display based on state`() = runTest {
        mockStateFlow.emit(
            DEFAULT_STATE.copy(
                dialog = VerifyPasswordState.DialogState.Loading("message".asText()),
            ),
        )
        composeTestRule
            .onAllNodesWithText("message")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }
}

private const val DEFAULT_USER_ID: String = "activeUserId"
private const val DEFAULT_ORGANIZATION_ID: String = "activeOrganizationId"
private val DEFAULT_USER_STATE = UserState(
    activeUserId = DEFAULT_USER_ID,
    accounts = listOf(
        UserState.Account(
            userId = "activeUserId",
            name = "Active User",
            email = "active@bitwarden.com",
            avatarColorHex = "#aa00aa",
            environment = Environment.Us,
            isPremium = true,
            isLoggedIn = true,
            isVaultUnlocked = true,
            needsPasswordReset = false,
            isBiometricsEnabled = false,
            organizations = listOf(
                Organization(
                    id = DEFAULT_ORGANIZATION_ID,
                    name = "Organization User",
                    shouldUseKeyConnector = false,
                    shouldManageResetPassword = false,
                    role = OrganizationType.USER,
                    keyConnectorUrl = null,
                    userIsClaimedByOrganization = false,
                ),
            ),
            needsMasterPassword = false,
            trustedDevice = null,
            hasMasterPassword = true,
            isUsingKeyConnector = false,
            onboardingStatus = OnboardingStatus.COMPLETE,
            firstTimeState = FirstTimeState(showImportLoginsCard = true),
            isExportable = true,
        ),
    ),
)
private val DEFAULT_ACCOUNT_SELECTION_LIST_ITEM = AccountSelectionListItem(
    userId = DEFAULT_USER_ID,
    email = DEFAULT_USER_STATE.activeAccount.email,
    avatarColorHex = DEFAULT_USER_STATE.activeAccount.avatarColorHex,
    isItemRestricted = false,
    initials = DEFAULT_USER_STATE.activeAccount.initials,
)
private val DEFAULT_STATE = VerifyPasswordState(
    title = BitwardenString.verify_your_master_password.asText(),
    subtext = null,
    accountSummaryListItem = DEFAULT_ACCOUNT_SELECTION_LIST_ITEM,
    input = "",
    dialog = null,
    hasOtherAccounts = true,
)
