package com.bitwarden.authenticator.ui.authenticator.feature.qrcodescan

import androidx.camera.core.ImageProxy
import com.bitwarden.authenticator.ui.authenticator.feature.qrcodescan.util.QrCodeAnalyzer

/**
 * A helper class that helps test scan outcomes.
 */
class FakeQrCodeAnalyzer : QrCodeAnalyzer {

    override lateinit var onQrCodeScanned: (String) -> Unit

    /**
     * The result of the scan that will be sent to the ViewModel (or `null` to indicate a
     * scanning error.
     */
    var scanResult: String? = null

    override fun analyze(image: ImageProxy) {
        scanResult?.let { onQrCodeScanned.invoke(it) }
    }
}
