package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.navOptions
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.FakeNavHostController
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test

class VaultUnlockedNavBarScreenTest : BaseComposeTest() {
    private val fakeNavHostController = FakeNavHostController()
    private val mutableEventFlow = bufferedMutableSharedFlow<VaultUnlockedNavBarEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    val viewModel = mockk<VaultUnlockedNavBarViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    private val expectedNavOptions = navOptions {
        // When changing root navigation state, pop everything else off the back stack:
        popUpTo(fakeNavHostController.graphId) {
            inclusive = false
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }

    @Before
    fun setup() {
        composeTestRule.apply {
            setContent {
                VaultUnlockedNavBarScreen(
                    viewModel = viewModel,
                    navController = fakeNavHostController,
                    onNavigateToVaultAddItem = { _, _, _ -> },
                    onNavigateToVaultItem = {},
                    onNavigateToVaultEditItem = {},
                    onNavigateToAddSend = {},
                    onNavigateToEditSend = {},
                    onNavigateToDeleteAccount = {},
                    onNavigateToExportVault = {},
                    onNavigateToFolders = {},
                    onNavigateToPasswordHistory = {},
                    onNavigateToPendingRequests = {},
                    onNavigateToSearchVault = {},
                    onNavigateToSearchSend = {},
                    onNavigateToSetupAutoFillScreen = {},
                    onNavigateToSetupUnlockScreen = {},
                    onNavigateToImportLogins = {},
                )
            }
        }
    }

    @Test
    fun `vault tab click should send VaultTabClick action`() {
        composeTestRule.onNodeWithText("My vault").performClick()
        verify { viewModel.trySendAction(VaultUnlockedNavBarAction.VaultTabClick) }
    }

    @Test
    fun `NavigateToVaultScreen should navigate to VaultScreen`() {
        mutableEventFlow.tryEmit(VaultUnlockedNavBarEvent.NavigateToSendScreen)
        composeTestRule.runOnIdle { fakeNavHostController.assertCurrentRoute("send_graph") }
        mutableEventFlow.tryEmit(
            VaultUnlockedNavBarEvent.NavigateToVaultScreen(
                labelRes = R.string.my_vault,
                contentDescRes = R.string.my_vault,
            ),
        )
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "vault_graph",
                navOptions = expectedNavOptions,
            )
        }
    }

    @Test
    fun `NavigateToVaultScreen shortcut event should navigate to VaultScreen`() {
        mutableEventFlow.tryEmit(VaultUnlockedNavBarEvent.NavigateToSendScreen)
        composeTestRule.runOnIdle { fakeNavHostController.assertCurrentRoute("send_graph") }
        mutableEventFlow.tryEmit(
            VaultUnlockedNavBarEvent.Shortcut.NavigateToVaultScreen(
                labelRes = R.string.my_vault,
                contentDescRes = R.string.my_vault,
            ),
        )
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "vault_graph",
                navOptions = expectedNavOptions,
            )
        }
    }

    @Test
    fun `NavigateToSettingsScreen shortcut event should navigate to SettingsScreen`() {
        mutableEventFlow.tryEmit(VaultUnlockedNavBarEvent.NavigateToSendScreen)
        composeTestRule.runOnIdle { fakeNavHostController.assertCurrentRoute("send_graph") }
        mutableEventFlow.tryEmit(
            VaultUnlockedNavBarEvent.Shortcut.NavigateToSettingsScreen,
        )
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "settings_graph",
                navOptions = expectedNavOptions,
            )
        }
    }

    @Test
    fun `send tab click should send SendTabClick action`() {
        composeTestRule.onNodeWithText("Send").performClick()
        verify { viewModel.trySendAction(VaultUnlockedNavBarAction.SendTabClick) }
    }

    @Test
    fun `NavigateToSendScreen should navigate to SendScreen`() {
        composeTestRule.apply {
            runOnIdle { fakeNavHostController.assertCurrentRoute("vault_graph") }
            mutableEventFlow.tryEmit(VaultUnlockedNavBarEvent.NavigateToSendScreen)
            runOnIdle {
                fakeNavHostController.assertLastNavigation(
                    route = "send_graph",
                    navOptions = expectedNavOptions,
                )
            }
        }
    }

    @Test
    fun `generator tab click should send GeneratorTabClick action`() {
        composeTestRule.onNodeWithText("Generator").performClick()
        verify { viewModel.trySendAction(VaultUnlockedNavBarAction.GeneratorTabClick) }
    }

    @Test
    fun `NavigateToGeneratorScreen should navigate to GeneratorScreen`() {
        composeTestRule.apply {
            runOnIdle { fakeNavHostController.assertCurrentRoute("vault_graph") }
            mutableEventFlow.tryEmit(VaultUnlockedNavBarEvent.NavigateToGeneratorScreen)
            runOnIdle {
                fakeNavHostController.assertLastNavigation(
                    route = "generator_graph",
                    navOptions = expectedNavOptions,
                )
            }
        }
    }

    @Test
    fun `NavigateToGeneratorScreen  shortcut event should navigate to GeneratorScreen`() {
        composeTestRule.apply {
            runOnIdle { fakeNavHostController.assertCurrentRoute("vault_graph") }
            mutableEventFlow.tryEmit(VaultUnlockedNavBarEvent.Shortcut.NavigateToGeneratorScreen)
            runOnIdle {
                fakeNavHostController.assertLastNavigation(
                    route = "generator_graph",
                    navOptions = expectedNavOptions,
                )
            }
        }
    }

    @Test
    fun `settings tab click should send SendTabClick action`() {
        composeTestRule.onNodeWithText("Settings").performClick()
        verify { viewModel.trySendAction(VaultUnlockedNavBarAction.SettingsTabClick) }
    }

    @Test
    fun `NavigateToSettingsScreen should navigate to SettingsScreen`() {
        composeTestRule.apply {
            runOnIdle { fakeNavHostController.assertCurrentRoute("vault_graph") }
            mutableEventFlow.tryEmit(VaultUnlockedNavBarEvent.NavigateToSettingsScreen)
            runOnIdle {
                fakeNavHostController.assertLastNavigation(
                    route = "settings_graph",
                    navOptions = expectedNavOptions,
                )
            }
        }
    }

    @Test
    fun `vault nav bar should update according to state`() {
        composeTestRule.onNodeWithText("My vault").assertExists()
        composeTestRule.onNodeWithText("Vaults").assertDoesNotExist()

        mutableStateFlow.tryEmit(
            VaultUnlockedNavBarState(
                vaultNavBarLabelRes = R.string.vaults,
                vaultNavBarContentDescriptionRes = R.string.vaults,
                notificationState = VaultUnlockedNavBarNotificationState(
                    settingsTabNotificationCount = 0,
                ),
            ),
        )

        composeTestRule.onNodeWithText("My vault").assertDoesNotExist()
        composeTestRule.onNodeWithText("Vaults").assertExists()
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
            .onNodeWithText("1", useUnmergedTree = true)
            .assertExists()

        mutableStateFlow.update {
            it.copy(
                notificationState = VaultUnlockedNavBarNotificationState(
                    settingsTabNotificationCount = 0,
                ),
            )
        }
        composeTestRule
            .onNodeWithText("1", useUnmergedTree = true)
            .assertDoesNotExist()
    }
}

private val DEFAULT_STATE = VaultUnlockedNavBarState(
    vaultNavBarLabelRes = R.string.my_vault,
    vaultNavBarContentDescriptionRes = R.string.my_vault,
    notificationState = VaultUnlockedNavBarNotificationState(
        settingsTabNotificationCount = 0,
    ),
)
