@file:OmitFromCoverage
package com.x8bit.bitwarden.ui.platform.feature.settings.autofill

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the autofill screen.
 */
@Serializable
data object AutofillRoute

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.autoFillDestination(
    onNavigateBack: () -> Unit,
    onNavigateToBlockAutoFillScreen: () -> Unit,
    onNavigateToSetupAutofill: () -> Unit,
    onNavigateToAboutPrivilegedAppsScreen: () -> Unit,
) {
    composableWithPushTransitions<AutofillRoute> {
        AutoFillScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToBlockAutoFillScreen = onNavigateToBlockAutoFillScreen,
            onNavigateToSetupAutofill = onNavigateToSetupAutofill,
            onNavigateToAboutPrivilegedAppsScreen = onNavigateToAboutPrivilegedAppsScreen,
        )
    }
}

/**
 * Navigate to the auto-fill screen.
 */
fun NavController.navigateToAutoFill(navOptions: NavOptions? = null) {
    this.navigate(route = AutofillRoute, navOptions = navOptions)
}
