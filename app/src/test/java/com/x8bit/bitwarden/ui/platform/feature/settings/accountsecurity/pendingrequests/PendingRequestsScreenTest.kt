package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.pendingrequests

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.platform.util.isBuildVersionBelow
import com.x8bit.bitwarden.data.platform.util.isFdroid
import com.x8bit.bitwarden.data.util.advanceTimeByAndRunCurrent
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.manager.permissions.FakePermissionManager
import com.x8bit.bitwarden.ui.util.assertNoDialogExists
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue

class PendingRequestsScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToLoginApprovalCalled = false

    private val mutableEventFlow = bufferedMutableSharedFlow<PendingRequestsEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<PendingRequestsViewModel> {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
        every { trySendAction(any()) } just runs
    }
    private val permissionsManager = FakePermissionManager().apply {
        checkPermissionResult = false
        shouldShowRequestRationale = false
    }

    @Before
    fun setUp() {
        mockkStatic(::isFdroid)
        mockkStatic(::isBuildVersionBelow)
        every { isFdroid } returns false
        every { isBuildVersionBelow(any()) } returns false
        composeTestRule.setContent {
            PendingRequestsScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToLoginApproval = { _ -> onNavigateToLoginApprovalCalled = true },
                viewModel = viewModel,
                permissionsManager = permissionsManager,
            )
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(::isFdroid)
        unmockkStatic(::isBuildVersionBelow)
    }

    @Test
    fun `on NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(PendingRequestsEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `on NavigateToLoginApproval should call onNavigateToLoginApproval`() = runTest {
        mutableEventFlow.tryEmit(PendingRequestsEvent.NavigateToLoginApproval("fingerprint"))
        assertTrue(onNavigateToLoginApprovalCalled)
    }

    @Test
    fun `on decline all requests confirmation should send DeclineAllRequestsConfirm`() = runTest {
        // set content so the Decline all requests button appears
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = PendingRequestsState.ViewState.Content(
                requests = listOf(
                    PendingRequestsState.ViewState.Content.PendingLoginRequest(
                        fingerprintPhrase = "pantry-overdue-survive-sleep-jab",
                        platform = "iOS",
                        timestamp = "8/24/2023 11:11 AM",
                    ),
                    PendingRequestsState.ViewState.Content.PendingLoginRequest(
                        fingerprintPhrase = "erupt-anew-matchbook-disk-student",
                        platform = "Android",
                        timestamp = "8/21/2023 9:43 AM",
                    ),
                ),
            ),
            hideBottomSheet = true,
        )
        composeTestRule.onNodeWithText("Decline all requests").performClick()
        composeTestRule
            .onNodeWithText("Yes")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.assertNoDialogExists()

        verify {
            viewModel.trySendAction(PendingRequestsAction.DeclineAllRequestsConfirm)
        }
    }

    @Test
    fun `on decline all requests cancel should hide confirmation dialog`() = runTest {
        // set content so the Decline all requests button appears
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = PendingRequestsState.ViewState.Content(
                requests = listOf(
                    PendingRequestsState.ViewState.Content.PendingLoginRequest(
                        fingerprintPhrase = "pantry-overdue-survive-sleep-jab",
                        platform = "iOS",
                        timestamp = "8/24/2023 11:11 AM",
                    ),
                    PendingRequestsState.ViewState.Content.PendingLoginRequest(
                        fingerprintPhrase = "erupt-anew-matchbook-disk-student",
                        platform = "Android",
                        timestamp = "8/21/2023 9:43 AM",
                    ),
                ),
            ),
            hideBottomSheet = true,
        )
        composeTestRule.onNodeWithText("Decline all requests").performClick()
        composeTestRule
            .onNodeWithText("Cancel")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.assertNoDialogExists()

        verify(exactly = 0) {
            viewModel.trySendAction(PendingRequestsAction.DeclineAllRequestsConfirm)
        }
    }

    @Test
    fun `on skip for now click should emit HideBottomSheet`() {
        composeTestRule
            .onNodeWithText(text = "Skip for now")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 1000L)
        verify(exactly = 1) {
            viewModel.trySendAction(PendingRequestsAction.HideBottomSheet)
        }
    }

    @Test
    fun `on Enable notifications click should emit HideBottomSheet`() {
        composeTestRule
            .onAllNodesWithText(text = "Enable notifications")
            .filterToOne(hasClickAction())
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 1000L)
        verify(exactly = 1) {
            viewModel.trySendAction(PendingRequestsAction.HideBottomSheet)
        }
    }
}

private val DEFAULT_STATE: PendingRequestsState = PendingRequestsState(
    authRequests = emptyList(),
    viewState = PendingRequestsState.ViewState.Loading,
    isPullToRefreshSettingEnabled = false,
    isRefreshing = false,
    hideBottomSheet = false,
)
