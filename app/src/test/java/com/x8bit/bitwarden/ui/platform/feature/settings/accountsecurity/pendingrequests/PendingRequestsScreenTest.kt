package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.pendingrequests

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

class PendingRequestsScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false

    private val mutableEventFlow = bufferedMutableSharedFlow<PendingRequestsEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<PendingRequestsViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            PendingRequestsScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `on NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(PendingRequestsEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `on DeclineAllRequestsClick should send DeclineAllRequestsClick`() = runTest {
        // set content so the Decline all requests button appears
        mutableStateFlow.tryEmit(
            PendingRequestsState(
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
            ),
        )
        composeTestRule.onNodeWithText("Decline all requests").performClick()
        verify {
            viewModel.trySendAction(PendingRequestsAction.DeclineAllRequestsClick)
        }
    }

    companion object {
        val DEFAULT_STATE: PendingRequestsState = PendingRequestsState(
            viewState = PendingRequestsState.ViewState.Loading,
        )
    }
}
