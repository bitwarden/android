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
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.cxf.importer.CredentialExchangeImporter
import com.bitwarden.cxf.importer.dsl.credentialExchangeImporter
import com.bitwarden.cxf.manager.CredentialExchangeCompletionManager
import com.bitwarden.cxf.manager.dsl.credentialExchangeCompletionManager
import com.bitwarden.cxf.ui.composition.LocalCredentialExchangeCompletionManager
import com.bitwarden.cxf.ui.composition.LocalCredentialExchangeImporter
import com.bitwarden.cxf.ui.composition.LocalCredentialExchangeRequestValidator
import com.bitwarden.cxf.validator.CredentialExchangeRequestValidator
import com.bitwarden.cxf.validator.dsl.credentialExchangeRequestValidator
import com.bitwarden.ui.platform.composition.LocalExitManager
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.composition.LocalQrCodeAnalyzer
import com.bitwarden.ui.platform.feature.qrcodescan.util.QrCodeAnalyzer
import com.bitwarden.ui.platform.feature.qrcodescan.util.QrCodeAnalyzerImpl
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.manager.exit.ExitManager
import com.bitwarden.ui.platform.manager.exit.ExitManagerImpl
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.util.AppResumeStateManager
import com.x8bit.bitwarden.data.platform.manager.util.AppResumeStateManagerImpl
import com.x8bit.bitwarden.ui.credentials.manager.CredentialProviderCompletionManager
import com.x8bit.bitwarden.ui.credentials.manager.CredentialProviderCompletionManagerImpl
import com.x8bit.bitwarden.ui.credentials.manager.CredentialProviderCompletionManagerUnsupportedApiImpl
import com.x8bit.bitwarden.ui.platform.manager.BitwardenBuildInfoManagerImpl
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricsManager
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricsManagerImpl
import com.x8bit.bitwarden.ui.platform.manager.keychain.KeyChainManager
import com.x8bit.bitwarden.ui.platform.manager.keychain.KeyChainManagerImpl
import com.x8bit.bitwarden.ui.platform.manager.nfc.NfcManager
import com.x8bit.bitwarden.ui.platform.manager.nfc.NfcManagerImpl
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManager
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManagerImpl
import com.x8bit.bitwarden.ui.platform.manager.review.AppReviewManager
import com.x8bit.bitwarden.ui.platform.manager.review.AppReviewManagerImpl
import com.x8bit.bitwarden.ui.platform.model.AuthTabLaunchers
import com.x8bit.bitwarden.ui.platform.model.FeatureFlagsState
import java.time.Clock

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
    clock: Clock = Clock.systemDefaultZone(),
    exitManager: ExitManager = ExitManagerImpl(activity = activity),
    buildInfoManager: BuildInfoManager = BitwardenBuildInfoManagerImpl(),
    intentManager: IntentManager = IntentManager.create(
        activity = activity,
        clock = clock,
        buildInfoManager = buildInfoManager,
    ),
    credentialProviderCompletionManager: CredentialProviderCompletionManager =
        createCredentialProviderCompletionManager(activity = activity),
    keyChainManager: KeyChainManager = KeyChainManagerImpl(activity = activity),
    nfcManager: NfcManager = NfcManagerImpl(activity = activity),
    permissionsManager: PermissionsManager = PermissionsManagerImpl(activity = activity),
    credentialExchangeImporter: CredentialExchangeImporter =
        credentialExchangeImporter(activity = activity),
    credentialExchangeCompletionManager: CredentialExchangeCompletionManager =
        credentialExchangeCompletionManager(activity = activity, clock = clock) {
            exporterRpId = activity.packageName
            exporterDisplayName = activity.getString(R.string.app_name)
        },
    credentialExchangeRequestValidator: CredentialExchangeRequestValidator =
        credentialExchangeRequestValidator(activity = activity),
    authTabLaunchers: AuthTabLaunchers,
    qrCodeAnalyzer: QrCodeAnalyzer = QrCodeAnalyzerImpl(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalFeatureFlagsState provides featureFlagsState,
        LocalAppResumeStateManager provides appResumeStateManager,
        LocalAppReviewManager provides appReviewManager,
        LocalBiometricsManager provides biometricsManager,
        LocalClock provides clock,
        LocalExitManager provides exitManager,
        LocalCredentialProviderCompletionManager provides credentialProviderCompletionManager,
        LocalIntentManager provides intentManager,
        LocalKeyChainManager provides keyChainManager,
        LocalNfcManager provides nfcManager,
        LocalPermissionsManager provides permissionsManager,
        LocalCredentialExchangeImporter provides credentialExchangeImporter,
        LocalCredentialExchangeCompletionManager provides credentialExchangeCompletionManager,
        LocalCredentialExchangeRequestValidator provides credentialExchangeRequestValidator,
        LocalAuthTabLaunchers provides authTabLaunchers,
        LocalQrCodeAnalyzer provides qrCodeAnalyzer,
        content = content,
    )
}

private fun createCredentialProviderCompletionManager(
    activity: Activity,
): CredentialProviderCompletionManager =
    if (!isBuildVersionAtLeast(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)) {
        CredentialProviderCompletionManagerUnsupportedApiImpl
    } else {
        CredentialProviderCompletionManagerImpl(activity)
    }

/**
 * Provides access to the biometrics manager throughout the app.
 */
val LocalBiometricsManager: ProvidableCompositionLocal<BiometricsManager> = compositionLocalOf {
    error("CompositionLocal BiometricsManager not present")
}

/**
 * Provides access to the clock throughout the app.
 */
val LocalClock: ProvidableCompositionLocal<Clock> = compositionLocalOf { Clock.systemDefaultZone() }

/**
 * Provides access to the Auth Tab launchers throughout the app.
 */
val LocalAuthTabLaunchers: ProvidableCompositionLocal<AuthTabLaunchers> = compositionLocalOf {
    error("CompositionLocal AuthTabLaunchers not present")
}

/**
 * Provides access to the feature flags throughout the app.
 */
val LocalFeatureFlagsState: ProvidableCompositionLocal<FeatureFlagsState> = compositionLocalOf {
    error("CompositionLocal FeatureFlagsState not present")
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
@Suppress("MaxLineLength")
val LocalCredentialProviderCompletionManager: ProvidableCompositionLocal<CredentialProviderCompletionManager> =
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
