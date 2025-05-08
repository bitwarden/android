package com.x8bit.bitwarden.ui.vault.feature.manualcodeentry

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the manual code entry screen.
 */
@Serializable
data object ManualCodeEntryRoute

/**
 * Add the manual code entry screen to the nav graph.
 */
fun NavGraphBuilder.vaultManualCodeEntryDestination(
    onNavigateBack: () -> Unit,
    onNavigateToQrCodeScreen: () -> Unit,
) {
    composableWithSlideTransitions<ManualCodeEntryRoute> {
        ManualCodeEntryScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToQrCodeScreen = onNavigateToQrCodeScreen,
        )
    }
}

/**
 * Navigate to the manual code entry screen.
 */
fun NavController.navigateToManualCodeEntryScreen(
    navOptions: NavOptions? = null,
) {
    this.navigate(route = ManualCodeEntryRoute, navOptions = navOptions)
}
