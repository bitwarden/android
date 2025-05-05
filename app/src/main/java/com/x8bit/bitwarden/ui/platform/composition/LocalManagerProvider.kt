@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.composition

import android.app.Activity
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import com.bitwarden.core.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.platform.manager.util.AppResumeStateManager
import com.x8bit.bitwarden.data.platform.manager.util.AppResumeStateManagerImpl
import com.x8bit.bitwarden.data.platform.util.isBuildVersionBelow
import com.x8bit.bitwarden.ui.autofill.fido2.manager.Fido2CompletionManager
import com.x8bit.bitwarden.ui.autofill.fido2.manager.Fido2CompletionManagerImpl
import com.x8bit.bitwarden.ui.autofill.fido2.manager.Fido2CompletionManagerUnsupportedApiImpl
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricsManager
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricsManagerImpl
import com.x8bit.bitwarden.ui.platform.manager.exit.ExitManager
import com.x8bit.bitwarden.ui.platform.manager.exit.ExitManagerImpl
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManagerImpl
import com.x8bit.bitwarden.ui.platform.manager.keychain.KeyChainManager
import com.x8bit.bitwarden.ui.platform.manager.keychain.KeyChainManagerImpl
import com.x8bit.bitwarden.ui.platform.manager.nfc.NfcManager
import com.x8bit.bitwarden.ui.platform.manager.nfc.NfcManagerImpl
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManager
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManagerImpl
import com.x8bit.bitwarden.ui.platform.manager.review.AppReviewManager
import com.x8bit.bitwarden.ui.platform.manager.review.AppReviewManagerImpl
import com.x8bit.bitwarden.ui.platform.model.FeatureFlagsState

/**
 * Helper [Composable] that wraps a [content] and provides manager classes via [CompositionLocal].
 */
@Composable
fun LocalManagerProvider(
    featureFlagsState: FeatureFlagsState,
    activity: Activity = requireNotNull(LocalActivity.current),
    appResumeStateManager: AppResumeStateManager = AppResumeStateManagerImpl(),
    appReviewManager: AppReviewManager = AppReviewManagerImpl(activity = activity),
    biometricsManager: BiometricsManager = BiometricsManagerImpl(activity = activity),
    exitManager: ExitManager = ExitManagerImpl(activity = activity),
    intentManager: IntentManager = IntentManagerImpl(context = activity),
    fido2CompletionManager: Fido2CompletionManager = createFido2CompletionManager(
        activity = activity,
        intentManager = intentManager,
    ),
    keyChainManager: KeyChainManager = KeyChainManagerImpl(activity = activity),
    nfcManager: NfcManager = NfcManagerImpl(activity = activity),
    permissionsManager: PermissionsManager = PermissionsManagerImpl(activity = activity),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalFeatureFlagsState provides featureFlagsState,
        LocalAppResumeStateManager provides appResumeStateManager,
        LocalAppReviewManager provides appReviewManager,
        LocalBiometricsManager provides biometricsManager,
        LocalExitManager provides exitManager,
        LocalFido2CompletionManager provides fido2CompletionManager,
        LocalIntentManager provides intentManager,
        LocalKeyChainManager provides keyChainManager,
        LocalNfcManager provides nfcManager,
        LocalPermissionsManager provides permissionsManager,
        content = content,
    )
}

private fun createFido2CompletionManager(
    activity: Activity,
    intentManager: IntentManager,
): Fido2CompletionManager =
    if (isBuildVersionBelow(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)) {
        Fido2CompletionManagerUnsupportedApiImpl
    } else {
        Fido2CompletionManagerImpl(activity)
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
 * Provides access to the feature flags throughout the app.
 */
val LocalFeatureFlagsState: ProvidableCompositionLocal<FeatureFlagsState> = compositionLocalOf {
    error("CompositionLocal FeatureFlagsState not present")
}

/**
 * Provides access to the intent manager throughout the app.
 */
val LocalIntentManager: ProvidableCompositionLocal<IntentManager> = compositionLocalOf {
    error("CompositionLocal LocalIntentManager not present")
}

/**
 * Provides access to the permission manager throughout the app.
 */
val LocalPermissionsManager: ProvidableCompositionLocal<PermissionsManager> = compositionLocalOf {
    error("CompositionLocal LocalPermissionsManager not present")
}

/**
 * Provides access to the NFC manager throughout the app.
 */
val LocalNfcManager: ProvidableCompositionLocal<NfcManager> = compositionLocalOf {
    error("CompositionLocal NfcManager not present")
}

/**
 * Provides access to the FIDO2 completion manager throughout the app.
 */
val LocalFido2CompletionManager: ProvidableCompositionLocal<Fido2CompletionManager> =
    compositionLocalOf {
        error("CompositionLocal Fido2CompletionManager not present")
    }

/**
 * Provides access to the app review manager throughout the app.
 */
val LocalAppReviewManager: ProvidableCompositionLocal<AppReviewManager> = compositionLocalOf {
    error("CompositionLocal AppReviewManager not present")
}

val LocalAppResumeStateManager = compositionLocalOf<AppResumeStateManager> {
    error("CompositionLocal AppResumeStateManager not present")
}

val LocalKeyChainManager: ProvidableCompositionLocal<KeyChainManager> = compositionLocalOf {
    error("CompositionLocal KeyChainManager not present")
}
