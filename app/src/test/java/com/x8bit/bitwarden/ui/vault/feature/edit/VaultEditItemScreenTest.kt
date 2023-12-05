package com.x8bit.bitwarden.ui.vault.feature.edit

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VaultEditItemScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false

    private val mutableEventFlow = MutableSharedFlow<VaultEditItemEvent>(
        extraBufferCapacity = Int.MAX_VALUE,
    )
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<VaultEditItemViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            VaultEditItemScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
            )
        }
    }

    @Test
    fun `NavigateBack event should invoke onNavigateBack`() {
        mutableEventFlow.tryEmit(VaultEditItemEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `on close click should send CloseClick event`() {
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(VaultEditItemAction.CloseClick)
        }
    }

    @Test
    fun `on save click should send SaveClick event`() {
        composeTestRule.onNodeWithText("Save").performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(VaultEditItemAction.SaveClick)
        }
    }
}

private const val DEFAULT_VAULT_ITEM_ID: String = "vault_item_id"

private val DEFAULT_STATE: VaultEditItemState = VaultEditItemState(
    vaultItemId = DEFAULT_VAULT_ITEM_ID,
)
