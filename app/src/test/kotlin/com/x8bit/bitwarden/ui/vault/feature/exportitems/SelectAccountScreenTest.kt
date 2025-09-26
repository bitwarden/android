package com.x8bit.bitwarden.ui.vault.feature.exportitems

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.cxf.manager.CredentialExchangeCompletionManager
import com.bitwarden.cxf.manager.model.ExportCredentialsResult
import com.bitwarden.ui.platform.components.account.model.AccountSummary
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.vault.feature.exportitems.model.AccountSelectionListItem
import com.x8bit.bitwarden.ui.vault.feature.exportitems.selectaccount.SelectAccountAction
import com.x8bit.bitwarden.ui.vault.feature.exportitems.selectaccount.SelectAccountEvent
import com.x8bit.bitwarden.ui.vault.feature.exportitems.selectaccount.SelectAccountScreen
import com.x8bit.bitwarden.ui.vault.feature.exportitems.selectaccount.SelectAccountState
import com.x8bit.bitwarden.ui.vault.feature.exportitems.selectaccount.SelectAccountViewModel
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SelectAccountScreenTest : BitwardenComposeTest() {
    private var onAccountSelectedCalled: Boolean = false
    private val mockkStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mockEventFlow = bufferedMutableSharedFlow<SelectAccountEvent>()

    private val credentialExchangeCompletionManager = mockk<CredentialExchangeCompletionManager>()
    private val viewModel = mockk<SelectAccountViewModel> {
        every { eventFlow } returns mockEventFlow
        every { stateFlow } returns mockkStateFlow
        every { trySendAction(any()) } just runs
    }

    @Before
    fun setUp() {
        setContent(
            credentialExchangeCompletionManager = credentialExchangeCompletionManager,
        ) {
            SelectAccountScreen(
                onAccountSelected = { onAccountSelectedCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `initial state should be correct`() = runTest {
        composeTestRule
            .onNodeWithText("Select account")
            .isDisplayed()

        composeTestRule
            .onNodeWithText(ACTIVE_ACCOUNT_SUMMARY.email)
            .isDisplayed()
    }

    @Test
    fun `close click should send CloseClick action`() = runTest {
        composeTestRule
            .onNodeWithContentDescription("Close")
            .performClick()

        verify {
            viewModel.trySendAction(SelectAccountAction.CloseClick)
        }
    }

    @Test
    fun `CancelExport event should complete credential exchange with cancellation error`() =
        runTest {
            val exportResultSlot = slot<ExportCredentialsResult>()
            every {
                credentialExchangeCompletionManager.completeCredentialExport(
                    exportResult = capture(exportResultSlot),
                )
            } just runs

            mockEventFlow.emit(SelectAccountEvent.CancelExport)

            verify {
                credentialExchangeCompletionManager.completeCredentialExport(
                    exportResult = exportResultSlot.captured,
                )
            }
            assertTrue(exportResultSlot.captured is ExportCredentialsResult.Failure)
            // TODO: [PM-26094] Uncomment check to verify error is
            //  ImportCredentialsCancellationException when it is public.
            // val result = exportResultSlot.captured as ExportCredentialsResult.Failure
            // assertTrue(result.error is ImportCredentialsCancellationException)
        }

    @Test
    fun `account list item click should send onAccountClick action`() = runTest {
        composeTestRule
            .onNodeWithText(ACTIVE_ACCOUNT_SUMMARY.email)
            .performClick()

        verify {
            viewModel.trySendAction(
                SelectAccountAction.AccountClick(
                    userId = ACTIVE_ACCOUNT_SUMMARY.userId,
                ),
            )
        }
    }

    @Test
    fun `NavigateToPasswordVerification event should navigate to password verification`() =
        runTest {
            mockEventFlow.emit(
                SelectAccountEvent.NavigateToPasswordVerification(
                    userId = ACTIVE_ACCOUNT_SUMMARY.userId,
                ),
            )

            assertTrue(onAccountSelectedCalled)
        }
}

private val ACTIVE_ACCOUNT_SUMMARY = AccountSummary(
    userId = "activeUserId",
    name = "Active User",
    email = "active@bitwarden.com",
    avatarColorHex = "#aa00aa",
    environmentLabel = "bitwarden.com",
    isActive = true,
    isLoggedIn = true,
    isVaultUnlocked = true,
)
private val LOCKED_ACCOUNT_SUMMARY = AccountSummary(
    userId = "lockedUserId",
    name = "Locked User",
    email = "locked@bitwarden.com",
    avatarColorHex = "#00aaaa",
    environmentLabel = "bitwarden.com",
    isActive = false,
    isLoggedIn = true,
    isVaultUnlocked = false,
)

private val DEFAULT_STATE = SelectAccountState(
    accountSelectionListItems = persistentListOf(
        AccountSelectionListItem(
            userId = ACTIVE_ACCOUNT_SUMMARY.userId,
            email = ACTIVE_ACCOUNT_SUMMARY.email,
            initials = "AA",
            avatarColorHex = "#FFFF0000",
            isItemRestricted = false,
        ),
        AccountSelectionListItem(
            userId = LOCKED_ACCOUNT_SUMMARY.userId,
            email = LOCKED_ACCOUNT_SUMMARY.email,
            initials = "LU",
            avatarColorHex = "#FF00FF00",
            isItemRestricted = false,
        ),
    ),
)
