package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Test

class VaultScreenTest : BaseComposeTest() {

    @Test
    fun `search icon click should send SearchIconClick action`() {
        val viewModel = mockk<VaultViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns MutableStateFlow(
                VaultState(
                    avatarColor = Color.Blue,
                    initials = "BW",
                    viewState = VaultState.ViewState.NoItems,
                ),
            )
        }
        composeTestRule.apply {
            setContent {
                VaultScreen(
                    viewModel = viewModel,
                )
            }
            onNodeWithContentDescription("Search vault").performClick()
        }
        verify { viewModel.trySendAction(VaultAction.SearchIconClick) }
    }

    @Test
    fun `floating action button click should send AddItemClick action`() {
        val viewModel = mockk<VaultViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns MutableStateFlow(
                VaultState(
                    avatarColor = Color.Blue,
                    initials = "BW",
                    viewState = VaultState.ViewState.NoItems,
                ),
            )
        }
        composeTestRule.apply {
            setContent {
                VaultScreen(
                    viewModel = viewModel,
                )
            }
            onNodeWithContentDescription("Add Item").performClick()
        }
        verify { viewModel.trySendAction(VaultAction.AddItemClick) }
    }

    @Test
    fun `add an item button click should send AddItemClick action`() {
        val viewModel = mockk<VaultViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns MutableStateFlow(
                VaultState(
                    avatarColor = Color.Blue,
                    initials = "BW",
                    viewState = VaultState.ViewState.NoItems,
                ),
            )
        }

        composeTestRule.apply {
            setContent {
                VaultScreen(
                    viewModel = viewModel,
                )
            }
            onNodeWithText("Add an Item").performClick()
        }
        verify { viewModel.trySendAction(VaultAction.AddItemClick) }
    }
}
