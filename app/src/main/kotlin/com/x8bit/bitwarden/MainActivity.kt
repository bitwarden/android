package com.x8bit.bitwarden

import android.app.ComponentCaller
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.browser.auth.AuthTabIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.platform.util.setupEdgeToEdge
import com.bitwarden.ui.platform.util.validate
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityCompletionManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillActivityManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillCompletionManager
import com.x8bit.bitwarden.data.platform.manager.util.ObserveScreenDataEffect
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.components.util.rememberBitwardenNavController
import com.x8bit.bitwarden.ui.platform.composition.LocalManagerProvider
import com.x8bit.bitwarden.ui.platform.feature.debugmenu.debugMenuDestination
import com.x8bit.bitwarden.ui.platform.feature.debugmenu.manager.DebugMenuLaunchManager
import com.x8bit.bitwarden.ui.platform.feature.debugmenu.navigateToDebugMenuScreen
import com.x8bit.bitwarden.ui.platform.feature.rootnav.RootNavigationRoute
import com.x8bit.bitwarden.ui.platform.feature.rootnav.rootNavDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import com.x8bit.bitwarden.ui.platform.model.AuthTabLaunchers
import com.x8bit.bitwarden.ui.platform.util.appLanguage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Primary entry point for the application.
 */
@Suppress("TooManyFunctions")
@OmitFromCoverage
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    @Inject
    lateinit var autofillActivityManager: AutofillActivityManager

    @Inject
    lateinit var autofillCompletionManager: AutofillCompletionManager

    @Inject
    lateinit var accessibilityCompletionManager: AccessibilityCompletionManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var debugLaunchManager: DebugMenuLaunchManager

    private val duoLauncher = AuthTabIntent.registerActivityResultLauncher(this) {
        mainViewModel.trySendAction(MainAction.DuoResult(it))
    }
    private val ssoLauncher = AuthTabIntent.registerActivityResultLauncher(this) {
        mainViewModel.trySendAction(MainAction.SsoResult(it))
    }
    private val webAuthnLauncher = AuthTabIntent.registerActivityResultLauncher(this) {
        mainViewModel.trySendAction(MainAction.WebAuthnResult(it))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        intent = intent.validate()
        var shouldShowSplashScreen = true
        installSplashScreen().setKeepOnScreenCondition { shouldShowSplashScreen }
        super.onCreate(savedInstanceState)
        window.decorView.filterTouchesWhenObscured = true
        if (savedInstanceState == null) {
            mainViewModel.trySendAction(MainAction.ReceiveFirstIntent(intent = intent))
        }

        // Within the app the theme will change dynamically and will be managed by the
        // OS, but we need to ensure we properly set the values when upgrading from older versions
        // that handle this differently or when the activity restarts.
        AppCompatDelegate.setDefaultNightMode(settingsRepository.appTheme.osValue)
        setupEdgeToEdge(appThemeFlow = mainViewModel.stateFlow.map { it.theme })
        setContent {
            val navController = rememberBitwardenNavController(name = "MainActivity")
            SetupEventsEffect(navController = navController)
            val state by mainViewModel.stateFlow.collectAsStateWithLifecycle()
            updateScreenCapture(isScreenCaptureAllowed = state.isScreenCaptureAllowed)
            LocalManagerProvider(
                featureFlagsState = state.featureFlagsState,
                authTabLaunchers = AuthTabLaunchers(
                    duo = duoLauncher,
                    sso = ssoLauncher,
                    webAuthn = webAuthnLauncher,
                ),
            ) {
                ObserveScreenDataEffect(
                    onDataUpdate = remember(mainViewModel) {
                        { mainViewModel.trySendAction(MainAction.ResumeScreenDataReceived(it)) }
                    },
                )
                BitwardenTheme(
                    theme = state.theme,
                    dynamicColor = state.isDynamicColorsEnabled,
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = RootNavigationRoute,
                    ) {
                        // Both root navigation and debug menu exist at this top level.
                        // The debug menu can appear on top of the rest of the app without
                        // interacting with the state-based navigation used by RootNavScreen.
                        rootNavDestination { shouldShowSplashScreen = false }
                        debugMenuDestination(
                            onNavigateBack = { navController.popBackStack() },
                            onSplashScreenRemoved = { shouldShowSplashScreen = false },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        val newIntent = intent.validate()
        super.onNewIntent(newIntent)
        mainViewModel.trySendAction(action = MainAction.ReceiveNewIntent(intent = newIntent))
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        val newIntent = intent.validate()
        super.onNewIntent(newIntent, caller)
        mainViewModel.trySendAction(action = MainAction.ReceiveNewIntent(intent = newIntent))
    }

    override fun onResume() {
        super.onResume()
        // When the app resumes check for any app specific language which may have been
        // set via the device settings. Similar to the theme setting in onCreate this
        // ensures we properly set the values when upgrading from older versions
        // that handle this differently or when the activity restarts.
        val appSpecificLanguage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales: LocaleListCompat = AppCompatDelegate.getApplicationLocales()
            if (locales.isEmpty) {
                // App is using the system language
                null
            } else {
                // App has specific language settings
                locales.get(0)?.appLanguage
            }
        } else {
            // For older versions, use what ever language is available from the repository.
            settingsRepository.appLanguage
        }

        mainViewModel.trySendAction(
            action = MainAction.AppSpecificLanguageUpdate(
                appLanguage = appSpecificLanguage ?: AppLanguage.DEFAULT,
            ),
        )
    }

    override fun onStop() {
        super.onStop()
        // In some scenarios on an emulator the Activity can leak when recreated
        // if we don't first clear focus anytime we exit and return to the app.
        currentFocus?.clearFocus()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean = debugLaunchManager
        .actionOnInputEvent(event = event, action = ::sendOpenDebugMenuEvent)
        .takeIf { it }
        ?: super.dispatchTouchEvent(event)

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = debugLaunchManager
        .actionOnInputEvent(event = event, action = ::sendOpenDebugMenuEvent)
        .takeIf { it }
        ?: super.dispatchKeyEvent(event)

    @Composable
    private fun SetupEventsEffect(navController: NavController) {
        EventsEffect(viewModel = mainViewModel) { event ->
            when (event) {
                is MainEvent.CompleteAccessibilityAutofill -> {
                    handleCompleteAccessibilityAutofill(event)
                }

                is MainEvent.CompleteAutofill -> handleCompleteAutofill(event)
                MainEvent.Recreate -> handleRecreate()
                MainEvent.NavigateToDebugMenu -> navController.navigateToDebugMenuScreen()
                is MainEvent.UpdateAppLocale -> {
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(event.localeName),
                    )
                }

                is MainEvent.UpdateAppTheme -> AppCompatDelegate.setDefaultNightMode(event.osTheme)
            }
        }
    }

    private fun sendOpenDebugMenuEvent() {
        mainViewModel.trySendAction(MainAction.OpenDebugMenu)
    }

    private fun handleCompleteAccessibilityAutofill(
        event: MainEvent.CompleteAccessibilityAutofill,
    ) {
        accessibilityCompletionManager.completeAccessibilityAutofill(
            activity = this,
            cipherView = event.cipherView,
        )
    }

    private fun handleCompleteAutofill(event: MainEvent.CompleteAutofill) {
        autofillCompletionManager.completeAutofill(
            activity = this,
            cipherView = event.cipherView,
        )
    }

    private fun handleRecreate() {
        ActivityCompat.recreate(this)
    }

    private fun updateScreenCapture(isScreenCaptureAllowed: Boolean) {
        if (isScreenCaptureAllowed) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
