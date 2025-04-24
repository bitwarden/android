@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.manualcodeentry

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.core.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val MANUAL_CODE_ENTRY_ROUTE: String = "manual_code_entry"

/**
 * Add the manual code entry screen to the nav graph.
 */
fun NavGraphBuilder.vaultManualCodeEntryDestination(
    onNavigateBack: () -> Unit,
    onNavigateToQrCodeScreen: () -> Unit,
) {
    composableWithSlideTransitions(
        route = MANUAL_CODE_ENTRY_ROUTE,
    ) {
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
    this.navigate(MANUAL_CODE_ENTRY_ROUTE, navOptions)
}
