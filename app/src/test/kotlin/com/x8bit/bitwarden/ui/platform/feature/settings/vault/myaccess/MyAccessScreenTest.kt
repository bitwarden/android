package com.x8bit.bitwarden.ui.platform.feature.settings.vault.myaccess

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.components.bottomsheet.IsBottomSheetKey
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.assertNoDialogExists
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.manager.permissions.FakePermissionManager
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemArgs
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class MyAccessScreenTest : BitwardenComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToVaultItemArgs: VaultItemArgs? = null
    private val mutableEventFlow = bufferedMutableSharedFlow<MyAccessEvent>()
    private val permissionsManager = FakePermissionManager()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<MyAccessViewModel> {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
        every { trySendAction(any()) } just runs
    }

    @Before
    fun setUp() {
        setContent {
            MyAccessScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToVaultItem = { onNavigateToVaultItemArgs = it },
                viewModel = viewModel,
                permissionsManager = permissionsManager,
            )
        }
    }

    @Test
    fun `notification card should show on the current tab when the prompt is due`() {
        mutableStateFlow.value = PROMPT_STATE
        composeTestRule.onNodeWithTag("PamNotificationActionCard").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Get notified about your access requests")
            .assertIsDisplayed()
    }

    @Test
    fun `notification card should be hidden on the history tab`() {
        mutableStateFlow.value = PROMPT_STATE.copy(selectedTab = MyAccessState.Tab.HISTORY)
        composeTestRule.onAllNodesWithTag("PamNotificationActionCard").assertCountEquals(0)
    }

    @Test
    fun `notification card should be hidden when the prompt is not due`() {
        mutableStateFlow.value = PROMPT_STATE.copy(showNotificationPrompt = false)
        composeTestRule.onAllNodesWithTag("PamNotificationActionCard").assertCountEquals(0)
    }

    @Test
    fun `notification card should be hidden once notifications are allowed`() {
        permissionsManager.getPermissionsResult = true
        mutableStateFlow.value = PROMPT_STATE
        composeTestRule.onNodeWithTag("PamEnableNotificationsButton").performClick()
        composeTestRule.onAllNodesWithTag("PamNotificationActionCard").assertCountEquals(0)
    }

    @Test
    fun `notification card dismiss should send NotificationPromptDismiss`() {
        mutableStateFlow.value = PROMPT_STATE
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        verify { viewModel.trySendAction(MyAccessAction.NotificationPromptDismiss) }
    }

    @Test
    fun `granting the permission should send NotificationPromptDismiss`() {
        permissionsManager.getPermissionsResult = true
        mutableStateFlow.value = PROMPT_STATE
        composeTestRule.onNodeWithTag("PamEnableNotificationsButton").performClick()
        verify { viewModel.trySendAction(MyAccessAction.NotificationPromptDismiss) }
    }

    @Test
    fun `denying the permission for good should offer the app settings`() {
        permissionsManager.getPermissionsResult = false
        permissionsManager.shouldShowRequestRationale = false
        mutableStateFlow.value = PROMPT_STATE
        composeTestRule.onNodeWithTag("PamEnableNotificationsButton").performClick()
        composeTestRule.onNodeWithText("Notifications are off").assertIsDisplayed()
    }

    @Test
    fun `back click should send BackClick`() {
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify { viewModel.trySendAction(MyAccessAction.BackClick) }
    }

    @Test
    fun `NavigateBack event should call onNavigateBack`() {
        mutableEventFlow.tryEmit(MyAccessEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `ShowSnackbar event should show the message`() {
        mutableEventFlow.tryEmit(
            MyAccessEvent.ShowSnackbar(BitwardenSnackbarData(message = "Saved".asText())),
        )
        composeTestRule.onNodeWithText("Saved").assertIsDisplayed()
    }

    @Test
    fun `tab clicks should send TabClick`() {
        composeTestRule.onNodeWithText("History").performClick()
        verify { viewModel.trySendAction(MyAccessAction.TabClick(MyAccessState.Tab.HISTORY)) }
    }

    @Test
    fun `empty current tab should show the empty message`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = MyAccessState.ViewState.Content(requests = emptyList()),
        )
        composeTestRule.onNodeWithText("No current access requests").assertIsDisplayed()
        composeTestRule.onNodeWithTag("PamEmptyText").assertIsDisplayed()
    }

    @Test
    fun `empty history tab should show the history empty message`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = MyAccessState.ViewState.Content(requests = emptyList()),
            selectedTab = MyAccessState.Tab.HISTORY,
        )
        composeTestRule.onNodeWithText("No past requests").assertIsDisplayed()
    }

    @Test
    fun `error state should show the generic error`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(viewState = MyAccessState.ViewState.Error)
        composeTestRule.onNodeWithText(GENERIC_ERROR).assertIsDisplayed()
    }

    @Test
    fun `request row click should send RequestClick`() {
        showRequests(createItem(stage = MyAccessState.Stage.PENDING))

        composeTestRule.onNodeWithText("Prod DB").performClick()

        verify { viewModel.trySendAction(MyAccessAction.RequestClick(cipherId = CIPHER_ID)) }
    }

    @Test
    fun `NavigateToVaultItem event should call onNavigateToVaultItem`() {
        val args = VaultItemArgs(vaultItemId = CIPHER_ID, cipherType = VaultItemCipherType.LOGIN)

        mutableEventFlow.tryEmit(MyAccessEvent.NavigateToVaultItem(args = args))

        assertEquals(args, onNavigateToVaultItemArgs)
    }

    @Test
    fun `pending request should show status and a cancel button only`() {
        showRequests(createItem(stage = MyAccessState.Stage.PENDING))

        composeTestRule.onNodeWithText("Prod DB").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pending").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reason: Because").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel request").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start access").assertDoesNotExist()
        composeTestRule.onNodeWithText("End access").assertDoesNotExist()
        composeTestRule.onNodeWithText("Cancel request").performClick()
        verify { viewModel.trySendAction(MyAccessAction.CancelRequestClick(REQUEST_ID)) }
    }

    @Test
    fun `approved request should offer to start access or cancel`() {
        showRequests(createItem(stage = MyAccessState.Stage.READY))

        composeTestRule.onNodeWithText("Approved").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start access").performClick()
        verify { viewModel.trySendAction(MyAccessAction.StartAccessClick(REQUEST_ID)) }
        composeTestRule.onNodeWithText("Cancel request").assertIsDisplayed()
    }

    @Test
    fun `active lease should show the countdown and end access`() {
        showRequests(
            createItem(
                stage = MyAccessState.Stage.ACTIVE,
                leaseId = LEASE_ID,
                leaseEndsAt = Instant.now().plusSeconds(3_600),
            ),
        )

        composeTestRule.onNodeWithText("Active").assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("PamCountdown", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Extend access").assertDoesNotExist()
        composeTestRule.onNodeWithText("End access").performClick()
        verify { viewModel.trySendAction(MyAccessAction.EndAccessClick(LEASE_ID)) }
    }

    @Test
    fun `extend button should only be shown when the lease can be extended`() {
        showRequests(
            createItem(
                stage = MyAccessState.Stage.ACTIVE,
                leaseId = LEASE_ID,
                leaseEndsAt = Instant.now().plusSeconds(3_600),
                canExtend = true,
            ),
        )

        composeTestRule.onNodeWithText("Extend access").performClick()
        verify { viewModel.trySendAction(MyAccessAction.ExtendAccessClick(LEASE_ID)) }
    }

    @Test
    fun `finished requests should show their status without actions`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            selectedTab = MyAccessState.Tab.HISTORY,
            viewState = MyAccessState.ViewState.Content(
                requests = listOf(
                    createItem(id = "1", stage = MyAccessState.Stage.DENIED),
                    createItem(id = "2", stage = MyAccessState.Stage.CANCELLED),
                ),
            ),
        )

        composeTestRule.onNodeWithText("Denied").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancelled").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel request").assertDoesNotExist()
    }

    @Test
    fun `confirm cancel dialog should send confirm and dismiss actions`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            dialogState = MyAccessState.DialogState.ConfirmCancel(REQUEST_ID),
        )

        composeTestRule
            .onNodeWithText("Yes")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(MyAccessAction.ConfirmCancelRequestClick) }
        composeTestRule
            .onNodeWithText("No")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(MyAccessAction.DismissDialog) }
    }

    @Test
    fun `confirm end dialog should send ConfirmEndAccessClick`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            dialogState = MyAccessState.DialogState.ConfirmEnd(LEASE_ID),
        )

        composeTestRule
            .onNodeWithText("Yes")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(MyAccessAction.ConfirmEndAccessClick) }
    }

    @Test
    fun `extend dialog should send field changes and require a valid form`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            dialogState = MyAccessState.DialogState.Extend(
                leaseId = LEASE_ID,
                durationMinutes = "",
                reason = "",
                maxDurationMinutes = 15L,
            ),
        )

        composeTestRule.onNodeWithText("Up to 15 minutes").assertIsDisplayed()
        composeTestRule.onNodeWithTag("PamSubmitExtendButton").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("PamExtendDurationField").performTextInput("10")
        verify { viewModel.trySendAction(MyAccessAction.ExtendDurationChange("10")) }
        composeTestRule.onNodeWithTag("PamExtendReasonField").performTextInput("Need it")
        verify { viewModel.trySendAction(MyAccessAction.ExtendReasonChange("Need it")) }
    }

    @Test
    fun `extend dialog should be a bottom sheet with the reason marked as required`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            dialogState = MyAccessState.DialogState.Extend(
                leaseId = LEASE_ID,
                durationMinutes = "10",
                reason = "",
                maxDurationMinutes = null,
            ),
        )

        composeTestRule
            .onNodeWithText("Reason (required)")
            .assertExists()
        composeTestRule
            .onNodeWithTag("PamSubmitExtendButton")
            .assert(hasAnyAncestor(SemanticsMatcher.expectValue(IsBottomSheetKey, true)))
    }

    @Test
    fun `extend dialog close should send DismissDialog`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            dialogState = MyAccessState.DialogState.Extend(
                leaseId = LEASE_ID,
                durationMinutes = "10",
                reason = "",
                maxDurationMinutes = null,
            ),
        )

        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.waitForIdle()
        verify { viewModel.trySendAction(MyAccessAction.DismissDialog) }
    }

    @Test
    fun `extend dialog submit should send SubmitExtendClick when valid`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            dialogState = MyAccessState.DialogState.Extend(
                leaseId = LEASE_ID,
                durationMinutes = "10",
                reason = "Need it",
                maxDurationMinutes = null,
            ),
        )

        composeTestRule.onNodeWithTag("PamSubmitExtendButton").assertIsEnabled().performClick()
        verify { viewModel.trySendAction(MyAccessAction.SubmitExtendClick) }
    }

    @Test
    fun `error dialog should show the message and dismiss`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            dialogState = MyAccessState.DialogState.Error("Broken".asText()),
        )

        composeTestRule.onNodeWithText("Broken").assertIsDisplayed()
        composeTestRule.onNodeWithText("Okay").performClick()
        verify { viewModel.trySendAction(MyAccessAction.DismissDialog) }
    }

    @Test
    fun `no dialog should be shown by default`() {
        composeTestRule.assertNoDialogExists()
    }

    private fun showRequests(vararg items: MyAccessState.RequestItem) {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = MyAccessState.ViewState.Content(requests = items.toList()),
        )
    }
}

private const val GENERIC_ERROR =
    "We were unable to process your request. Please try again or contact us."
private const val REQUEST_ID = "requestId"
private const val CIPHER_ID = "cipherId"
private const val LEASE_ID = "leaseId"

private val DEFAULT_STATE = MyAccessState(
    viewState = MyAccessState.ViewState.Loading,
    selectedTab = MyAccessState.Tab.CURRENT,
    isRefreshing = false,
    dialogState = null,
    showNotificationPrompt = false,
)

private val PROMPT_STATE = DEFAULT_STATE.copy(
    viewState = MyAccessState.ViewState.Content(requests = emptyList()),
    showNotificationPrompt = true,
)

private fun createItem(
    id: String = REQUEST_ID,
    stage: MyAccessState.Stage,
    leaseId: String? = null,
    leaseEndsAt: Instant? = null,
    canExtend: Boolean = false,
): MyAccessState.RequestItem = MyAccessState.RequestItem(
    id = id,
    cipherId = CIPHER_ID,
    itemName = "Prod DB",
    itemIcon = IconData.Local(iconRes = BitwardenDrawable.ic_globe),
    stage = stage,
    isExtension = false,
    reason = "Because",
    submittedAt = Instant.parse("2026-01-01T12:00:00Z"),
    leaseId = leaseId,
    leaseEndsAt = leaseEndsAt,
    canExtend = canExtend,
    maxExtensionMinutes = null,
)
