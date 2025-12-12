package com.x8bit.bitwarden.ui.vault.feature.migratetomyitems

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.net.toUri
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue

class MigrateToMyItemsScreenTest : BitwardenComposeTest() {
    private var onNavigateToVaultCalled = false
    private var onNavigateToLeaveOrganizationCalled = false

    private val intentManager: IntentManager = mockk {
        every { launchUri(any()) } just runs
    }

    private val mutableEventFlow = bufferedMutableSharedFlow<MigrateToMyItemsEvent>()

    private val mutableStateFlow = MutableStateFlow(
        MigrateToMyItemsState(
            organizationId = "test-org-id",
            organizationName = "Test Organization",
            dialog = null,
        ),
    )

    private val viewModel = mockk<MigrateToMyItemsViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        setContent(intentManager = intentManager) {
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
    fun `Accept button click should send AcceptClicked action`() {
        composeTestRule.onNodeWithText("Accept").performClick()

        verify {
            viewModel.trySendAction(MigrateToMyItemsAction.AcceptClicked)
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

    @Test
    fun `LaunchUri event should launch URI via intent manager`() {
        val testUri = "https://bitwarden.com/help/transfer-ownership/"
        mutableEventFlow.tryEmit(MigrateToMyItemsEvent.LaunchUri(testUri))
        verify {
            intentManager.launchUri(testUri.toUri())
        }
    }

    @Test
    fun `Loading dialog should display when dialog state is Loading`() {
        mutableStateFlow.value = MigrateToMyItemsState(
            organizationId = "test-org-id",
            organizationName = "Test Organization",
            dialog = MigrateToMyItemsState.DialogState.Loading(
                message = "Migrating items to Test Organization...".asText(),
            ),
        )

        composeTestRule
            .onNodeWithText("Migrating items to Test Organization...")
            .assertIsDisplayed()
    }

    @Test
    fun `Error dialog should display when dialog state is Error`() {
        mutableStateFlow.value = MigrateToMyItemsState(
            organizationId = "test-org-id",
            organizationName = "Test Organization",
            dialog = MigrateToMyItemsState.DialogState.Error(
                title = "An error has occurred".asText(),
                message = "Failed to migrate items".asText(),
            ),
        )

        composeTestRule
            .onNodeWithText("An error has occurred")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Failed to migrate items")
            .assertIsDisplayed()
    }

    @Test
    fun `Error dialog dismiss should send DismissDialogClicked action`() {
        mutableStateFlow.value = MigrateToMyItemsState(
            organizationId = "test-org-id",
            organizationName = "Test Organization",
            dialog = MigrateToMyItemsState.DialogState.Error(
                title = BitwardenString.an_error_has_occurred.asText(),
                message = "Failed to migrate items".asText(),
            ),
        )

        composeTestRule
            .onNodeWithText("Okay")
            .performClick()

        verify {
            viewModel.trySendAction(MigrateToMyItemsAction.DismissDialogClicked)
        }
    }
}
