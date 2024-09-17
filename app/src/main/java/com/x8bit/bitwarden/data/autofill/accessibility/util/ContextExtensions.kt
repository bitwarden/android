package com.x8bit.bitwarden.data.autofill.accessibility.util

import android.content.Context
import android.provider.Settings
import com.x8bit.bitwarden.LEGACY_ACCESSIBILITY_SERVICE_NAME
import com.x8bit.bitwarden.data.autofill.accessibility.BitwardenAccessibilityService

/**
 * Helper method to determine if the [BitwardenAccessibilityService] is enabled.
 */
val Context.isAccessibilityServiceEnabled: Boolean
    get() {
        val appContext = this.applicationContext
        val accessibilityServiceName = appContext
            .packageName
            ?.let { "$it/$LEGACY_ACCESSIBILITY_SERVICE_NAME" }
            ?: return false
        return Settings
            .Secure
            .getString(
                appContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            )
            ?.contains(accessibilityServiceName)
            ?: false
    }
