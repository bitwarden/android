package com.bitwarden.authenticator.ui.authenticator.feature.navbar

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.ItemListingGraph
import com.bitwarden.authenticator.ui.platform.feature.settings.SettingsGraph
import com.bitwarden.ui.platform.navigation.BottomNavigationController
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.rememberNavigationController

/**
 * Contains the [NavigationController] for the [AuthenticatorNavBarGraph] and each underlying
 * tab `NavigationController`.
 */
data class AuthenticatorNavBarNavigationControllers(
    override val root: NavigationController<AuthenticatorNavBarGraph>,
    val itemListing: NavigationController<ItemListingGraph>,
    val settings: NavigationController<SettingsGraph>,
) : BottomNavigationController<AuthenticatorNavBarGraph> {
    override val backStacks: List<List<NavKey>>
        get() = listOf(
            root.backStack.toList(),
            itemListing.backStack.toList(),
            settings.backStack.toList(),
        )

    override fun navigateToTabOrPopToRoot(route: AuthenticatorNavBarGraph) {
        if (root.backStack.last() == route) {
            when (route) {
                AuthenticatorNavBarGraph.VerificationCodes -> itemListing.popToRoot()
                AuthenticatorNavBarGraph.Settings -> settings.popToRoot()
            }
        } else {
            root.navigateToTopLevelRoute(route = route)
        }
    }

    override fun navigateBack(): Boolean {
        val isHandledByTab = when (root.backStack.last()) {
            AuthenticatorNavBarGraph.VerificationCodes -> itemListing.navigateBack()
            AuthenticatorNavBarGraph.Settings -> settings.navigateBack()
        }
        return isHandledByTab || root.navigateBack()
    }
}

/**
 * Remembers the [AuthenticatorNavBarNavigationControllers].
 */
@Composable
fun rememberAuthenticatorNavBarNavigationControllers(
    navBarNavigationController: NavigationController<AuthenticatorNavBarGraph>,
): AuthenticatorNavBarNavigationControllers {
    val itemListing = rememberNavigationController<ItemListingGraph>(
        routes = arrayOf(ItemListingGraph.ItemListingRoute),
    )
    val settings = rememberNavigationController<SettingsGraph>(
        routes = arrayOf(SettingsGraph.SettingsRoute),
    )
    return AuthenticatorNavBarNavigationControllers(
        root = navBarNavigationController,
        itemListing = itemListing,
        settings = settings,
    )
}
