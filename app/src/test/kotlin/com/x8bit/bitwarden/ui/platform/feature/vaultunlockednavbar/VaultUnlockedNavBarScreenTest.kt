package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.navigation.util.navigationControllerOf
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.feature.settings.SettingsGraph
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorGraph
import com.x8bit.bitwarden.ui.tools.feature.send.SendGraph
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultGraph
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class VaultUnlockedNavBarScreenTest : BitwardenComposeTest() {
    private val mutableEventFlow = bufferedMutableSharedFlow<VaultUnlockedNavBarEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    val viewModel = mockk<VaultUnlockedNavBarViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }
    private val tabNavigationControllers = VaultUnlockedNavBarNavigationControllers(
        root = navigationControllerOf(routes = arrayOf(VaultUnlockedNavBarGraph.Vault)),
        vault = navigationControllerOf(routes = arrayOf(VaultGraph.VaultRoute)),
        send = navigationControllerOf(routes = arrayOf(SendGraph.SendRoute)),
        generator = navigationControllerOf(routes = arrayOf(GeneratorGraph.GeneratorRoute)),
        settings = navigationControllerOf(routes = arrayOf(SettingsGraph.SettingsRoute)),
    )
    private val rootNavigationController = tabNavigationControllers.root

    @Before
    fun setup() {
        setContent {
            VaultUnlockedNavBarScreen(
                viewModel = viewModel,
                bottomNavigationController = tabNavigationControllers,
            ) { _ ->
                Text(text = "Content for ${rootNavigationController.backStack.last()}")
            }
        }
    }

    @Test
    fun `content should be displayed for the current tab`() {
        composeTestRule
            .onNodeWithText(text = "Content for ${VaultUnlockedNavBarGraph.Vault}")
            .assertExists()

        rootNavigationController.navigateToTopLevelRoute(route = VaultUnlockedNavBarGraph.Send)

        composeTestRule
            .onNodeWithText(text = "Content for ${VaultUnlockedNavBarGraph.Send}")
            .assertExists()
    }

    @Test
    fun `selected tab should match the current tab`() {
        composeTestRule.onNodeWithText(text = "My vault").assertIsSelected()
        composeTestRule.onNodeWithText(text = "Settings").assertIsNotSelected()

        rootNavigationController.navigateToTopLevelRoute(
            route = VaultUnlockedNavBarGraph.Settings,
        )

        composeTestRule.onNodeWithText(text = "My vault").assertIsNotSelected()
        composeTestRule.onNodeWithText(text = "Settings").assertIsSelected()
    }

    @Test
    fun `vault tab click should send VaultTabClick action`() {
        composeTestRule.onNodeWithText(text = "My vault").performClick()
        verify { viewModel.trySendAction(VaultUnlockedNavBarAction.VaultTabClick) }
    }

    @Test
    fun `NavigateToVaultScreen should navigate to VaultScreen`() {
        rootNavigationController.navigateToTopLevelRoute(route = VaultUnlockedNavBarGraph.Send)

        mutableEventFlow.tryEmit(
            VaultUnlockedNavBarEvent.NavigateToVaultScreen(labelRes = BitwardenString.my_vault),
        )

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(VaultUnlockedNavBarGraph.Vault),
                rootNavigationController.backStack.toList(),
            )
        }
    }

    @Test
    fun `NavigateToVaultScreen when already on the vault tab should pop to the vault root`() {
        tabNavigationControllers.vault.navigate(route = VaultGraph.VerificationCodeRoute)

        mutableEventFlow.tryEmit(
            VaultUnlockedNavBarEvent.NavigateToVaultScreen(labelRes = BitwardenString.my_vault),
        )

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(VaultUnlockedNavBarGraph.Vault),
                rootNavigationController.backStack.toList(),
            )
            assertEquals(
                listOf(VaultGraph.VaultRoute),
                tabNavigationControllers.vault.backStack.toList(),
            )
        }
    }

    @Test
    fun `NavigateToVaultScreen shortcut event should navigate to VaultScreen`() {
        rootNavigationController.navigateToTopLevelRoute(
            route = VaultUnlockedNavBarGraph.Settings,
        )

        mutableEventFlow.tryEmit(
            VaultUnlockedNavBarEvent.Shortcut.NavigateToVaultScreen(
                labelRes = BitwardenString.my_vault,
            ),
        )

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(VaultUnlockedNavBarGraph.Vault),
                rootNavigationController.backStack.toList(),
            )
        }
    }

    @Test
    fun `send tab click should send SendTabClick action`() {
        composeTestRule.onNodeWithText(text = "Send").performClick()
        verify { viewModel.trySendAction(VaultUnlockedNavBarAction.SendTabClick) }
    }

    @Test
    fun `NavigateToSendScreen should navigate to SendScreen`() {
        mutableEventFlow.tryEmit(VaultUnlockedNavBarEvent.NavigateToSendScreen)
        composeTestRule.runOnIdle {
            assertEquals(
                listOf(VaultUnlockedNavBarGraph.Vault, VaultUnlockedNavBarGraph.Send),
                rootNavigationController.backStack.toList(),
            )
        }
    }

    @Test
    fun `NavigateToSendScreen when already on the send tab should pop to the send root`() {
        rootNavigationController.navigateToTopLevelRoute(route = VaultUnlockedNavBarGraph.Send)
        tabNavigationControllers.send.navigate(
            route = SendGraph.SendItemListingRoute(
                vaultItemListingType = VaultItemListingType.SendText,
            ),
        )

        mutableEventFlow.tryEmit(VaultUnlockedNavBarEvent.NavigateToSendScreen)

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(VaultUnlockedNavBarGraph.Vault, VaultUnlockedNavBarGraph.Send),
                rootNavigationController.backStack.toList(),
            )
            assertEquals(
                listOf(SendGraph.SendRoute),
                tabNavigationControllers.send.backStack.toList(),
            )
        }
    }

    @Test
    fun `generator tab click should send GeneratorTabClick action`() {
        composeTestRule.onNodeWithText(text = "Generator").performClick()
        verify { viewModel.trySendAction(VaultUnlockedNavBarAction.GeneratorTabClick) }
    }

    @Test
    fun `NavigateToGeneratorScreen should navigate to GeneratorScreen`() {
        mutableEventFlow.tryEmit(VaultUnlockedNavBarEvent.NavigateToGeneratorScreen)
        composeTestRule.runOnIdle {
            assertEquals(
                listOf(VaultUnlockedNavBarGraph.Vault, VaultUnlockedNavBarGraph.Generator),
                rootNavigationController.backStack.toList(),
            )
        }
    }

    @Test
    fun `NavigateToGeneratorScreen when already on the generator tab should not pop other tabs`() {
        rootNavigationController.navigateToTopLevelRoute(
            route = VaultUnlockedNavBarGraph.Generator,
        )
        tabNavigationControllers.vault.navigate(route = VaultGraph.VerificationCodeRoute)

        mutableEventFlow.tryEmit(VaultUnlockedNavBarEvent.NavigateToGeneratorScreen)

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(VaultUnlockedNavBarGraph.Vault, VaultUnlockedNavBarGraph.Generator),
                rootNavigationController.backStack.toList(),
            )
            assertEquals(
                listOf(VaultGraph.VaultRoute, VaultGraph.VerificationCodeRoute),
                tabNavigationControllers.vault.backStack.toList(),
            )
        }
    }

    @Test
    fun `NavigateToGeneratorScreen shortcut event should navigate to GeneratorScreen`() {
        mutableEventFlow.tryEmit(VaultUnlockedNavBarEvent.Shortcut.NavigateToGeneratorScreen)
        composeTestRule.runOnIdle {
            assertEquals(
                listOf(VaultUnlockedNavBarGraph.Vault, VaultUnlockedNavBarGraph.Generator),
                rootNavigationController.backStack.toList(),
            )
        }
    }

    @Test
    fun `settings tab click should send SettingsTabClick action`() {
        composeTestRule.onNodeWithText(text = "Settings").performClick()
        verify { viewModel.trySendAction(VaultUnlockedNavBarAction.SettingsTabClick) }
    }

    @Test
    fun `NavigateToSettingsScreen should navigate to SettingsScreen`() {
        mutableEventFlow.tryEmit(VaultUnlockedNavBarEvent.NavigateToSettingsScreen)
        composeTestRule.runOnIdle {
            assertEquals(
                listOf(VaultUnlockedNavBarGraph.Vault, VaultUnlockedNavBarGraph.Settings),
                rootNavigationController.backStack.toList(),
            )
        }
    }

    @Test
    fun `NavigateToSettingsScreen when already on the settings tab should pop to the root`() {
        rootNavigationController.navigateToTopLevelRoute(
            route = VaultUnlockedNavBarGraph.Settings,
        )
        tabNavigationControllers.settings.navigate(route = SettingsGraph.AboutRoute)

        mutableEventFlow.tryEmit(VaultUnlockedNavBarEvent.NavigateToSettingsScreen)

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(VaultUnlockedNavBarGraph.Vault, VaultUnlockedNavBarGraph.Settings),
                rootNavigationController.backStack.toList(),
            )
            assertEquals(
                listOf(SettingsGraph.SettingsRoute),
                tabNavigationControllers.settings.backStack.toList(),
            )
        }
    }

    @Test
    fun `NavigateToSettingsScreen shortcut event should navigate to SettingsScreen`() {
        mutableEventFlow.tryEmit(VaultUnlockedNavBarEvent.Shortcut.NavigateToSettingsScreen)
        composeTestRule.runOnIdle {
            assertEquals(
                listOf(VaultUnlockedNavBarGraph.Vault, VaultUnlockedNavBarGraph.Settings),
                rootNavigationController.backStack.toList(),
            )
        }
    }

    @Test
    fun `vault nav bar should update according to state`() {
        composeTestRule.onNodeWithText(text = "My vault").assertExists()
        composeTestRule.onNodeWithText(text = "Vaults").assertDoesNotExist()

        mutableStateFlow.tryEmit(
            VaultUnlockedNavBarState(
                vaultNavBarLabelRes = BitwardenString.vaults,
                notificationState = VaultUnlockedNavBarNotificationState(
                    settingsTabNotificationCount = 0,
                ),
                areSendsDisabled = false,
            ),
        )

        composeTestRule.onNodeWithText(text = "My vault").assertDoesNotExist()
        composeTestRule.onNodeWithText(text = "Vaults").assertExists()
    }

    @Test
    fun `send tab should be hidden when areSendsDisabled is true`() {
        composeTestRule.onNodeWithText(text = "Send").assertExists()

        mutableStateFlow.update { it.copy(areSendsDisabled = true) }

        composeTestRule.onNodeWithText(text = "Send").assertDoesNotExist()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `settings tab notification count should update according to state and show correct count`() {
        mutableStateFlow.update {
            it.copy(
                notificationState = VaultUnlockedNavBarNotificationState(
                    settingsTabNotificationCount = 1,
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = "1", useUnmergedTree = true)
            .assertExists()

        mutableStateFlow.update {
            it.copy(
                notificationState = VaultUnlockedNavBarNotificationState(
                    settingsTabNotificationCount = 0,
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = "1", useUnmergedTree = true)
            .assertDoesNotExist()
    }
}

private val DEFAULT_STATE = VaultUnlockedNavBarState(
    vaultNavBarLabelRes = BitwardenString.my_vault,
    notificationState = VaultUnlockedNavBarNotificationState(
        settingsTabNotificationCount = 0,
    ),
    areSendsDisabled = false,
)
