package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.navigateToTabOrRoot
import com.bitwarden.ui.platform.components.navigation.model.NavigationItem
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.scaffold.model.ScaffoldNavigationData
import com.bitwarden.ui.platform.theme.RootTransitionProviders
import com.bitwarden.ui.platform.util.toObjectNavigationRoute
import com.x8bit.bitwarden.ui.platform.components.util.rememberBitwardenNavController
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.feature.settings.about.navigateToAbout
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.navigateToAutoFill
import com.x8bit.bitwarden.ui.platform.feature.settings.navigateToSettingsGraphRoot
import com.x8bit.bitwarden.ui.platform.feature.settings.settingsGraph
import com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar.model.VaultUnlockedNavBarTab
import com.x8bit.bitwarden.ui.tools.feature.generator.generatorGraph
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.AddEditSendRoute
import com.x8bit.bitwarden.ui.tools.feature.send.sendGraph
import com.x8bit.bitwarden.ui.tools.feature.send.viewsend.ViewSendRoute
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditArgs
import com.x8bit.bitwarden.ui.vault.feature.importitems.navigateToImportItemsScreen
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemArgs
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultGraphRoute
import com.x8bit.bitwarden.ui.vault.feature.vault.vaultGraph
import kotlinx.collections.immutable.persistentListOf

/**
 * Top level composable for the Vault Unlocked Screen.
 */
@Suppress("LongParameterList", "LongMethod")
@Composable
fun VaultUnlockedNavBarScreen(
    viewModel: VaultUnlockedNavBarViewModel = hiltViewModel(),
    navController: NavHostController = rememberBitwardenNavController(
        name = "VaultUnlockedNavBarScreen",
    ),
    onNavigateToVaultAddItem: (args: VaultAddEditArgs) -> Unit,
    onNavigateToVaultItem: (args: VaultItemArgs) -> Unit,
    onNavigateToVaultEditItem: (args: VaultAddEditArgs) -> Unit,
    onNavigateToSearchSend: (searchType: SearchType.Sends) -> Unit,
    onNavigateToSearchVault: (searchType: SearchType.Vault) -> Unit,
    onNavigateToAddEditSend: (route: AddEditSendRoute) -> Unit,
    onNavigateToViewSend: (ViewSendRoute) -> Unit,
    onNavigateToDeleteAccount: () -> Unit,
    onNavigateToExportVault: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToPendingRequests: () -> Unit,
    onNavigateToPasswordHistory: () -> Unit,
    onNavigateToSetupUnlockScreen: () -> Unit,
    onNavigateToSetupAutoFillScreen: () -> Unit,
    onNavigateToSetupBrowserAutofill: () -> Unit,
    onNavigateToFlightRecorder: () -> Unit,
    onNavigateToRecordedLogs: () -> Unit,
    onNavigateToImportLogins: () -> Unit,
    onNavigateToAddFolderScreen: (selectedFolderId: String?) -> Unit,
    onNavigateToAboutPrivilegedApps: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel = viewModel) { event ->
        navController.navigateToTabOrRoot(target = event.tab)
    }

    VaultUnlockedNavBarScaffold(
        state = state,
        navController = navController,
        onNavigateToVaultItem = onNavigateToVaultItem,
        onNavigateToVaultEditItem = onNavigateToVaultEditItem,
        navigateToVaultAddItem = onNavigateToVaultAddItem,
        onNavigateToSearchSend = onNavigateToSearchSend,
        onNavigateToSearchVault = onNavigateToSearchVault,
        onNavigateToAddEditSend = onNavigateToAddEditSend,
        onNavigateToViewSend = onNavigateToViewSend,
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
        onNavigateToSetupBrowserAutofill = onNavigateToSetupBrowserAutofill,
        onNavigateToImportLogins = onNavigateToImportLogins,
        onNavigateToAddFolderScreen = onNavigateToAddFolderScreen,
        onNavigateToFlightRecorder = onNavigateToFlightRecorder,
        onNavigateToRecordedLogs = onNavigateToRecordedLogs,
        onNavigateToAboutPrivilegedApps = onNavigateToAboutPrivilegedApps,
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
    onNavigateToAddEditSend: (route: AddEditSendRoute) -> Unit,
    onNavigateToViewSend: (ViewSendRoute) -> Unit,
    navigateToDeleteAccount: () -> Unit,
    navigateToExportVault: () -> Unit,
    navigateToFolders: () -> Unit,
    navigateToPendingRequests: () -> Unit,
    navigateToPasswordHistory: () -> Unit,
    onNavigateToSetupUnlockScreen: () -> Unit,
    onNavigateToSetupAutoFillScreen: () -> Unit,
    onNavigateToSetupBrowserAutofill: () -> Unit,
    onNavigateToFlightRecorder: () -> Unit,
    onNavigateToRecordedLogs: () -> Unit,
    onNavigateToImportLogins: () -> Unit,
    onNavigateToAddFolderScreen: (selectedFolderId: String?) -> Unit,
    onNavigateToAboutPrivilegedApps: () -> Unit,
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
                navBackStackEntry.isCurrentRoute(route = it.graphRoute)
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
            startDestination = VaultGraphRoute,
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
                    navController.navigateToAbout(isPreAuth = false)
                },
                onNavigateToAutofillScreen = {
                    navController.navigateToSettingsGraphRoot()
                    navController.navigateToAutoFill()
                },
            )
            sendGraph(
                navController = navController,
                onNavigateToAddEditSend = onNavigateToAddEditSend,
                onNavigateToViewSend = onNavigateToViewSend,
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
                onNavigateToSetupBrowserAutofill = onNavigateToSetupBrowserAutofill,
                onNavigateToImportLogins = onNavigateToImportLogins,
                onNavigateToImportItems = { navController.navigateToImportItemsScreen() },
                onNavigateToFlightRecorder = onNavigateToFlightRecorder,
                onNavigateToRecordedLogs = onNavigateToRecordedLogs,
                onNavigateToAboutPrivilegedApps = onNavigateToAboutPrivilegedApps,
            )
        }
    }
}

/**
 * Determine if the current destination is the same as the given tab.
 */
private fun NavBackStackEntry?.isCurrentRoute(route: Any): Boolean =
    this
        ?.destination
        ?.hierarchy
        ?.any { it.route == route.toObjectNavigationRoute() } == true
