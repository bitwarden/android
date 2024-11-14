package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.material3.BottomAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.toDp
import com.x8bit.bitwarden.ui.platform.components.navigation.BitwardenNavigationBarItem
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.scrim.BitwardenAnimatedScrim
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.feature.settings.navigateToSettingsGraph
import com.x8bit.bitwarden.ui.platform.feature.settings.settingsGraph
import com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar.model.VaultUnlockedNavBarTab
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelay
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.theme.RootTransitionProviders
import com.x8bit.bitwarden.ui.tools.feature.generator.generatorGraph
import com.x8bit.bitwarden.ui.tools.feature.generator.navigateToGeneratorGraph
import com.x8bit.bitwarden.ui.tools.feature.send.navigateToSendGraph
import com.x8bit.bitwarden.ui.tools.feature.send.sendGraph
import com.x8bit.bitwarden.ui.vault.feature.vault.VAULT_GRAPH_ROUTE
import com.x8bit.bitwarden.ui.vault.feature.vault.navigateToVaultGraph
import com.x8bit.bitwarden.ui.vault.feature.vault.vaultGraph
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType

/**
 * Top level composable for the Vault Unlocked Screen.
 */
@Suppress("LongParameterList", "LongMethod")
@Composable
fun VaultUnlockedNavBarScreen(
    viewModel: VaultUnlockedNavBarViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
    onNavigateToVaultAddItem: (VaultItemCipherType, String?, String?) -> Unit,
    onNavigateToVaultItem: (vaultItemId: String) -> Unit,
    onNavigateToVaultEditItem: (vaultItemId: String) -> Unit,
    onNavigateToSearchSend: (searchType: SearchType.Sends) -> Unit,
    onNavigateToSearchVault: (searchType: SearchType.Vault) -> Unit,
    onNavigateToAddSend: () -> Unit,
    onNavigateToEditSend: (sendItemId: String) -> Unit,
    onNavigateToDeleteAccount: () -> Unit,
    onNavigateToExportVault: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToPendingRequests: () -> Unit,
    onNavigateToPasswordHistory: () -> Unit,
    onNavigateToSetupUnlockScreen: () -> Unit,
    onNavigateToSetupAutoFillScreen: () -> Unit,
    onNavigateToImportLogins: (SnackbarRelay) -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel = viewModel) { event ->
        navController.apply {
            val navOptions = vaultUnlockedNavBarScreenNavOptions(tabToNavigateTo = event.tab)
            when (event) {
                is VaultUnlockedNavBarEvent.Shortcut.NavigateToVaultScreen,
                is VaultUnlockedNavBarEvent.NavigateToVaultScreen,
                    -> {
                    navigateToVaultGraph(navOptions)
                }

                VaultUnlockedNavBarEvent.NavigateToSendScreen -> {
                    navigateToSendGraph(navOptions)
                }

                VaultUnlockedNavBarEvent.Shortcut.NavigateToGeneratorScreen,
                VaultUnlockedNavBarEvent.NavigateToGeneratorScreen,
                    -> {
                    navigateToGeneratorGraph(navOptions)
                }

                VaultUnlockedNavBarEvent.NavigateToSettingsScreen -> {
                    navigateToSettingsGraph(navOptions)
                }

                VaultUnlockedNavBarEvent.Shortcut.NavigateToSettingsScreen -> {
                    navigateToSettingsGraph(navOptions)
                }
            }
        }
    }

    VaultUnlockedNavBarScaffold(
        state = state,
        navController = navController,
        onNavigateToVaultItem = onNavigateToVaultItem,
        onNavigateToVaultEditItem = onNavigateToVaultEditItem,
        navigateToVaultAddItem = onNavigateToVaultAddItem,
        onNavigateToSearchSend = onNavigateToSearchSend,
        onNavigateToSearchVault = onNavigateToSearchVault,
        navigateToAddSend = onNavigateToAddSend,
        onNavigateToEditSend = onNavigateToEditSend,
        navigateToDeleteAccount = onNavigateToDeleteAccount,
        navigateToExportVault = onNavigateToExportVault,
        navigateToFolders = onNavigateToFolders,
        navigateToPendingRequests = onNavigateToPendingRequests,
        navigateToPasswordHistory = onNavigateToPasswordHistory,
        generatorTabClickedAction = remember(viewModel) {
            { viewModel.trySendAction(VaultUnlockedNavBarAction.GeneratorTabClick) }
        },
        sendTabClickedAction = remember(viewModel) {
            { viewModel.trySendAction(VaultUnlockedNavBarAction.SendTabClick) }
        },
        vaultTabClickedAction = remember(viewModel) {
            { viewModel.trySendAction(VaultUnlockedNavBarAction.VaultTabClick) }
        },
        settingsTabClickedAction = remember(viewModel) {
            { viewModel.trySendAction(VaultUnlockedNavBarAction.SettingsTabClick) }
        },
        onNavigateToSetupUnlockScreen = onNavigateToSetupUnlockScreen,
        onNavigateToSetupAutoFillScreen = onNavigateToSetupAutoFillScreen,
        onNavigateToImportLogins = onNavigateToImportLogins,
    )
}

/**
 * Scaffold that contains the bottom nav bar for the [VaultUnlockedNavBarScreen]
 */
@Composable
@Suppress("LongMethod")
private fun VaultUnlockedNavBarScaffold(
    state: VaultUnlockedNavBarState,
    navController: NavHostController,
    vaultTabClickedAction: () -> Unit,
    sendTabClickedAction: () -> Unit,
    generatorTabClickedAction: () -> Unit,
    settingsTabClickedAction: () -> Unit,
    navigateToVaultAddItem: (VaultItemCipherType, String?, String?) -> Unit,
    onNavigateToVaultItem: (vaultItemId: String) -> Unit,
    onNavigateToVaultEditItem: (vaultItemId: String) -> Unit,
    onNavigateToSearchSend: (searchType: SearchType.Sends) -> Unit,
    onNavigateToSearchVault: (searchType: SearchType.Vault) -> Unit,
    navigateToAddSend: () -> Unit,
    onNavigateToEditSend: (sendItemId: String) -> Unit,
    navigateToDeleteAccount: () -> Unit,
    navigateToExportVault: () -> Unit,
    navigateToFolders: () -> Unit,
    navigateToPendingRequests: () -> Unit,
    navigateToPasswordHistory: () -> Unit,
    onNavigateToSetupUnlockScreen: () -> Unit,
    onNavigateToSetupAutoFillScreen: () -> Unit,
    onNavigateToImportLogins: (SnackbarRelay) -> Unit,
) {
    var shouldDimNavBar by remember { mutableStateOf(false) }

    // This scaffold will host screens that contain top bars while not hosting one itself.
    // We need to ignore the all insets here and let the content screens handle it themselves.
    BitwardenScaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            Box {
                var appBarHeightPx by remember { mutableIntStateOf(0) }
                VaultBottomAppBar(
                    state = state,
                    navController = navController,
                    vaultTabClickedAction = vaultTabClickedAction,
                    sendTabClickedAction = sendTabClickedAction,
                    generatorTabClickedAction = generatorTabClickedAction,
                    settingsTabClickedAction = settingsTabClickedAction,
                    modifier = Modifier
                        .testTag("NavigationBarContainer")
                        .onGloballyPositioned {
                            appBarHeightPx = it.size.height
                        },
                )
                BitwardenAnimatedScrim(
                    isVisible = shouldDimNavBar,
                    onClick = {
                        // Do nothing
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(appBarHeightPx.toDp()),
                )
            }
        },
    ) {
        // Because this Scaffold has a bottom navigation bar, the NavHost will:
        // - consume the vertical navigation bar insets.
        // - consume the IME insets.
        NavHost(
            navController = navController,
            startDestination = VAULT_GRAPH_ROUTE,
            modifier = Modifier
                .consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Vertical))
                .consumeWindowInsets(WindowInsets.ime),
            enterTransition = RootTransitionProviders.Enter.fadeIn,
            exitTransition = RootTransitionProviders.Exit.fadeOut,
            popEnterTransition = RootTransitionProviders.Enter.fadeIn,
            popExitTransition = RootTransitionProviders.Exit.fadeOut,
        ) {
            vaultGraph(
                navController = navController,
                onNavigateToVaultAddItemScreen = navigateToVaultAddItem,
                onNavigateToVaultItemScreen = onNavigateToVaultItem,
                onNavigateToVaultEditItemScreen = onNavigateToVaultEditItem,
                onNavigateToSearchVault = onNavigateToSearchVault,
                onDimBottomNavBarRequest = { shouldDim ->
                    shouldDimNavBar = shouldDim
                },
                onNavigateToImportLogins = onNavigateToImportLogins,
            )
            sendGraph(
                navController = navController,
                onNavigateToAddSend = navigateToAddSend,
                onNavigateToEditSend = onNavigateToEditSend,
                onNavigateToSearchSend = onNavigateToSearchSend,
            )
            generatorGraph(
                onNavigateToPasswordHistory = { navigateToPasswordHistory() },
            )
            settingsGraph(
                navController = navController,
                onNavigateToDeleteAccount = navigateToDeleteAccount,
                onNavigateToExportVault = navigateToExportVault,
                onNavigateToFolders = navigateToFolders,
                onNavigateToPendingRequests = navigateToPendingRequests,
                onNavigateToSetupUnlockScreen = onNavigateToSetupUnlockScreen,
                onNavigateToSetupAutoFillScreen = onNavigateToSetupAutoFillScreen,
                onNavigateToImportLogins = onNavigateToImportLogins,
            )
        }
    }
}

@Composable
private fun VaultBottomAppBar(
    state: VaultUnlockedNavBarState,
    navController: NavHostController,
    vaultTabClickedAction: () -> Unit,
    sendTabClickedAction: () -> Unit,
    generatorTabClickedAction: () -> Unit,
    settingsTabClickedAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BottomAppBar(
        containerColor = BitwardenTheme.colorScheme.background.secondary,
        modifier = modifier,
    ) {
        val destinations = listOf(
            VaultUnlockedNavBarTab.Vault(
                labelRes = state.vaultNavBarLabelRes,
                contentDescriptionRes = state.vaultNavBarContentDescriptionRes,
            ),
            VaultUnlockedNavBarTab.Send,
            VaultUnlockedNavBarTab.Generator,
            VaultUnlockedNavBarTab.Settings(state.notificationState.settingsTabNotificationCount),
        )
        // Collecting the back stack entry here as state is crucial to ensuring that the items
        // below recompose when the navigation state changes to update the selected tab.
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        destinations.forEach { destination ->
            BitwardenNavigationBarItem(
                labelRes = destination.labelRes,
                contentDescriptionRes = destination.contentDescriptionRes,
                selectedIconRes = destination.iconResSelected,
                unselectedIconRes = destination.iconRes,
                notificationCount = destination.notificationCount,
                isSelected = navBackStackEntry.isCurrentTab(tab = destination),
                onClick = when (destination) {
                    is VaultUnlockedNavBarTab.Vault -> vaultTabClickedAction
                    VaultUnlockedNavBarTab.Send -> sendTabClickedAction
                    VaultUnlockedNavBarTab.Generator -> generatorTabClickedAction
                    is VaultUnlockedNavBarTab.Settings -> settingsTabClickedAction
                },
                modifier = Modifier.testTag(tag = destination.testTag),
            )
        }
    }
}

/**
 * Helper function to generate [NavOptions] for [VaultUnlockedNavBarScreen].
 *
 * @param tabToNavigateTo The [VaultUnlockedNavBarTab] to prepare the NavOptions for.
 * NavOptions are determined on whether or not the tab is already selected.
 */
private fun NavController.vaultUnlockedNavBarScreenNavOptions(
    tabToNavigateTo: VaultUnlockedNavBarTab,
): NavOptions {
    val returnToCurrentSubRoot = currentBackStackEntry.isCurrentTab(tabToNavigateTo)
    val currentSubRootGraph = currentDestination?.parent?.id
    // determine the destination to navigate to, if we are navigating to the same sub-root for the
    // selected tab we want to find the start destination of the sub-root and pop up to it, which
    // will maintain its state (i.e. scroll position). If we are navigating to a different sub-root,
    // we can safely pop up to the start of the graph, the "home" tab destination.
    val popUpToDestination = graph
        .getSubgraphStartDestinationOrNull(currentSubRootGraph)
        .takeIf { returnToCurrentSubRoot }
        ?: graph.findStartDestination().id
    // If we are popping up the start of the whole nav graph we want to maintain the state of the
    // the popped destinations in the other sub-roots. If we are navigating to the same sub-root,
    // we want to pop off the nested destinations without maintaining their state.
    val maintainStateOfPoppedDestinations = !returnToCurrentSubRoot
    return navOptions {
        popUpTo(popUpToDestination) {
            saveState = maintainStateOfPoppedDestinations
        }
        launchSingleTop = true
        restoreState = maintainStateOfPoppedDestinations
    }
}

/**
 * Determine if the current destination is the same as the given tab.
 */
private fun NavBackStackEntry?.isCurrentTab(tab: VaultUnlockedNavBarTab): Boolean =
    this?.destination?.hierarchy?.any {
        it.route == tab.route
    } == true

/**
 * Helper function to determine the start destination of a subgraph.
 *
 * @param subgraphId the id of the subgraph to find the start destination of.
 *
 * @return the ID of the start destination of the subgraph, or null if the subgraph does not exist.
 */
private fun NavGraph.getSubgraphStartDestinationOrNull(subgraphId: Int?): Int? {
    subgraphId ?: return null
    return nodes[subgraphId]?.let {
        (it as? NavGraph)?.findStartDestination()?.id
    }
}
