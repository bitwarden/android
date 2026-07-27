package com.x8bit.bitwarden.data.autofill.accessibility.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.getSystemService
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
        val isEnabledViaManager = appContext
            .getSystemService<AccessibilityManager>()
            ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            ?.any { service ->
                val serviceInfo = service.resolveInfo?.serviceInfo ?: return@any false
                if (serviceInfo.packageName != appContext.packageName) return@any false
                when (serviceInfo.name) {
                    BitwardenAccessibilityService::class.java.name,
                    LEGACY_ACCESSIBILITY_SERVICE_NAME,
                    LEGACY_SHORT_ACCESSIBILITY_SERVICE_NAME,
                        -> true

                    else -> false
                }
            }
            ?: false

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
