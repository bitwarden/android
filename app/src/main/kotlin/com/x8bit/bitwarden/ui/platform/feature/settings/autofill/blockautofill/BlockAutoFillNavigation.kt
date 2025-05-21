package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.blockautofill

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the block autofill settings screen.
 */
@Serializable
data object BlockAutofillSettingsRoute

/**
 * Add block auto-fill destination to the nav graph.
 */
fun NavGraphBuilder.blockAutoFillDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithPushTransitions<BlockAutofillSettingsRoute> {
        BlockAutoFillScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the block auto-fill screen.
 */
fun NavController.navigateToBlockAutoFillScreen(navOptions: NavOptions? = null) {
    this.navigate(route = BlockAutofillSettingsRoute, navOptions = navOptions)
}
