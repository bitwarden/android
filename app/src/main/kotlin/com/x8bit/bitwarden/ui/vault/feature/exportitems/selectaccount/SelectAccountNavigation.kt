@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.exportitems.selectaccount

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithRootPushTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the select account screen.
 */
@OmitFromCoverage
@Serializable
data object SelectAccountRoute

/**
 * Add the [SelectAccountScreen] to the nav graph.
 */
fun NavGraphBuilder.selectAccountDestination(
    onAccountSelected: (id: String, hasOtherAccounts: Boolean) -> Unit,
) {
    composableWithRootPushTransitions<SelectAccountRoute> {
        SelectAccountScreen(
            onAccountSelected = onAccountSelected,
        )
    }
}

/**
 * Navigate to the [SelectAccountScreen].
 */
fun NavController.navigateToSelectAccountScreen(
    navOptions: NavOptions? = null,
) {
    navigate(
        route = SelectAccountRoute,
        navOptions = navOptions,
    )
}

/**
 * Pop up to the [SelectAccountScreen].
 */
fun NavController.popUpToSelectAccountScreen() {
    popBackStack(
        route = SelectAccountRoute,
        inclusive = false,
    )
}
