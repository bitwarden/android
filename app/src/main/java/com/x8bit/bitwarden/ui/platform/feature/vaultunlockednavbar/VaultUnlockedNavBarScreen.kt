package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar

import android.os.Parcelable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.base.util.EventsEffect
import com.x8bit.bitwarden.ui.components.PlaceholderComposable
import kotlinx.parcelize.Parcelize

/**
 * Top level composable for the Vault Unlocked Screen.
 */
@Composable
fun VaultUnlockedNavBarScreen(
    viewModel: VaultUnlockedNavBarViewModel = viewModel(),
    navController: NavHostController = rememberNavController(),
) {
    EventsEffect(viewModel = viewModel) { event ->
        navController.apply {
            val navOptions = vaultUnlockedNavBarScreenNavOptions()
            when (event) {
                VaultUnlockedNavBarEvent.NavigateToVaultScreenNavBar -> navigateToVault(navOptions)
                VaultUnlockedNavBarEvent.NavigateToSendScreen -> navigateToSend(navOptions)
                VaultUnlockedNavBarEvent.NavigateToGeneratorScreen -> navigateToGenerator(navOptions)
                VaultUnlockedNavBarEvent.NavigateToSettingsScreen -> navigateToSettings(navOptions)
            }
        }
    }
    VaultUnlockedNavBarScaffold(
        navController = navController,
        generatorTabClickedAction = { viewModel.trySendAction(VaultUnlockedNavBarAction.GeneratorTabClick) },
        sendTabClickedAction = { viewModel.trySendAction(VaultUnlockedNavBarAction.SendTabClick) },
        vaultTabClickedAction = { viewModel.trySendAction(VaultUnlockedNavBarAction.VaultTabClick) },
        settingsTabClickedAction = { viewModel.trySendAction(VaultUnlockedNavBarAction.SettingsTabClick) },
    )
}

/**
 * Scaffold that contains the bottom nav bar for the [VaultUnlockedNavBarScreen]
 */
@Composable
private fun VaultUnlockedNavBarScaffold(
    navController: NavHostController,
    vaultTabClickedAction: () -> Unit,
    sendTabClickedAction: () -> Unit,
    generatorTabClickedAction: () -> Unit,
    settingsTabClickedAction: () -> Unit,
) {
    var state by rememberSaveable {
        mutableStateOf<VaultUnlockedNavBarTab>(VaultUnlockedNavBarTab.Vault)
    }
    Scaffold(
        bottomBar = {
            BottomAppBar {
                val destinations = listOf(
                    VaultUnlockedNavBarTab.Vault,
                    VaultUnlockedNavBarTab.Send,
                    VaultUnlockedNavBarTab.Generator,
                    VaultUnlockedNavBarTab.Settings,
                )
                destinations.forEach { destination ->
                    NavigationBarItem(
                        modifier = Modifier.testTag(destination.route),
                        icon = {
                            Icon(
                                painter = painterResource(id = destination.iconRes),
                                contentDescription = stringResource(id = destination.contentDescriptionRes),
                            )
                        },
                        label = {
                            Text(text = stringResource(id = destination.labelRes))
                        },
                        selected = destination == state,
                        onClick = {
                            state = destination
                            when (destination) {
                                VaultUnlockedNavBarTab.Vault -> vaultTabClickedAction()
                                VaultUnlockedNavBarTab.Send -> sendTabClickedAction()
                                VaultUnlockedNavBarTab.Generator -> generatorTabClickedAction()
                                VaultUnlockedNavBarTab.Settings -> settingsTabClickedAction()
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = state.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            vaultDestination()
            sendDestination()
            generatorDestination()
            settingsDestination()
        }
    }
}

/**
 * Models tabs for the nav bar of the vault unlocked portion of the app.
 */
@Parcelize
private sealed class VaultUnlockedNavBarTab : Parcelable {
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
     * Show the Generator screen.
     */
    @Parcelize
    data object Generator : VaultUnlockedNavBarTab() {
        override val iconRes get() = R.drawable.generator_icon
        override val labelRes get() = R.string.generator_label
        override val contentDescriptionRes get() = R.string.generator_tab_content_description
        override val route get() = GENERATOR_ROUTE
    }

    /**
     * Show the Send screen.
     */
    @Parcelize
    data object Send : VaultUnlockedNavBarTab() {
        override val iconRes get() = R.drawable.send_icon
        override val labelRes get() = R.string.send_label
        override val contentDescriptionRes get() = R.string.send_tab_content_description
        override val route get() = SEND_ROUTE
    }

    /**
     * Show the Vault screen.
     */
    @Parcelize
    data object Vault : VaultUnlockedNavBarTab() {
        override val iconRes get() = R.drawable.sheild_icon
        override val labelRes get() = R.string.vault_label
        override val contentDescriptionRes get() = R.string.vault_tab_content_description
        override val route get() = VAULT_ROUTE
    }

    /**
     * Show the Settings screen.
     */
    @Parcelize
    data object Settings : VaultUnlockedNavBarTab() {
        override val iconRes get() = R.drawable.settings_icon
        override val labelRes get() = R.string.settings_label
        override val contentDescriptionRes get() = R.string.settings_tab_content_description
        override val route get() = SETTINGS_ROUTE
    }
}

/**
 * Helper function to generate [NavOptions] for [VaultUnlockedNavBarScreen].
 */
private fun NavController.vaultUnlockedNavBarScreenNavOptions(): NavOptions =
    navOptions {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }

/**
 * The functions below should be moved to their respective feature packages once they exist.
 *
 * For an example of how to setup these nav extensions, see NIA project.
 */

// #region Generator
/**
 * TODO: move to generator package (BIT-148)
 */
private const val GENERATOR_ROUTE = "generator"

/**
 * Add generator destination to the nav graph.
 *
 * TODO: move to generator package (BIT-148)
 */
private fun NavGraphBuilder.generatorDestination() {
    composable(GENERATOR_ROUTE) {
        PlaceholderComposable(text = "Generator")
    }
}

/**
 * Navigate to the generator screen. Note this will only work if generator screen was added
 * via [generatorDestination].
 *
 * TODO: move to generator package (BIT-148)
 *
 */
private fun NavController.navigateToGenerator(navOptions: NavOptions? = null) {
    navigate(GENERATOR_ROUTE, navOptions)
}
// #endregion Generator

// #region Send
/**
 * TODO: move to send package (BIT-149)
 */
private const val SEND_ROUTE = "send"

/**
 * Add send destination to the nav graph.
 *
 * TODO: move to send package (BIT-149)
 */
private fun NavGraphBuilder.sendDestination() {
    composable(SEND_ROUTE) {
        PlaceholderComposable(text = "Send")
    }
}

/**
 * Navigate to the send screen. Note this will only work if send screen was added
 * via [sendDestination].
 *
 * TODO: move to send package (BIT-149)
 *
 */
private fun NavController.navigateToSend(navOptions: NavOptions? = null) {
    navigate(SEND_ROUTE, navOptions)
}
// #endregion Send

// #region Settings
/**
 * TODO: move to settings package (BIT-147)
 */
private const val SETTINGS_ROUTE = "settings"

/**
 * Add settings destination to the nav graph.
 *
 * TODO: move to settings package (BIT-147)
 */
private fun NavGraphBuilder.settingsDestination() {
    composable(SETTINGS_ROUTE) {
        PlaceholderComposable(text = "Settings")
    }
}

/**
 * Navigate to the generator screen. Note this will only work if generator screen was added
 * via [settingsDestination].
 *
 * TODO: move to settings package (BIT-147)
 *
 */
private fun NavController.navigateToSettings(navOptions: NavOptions? = null) {
    navigate(SETTINGS_ROUTE, navOptions)
}
// #endregion Settings

// #region Vault
/**
 * TODO: move to vault package (BIT-178)
 */
private const val VAULT_ROUTE = "vault"

/**
 * Add vault destination to the nav graph.
 *
 * TODO: move to vault package (BIT-178)
 */
private fun NavGraphBuilder.vaultDestination() {
    composable(VAULT_ROUTE) {
        PlaceholderComposable(text = "Vault")
    }
}

/**
 * Navigate to the vault screen. Note this will only work if vault screen was added
 * via [vaultDestination].
 *
 * TODO: move to vault package (BIT-178)
 *
 */
private fun NavController.navigateToVault(navOptions: NavOptions? = null) {
    navigate(VAULT_ROUTE, navOptions)
}
// #endregion Vault
