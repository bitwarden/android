package com.x8bit.bitwarden.ui.vault.feature.migratetomyitems

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.net.toUri
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.performCustomAccessibilityAction
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
    private var onNavigateToLeaveOrganizationCalled = false

    private val intentManager: IntentManager = mockk {
        every { launchUri(any()) } just runs
    }

    private val mutableEventFlow = bufferedMutableSharedFlow<MigrateToMyItemsEvent>()

    private val mutableStateFlow = MutableStateFlow(
        MigrateToMyItemsState(
            organizationId = "test-org-id",
            organizationName = ORGANIZATION_NAME,
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
                onNavigateToLeaveOrganization = { _, _ ->
                    onNavigateToLeaveOrganizationCalled = true
                },
            )
        }
    }

    @Test
    fun `title should display with organization name`() {
        composeTestRule
            .onNodeWithText("Transfer items to $ORGANIZATION_NAME")
            .assertIsDisplayed()
    }

    @Test
    fun `description text should be displayed`() {
        composeTestRule
            .onNodeWithText(
                "$ORGANIZATION_NAME is requiring all items to be owned by the " +
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
        composeTestRule
            .onNodeWithText(
                "$ORGANIZATION_NAME is requiring all items to be owned by the " +
                    "organization for security and compliance. " +
                    "Click accept to transfer ownership of your items. Learn more",
            )
            .performCustomAccessibilityAction(label = "Learn more, External link")

        verify {
            viewModel.trySendAction(MigrateToMyItemsAction.HelpLinkClicked)
        }
    }

    @Test
    fun `NavigateToLeaveOrganization event should trigger navigation callback`() {
        mutableEventFlow.tryEmit(
            MigrateToMyItemsEvent.NavigateToLeaveOrganization(
                organizationId = "test-org-id",
                organizationName = ORGANIZATION_NAME,
            ),
        )
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
            organizationName = ORGANIZATION_NAME,
            dialog = MigrateToMyItemsState.DialogState.Loading(
                message = "Migrating items to $ORGANIZATION_NAME...".asText(),
            ),
        )

        composeTestRule
            .onNodeWithText("Migrating items to $ORGANIZATION_NAME...")
            .assertIsDisplayed()
    }

    @Test
    fun `Error dialog should display when dialog state is Error`() {
        mutableStateFlow.value = MigrateToMyItemsState(
            organizationId = "test-org-id",
            organizationName = ORGANIZATION_NAME,
            dialog = MigrateToMyItemsState.DialogState.Error(
                title = "An error has occurred".asText(),
                message = "Failed to migrate items".asText(),
                throwable = null,
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
            organizationName = ORGANIZATION_NAME,
            dialog = MigrateToMyItemsState.DialogState.Error(
                title = BitwardenString.an_error_has_occurred.asText(),
                message = "Failed to migrate items".asText(),
                throwable = IllegalStateException("Missing property"),
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

private const val ORGANIZATION_NAME = "Test Organization"
