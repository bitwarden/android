package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.x8bit.bitwarden.ui.platform.components.model.NavigationItem
import com.x8bit.bitwarden.ui.platform.components.model.ScaffoldNavigationData
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.feature.settings.about.navigateToAbout
import com.x8bit.bitwarden.ui.platform.feature.settings.navigateToSettingsGraph
import com.x8bit.bitwarden.ui.platform.feature.settings.navigateToSettingsGraphRoot
import com.x8bit.bitwarden.ui.platform.feature.settings.settingsGraph
import com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar.model.VaultUnlockedNavBarTab
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelay
import com.x8bit.bitwarden.ui.platform.theme.RootTransitionProviders
import com.x8bit.bitwarden.ui.tools.feature.generator.generatorGraph
import com.x8bit.bitwarden.ui.tools.feature.generator.navigateToGeneratorGraph
import com.x8bit.bitwarden.ui.tools.feature.send.navigateToSendGraph
import com.x8bit.bitwarden.ui.tools.feature.send.sendGraph
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditArgs
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemArgs
import com.x8bit.bitwarden.ui.vault.feature.vault.VAULT_GRAPH_ROUTE
import com.x8bit.bitwarden.ui.vault.feature.vault.navigateToVaultGraph
import com.x8bit.bitwarden.ui.vault.feature.vault.vaultGraph
import kotlinx.collections.immutable.persistentListOf

/**
 * Top level composable for the Vault Unlocked Screen.
 */
@Suppress("LongParameterList", "LongMethod")
@Composable
fun VaultUnlockedNavBarScreen(
    viewModel: VaultUnlockedNavBarViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
    onNavigateToVaultAddItem: (args: VaultAddEditArgs) -> Unit,
    onNavigateToVaultItem: (args: VaultItemArgs) -> Unit,
    onNavigateToVaultEditItem: (args: VaultAddEditArgs) -> Unit,
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
    onNavigateToFlightRecorder: () -> Unit,
    onNavigateToRecordedLogs: () -> Unit,
    onNavigateToImportLogins: (SnackbarRelay) -> Unit,
    onNavigateToAddFolderScreen: (selectedFolderId: String?) -> Unit,
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

                VaultUnlockedNavBarEvent.Shortcut.NavigateToSendScreen,
                VaultUnlockedNavBarEvent.NavigateToSendScreen,
                    -> {
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
        onNavigateToAddFolderScreen = onNavigateToAddFolderScreen,
        onNavigateToFlightRecorder = onNavigateToFlightRecorder,
        onNavigateToRecordedLogs = onNavigateToRecordedLogs,
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
    navigateToVaultAddItem: (args: VaultAddEditArgs) -> Unit,
    onNavigateToVaultItem: (args: VaultItemArgs) -> Unit,
    onNavigateToVaultEditItem: (args: VaultAddEditArgs) -> Unit,
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
    onNavigateToFlightRecorder: () -> Unit,
    onNavigateToRecordedLogs: () -> Unit,
    onNavigateToImportLogins: (SnackbarRelay) -> Unit,
    onNavigateToAddFolderScreen: (selectedFolderId: String?) -> Unit,
) {
    var shouldDimNavBar by rememberSaveable { mutableStateOf(value = false) }

    // This scaffold will host screens that contain top bars while not hosting one itself.
    // We need to ignore the all insets here and let the content screens handle it themselves.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val navigationItems = persistentListOf<NavigationItem>(
        VaultUnlockedNavBarTab.Vault(
            labelRes = state.vaultNavBarLabelRes,
            contentDescriptionRes = state.vaultNavBarContentDescriptionRes,
        ),
        VaultUnlockedNavBarTab.Send,
        VaultUnlockedNavBarTab.Generator,
        VaultUnlockedNavBarTab.Settings(state.notificationState.settingsTabNotificationCount),
    )
    BitwardenScaffold(
        contentWindowInsets = WindowInsets(0.dp),
        navigationData = ScaffoldNavigationData(
            navigationItems = navigationItems,
            selectedNavigationItem = navigationItems.find {
                navBackStackEntry.isCurrentRoute(route = it.route)
            },
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
        // Because this Scaffold has a bottom navigation bar, the NavHost will:
        // - consume the vertical navigation bar insets.
        // - consume the IME insets.
        NavHost(
            navController = navController,
            startDestination = VAULT_GRAPH_ROUTE,
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
                onDimBottomNavBarRequest = { shouldDim -> shouldDimNavBar = shouldDim },
                onNavigateToImportLogins = onNavigateToImportLogins,
                onNavigateToAddFolderScreen = onNavigateToAddFolderScreen,
                onNavigateToAboutScreen = {
                    navController.navigateToSettingsGraphRoot()
                    navController.navigateToAbout()
                },
            )
            sendGraph(
                navController = navController,
                onNavigateToAddSend = navigateToAddSend,
                onNavigateToEditSend = onNavigateToEditSend,
                onNavigateToSearchSend = onNavigateToSearchSend,
            )
            generatorGraph(
                onNavigateToPasswordHistory = { navigateToPasswordHistory() },
                onDimNavBarRequest = { shouldDim -> shouldDimNavBar = shouldDim },
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
                onNavigateToFlightRecorder = onNavigateToFlightRecorder,
                onNavigateToRecordedLogs = onNavigateToRecordedLogs,
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
    val returnToCurrentSubRoot = currentBackStackEntry.isCurrentRoute(tabToNavigateTo.route)
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
private fun NavBackStackEntry?.isCurrentRoute(route: String): Boolean =
    this
        ?.destination
        ?.hierarchy
        ?.any { it.route == route } == true

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
