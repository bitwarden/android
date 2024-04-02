package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.qrcodescan

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.composableWithSlideTransitions

private const val QR_CODE_SCAN_ROUTE: String = "qr_code_scan"

/**
 * Add the QR code scan screen to the nav graph.
 */
fun NavGraphBuilder.qrCodeScanDestination(
    onNavigateBack: () -> Unit,
    onNavigateToManualCodeEntryScreen: () -> Unit,
) {
    composableWithSlideTransitions(
        route = QR_CODE_SCAN_ROUTE,
    ) {
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
    this.navigate(QR_CODE_SCAN_ROUTE, navOptions)
}
