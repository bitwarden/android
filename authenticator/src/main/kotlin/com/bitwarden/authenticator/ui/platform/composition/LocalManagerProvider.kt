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
import com.bitwarden.authenticator.ui.platform.manager.AuthenticatorBuildInfoManagerImpl
import com.bitwarden.authenticator.ui.platform.manager.biometrics.BiometricsManager
import com.bitwarden.authenticator.ui.platform.manager.biometrics.BiometricsManagerImpl
import com.bitwarden.authenticator.ui.platform.manager.exit.ExitManager
import com.bitwarden.authenticator.ui.platform.manager.exit.ExitManagerImpl
import com.bitwarden.authenticator.ui.platform.manager.permissions.PermissionsManager
import com.bitwarden.authenticator.ui.platform.manager.permissions.PermissionsManagerImpl
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import java.time.Clock

/**
 * Helper [Composable] that wraps a [content] and provides manager classes via [CompositionLocal].
 */
@Composable
fun LocalManagerProvider(
    activity: Activity = requireNotNull(LocalActivity.current),
    permissionsManager: PermissionsManager = PermissionsManagerImpl(activity),
    clock: Clock = Clock.systemDefaultZone(),
    buildInfoManager: BuildInfoManager = AuthenticatorBuildInfoManagerImpl(),
    intentManager: IntentManager = IntentManager.create(activity, clock, buildInfoManager),
    exitManager: ExitManager = ExitManagerImpl(activity),
    biometricsManager: BiometricsManager = BiometricsManagerImpl(activity),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalPermissionsManager provides permissionsManager,
        LocalIntentManager provides intentManager,
        LocalExitManager provides exitManager,
        LocalBiometricsManager provides biometricsManager,
        content = content,
    )
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
