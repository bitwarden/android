package com.x8bit.bitwarden.ui.vault.feature.item

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test

class VaultItemScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false

    private val mutableEventFlow = MutableSharedFlow<VaultItemEvent>(
        extraBufferCapacity = Int.MAX_VALUE,
    )
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<VaultItemViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            VaultItemScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
            )
        }
    }

    @Test
    fun `clicking close button should send CloseClick action`() {
        composeTestRule.onNodeWithContentDescription(label = "Close").performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.CloseClick)
        }
    }
}

private const val VAULT_ITEM_ID = "vault_item_id"

private val DEFAULT_STATE: VaultItemState = VaultItemState(
    vaultItemId = VAULT_ITEM_ID,
)
