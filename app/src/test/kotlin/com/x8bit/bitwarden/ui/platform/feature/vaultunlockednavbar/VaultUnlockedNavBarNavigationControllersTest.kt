package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar

import com.bitwarden.ui.platform.navigation.util.navigationControllerOf
import com.x8bit.bitwarden.ui.platform.feature.settings.SettingsGraph
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorGraph
import com.x8bit.bitwarden.ui.tools.feature.send.SendGraph
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultGraph
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VaultUnlockedNavBarNavigationControllersTest {

    private val navigationControllers = VaultUnlockedNavBarNavigationControllers(
        root = navigationControllerOf(VaultUnlockedNavBarGraph.Vault),
        vault = navigationControllerOf(VaultGraph.VaultRoute),
        send = navigationControllerOf(SendGraph.SendRoute),
        generator = navigationControllerOf(GeneratorGraph.GeneratorRoute),
        settings = navigationControllerOf(SettingsGraph.SettingsRoute),
    )

    @Test
    fun `navigateToTabOrPopToRoot should select a different tab above the start tab`() {
        navigationControllers.navigateToTabOrPopToRoot(route = VaultUnlockedNavBarGraph.Send)

        assertEquals(
            listOf(VaultUnlockedNavBarGraph.Vault, VaultUnlockedNavBarGraph.Send),
            navigationControllers.root.backStack,
        )
    }

    @Test
    fun `navigateToTabOrPopToRoot should replace the previously selected tab`() {
        navigationControllers.navigateToTabOrPopToRoot(route = VaultUnlockedNavBarGraph.Send)

        navigationControllers.navigateToTabOrPopToRoot(route = VaultUnlockedNavBarGraph.Settings)

        assertEquals(
            listOf(VaultUnlockedNavBarGraph.Vault, VaultUnlockedNavBarGraph.Settings),
            navigationControllers.root.backStack,
        )
    }

    @Test
    fun `navigateToTabOrPopToRoot should pop the selected tab to its root when reselected`() {
        navigationControllers.vault.navigate(route = VaultGraph.VerificationCodeRoute)

        navigationControllers.navigateToTabOrPopToRoot(route = VaultUnlockedNavBarGraph.Vault)

        assertEquals(listOf(VaultGraph.VaultRoute), navigationControllers.vault.backStack)
        assertEquals(
            listOf(VaultUnlockedNavBarGraph.Vault),
            navigationControllers.root.backStack,
        )
    }

    @Test
    fun `navigateBack should pop the back stack of the selected tab first`() {
        navigationControllers.navigateToTabOrPopToRoot(route = VaultUnlockedNavBarGraph.Settings)
        navigationControllers.settings.navigate(route = SettingsGraph.AboutRoute)

        val result = navigationControllers.navigateBack()

        assertTrue(result)
        assertEquals(listOf(SettingsGraph.SettingsRoute), navigationControllers.settings.backStack)
        assertEquals(
            listOf(VaultUnlockedNavBarGraph.Vault, VaultUnlockedNavBarGraph.Settings),
            navigationControllers.root.backStack,
        )
    }

    @Test
    fun `navigateBack at the root of another tab should return to the start tab`() {
        navigationControllers.navigateToTabOrPopToRoot(route = VaultUnlockedNavBarGraph.Generator)

        val result = navigationControllers.navigateBack()

        assertTrue(result)
        assertEquals(
            listOf(VaultUnlockedNavBarGraph.Vault),
            navigationControllers.root.backStack,
        )
    }

    @Test
    fun `navigateBack should retain the back stack of the tab that is left`() {
        navigationControllers.navigateToTabOrPopToRoot(route = VaultUnlockedNavBarGraph.Settings)
        navigationControllers.settings.navigate(route = SettingsGraph.AboutRoute)
        navigationControllers.navigateToTabOrPopToRoot(route = VaultUnlockedNavBarGraph.Send)

        navigationControllers.navigateToTabOrPopToRoot(route = VaultUnlockedNavBarGraph.Settings)

        assertEquals(
            listOf(SettingsGraph.SettingsRoute, SettingsGraph.AboutRoute),
            navigationControllers.settings.backStack,
        )
    }

    @Test
    fun `navigateBack within the start tab should pop its back stack`() {
        navigationControllers.vault.navigate(route = VaultGraph.VerificationCodeRoute)

        val result = navigationControllers.navigateBack()

        assertTrue(result)
        assertEquals(listOf(VaultGraph.VaultRoute), navigationControllers.vault.backStack)
    }

    @Test
    fun `navigateBack at the root of the start tab should not be handled`() {
        val result = navigationControllers.navigateBack()

        assertFalse(result)
        assertEquals(
            listOf(VaultUnlockedNavBarGraph.Vault),
            navigationControllers.root.backStack,
        )
    }
}
