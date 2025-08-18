package com.x8bit.bitwarden.data.autofill.accessibility.util

import android.content.Context
import android.provider.Settings
import com.x8bit.bitwarden.LEGACY_ACCESSIBILITY_SERVICE_NAME
import com.x8bit.bitwarden.LEGACY_SHORT_ACCESSIBILITY_SERVICE_NAME
import com.x8bit.bitwarden.data.autofill.accessibility.BitwardenAccessibilityService
import com.x8bit.bitwarden.data.autofill.util.containsAnyTerms

/**
 * Helper method to determine if the [BitwardenAccessibilityService] is enabled.
 */
val Context.isAccessibilityServiceEnabled: Boolean
    get() {
        val appContext = this.applicationContext
        val packageName = appContext.packageName
        val accessibilityServiceName = packageName?.let {
            "$it/$LEGACY_ACCESSIBILITY_SERVICE_NAME"
        }
        val shortAccessibilityServiceName = packageName.let {
            "$it/$LEGACY_SHORT_ACCESSIBILITY_SERVICE_NAME"
        }
        return Settings
            .Secure
            .getString(
                appContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            )
            ?.containsAnyTerms(
                terms = listOfNotNull(
                    accessibilityServiceName,
                    shortAccessibilityServiceName,
                ),
                ignoreCase = true,
            )
            ?: false
    }
