package com.bitwarden.authenticator.ui.authenticator.feature.navbar

import android.os.Parcelable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.ItemListingGraphRoute
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.ItemListingRoute
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.itemListingGraph
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.navigateToItemListGraph
import com.bitwarden.authenticator.ui.platform.feature.settings.SettingsGraphRoute
import com.bitwarden.authenticator.ui.platform.feature.settings.SettingsRoute
import com.bitwarden.authenticator.ui.platform.feature.settings.navigateToSettingsGraph
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.components.navigation.model.NavigationItem
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.scaffold.model.ScaffoldNavigationData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.RootTransitionProviders
import com.bitwarden.ui.platform.util.toObjectNavigationRoute
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize

/**
 * Top level composable for the authenticator screens.
 */
@Composable
fun AuthenticatorNavBarScreen(
    viewModel: AuthenticatorNavBarViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
    onNavigateBack: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToQrCodeScanner: () -> Unit,
    onNavigateToManualKeyEntry: () -> Unit,
    onNavigateToEditItem: (itemId: String) -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToTutorial: () -> Unit,
) {
    EventsEffect(viewModel = viewModel) { event ->
        navController.apply {
            val navOptions = navController.authenticatorNavBarScreenNavOptions()
            when (event) {
                AuthenticatorNavBarEvent.NavigateToSettings -> {
                    navigateToSettingsGraph(navOptions)
                }

                AuthenticatorNavBarEvent.NavigateToVerificationCodes -> {
                    navigateToItemListGraph(navOptions)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        navController
            .currentBackStackEntryFlow
            .onEach {
                viewModel.trySendAction(AuthenticatorNavBarAction.BackStackUpdate)
            }
            .launchIn(this)
    }

    AuthenticatorNavBarScaffold(
        navController = navController,
        verificationTabClickedAction = {
            viewModel.trySendAction(AuthenticatorNavBarAction.VerificationCodesTabClick)
        },
        settingsTabClickedAction = {
            viewModel.trySendAction(AuthenticatorNavBarAction.SettingsTabClick)
        },
        navigateBack = onNavigateBack,
        navigateToSearch = onNavigateToSearch,
        navigateToQrCodeScanner = onNavigateToQrCodeScanner,
        navigateToManualKeyEntry = onNavigateToManualKeyEntry,
        navigateToEditItem = onNavigateToEditItem,
        navigateToExport = onNavigateToExport,
        navigateToImport = onNavigateToImport,
        navigateToTutorial = onNavigateToTutorial,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatorNavBarScaffold(
    navController: NavHostController,
    verificationTabClickedAction: () -> Unit,
    settingsTabClickedAction: () -> Unit,
    navigateBack: () -> Unit,
    navigateToSearch: () -> Unit,
    navigateToQrCodeScanner: () -> Unit,
    navigateToManualKeyEntry: () -> Unit,
    navigateToEditItem: (itemId: String) -> Unit,
    navigateToExport: () -> Unit,
    navigateToImport: () -> Unit,
    navigateToTutorial: () -> Unit,
) {
    var shouldDimNavBar by rememberSaveable { mutableStateOf(value = false) }

    // This scaffold will host screens that contain top bars while not hosting one itself.
    // We need to ignore the all insets here and let the content screens handle it themselves.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    BitwardenScaffold(
        contentWindowInsets = WindowInsets(),
        navigationData = ScaffoldNavigationData(
            navigationItems = AuthenticatorNavBarTab.navigationItems,
            selectedNavigationItem = AuthenticatorNavBarTab
                .navigationItems
                .find { navBackStackEntry.isCurrentRoute(route = it.graphRoute) },
            onNavigationClick = { navigationItem ->
                when (navigationItem) {
                    AuthenticatorNavBarTab.VerificationCodes -> verificationTabClickedAction()
                    AuthenticatorNavBarTab.Settings -> settingsTabClickedAction()
                }
            },
            shouldDimNavigation = shouldDimNavBar,
        ),
    ) {
        NavHost(
            navController = navController,
            startDestination = ItemListingGraphRoute,
            enterTransition = RootTransitionProviders.Enter.fadeIn,
            exitTransition = RootTransitionProviders.Exit.fadeOut,
            popEnterTransition = RootTransitionProviders.Enter.fadeIn,
            popExitTransition = RootTransitionProviders.Exit.fadeOut,
        ) {
            itemListingGraph(
                navController = navController,
                navigateBack = navigateBack,
                navigateToSearch = navigateToSearch,
                navigateToQrCodeScanner = navigateToQrCodeScanner,
                navigateToManualKeyEntry = navigateToManualKeyEntry,
                navigateToEditItem = navigateToEditItem,
                navigateToExport = navigateToExport,
                navigateToImport = navigateToImport,
                navigateToTutorial = navigateToTutorial,
            )
        }
    }
}

/**
 * Represents the different tabs available in the navigation bar
 * for the authenticator screens.
 *
 * Each tab is modeled with properties that provide information on:
 * - Regular icon resource
 * - Icon resource when selected
 * and other essential UI and navigational data.
 *
 * @property iconRes The resource ID for the regular (unselected) icon representing the tab.
 * @property iconResSelected The resource ID for the icon representing the tab when it's selected.
 */
@Parcelize
private sealed class AuthenticatorNavBarTab : NavigationItem, Parcelable {

    companion object {
        /**
         * The list of navigation tabs available in the authenticator.
         */
        val navigationItems: ImmutableList<AuthenticatorNavBarTab> = persistentListOf(
            VerificationCodes,
            Settings,
        )
    }

    /**
     * Show the Verification Codes screen.
     */
    @Parcelize
    data object VerificationCodes : AuthenticatorNavBarTab() {
        override val iconResSelected get() = BitwardenDrawable.ic_verification_codes_filled
        override val iconRes get() = BitwardenDrawable.ic_verification_codes
        override val labelRes get() = BitwardenString.verification_codes
        override val contentDescriptionRes get() = BitwardenString.verification_codes
        override val graphRoute get() = ItemListingGraphRoute
        override val startDestinationRoute get() = ItemListingRoute
        override val testTag get() = "VerificationCodesTab"
        override val notificationCount: Int get() = 0
    }

    /**
     * Show the Settings screen.
     */
    @Parcelize
    data object Settings : AuthenticatorNavBarTab() {
        override val iconResSelected get() = BitwardenDrawable.ic_settings_filled
        override val iconRes get() = BitwardenDrawable.ic_settings
        override val labelRes get() = BitwardenString.settings
        override val contentDescriptionRes get() = BitwardenString.settings
        override val graphRoute get() = SettingsGraphRoute
        override val startDestinationRoute get() = SettingsRoute
        override val testTag get() = "SettingsTab"
        override val notificationCount: Int get() = 0
    }
}

/**
 * Helper function to generate [NavOptions] for [AuthenticatorNavBarScreen].
 */
private fun NavController.authenticatorNavBarScreenNavOptions(): NavOptions =
    navOptions {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }

/**
 * Determine if the current destination is the same as the given tab.
 */
private fun NavBackStackEntry?.isCurrentRoute(route: Any): Boolean =
    this
        ?.destination
        ?.parent
        ?.route == route.toObjectNavigationRoute()
