package com.bitwarden.authenticator.ui.authenticator.feature.qrcodescan.util

import androidx.camera.core.ImageAnalysis
import androidx.compose.runtime.Stable

/**
 * An interface that is used to help scan QR codes.
 */
@Stable
interface QrCodeAnalyzer : ImageAnalysis.Analyzer {

    /**
     * The method that is called once the code is scanned.
     */
    var onQrCodeScanned: (String) -> Unit
}
