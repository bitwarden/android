@file:OmitFromCoverage

package com.bitwarden.authenticator.ui.platform.composition

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.authenticator.ui.platform.manager.biometrics.BiometricsManager
import com.bitwarden.authenticator.ui.platform.manager.biometrics.BiometricsManagerImpl
import com.bitwarden.authenticator.ui.platform.manager.exit.ExitManager
import com.bitwarden.authenticator.ui.platform.manager.exit.ExitManagerImpl
import com.bitwarden.authenticator.ui.platform.manager.intent.IntentManager
import com.bitwarden.authenticator.ui.platform.manager.intent.IntentManagerImpl
import com.bitwarden.authenticator.ui.platform.manager.permissions.PermissionsManager
import com.bitwarden.authenticator.ui.platform.manager.permissions.PermissionsManagerImpl

/**
 * Helper [Composable] that wraps a [content] and provides manager classes via [CompositionLocal].
 */
@Composable
fun LocalManagerProvider(
    activity: Activity = requireNotNull(LocalActivity.current),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalPermissionsManager provides PermissionsManagerImpl(activity),
        LocalIntentManager provides IntentManagerImpl(activity),
        LocalExitManager provides ExitManagerImpl(activity),
        LocalBiometricsManager provides BiometricsManagerImpl(activity),
        content = content,
    )
}

/**
 * Provides access to the intent manager throughout the app.
 */
val LocalIntentManager: ProvidableCompositionLocal<IntentManager> = compositionLocalOf {
    error("CompositionLocal LocalIntentManager not present")
}

/**
 * Provides access to the biometrics manager throughout the app.
 */
val LocalBiometricsManager: ProvidableCompositionLocal<BiometricsManager> = compositionLocalOf {
    error("CompositionLocal BiometricsManager not present")
}

/**
 * Provides access to the exit manager throughout the app.
 */
val LocalExitManager: ProvidableCompositionLocal<ExitManager> = compositionLocalOf {
    error("CompositionLocal ExitManager not present")
}

/**
 * Provides access to the permission manager throughout the app.
 */
val LocalPermissionsManager: ProvidableCompositionLocal<PermissionsManager> = compositionLocalOf {
    error("CompositionLocal LocalPermissionsManager not present")
}
