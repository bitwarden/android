package com.x8bit.bitwarden.data.autofill.accessibility.manager

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.LifecycleCoroutineScope
import com.x8bit.bitwarden.data.platform.manager.AppForegroundManager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * The accessibility service name as it is known in the AndroidManifest.
 *
 * Note: This is not the name of the actual service but the name defined in the Android Manifest
 * which matches the service in the Xamarin app.
 */
private const val LEGACY_ACCESSIBILITY_SERVICE_NAME =
    "com.x8bit.bitwarden.Accessibility.AccessibilityService"

/**
 * The default implementation of the [AccessibilityActivityManager].
 */
class AccessibilityActivityManagerImpl(
    private val context: Context,
    private val accessibilityEnabledManager: AccessibilityEnabledManager,
    appForegroundManager: AppForegroundManager,
    lifecycleScope: LifecycleCoroutineScope,
) : AccessibilityActivityManager {
    private val isAccessibilityServiceEnabled: Boolean
        get() {
            val appContext = context.applicationContext
            val accessibilityService = appContext
                .packageName
                ?.let { "$it/$LEGACY_ACCESSIBILITY_SERVICE_NAME" }
                ?: return false
            return Settings
                .Secure
                .getString(
                    appContext.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                )
                ?.contains(accessibilityService)
                ?: false
        }

    init {
        appForegroundManager
            .appForegroundStateFlow
            .onEach {
                accessibilityEnabledManager.isAccessibilityEnabled = isAccessibilityServiceEnabled
            }
            .launchIn(lifecycleScope)
    }
}
