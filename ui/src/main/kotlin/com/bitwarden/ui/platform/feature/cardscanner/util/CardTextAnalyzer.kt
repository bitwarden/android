package com.bitwarden.ui.platform.feature.cardscanner.util

import androidx.camera.core.ImageAnalysis
import androidx.compose.runtime.Stable

/**
 * An interface used to analyze camera frames for credit card text.
 */
@Stable
interface CardTextAnalyzer : ImageAnalysis.Analyzer {

    /**
     * Callback invoked when a card is successfully scanned.
     */
    var onCardScanned: (CardScanData) -> Unit
}
