@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.composition

import android.app.Activity
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
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
import com.x8bit.bitwarden.ui.platform.manager.nfc.NfcManager
import com.x8bit.bitwarden.ui.platform.manager.nfc.NfcManagerImpl
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManager
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManagerImpl
import com.x8bit.bitwarden.ui.platform.manager.review.AppReviewManager
import com.x8bit.bitwarden.ui.platform.manager.review.AppReviewManagerImpl

/**
 * Helper [Composable] that wraps a [content] and provides manager classes via [CompositionLocal].
 */
@Composable
fun LocalManagerProvider(
    content: @Composable () -> Unit,
) {
    val activity = LocalContext.current as Activity
    val fido2IntentManager: IntentManager = IntentManagerImpl(activity)
    val fido2CompletionManager =
        if (isBuildVersionBelow(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)) {
            Fido2CompletionManagerUnsupportedApiImpl
        } else {
            Fido2CompletionManagerImpl(activity, fido2IntentManager)
        }
    CompositionLocalProvider(
        LocalPermissionsManager provides PermissionsManagerImpl(activity),
        LocalIntentManager provides fido2IntentManager,
        LocalExitManager provides ExitManagerImpl(activity),
        LocalBiometricsManager provides BiometricsManagerImpl(activity),
        LocalNfcManager provides NfcManagerImpl(activity),
        LocalFido2CompletionManager provides fido2CompletionManager,
        LocalAppReviewManager provides AppReviewManagerImpl(activity),
    ) {
        content()
    }
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
