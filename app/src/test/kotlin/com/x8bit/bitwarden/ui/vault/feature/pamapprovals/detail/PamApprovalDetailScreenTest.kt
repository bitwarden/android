package com.x8bit.bitwarden.ui.vault.feature.pamapprovals.detail

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.components.bottomsheet.IsBottomSheetKey
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.pam.repository.model.PamDecisionVerdict
import com.x8bit.bitwarden.data.pam.repository.model.PamRequestStatus
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class PamApprovalDetailScreenTest : BitwardenComposeTest() {

    private var onNavigateBackCalled = false
    private val mutableEventFlow = bufferedMutableSharedFlow<PamApprovalDetailEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<PamApprovalDetailViewModel> {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
        every { trySendAction(any()) } just runs
    }

    @Before
    fun setUp() {
        setContent {
            PamApprovalDetailScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `back click should send BackClick`() {
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify { viewModel.trySendAction(PamApprovalDetailAction.BackClick) }
    }

    @Test
    fun `NavigateBack event should call onNavigateBack`() {
        mutableEventFlow.tryEmit(PamApprovalDetailEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `ShowSnackbar event should show the message`() {
        mutableEventFlow.tryEmit(
            PamApprovalDetailEvent.ShowSnackbar(
                BitwardenSnackbarData(message = "Request approved".asText()),
            ),
        )
        composeTestRule.onNodeWithText("Request approved").assertIsDisplayed()
    }

    @Test
    fun `error state should show the generic error`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = PamApprovalDetailState.ViewState.Error,
        )
        composeTestRule.onNodeWithText(GENERIC_ERROR).assertIsDisplayed()
    }

    @Test
    fun `not found state should say the request is no longer available`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = PamApprovalDetailState.ViewState.NotFound,
        )
        composeTestRule.onNodeWithText("This request is no longer available.").assertIsDisplayed()
    }

    @Test
    fun `content should show the item, organization, requester, window and reason`() {
        showDetail(createDetail())

        composeTestRule.onNodeWithTag("PamDetailItem").assertTextEquals("Prod DB")
        composeTestRule.onNodeWithTag("PamDetailOrganization").assertTextEquals("Meridian Group")
        composeTestRule.onNodeWithTag("PamDetailCollection").assertTextEquals("Production")
        composeTestRule
            .onNodeWithTag("PamDetailRequester")
            .assertTextEquals("Grace\ngrace@example.com")
        composeTestRule.onNodeWithTag("PamDetailDuration").assertTextEquals("1 h 30 min")
        composeTestRule.onNodeWithTag("PamDetailWindow").assertExists()
        composeTestRule.onNodeWithTag("PamDetailReason").assertTextEquals("Prod incident")
        composeTestRule.onNodeWithTag("PamDetailStatus").assertHasText("Pending")
        composeTestRule.onNodeWithTag("PamDetailItemIcon").assertExists()
    }

    @Test
    fun `missing details should fall back and organization and collection are omitted`() {
        showDetail(
            createDetail(
                itemName = null,
                organizationName = null,
                collectionName = null,
                requesterName = null,
                requesterEmail = null,
                reason = null,
            ),
        )

        composeTestRule.onNodeWithTag("PamDetailItem").assertTextEquals("Privileged item")
        composeTestRule.onNodeWithTag("PamDetailOrganization").assertDoesNotExist()
        composeTestRule.onNodeWithTag("PamDetailCollection").assertDoesNotExist()
        composeTestRule.onNodeWithTag("PamDetailRequester").assertTextEquals("Unknown requester")
        composeTestRule.onNodeWithTag("PamDetailReason").assertTextEquals("No reason provided")
    }

    @Test
    fun `duration should be shown in hours or minutes`() {
        showDetail(createDetail(durationMinutes = 45L))
        composeTestRule.onNodeWithTag("PamDetailDuration").assertTextEquals("45 min")

        showDetail(createDetail(durationMinutes = 120L))
        composeTestRule.onNodeWithTag("PamDetailDuration").assertTextEquals("2 h")
    }

    @Test
    fun `approve and deny buttons should send actions`() {
        showDetail(createDetail())

        composeTestRule.onNodeWithTag("PamApproveButton").performScrollTo().performClick()
        verify { viewModel.trySendAction(PamApprovalDetailAction.ApproveClick) }
        composeTestRule.onNodeWithTag("PamDenyButton").performScrollTo().performClick()
        verify { viewModel.trySendAction(PamApprovalDetailAction.DenyClick) }
    }

    @Test
    fun `own request should not offer approve or deny and explain why`() {
        showDetail(createDetail(isOwnRequest = true, canDecide = false))

        composeTestRule.onNodeWithTag("PamApproveButton").assertDoesNotExist()
        composeTestRule.onNodeWithTag("PamDenyButton").assertDoesNotExist()
        composeTestRule.onNodeWithTag("PamCannotDecideOwn").assertExists()
    }

    @Test
    fun `decided request should show its status and decision and no buttons`() {
        showDetail(
            createDetail(
                status = PamRequestStatus.DENIED,
                canDecide = false,
                decidedBy = "Ada",
                decisionComment = "Not needed",
            ),
        )

        composeTestRule.onNodeWithTag("PamDetailStatus").assertHasText("Denied")
        composeTestRule.onNodeWithTag("PamDetailDecidedBy").assertTextEquals("Ada")
        composeTestRule.onNodeWithTag("PamDetailDecisionComment").assertTextEquals("Not needed")
        composeTestRule.onNodeWithTag("PamApproveButton").assertDoesNotExist()
        composeTestRule.onNodeWithTag("PamDenyButton").assertDoesNotExist()
        composeTestRule.onNodeWithTag("PamCannotDecideOwn").assertDoesNotExist()
    }

    @Test
    fun `revocable access should show when it ends and a revoke button that sends RevokeClick`() {
        showDetail(
            createDetail(
                status = PamRequestStatus.APPROVED,
                canDecide = false,
                canRevoke = true,
                accessEndsAt = Instant.parse("2026-01-01T13:00:00Z"),
            ),
        )

        composeTestRule.onNodeWithTag("PamDetailAccessEnds").performScrollTo().assertExists()
        composeTestRule.onNodeWithTag("PamApproveButton").assertDoesNotExist()
        composeTestRule
            .onNodeWithTag("PamRevokeAccessButton")
            .performScrollTo()
            .assertTextEquals("Revoke access")
            .performClick()
        verify { viewModel.trySendAction(PamApprovalDetailAction.RevokeClick) }
    }

    @Test
    fun `access that cannot be revoked should not show a revoke button or an end time`() {
        showDetail(createDetail(status = PamRequestStatus.APPROVED, canDecide = false))

        composeTestRule.onNodeWithTag("PamRevokeAccessButton").assertDoesNotExist()
        composeTestRule.onNodeWithTag("PamDetailAccessEnds").assertDoesNotExist()
    }

    @Test
    fun `revoke confirmation should explain the effect and confirm`() {
        showDetail(createDetail(canDecide = false, canRevoke = true))
        mutableStateFlow.value = mutableStateFlow.value.copy(
            dialogState = PamApprovalDetailState.DialogState.ConfirmRevoke,
        )

        composeTestRule
            .onNodeWithText(
                "This ends their access now. They will need a new approval to regain it.",
            )
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Revoke access")
            .filterToOne(hasAnyAncestor(isDialog()) and hasClickAction())
            .performClick()
        verify { viewModel.trySendAction(PamApprovalDetailAction.ConfirmRevokeClick) }
    }

    @Test
    fun `revoke confirmation cancel should send DismissDialog`() {
        showDetail(createDetail(canDecide = false, canRevoke = true))
        mutableStateFlow.value = mutableStateFlow.value.copy(
            dialogState = PamApprovalDetailState.DialogState.ConfirmRevoke,
        )

        composeTestRule.onNodeWithText("Cancel").performClick()

        verify { viewModel.trySendAction(PamApprovalDetailAction.DismissDialog) }
    }

    @Test
    fun `approve dialog should allow an empty comment and confirm`() {
        showDetail(createDetail())
        mutableStateFlow.value = mutableStateFlow.value.copy(
            dialogState = PamApprovalDetailState.DialogState.Decide(
                verdict = PamDecisionVerdict.APPROVE,
                comment = "",
            ),
        )

        composeTestRule
            .onNodeWithText("Approve access request")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("PamConfirmDecisionButton").assert(isInBottomSheet())
        composeTestRule.onNodeWithText("Optional, saved to the audit log").assertIsDisplayed()
        composeTestRule.onNodeWithTag("PamDecisionCommentField").performTextInput("Fine")
        verify { viewModel.trySendAction(PamApprovalDetailAction.CommentChange("Fine")) }
        composeTestRule
            .onNodeWithTag("PamConfirmDecisionButton")
            .assertIsEnabled()
            .performClick()
        verify { viewModel.trySendAction(PamApprovalDetailAction.ConfirmDecisionClick) }
    }

    @Test
    fun `deny dialog should require a reason before confirming`() {
        showDetail(createDetail())
        mutableStateFlow.value = mutableStateFlow.value.copy(
            dialogState = PamApprovalDetailState.DialogState.Decide(
                verdict = PamDecisionVerdict.DENY,
                comment = "",
            ),
        )

        composeTestRule
            .onNodeWithText("Deny access request")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("PamConfirmDecisionButton").assert(isInBottomSheet())
        composeTestRule.onNodeWithText("Reason for denial (required)").assertIsDisplayed()
        composeTestRule.onNodeWithTag("PamConfirmDecisionButton").assertIsNotEnabled()

        mutableStateFlow.value = mutableStateFlow.value.copy(
            dialogState = PamApprovalDetailState.DialogState.Decide(
                verdict = PamDecisionVerdict.DENY,
                comment = "No longer needed",
            ),
        )
        composeTestRule.onNodeWithTag("PamConfirmDecisionButton").assertIsEnabled()
    }

    @Test
    fun `decision sheet close should send DismissDialog`() {
        showDetail(createDetail())
        mutableStateFlow.value = mutableStateFlow.value.copy(
            dialogState = PamApprovalDetailState.DialogState.Decide(
                verdict = PamDecisionVerdict.DENY,
                comment = "",
            ),
        )

        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.waitForIdle()

        verify { viewModel.trySendAction(PamApprovalDetailAction.DismissDialog) }
    }

    @Test
    fun `error dialog should show the message and dismiss`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            dialogState = PamApprovalDetailState.DialogState.Error(
                message = "Already decided".asText(),
            ),
        )

        composeTestRule
            .onNodeWithText("Already decided")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Okay").performClick()
        verify { viewModel.trySendAction(PamApprovalDetailAction.DismissDialog) }
    }

    @Test
    fun `loading dialog should be shown while the decision is sent`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            dialogState = PamApprovalDetailState.DialogState.Loading,
        )

        composeTestRule
            .onNodeWithText("Loading")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    private fun showDetail(detail: PamApprovalDetailState.Detail) {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = PamApprovalDetailState.ViewState.Content(detail),
        )
    }
}

private const val GENERIC_ERROR =
    "We were unable to process your request. Please try again or contact us."

private val DEFAULT_STATE = PamApprovalDetailState(
    requestId = "requestId",
    viewState = PamApprovalDetailState.ViewState.Loading,
    dialogState = null,
)

@Suppress("LongParameterList")
private fun createDetail(
    itemName: String? = "Prod DB",
    organizationName: String? = "Meridian Group",
    collectionName: String? = "Production",
    requesterName: String? = "Grace",
    requesterEmail: String? = "grace@example.com",
    durationMinutes: Long = 90L,
    reason: String? = "Prod incident",
    status: PamRequestStatus = PamRequestStatus.PENDING,
    isOwnRequest: Boolean = false,
    canDecide: Boolean = true,
    decidedBy: String? = null,
    decisionComment: String? = null,
    canRevoke: Boolean = false,
    accessEndsAt: Instant? = null,
): PamApprovalDetailState.Detail = PamApprovalDetailState.Detail(
    itemName = itemName,
    itemIcon = IconData.Local(iconRes = BitwardenDrawable.ic_globe),
    organizationName = organizationName,
    collectionName = collectionName,
    requesterName = requesterName,
    requesterEmail = requesterEmail,
    durationMinutes = durationMinutes,
    windowStart = Instant.parse("2026-01-01T12:00:00Z"),
    windowEnd = Instant.parse("2026-01-01T13:30:00Z"),
    reason = reason,
    status = status,
    isOwnRequest = isOwnRequest,
    canDecide = canDecide,
    decidedBy = decidedBy,
    decisionComment = decisionComment,
    canRevoke = canRevoke,
    accessEndsAt = accessEndsAt,
)

private fun isInBottomSheet(): SemanticsMatcher =
    hasAnyAncestor(SemanticsMatcher.expectValue(IsBottomSheetKey, true))

private fun SemanticsNodeInteraction.assertHasText(text: String): SemanticsNodeInteraction =
    assert(hasAnyDescendant(hasText(text)))
