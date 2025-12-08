package com.x8bit.bitwarden.ui.vault.feature.migratetomyitems

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue

class MigrateToMyItemsScreenTest : BitwardenComposeTest() {
    private var onNavigateToVaultCalled = false
    private var onNavigateToLeaveOrganizationCalled = false

    private val mutableEventFlow = bufferedMutableSharedFlow<MigrateToMyItemsEvent>()

    private val mutableStateFlow = MutableStateFlow(
        MigrateToMyItemsState(
            organizationName = "Test Organization",
        ),
    )

    private val viewModel = mockk<MigrateToMyItemsViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        setContent {
            MigrateToMyItemsScreen(
                viewModel = viewModel,
                onNavigateToVault = { onNavigateToVaultCalled = true },
                onNavigateToLeaveOrganization = { onNavigateToLeaveOrganizationCalled = true },
            )
        }
    }

    @Test
    fun `title should display with organization name`() {
        composeTestRule
            .onNodeWithText("Transfer items to Test Organization")
            .assertIsDisplayed()
    }

    @Test
    fun `description text should be displayed`() {
        composeTestRule
            .onNodeWithText(
                "Test Organization is requiring all items to be owned by the " +
                    "organization for security and compliance. Click accept to transfer " +
                    "ownership of your items.",
                substring = true,
            )
            .assertIsDisplayed()
    }

    @Test
    fun `Continue button click should send ContinueClicked action`() {
        composeTestRule.onNodeWithText("Continue").performClick()

        verify {
            viewModel.trySendAction(MigrateToMyItemsAction.ContinueClicked)
        }
    }

    @Test
    fun `Decline and leave button click should send DeclineAndLeaveClicked action`() {
        composeTestRule.onNodeWithText("Decline and leave").performClick()

        verify {
            viewModel.trySendAction(MigrateToMyItemsAction.DeclineAndLeaveClicked)
        }
    }

    @Test
    fun `Why am I seeing this link click should send HelpLinkClicked action`() {
        composeTestRule.onNodeWithText("Why am I seeing this?").performClick()

        verify {
            viewModel.trySendAction(MigrateToMyItemsAction.HelpLinkClicked)
        }
    }

    @Test
    fun `NavigateToVault event should trigger navigation callback`() {
        mutableEventFlow.tryEmit(MigrateToMyItemsEvent.NavigateToVault)
        assertTrue(onNavigateToVaultCalled)
    }

    @Test
    fun `NavigateToLeaveOrganization event should trigger navigation callback`() {
        mutableEventFlow.tryEmit(MigrateToMyItemsEvent.NavigateToLeaveOrganization)
        assertTrue(onNavigateToLeaveOrganizationCalled)
    }
}
