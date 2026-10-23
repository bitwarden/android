package com.x8bit.bitwarden.ui.vault.feature.pamapprovals

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.manager.util.startAppSettingsActivity
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.x8bit.bitwarden.data.pam.repository.model.PamRequestStatus
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.manager.permissions.FakePermissionManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class PamApprovalsScreenTest : BitwardenComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToDetailId: String? = null
    private val mutableEventFlow = bufferedMutableSharedFlow<PamApprovalsEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<PamApprovalsViewModel> {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
        every { trySendAction(any()) } just runs
    }
    private val intentManager = mockk<IntentManager>()
    private val permissionsManager = FakePermissionManager()

    @Before
    fun setUp() {
        setContent(
            intentManager = intentManager,
            permissionsManager = permissionsManager,
        ) {
            PamApprovalsScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToDetail = { onNavigateToDetailId = it },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `back click should send BackClick`() {
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify { viewModel.trySendAction(PamApprovalsAction.BackClick) }
    }

    @Test
    fun `NavigateBack event should call onNavigateBack`() {
        mutableEventFlow.tryEmit(PamApprovalsEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `NavigateToDetail event should call onNavigateToDetail`() {
        mutableEventFlow.tryEmit(PamApprovalsEvent.NavigateToDetail(requestId = "abc"))
        assertEquals("abc", onNavigateToDetailId)
    }

    @Test
    fun `tab clicks should send TabClick`() {
        composeTestRule.onNodeWithText("History").performClick()
        verify {
            viewModel.trySendAction(PamApprovalsAction.TabClick(PamApprovalsState.Tab.HISTORY))
        }
        composeTestRule.onNodeWithText("Inbox").performClick()
        verify {
            viewModel.trySendAction(PamApprovalsAction.TabClick(PamApprovalsState.Tab.INBOX))
        }
    }

    @Test
    fun `empty inbox should show inbox zero`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = PamApprovalsState.ViewState.Content(
                inbox = emptyList(),
                history = emptyList(),
            ),
        )

        composeTestRule.onNodeWithText("You’re all caught up").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("There are no requests waiting for your decision.")
            .assertIsDisplayed()
    }

    @Test
    fun `empty history should show the history empty message`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = PamApprovalsState.ViewState.Content(
                inbox = emptyList(),
                history = emptyList(),
            ),
            selectedTab = PamApprovalsState.Tab.HISTORY,
        )

        composeTestRule.onNodeWithText("No decisions yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("You’re all caught up").assertDoesNotExist()
    }

    @Test
    fun `error state should show the load error`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = PamApprovalsState.ViewState.Error,
        )

        composeTestRule.onNodeWithText("Could not load access requests").assertIsDisplayed()
    }

    @Test
    fun `inbox request should show the item, collection and requester and be clickable`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = PamApprovalsState.ViewState.Content(
                inbox = listOf(createItem()),
                history = emptyList(),
            ),
        )

        composeTestRule.onNodeWithText("Prod DB").assertIsDisplayed()
        composeTestRule.onNodeWithText("Production").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grace").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pending").assertIsDisplayed()
        composeTestRule.onNodeWithTag("PamApprovalCard").performClick()
        verify { viewModel.trySendAction(PamApprovalsAction.RequestClick("requestId")) }
    }

    @Test
    fun `unknown names should fall back to neutral labels`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = PamApprovalsState.ViewState.Content(
                inbox = listOf(createItem(itemName = null, requesterName = null)),
                history = emptyList(),
            ),
        )

        composeTestRule.onNodeWithText("Privileged item").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unknown requester").assertIsDisplayed()
    }

    @Test
    fun `own request should say it cannot be approved`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = PamApprovalsState.ViewState.Content(
                inbox = listOf(createItem(isOwnRequest = true)),
                history = emptyList(),
            ),
        )

        composeTestRule.onNodeWithText("You cannot approve your own request.").assertIsDisplayed()
    }

    @Test
    fun `history request should show its status`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = PamApprovalsState.ViewState.Content(
                inbox = emptyList(),
                history = listOf(createItem(status = PamRequestStatus.DENIED)),
            ),
            selectedTab = PamApprovalsState.Tab.HISTORY,
        )

        composeTestRule.onNodeWithText("Denied").assertIsDisplayed()
    }

    @Test
    fun `notification card should show on the inbox when the prompt is due`() {
        mutableStateFlow.value = PROMPT_STATE

        composeTestRule.onNodeWithTag("PamNotificationActionCard").assertIsDisplayed()
        composeTestRule.onNodeWithText("Get notified about access requests").assertIsDisplayed()
    }

    @Test
    fun `notification card should be hidden on the history tab`() {
        mutableStateFlow.value = PROMPT_STATE.copy(selectedTab = PamApprovalsState.Tab.HISTORY)

        composeTestRule
            .onAllNodesWithText("Get notified about access requests")
            .assertCountEquals(0)
    }

    @Test
    fun `notification card should be hidden when the prompt is not due`() {
        mutableStateFlow.value = PROMPT_STATE.copy(showNotificationPrompt = false)

        composeTestRule
            .onAllNodesWithText("Get notified about access requests")
            .assertCountEquals(0)
    }

    @Test
    fun `notification card dismiss should send NotificationPromptDismiss`() {
        mutableStateFlow.value = PROMPT_STATE

        composeTestRule.onNodeWithContentDescription("Close").performClick()

        verify { viewModel.trySendAction(PamApprovalsAction.NotificationPromptDismiss) }
    }

    @Test
    fun `granting the permission should send NotificationPromptDismiss and hide the card`() {
        permissionsManager.getPermissionsResult = true
        mutableStateFlow.value = PROMPT_STATE

        composeTestRule.onNodeWithTag("PamEnableNotificationsButton").performClick()

        verify { viewModel.trySendAction(PamApprovalsAction.NotificationPromptDismiss) }
        composeTestRule
            .onAllNodesWithText("Get notified about access requests")
            .assertCountEquals(0)
    }

    @Test
    fun `first denial should keep the card and not show the settings dialog`() {
        permissionsManager.getPermissionsResult = false
        permissionsManager.shouldShowRequestRationale = true
        mutableStateFlow.value = PROMPT_STATE

        composeTestRule.onNodeWithTag("PamEnableNotificationsButton").performClick()

        composeTestRule.onAllNodesWithText("Notifications are off").assertCountEquals(0)
        composeTestRule.onNodeWithTag("PamNotificationActionCard").assertIsDisplayed()
    }

    @Test
    fun `permanent denial should offer to open app settings`() {
        permissionsManager.getPermissionsResult = false
        permissionsManager.shouldShowRequestRationale = false
        mutableStateFlow.value = PROMPT_STATE

        composeTestRule.onNodeWithTag("PamEnableNotificationsButton").performClick()
        composeTestRule.onNodeWithText("Notifications are off").assertIsDisplayed()

        mockkStatic(IntentManager::startAppSettingsActivity) {
            every { intentManager.startAppSettingsActivity() } returns true
            composeTestRule.onNodeWithText("Go to settings").performClick()
            verify(exactly = 1) { intentManager.startAppSettingsActivity() }
        }
        composeTestRule.onAllNodesWithText("Notifications are off").assertCountEquals(0)
    }
}

private val DEFAULT_STATE = PamApprovalsState(
    viewState = PamApprovalsState.ViewState.Loading,
    selectedTab = PamApprovalsState.Tab.INBOX,
    isRefreshing = false,
    showNotificationPrompt = false,
)

private val PROMPT_STATE = DEFAULT_STATE.copy(
    viewState = PamApprovalsState.ViewState.Content(inbox = emptyList(), history = emptyList()),
    showNotificationPrompt = true,
)

private fun createItem(
    itemName: String? = "Prod DB",
    requesterName: String? = "Grace",
    status: PamRequestStatus = PamRequestStatus.PENDING,
    isOwnRequest: Boolean = false,
): PamApprovalsState.RequestItem = PamApprovalsState.RequestItem(
    id = "requestId",
    itemName = itemName,
    itemIcon = IconData.Local(iconRes = BitwardenDrawable.ic_globe),
    collectionName = "Production",
    requesterName = requesterName,
    status = status,
    windowStart = Instant.parse("2026-01-01T12:00:00Z"),
    windowEnd = Instant.parse("2026-01-01T13:30:00Z"),
    submittedAt = Instant.parse("2026-01-01T11:00:00Z"),
    isOwnRequest = isOwnRequest,
)
