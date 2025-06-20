package com.x8bit.bitwarden.ui.vault.feature.qrcodescan

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the QR code scan screen.
 */
@Serializable
data object QrCodeScanRoute

/**
 * Add the QR code scan screen to the nav graph.
 */
fun NavGraphBuilder.vaultQrCodeScanDestination(
    onNavigateBack: () -> Unit,
    onNavigateToManualCodeEntryScreen: () -> Unit,
) {
    composableWithSlideTransitions<QrCodeScanRoute> {
        QrCodeScanScreen(
            onNavigateToManualCodeEntryScreen = onNavigateToManualCodeEntryScreen,
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the QR code scan screen.
 */
fun NavController.navigateToQrCodeScanScreen(
    navOptions: NavOptions? = null,
) {
    this.navigate(route = QrCodeScanRoute, navOptions = navOptions)
}
