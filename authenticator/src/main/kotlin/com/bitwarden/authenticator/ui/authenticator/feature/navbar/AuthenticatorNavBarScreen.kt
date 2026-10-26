package com.bitwarden.authenticator.ui.authenticator.feature.navbar

import android.os.Parcelable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.components.navigation.model.NavigationItem
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.scaffold.model.ScaffoldNavigationData
import com.bitwarden.ui.platform.navigation.BottomNavigationController
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.parcelize.Parcelize

/**
 * Top level composable for the authenticator screens.
 */
@Composable
fun AuthenticatorNavBarScreen(
    viewModel: AuthenticatorNavBarViewModel = hiltViewModel(),
    bottomNavigationController: BottomNavigationController<AuthenticatorNavBarGraph>,
    content: @Composable () -> Unit,
) {
    EventsEffect(viewModel = viewModel) { event ->
        bottomNavigationController.navigateToTabOrPopToRoot(route = event.tab.rootRoute)
    }

    LaunchedEffect(key1 = bottomNavigationController) {
        snapshotFlow { bottomNavigationController.backStacks }.collect {
            viewModel.trySendAction(AuthenticatorNavBarAction.BackStackUpdate)
        }
    }

    AuthenticatorNavBarScaffold(
        selectedTab = bottomNavigationController.root.backStack.last(),
        verificationTabClickedAction = {
            viewModel.trySendAction(AuthenticatorNavBarAction.VerificationCodesTabClick)
        },
        settingsTabClickedAction = {
            viewModel.trySendAction(AuthenticatorNavBarAction.SettingsTabClick)
        },
        content = content,
    )
}

@Composable
private fun AuthenticatorNavBarScaffold(
    selectedTab: AuthenticatorNavBarGraph,
    verificationTabClickedAction: () -> Unit,
    settingsTabClickedAction: () -> Unit,
    content: @Composable () -> Unit,
) {
    var shouldDimNavBar by rememberSaveable { mutableStateOf(value = false) }

    // This scaffold will host screens that contain top bars while not hosting one itself.
    // We need to ignore the all insets here and let the content screens handle it themselves.
    BitwardenScaffold(
        contentWindowInsets = WindowInsets(),
        navigationData = ScaffoldNavigationData(
            navigationItems = AuthenticatorNavBarTab.navigationItems,
            selectedNavigationItem = AuthenticatorNavBarTab
                .navigationItems
                .find { it.rootRoute == selectedTab },
            onNavigationClick = { navigationItem ->
                when (navigationItem) {
                    AuthenticatorNavBarTab.VerificationCodes -> verificationTabClickedAction()
                    AuthenticatorNavBarTab.Settings -> settingsTabClickedAction()
                }
            },
            shouldDimNavigation = shouldDimNavBar,
        ),
        content = content,
    )
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
sealed class AuthenticatorNavBarTab : NavigationItem, Parcelable {
    /**
     * The route of the tab within the [AuthenticatorNavBarGraph].
     */
    abstract override val rootRoute: AuthenticatorNavBarGraph

    @Suppress("UndocumentedPublicClass")
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
        override val rootRoute get() = AuthenticatorNavBarGraph.VerificationCodes
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
        override val rootRoute get() = AuthenticatorNavBarGraph.Settings
        override val testTag get() = "SettingsTab"
        override val notificationCount: Int get() = 0
    }
}
