package com.x8bit.bitwarden

import android.content.Intent
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
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityCompletionManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillActivityManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillCompletionManager
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.composition.LocalManagerProvider
import com.x8bit.bitwarden.ui.platform.feature.debugmenu.manager.DebugMenuLaunchManager
import com.x8bit.bitwarden.ui.platform.feature.debugmenu.navigateToDebugMenuScreen
import com.x8bit.bitwarden.ui.platform.feature.rootnav.RootNavScreen
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Primary entry point for the application.
 */
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

        // Within the app the language will change dynamically and will be managed
        // by the OS, but we need to ensure we properly set the language when
        // upgrading from older versions that handle this differently.
        settingsRepository.appLanguage.localeName?.let { localeName ->
            val localeList = LocaleListCompat.forLanguageTags(localeName)
            AppCompatDelegate.setApplicationLocales(localeList)
        }
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
                }
            }
            updateScreenCapture(isScreenCaptureAllowed = state.isScreenCaptureAllowed)
            LocalManagerProvider {
                BitwardenTheme(theme = state.theme) {
                    RootNavScreen(
                        onSplashScreenRemoved = { shouldShowSplashScreen = false },
                        navController = navController,
                    )
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
