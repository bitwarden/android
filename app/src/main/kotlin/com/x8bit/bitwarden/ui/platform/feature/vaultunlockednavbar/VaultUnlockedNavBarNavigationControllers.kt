package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import com.bitwarden.ui.platform.navigation.BottomNavigationController
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.rememberNavigationController
import com.x8bit.bitwarden.ui.platform.feature.settings.SettingsGraph
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorGraph
import com.x8bit.bitwarden.ui.tools.feature.send.SendGraph
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultGraph

/**
 * The [NavigationController] for the navigation graph of each tab of the
 * [VaultUnlockedNavBarGraph].
 */
data class VaultUnlockedNavBarNavigationControllers(
    override val root: NavigationController<VaultUnlockedNavBarGraph>,
    val vault: NavigationController<VaultGraph>,
    val send: NavigationController<SendGraph>,
    val generator: NavigationController<GeneratorGraph>,
    val settings: NavigationController<SettingsGraph>,
) : BottomNavigationController<VaultUnlockedNavBarGraph> {
    override val backStacks: List<List<NavKey>>
        get() = listOf(
            root.backStack.toList(),
            vault.backStack.toList(),
            send.backStack.toList(),
            generator.backStack.toList(),
            settings.backStack.toList(),
        )

    override fun navigateToTabOrPopToRoot(route: VaultUnlockedNavBarGraph) {
        if (root.backStack.last() == route) {
            when (route) {
                VaultUnlockedNavBarGraph.Generator -> generator.popToRoot()
                VaultUnlockedNavBarGraph.Send -> send.popToRoot()
                VaultUnlockedNavBarGraph.Settings -> settings.popToRoot()
                VaultUnlockedNavBarGraph.Vault -> vault.popToRoot()
            }
        } else {
            root.navigateToTopLevelRoute(route = route)
        }
    }

    override fun navigateBack(): Boolean {
        val isHandledByTab = when (root.backStack.last()) {
            VaultUnlockedNavBarGraph.Generator -> generator.navigateBack()
            VaultUnlockedNavBarGraph.Send -> send.navigateBack()
            VaultUnlockedNavBarGraph.Settings -> settings.navigateBack()
            VaultUnlockedNavBarGraph.Vault -> vault.navigateBack()
        }
        return isHandledByTab || root.navigateBack()
    }
}

/**
 * Remembers the [VaultUnlockedNavBarNavigationControllers].
 */
@Composable
fun rememberVaultUnlockedNavBarNavigationControllers(
    navBarNavigationController: NavigationController<VaultUnlockedNavBarGraph>,
): VaultUnlockedNavBarNavigationControllers {
    val vault = rememberNavigationController<VaultGraph>(routes = arrayOf(VaultGraph.VaultRoute))
    val send = rememberNavigationController<SendGraph>(routes = arrayOf(SendGraph.SendRoute))
    val generator = rememberNavigationController<GeneratorGraph>(
        routes = arrayOf(GeneratorGraph.GeneratorRoute),
    )
    val settings = rememberNavigationController<SettingsGraph>(
        routes = arrayOf(SettingsGraph.SettingsRoute),
    )
    return VaultUnlockedNavBarNavigationControllers(
        root = navBarNavigationController,
        vault = vault,
        send = send,
        generator = generator,
        settings = settings,
    )
}
