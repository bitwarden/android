package com.x8bit.bitwarden.ui.platform.base

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import com.x8bit.bitwarden.data.platform.manager.util.AppResumeStateManager
import com.x8bit.bitwarden.ui.autofill.fido2.manager.Fido2CompletionManager
import com.x8bit.bitwarden.ui.platform.composition.LocalManagerProvider
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricsManager
import com.x8bit.bitwarden.ui.platform.manager.exit.ExitManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.manager.keychain.KeyChainManager
import com.x8bit.bitwarden.ui.platform.manager.nfc.NfcManager
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManager
import com.x8bit.bitwarden.ui.platform.manager.review.AppReviewManager
import com.x8bit.bitwarden.ui.platform.model.FeatureFlagsState
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule

/**
 * A base class that can be used for performing Compose-layer testing using Robolectric, Compose
 * Testing, and JUnit 4.
 */
abstract class BaseComposeTest : BaseRobolectricTest() {
    @OptIn(ExperimentalCoroutinesApi::class)
    protected val dispatcher = UnconfinedTestDispatcher()

    @OptIn(ExperimentalTestApi::class)
    @get:Rule
    val composeTestRule = createComposeRule(effectContext = dispatcher)

    /**
     * instance of [OnBackPressedDispatcher] made available if testing using [setContent].
     */
    var backDispatcher: OnBackPressedDispatcher? = null
        private set

    /**
     * Helper for testing a basic Composable function that only requires a [Composable]. The
     * [AppTheme] is overridable and the [backDispatcher] is configured automatically.
     */
    @Suppress("LongParameterList")
    protected fun setContent(
        theme: AppTheme = AppTheme.DEFAULT,
        featureFlagsState: FeatureFlagsState = FeatureFlagsState(
            isErrorReportingDialogEnabled = false,
        ),
        appResumeStateManager: AppResumeStateManager = mockk(),
        appReviewManager: AppReviewManager = mockk(),
        biometricsManager: BiometricsManager = mockk(),
        exitManager: ExitManager = mockk(),
        intentManager: IntentManager = mockk(),
        fido2CompletionManager: Fido2CompletionManager = mockk(),
        keyChainManager: KeyChainManager = mockk(),
        nfcManager: NfcManager = mockk(),
        permissionsManager: PermissionsManager = mockk(),
        test: @Composable () -> Unit,
    ) {
        composeTestRule.setContent {
            backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            LocalManagerProvider(
                featureFlagsState = featureFlagsState,
                appResumeStateManager = appResumeStateManager,
                appReviewManager = appReviewManager,
                biometricsManager = biometricsManager,
                exitManager = exitManager,
                intentManager = intentManager,
                fido2CompletionManager = fido2CompletionManager,
                keyChainManager = keyChainManager,
                nfcManager = nfcManager,
                permissionsManager = permissionsManager,
            ) {
                BitwardenTheme(
                    theme = theme,
                    content = test,
                )
            }
        }
    }
}
