package com.x8bit.bitwarden.ui.vault.feature.movetoorganization

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VaultMoveToOrganizationScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false

    private val mutableEventFlow = bufferedMutableSharedFlow<VaultMoveToOrganizationEvent>()
    private val mutableStateFlow = MutableStateFlow(createVaultMoveToOrganizationState())

    private val viewModel = mockk<VaultMoveToOrganizationViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            VaultMoveToOrganizationScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `on NavigateBack event should invoke onNavigateBack`() {
        mutableEventFlow.tryEmit(VaultMoveToOrganizationEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `clicking close button should send BackClick action`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Close")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultMoveToOrganizationAction.BackClick,
            )
        }
    }
}

private fun createVaultMoveToOrganizationState(): VaultMoveToOrganizationState =
    VaultMoveToOrganizationState(
        vaultItemId = "mockId",
        viewState = VaultMoveToOrganizationState.ViewState.Content,
    )
