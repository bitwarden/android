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
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
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
                    onNavigateToVaultAddItemScreen = {},
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
                    onNavigateToVaultAddItemScreen = {},
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
                    onNavigateToVaultAddItemScreen = {},
                )
            }
            onNodeWithText("Add an Item").performClick()
        }
        verify { viewModel.trySendAction(VaultAction.AddItemClick) }
    }

    @Test
    fun `NavigateToAddItemScreen event should call onNavigateToVaultAddItemScreen`() {
        var onNavigateToVaultAddItemScreenCalled = false
        val viewModel = mockk<VaultViewModel>(relaxed = true) {
            every { eventFlow } returns flowOf(VaultEvent.NavigateToAddItemScreen)
            every { stateFlow } returns MutableStateFlow(
                VaultState(
                    avatarColor = Color.Blue,
                    initials = "BW",
                    viewState = VaultState.ViewState.NoItems,
                ),
            )
        }

        composeTestRule.setContent {
            VaultScreen(
                onNavigateToVaultAddItemScreen = { onNavigateToVaultAddItemScreenCalled = true },
                viewModel = viewModel,
            )
        }

        assertTrue(onNavigateToVaultAddItemScreenCalled)
    }
}
