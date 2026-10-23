package com.x8bit.bitwarden.ui.platform.feature.settings.vault.myaccess

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemArgs
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the my access requests screen.
 */
@Serializable
data object MyAccessRoute

/**
 * Add the my access requests destination to the nav graph.
 */
fun NavGraphBuilder.myAccessDestination(
    onNavigateBack: () -> Unit,
    onNavigateToVaultItem: (VaultItemArgs) -> Unit,
) {
    composableWithPushTransitions<MyAccessRoute> {
        MyAccessScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToVaultItem = onNavigateToVaultItem,
        )
    }
}

/**
 * Navigate to the my access requests screen.
 */
fun NavController.navigateToMyAccess(navOptions: NavOptions? = null) {
    this.navigate(route = MyAccessRoute, navOptions = navOptions)
}
