package com.bitwarden.authenticator.ui.authenticator.feature.navbar

import android.os.Parcelable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.ITEM_LISTING_GRAPH_ROUTE
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.ITEM_LIST_ROUTE
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.itemListingGraph
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.navigateToItemListGraph
import com.bitwarden.authenticator.ui.platform.base.util.EventsEffect
import com.bitwarden.authenticator.ui.platform.base.util.max
import com.bitwarden.authenticator.ui.platform.base.util.toDp
import com.bitwarden.authenticator.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.authenticator.ui.platform.components.scrim.BitwardenAnimatedScrim
import com.bitwarden.authenticator.ui.platform.feature.settings.SETTINGS_GRAPH_ROUTE
import com.bitwarden.authenticator.ui.platform.feature.settings.navigateToSettingsGraph
import com.bitwarden.authenticator.ui.platform.theme.RootTransitionProviders
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
    navigateToTutorial: () -> Unit,
) {
    BitwardenScaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.statusBars),
        bottomBar = {
            Box {
                var appBarHeightPx by remember { mutableIntStateOf(0) }
                AuthenticatorBottomAppBar(
                    modifier = Modifier
                        .onGloballyPositioned {
                            appBarHeightPx = it.size.height
                        },
                    navController = navController,
                    verificationCodesTabClickedAction = verificationTabClickedAction,
                    settingsTabClickedAction = settingsTabClickedAction,
                )
                BitwardenAnimatedScrim(
                    isVisible = false,
                    onClick = {
                        // Do nothing
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(appBarHeightPx.toDp())
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ITEM_LISTING_GRAPH_ROUTE,
            modifier = Modifier
                .consumeWindowInsets(WindowInsets.navigationBars)
                .consumeWindowInsets(WindowInsets.ime)
                .padding(innerPadding.max(WindowInsets.ime)),
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
                navigateToTutorial = navigateToTutorial,
            )
        }
    }
}

@Composable
private fun AuthenticatorBottomAppBar(
    navController: NavController,
    verificationCodesTabClickedAction: () -> Unit,
    settingsTabClickedAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier,
    ) {
        val destinations = listOf(
            AuthenticatorNavBarTab.VerificationCodes,
            AuthenticatorNavBarTab.Settings,
        )
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        destinations.forEach { destination ->
            val isSelected = currentDestination?.hierarchy?.any {
                it.route == destination.route
            } == true

            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(
                            id = if (isSelected) {
                                destination.iconResSelected
                            } else {
                                destination.iconRes
                            },
                        ),
                        contentDescription = stringResource(
                            id = destination.contentDescriptionRes,
                        ),
                    )
                },
                label = {
                    Text(
                        text = stringResource(id = destination.labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                selected = isSelected,
                onClick = {
                    when (destination) {
                        AuthenticatorNavBarTab.VerificationCodes -> {
                            verificationCodesTabClickedAction()
                        }

                        AuthenticatorNavBarTab.Settings -> {
                            settingsTabClickedAction()
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface,
                    selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.semantics { testTag = destination.testTag },
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
private sealed class AuthenticatorNavBarTab : Parcelable {
    /**
     * The resource ID for the icon representing the tab when it is selected.
     */
    abstract val iconResSelected: Int

    /**
     * Resource id for the icon representing the tab.
     */
    abstract val iconRes: Int

    /**
     * Resource id for the label describing the tab.
     */
    abstract val labelRes: Int

    /**
     * Resource id for the content description describing the tab.
     */
    abstract val contentDescriptionRes: Int

    /**
     * Route of the tab.
     */
    abstract val route: String

    /**
     * The test tag of the tab.
     */
    abstract val testTag: String

    /**
     * Show the Verification Codes screen.
     */
    @Parcelize
    data object VerificationCodes : AuthenticatorNavBarTab() {
        override val iconResSelected get() = R.drawable.ic_verification_codes_filled
        override val iconRes get() = R.drawable.ic_verification_codes
        override val labelRes get() = R.string.verification_codes
        override val contentDescriptionRes get() = R.string.verification_codes
        override val route get() = ITEM_LIST_ROUTE
        override val testTag get() = "VerificationCodesTab"
    }

    /**
     * Show the Settings screen.
     */
    @Parcelize
    data object Settings : AuthenticatorNavBarTab() {
        override val iconResSelected get() = R.drawable.ic_settings_filled
        override val iconRes get() = R.drawable.ic_settings
        override val labelRes get() = R.string.settings
        override val contentDescriptionRes get() = R.string.settings

        // TODO: Replace with constant when settings screen is complete.
        override val route get() = SETTINGS_GRAPH_ROUTE
        override val testTag get() = "SettingsTab"
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
