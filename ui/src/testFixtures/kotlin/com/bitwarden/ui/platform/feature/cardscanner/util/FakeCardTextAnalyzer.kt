package com.bitwarden.ui.platform.feature.cardscanner.util

import androidx.camera.core.ImageProxy

/**
 * A fake implementation of [CardTextAnalyzer] for testing.
 */
class FakeCardTextAnalyzer : CardTextAnalyzer {

    override lateinit var onCardScanned: (CardScanData) -> Unit

    /**
     * The scan result that will be sent to the callback, or `null` to simulate no detection.
     */
    var scanResult: CardScanData? = null

    override fun analyze(image: ImageProxy) {
        scanResult?.let { onCardScanned.invoke(it) }
    }
}
