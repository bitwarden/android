package com.bitwarden.authenticator.ui.authenticator.feature.navbar

import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.ItemListingGraph
import com.bitwarden.authenticator.ui.platform.feature.settings.SettingsGraph
import com.bitwarden.ui.platform.navigation.util.navigationControllerOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthenticatorNavBarNavigationControllersTest {

    private val navigationControllers = AuthenticatorNavBarNavigationControllers(
        root = navigationControllerOf(AuthenticatorNavBarGraph.VerificationCodes),
        itemListing = navigationControllerOf(ItemListingGraph.ItemListingRoute),
        settings = navigationControllerOf(SettingsGraph.SettingsRoute),
    )

    @Test
    fun `navigateToTabOrPopToRoot should select a different tab above the start tab`() {
        navigationControllers.navigateToTabOrPopToRoot(route = AuthenticatorNavBarGraph.Settings)

        assertEquals(
            listOf(AuthenticatorNavBarGraph.VerificationCodes, AuthenticatorNavBarGraph.Settings),
            navigationControllers.root.backStack,
        )
    }

    @Test
    fun `navigateToTabOrPopToRoot should pop the selected tab to its root when reselected`() {
        navigationControllers.navigateToTabOrPopToRoot(route = AuthenticatorNavBarGraph.Settings)
        navigationControllers.settings.navigate(route = SettingsGraph.ExportRoute)

        navigationControllers.navigateToTabOrPopToRoot(route = AuthenticatorNavBarGraph.Settings)

        assertEquals(listOf(SettingsGraph.SettingsRoute), navigationControllers.settings.backStack)
        assertEquals(
            listOf(AuthenticatorNavBarGraph.VerificationCodes, AuthenticatorNavBarGraph.Settings),
            navigationControllers.root.backStack,
        )
    }

    @Test
    fun `navigateBack should pop the back stack of the selected tab first`() {
        navigationControllers.navigateToTabOrPopToRoot(route = AuthenticatorNavBarGraph.Settings)
        navigationControllers.settings.navigate(route = SettingsGraph.ExportRoute)

        val result = navigationControllers.navigateBack()

        assertTrue(result)
        assertEquals(listOf(SettingsGraph.SettingsRoute), navigationControllers.settings.backStack)
        assertEquals(
            listOf(AuthenticatorNavBarGraph.VerificationCodes, AuthenticatorNavBarGraph.Settings),
            navigationControllers.root.backStack,
        )
    }

    @Test
    fun `navigateBack at the root of another tab should return to the start tab`() {
        navigationControllers.navigateToTabOrPopToRoot(route = AuthenticatorNavBarGraph.Settings)

        val result = navigationControllers.navigateBack()

        assertTrue(result)
        assertEquals(
            listOf(AuthenticatorNavBarGraph.VerificationCodes),
            navigationControllers.root.backStack,
        )
    }

    @Test
    fun `navigateBack should retain the back stack of the tab that is left`() {
        navigationControllers.navigateToTabOrPopToRoot(route = AuthenticatorNavBarGraph.Settings)
        navigationControllers.settings.navigate(route = SettingsGraph.ExportRoute)
        navigationControllers.navigateToTabOrPopToRoot(
            route = AuthenticatorNavBarGraph.VerificationCodes,
        )

        navigationControllers.navigateToTabOrPopToRoot(route = AuthenticatorNavBarGraph.Settings)

        assertEquals(
            listOf(SettingsGraph.SettingsRoute, SettingsGraph.ExportRoute),
            navigationControllers.settings.backStack,
        )
    }

    @Test
    fun `navigateBack at the root of the start tab should not be handled`() {
        val result = navigationControllers.navigateBack()

        assertFalse(result)
        assertEquals(
            listOf(AuthenticatorNavBarGraph.VerificationCodes),
            navigationControllers.root.backStack,
        )
    }
}
