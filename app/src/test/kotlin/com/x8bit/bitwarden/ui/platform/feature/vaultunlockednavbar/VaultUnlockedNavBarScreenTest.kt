package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.navOptions
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.base.createMockNavHostController
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.feature.settings.SettingsGraphRoute
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorGraphRoute
import com.x8bit.bitwarden.ui.tools.feature.send.SendGraphRoute
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultGraphRoute
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test

class VaultUnlockedNavBarScreenTest : BitwardenComposeTest() {
    private val mockNavHostController = createMockNavHostController()
    private val mutableEventFlow = bufferedMutableSharedFlow<VaultUnlockedNavBarEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    val viewModel = mockk<VaultUnlockedNavBarViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    private val expectedNavOptions = navOptions {
        // When changing root navigation state, pop everything else off the back stack:
        popUpTo(id = mockNavHostController.graph.id) {
            inclusive = false
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }

    @Before
    fun setup() {
        setContent {
            VaultUnlockedNavBarScreen(
                viewModel = viewModel,
                navController = mockNavHostController,
                onNavigateToVaultAddItem = {},
                onNavigateToVaultItem = {},
                onNavigateToVaultEditItem = {},
                onNavigateToAddEditSend = {},
                onNavigateToViewSend = {},
                onNavigateToDeleteAccount = {},
                onNavigateToExportVault = {},
                onNavigateToFolders = {},
                onNavigateToPasswordHistory = {},
                onNavigateToPendingRequests = {},
                onNavigateToSearchVault = {},
                onNavigateToSearchSend = {},
                onNavigateToSetupAutoFillScreen = {},
                onNavigateToSetupBrowserAutofill = {},
                onNavigateToSetupUnlockScreen = {},
                onNavigateToImportLogins = {},
                onNavigateToAddFolderScreen = {},
                onNavigateToFlightRecorder = {},
                onNavigateToRecordedLogs = {},
                onNavigateToAboutPrivilegedApps = {},
            )
        }
    }

    @Test
    fun `vault tab click should send VaultTabClick action`() {
        composeTestRule.onNodeWithText(text = "My vault").performClick()
        verify { viewModel.trySendAction(VaultUnlockedNavBarAction.VaultTabClick) }
    }

    @Test
    fun `NavigateToVaultScreen should navigate to VaultScreen`() {
        mutableEventFlow.tryEmit(
            VaultUnlockedNavBarEvent.NavigateToVaultScreen(
                labelRes = BitwardenString.my_vault,
                contentDescRes = BitwardenString.my_vault,
            ),
        )
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = VaultGraphRoute,
                    navOptions = expectedNavOptions,
                )
            }
        }
    }

    @Test
    fun `NavigateToVaultScreen shortcut event should navigate to VaultScreen`() {
        mutableEventFlow.tryEmit(
            VaultUnlockedNavBarEvent.Shortcut.NavigateToVaultScreen(
                labelRes = BitwardenString.my_vault,
                contentDescRes = BitwardenString.my_vault,
            ),
        )
        composeTestRule.runOnIdle {
            mockNavHostController.navigate(
                route = VaultGraphRoute,
                navOptions = expectedNavOptions,
            )
        }
    }

    @Test
    fun `NavigateToSettingsScreen shortcut event should navigate to SettingsScreen`() {
        mutableEventFlow.tryEmit(VaultUnlockedNavBarEvent.Shortcut.NavigateToSettingsScreen)
        composeTestRule.runOnIdle {
            verify {
                mockNavHostController.navigate(
                    route = SettingsGraphRoute,
                    navOptions = expectedNavOptions,
                )
            }
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
            mockNavHostController.navigate(
                route = SendGraphRoute,
                navOptions = expectedNavOptions,
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
            mockNavHostController.navigate(
                route = GeneratorGraphRoute,
                navOptions = expectedNavOptions,
            )
        }
    }

    @Test
    fun `NavigateToGeneratorScreen shortcut event should navigate to GeneratorScreen`() {
        mutableEventFlow.tryEmit(VaultUnlockedNavBarEvent.Shortcut.NavigateToGeneratorScreen)
        composeTestRule.runOnIdle {
            mockNavHostController.navigate(
                route = GeneratorGraphRoute,
                navOptions = expectedNavOptions,
            )
        }
    }

    @Test
    fun `settings tab click should send SendTabClick action`() {
        composeTestRule.onNodeWithText(text = "Settings").performClick()
        verify { viewModel.trySendAction(VaultUnlockedNavBarAction.SettingsTabClick) }
    }

    @Test
    fun `NavigateToSettingsScreen should navigate to SettingsScreen`() {
        mutableEventFlow.tryEmit(VaultUnlockedNavBarEvent.NavigateToSettingsScreen)
        composeTestRule.runOnIdle {
            mockNavHostController.navigate(
                route = SettingsGraphRoute,
                navOptions = expectedNavOptions,
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
                vaultNavBarContentDescriptionRes = BitwardenString.vaults,
                notificationState = VaultUnlockedNavBarNotificationState(
                    settingsTabNotificationCount = 0,
                ),
            ),
        )

        composeTestRule.onNodeWithText(text = "My vault").assertDoesNotExist()
        composeTestRule.onNodeWithText(text = "Vaults").assertExists()
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
    vaultNavBarContentDescriptionRes = BitwardenString.my_vault,
    notificationState = VaultUnlockedNavBarNotificationState(
        settingsTabNotificationCount = 0,
    ),
)
