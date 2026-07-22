package com.x8bit.bitwarden.data.autofill.accessibility.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.x8bit.bitwarden.LEGACY_ACCESSIBILITY_SERVICE_NAME
import com.x8bit.bitwarden.LEGACY_SHORT_ACCESSIBILITY_SERVICE_NAME
import com.x8bit.bitwarden.data.autofill.accessibility.BitwardenAccessibilityService
import com.x8bit.bitwarden.data.autofill.util.containsAnyTerms

/**
 * Helper method to determine if the [BitwardenAccessibilityService] is enabled.
 *
 * Uses [AccessibilityManager.getEnabledAccessibilityServiceList] as the primary check
 * (required for Android 16+ where [Settings.Secure] is restricted for third-party apps),
 * falling back to [Settings.Secure] string parsing for older Android versions.
 */
val Context.isAccessibilityServiceEnabled: Boolean
    get() {
        val appContext = this.applicationContext

        // Primary check: AccessibilityManager API (Android 16+ compatible).
        val accessibilityManager =
            appContext.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val enabledServices = accessibilityManager?.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK,
        )
        val isEnabledViaManager = enabledServices?.any { service ->
            val serviceInfo = service.resolveInfo?.serviceInfo
            serviceInfo?.packageName == appContext.packageName &&
                serviceInfo.name == BitwardenAccessibilityService::class.java.name
        } ?: false

        if (isEnabledViaManager) return true

        // Fallback: legacy Settings.Secure string parsing.
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
