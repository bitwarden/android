package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.core.util.persistentListOfNotNull
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.components.navigation.model.NavigationItem
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.scaffold.model.ScaffoldNavigationData
import com.bitwarden.ui.platform.navigation.BottomNavigationController
import com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar.model.VaultUnlockedNavBarTab

/**
 * Top level composable for the Vault Unlocked Screen.
 */
@Composable
fun VaultUnlockedNavBarScreen(
    viewModel: VaultUnlockedNavBarViewModel = hiltViewModel(),
    bottomNavigationController: BottomNavigationController<VaultUnlockedNavBarGraph>,
    content: @Composable (onDimNavBarRequest: (shouldDim: Boolean) -> Unit) -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel = viewModel) { event ->
        bottomNavigationController.navigateToTabOrPopToRoot(route = event.tab.rootRoute)
    }

    VaultUnlockedNavBarScaffold(
        state = state,
        selectedTab = bottomNavigationController.root.backStack.last(),
        generatorTabClickedAction = {
            viewModel.trySendAction(VaultUnlockedNavBarAction.GeneratorTabClick)
        },
        sendTabClickedAction = { viewModel.trySendAction(VaultUnlockedNavBarAction.SendTabClick) },
        vaultTabClickedAction = {
            viewModel.trySendAction(VaultUnlockedNavBarAction.VaultTabClick)
        },
        settingsTabClickedAction = {
            viewModel.trySendAction(VaultUnlockedNavBarAction.SettingsTabClick)
        },
        content = content,
    )
}

/**
 * Scaffold that contains the bottom nav bar for the [VaultUnlockedNavBarScreen]
 */
@Composable
private fun VaultUnlockedNavBarScaffold(
    state: VaultUnlockedNavBarState,
    selectedTab: VaultUnlockedNavBarGraph,
    vaultTabClickedAction: () -> Unit,
    sendTabClickedAction: () -> Unit,
    generatorTabClickedAction: () -> Unit,
    settingsTabClickedAction: () -> Unit,
    content: @Composable (onDimNavBarRequest: (shouldDim: Boolean) -> Unit) -> Unit,
) {
    var shouldDimNavBar by rememberSaveable { mutableStateOf(value = false) }

    // This scaffold will host screens that contain top bars while not hosting one itself.
    // We need to ignore the all insets here and let the content screens handle it themselves.
    val navigationItems = persistentListOfNotNull<NavigationItem>(
        VaultUnlockedNavBarTab.Vault(labelRes = state.vaultNavBarLabelRes),
        VaultUnlockedNavBarTab.Send.takeUnless { state.areSendsDisabled },
        VaultUnlockedNavBarTab.Generator,
        VaultUnlockedNavBarTab.Settings(state.notificationState.settingsTabNotificationCount),
    )
    BitwardenScaffold(
        contentWindowInsets = WindowInsets(0.dp),
        navigationData = ScaffoldNavigationData(
            navigationItems = navigationItems,
            selectedNavigationItem = navigationItems.find { it.rootRoute == selectedTab },
            onNavigationClick = { navigationItem ->
                when (navigationItem) {
                    VaultUnlockedNavBarTab.Generator -> generatorTabClickedAction()
                    VaultUnlockedNavBarTab.Send -> sendTabClickedAction()
                    is VaultUnlockedNavBarTab.Settings -> settingsTabClickedAction()
                    is VaultUnlockedNavBarTab.Vault -> vaultTabClickedAction()
                }
            },
            shouldDimNavigation = shouldDimNavBar,
        ),
    ) {
        // Because this Scaffold has a bottom navigation bar, the NavDisplay will:
        // - consume the vertical navigation bar insets.
        // - consume the IME insets.
        content { shouldDim -> shouldDimNavBar = shouldDim }
    }
}
