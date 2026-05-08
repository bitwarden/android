package com.bitwarden.ui.platform.feature.cardscanner.util

import androidx.camera.core.ImageProxy
import com.bitwarden.annotation.OmitFromCoverage

/**
 * No-op [CardTextAnalyzer] for the F-Droid build flavor.
 *
 * Google ML Kit is not permitted in F-Droid releases, so this stub replaces the
 * standard analyzer at build time. The Scan Card UI is hidden via
 * `BuildInfoManager.isFdroid`; this implementation exists solely to satisfy the
 * flavor-uniform construction path used by `LocalManagerProvider`. The
 * `cardDataParser` argument is unused, retained so the constructor signature
 * matches the standard flavor and call sites remain identical.
 */
@OmitFromCoverage
@Suppress("UnusedParameter")
class CardTextAnalyzerImpl(
    cardDataParser: CardDataParser,
) : CardTextAnalyzer {

    override lateinit var onCardScanned: (CardScanData) -> Unit

    override fun analyze(image: ImageProxy) {
        image.close()
    }
}
