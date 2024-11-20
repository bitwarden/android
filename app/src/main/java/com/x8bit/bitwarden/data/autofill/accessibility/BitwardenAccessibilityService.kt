package com.x8bit.bitwarden.data.autofill.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.Keep
import com.x8bit.bitwarden.data.autofill.accessibility.processor.BitwardenAccessibilityProcessor
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.tiles.BitwardenAutofillTileService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The [AccessibilityService] implementation for the app. This is not used in the traditional
 * way, we use the [BitwardenAutofillTileService] to invoke this service in order to provide an
 * autofill fallback mechanism.
 */
@Keep
@OmitFromCoverage
@AndroidEntryPoint
class BitwardenAccessibilityService : AccessibilityService() {
    @Inject
    lateinit var processor: BitwardenAccessibilityProcessor

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        processor.processAccessibilityEvent(event = event) { rootInActiveWindow }
    }

    override fun onInterrupt() = Unit
}
