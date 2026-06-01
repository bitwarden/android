package com.x8bit.bitwarden.data.autofill.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.Keep
import com.bitwarden.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityEnabledManager
import com.x8bit.bitwarden.data.autofill.accessibility.processor.BitwardenAccessibilityProcessor
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

    @Inject
    lateinit var accessibilityEnabledManager: AccessibilityEnabledManager

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        processor.processAccessibilityEvent(event = event) { rootInActiveWindow }
    }

    override fun onInterrupt() = Unit

    override fun onCreate() {
        super.onCreate()
        accessibilityEnabledManager.refreshAccessibilityEnabledFromSettings()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super
            .onUnbind(intent)
            .also { accessibilityEnabledManager.refreshAccessibilityEnabledFromSettings() }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        accessibilityEnabledManager.refreshAccessibilityEnabledFromSettings()
    }
}
