package com.x8bit.bitwarden

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.bitwarden.core.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityCompletionManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillActivityManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillCompletionManager
import com.x8bit.bitwarden.data.platform.manager.util.ObserveScreenDataEffect
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.composition.LocalManagerProvider
import com.x8bit.bitwarden.ui.platform.feature.debugmenu.debugMenuDestination
import com.x8bit.bitwarden.ui.platform.feature.debugmenu.manager.DebugMenuLaunchManager
import com.x8bit.bitwarden.ui.platform.feature.debugmenu.navigateToDebugMenuScreen
import com.x8bit.bitwarden.ui.platform.feature.rootnav.ROOT_ROUTE
import com.x8bit.bitwarden.ui.platform.feature.rootnav.rootNavDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.util.appLanguage
import dagger.hilt.android.AndroidEntryPoint
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

    @Suppress("LongMethod")
    override fun onCreate(savedInstanceState: Bundle?) {
        var shouldShowSplashScreen = true
        installSplashScreen().setKeepOnScreenCondition { shouldShowSplashScreen }
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            mainViewModel.trySendAction(
                MainAction.ReceiveFirstIntent(
                    intent = intent,
                ),
            )
        }

        // Within the app the theme will change dynamically and will be managed by the
        // OS, but we need to ensure we properly set the values when upgrading from older versions
        // that handle this differently or when the activity restarts.
        AppCompatDelegate.setDefaultNightMode(settingsRepository.appTheme.osValue)
        setContent {
            val state by mainViewModel.stateFlow.collectAsStateWithLifecycle()
            val navController = rememberNavController()
            EventsEffect(viewModel = mainViewModel) { event ->
                when (event) {
                    is MainEvent.CompleteAccessibilityAutofill -> {
                        handleCompleteAccessibilityAutofill(event)
                    }

                    is MainEvent.CompleteAutofill -> handleCompleteAutofill(event)
                    MainEvent.Recreate -> handleRecreate()
                    MainEvent.NavigateToDebugMenu -> navController.navigateToDebugMenuScreen()
                    is MainEvent.ShowToast -> {
                        Toast
                            .makeText(
                                baseContext,
                                event.message.invoke(resources),
                                Toast.LENGTH_SHORT,
                            )
                            .show()
                    }

                    is MainEvent.UpdateAppLocale -> {
                        AppCompatDelegate.setApplicationLocales(
                            LocaleListCompat.forLanguageTags(event.localeName),
                        )
                    }

                    is MainEvent.UpdateAppTheme -> {
                        AppCompatDelegate.setDefaultNightMode(event.osTheme)
                    }
                }
            }
            updateScreenCapture(isScreenCaptureAllowed = state.isScreenCaptureAllowed)
            LocalManagerProvider(featureFlagsState = state.featureFlagsState) {
                ObserveScreenDataEffect(
                    onDataUpdate = remember(mainViewModel) {
                        {
                            mainViewModel.trySendAction(
                                MainAction.ResumeScreenDataReceived(it),
                            )
                        }
                    },
                )
                BitwardenTheme(
                    theme = state.theme,
                    dynamicColor = state.isDynamicColorsEnabled,
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ROOT_ROUTE,
                    ) {
                        // Nothing else should end up at this top level, we just want the ability
                        // to have the debug menu appear on top of the rest of the app without
                        // interacting with the state-based navigation used by the RootNavScreen.
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
        super.onNewIntent(intent)
        mainViewModel.trySendAction(
            action = MainAction.ReceiveNewIntent(
                intent = intent,
            ),
        )
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
        recreate()
    }

    private fun updateScreenCapture(isScreenCaptureAllowed: Boolean) {
        if (isScreenCaptureAllowed) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
