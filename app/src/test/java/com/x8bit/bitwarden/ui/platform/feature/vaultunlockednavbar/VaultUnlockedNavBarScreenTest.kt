package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class VaultUnlockedNavBarScreenTest : BaseComposeTest() {

    @Test
    fun `vault tab click should send VaultTabClick action`() {
        val viewModel = mockk<VaultUnlockedNavBarViewModel>(relaxed = true)
        composeTestRule.apply {
            setContent {
                VaultUnlockedNavBarScreen(
                    viewModel = viewModel,
                )
            }
            onNodeWithTag("vault").performClick()
        }
        verify { viewModel.trySendAction(VaultUnlockedNavBarAction.VaultTabClick) }
    }

    @Test
    fun `send tab click should send SendTabClick action`() {
        val viewModel = mockk<VaultUnlockedNavBarViewModel>(relaxed = true)
        composeTestRule.apply {
            setContent {
                VaultUnlockedNavBarScreen(
                    viewModel = viewModel,
                )
            }
            onNodeWithTag("send").performClick()
        }
        verify { viewModel.trySendAction(VaultUnlockedNavBarAction.SendTabClick) }
    }

    @Test
    fun `generator tab click should send GeneratorTabClick action`() {
        val viewModel = mockk<VaultUnlockedNavBarViewModel>(relaxed = true)
        composeTestRule.apply {
            setContent {
                VaultUnlockedNavBarScreen(
                    viewModel = viewModel,
                )
            }
            onNodeWithTag("generator").performClick()
        }
        verify { viewModel.trySendAction(VaultUnlockedNavBarAction.GeneratorTabClick) }
    }

    @Test
    fun `settings tab click should send SendTabClick action`() {
        val viewModel = mockk<VaultUnlockedNavBarViewModel>(relaxed = true)
        composeTestRule.apply {
            setContent {
                VaultUnlockedNavBarScreen(
                    viewModel = viewModel,
                )
            }
            onNodeWithTag("settings").performClick()
        }
        verify { viewModel.trySendAction(VaultUnlockedNavBarAction.SettingsTabClick) }
    }
}
