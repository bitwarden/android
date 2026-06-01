package com.bitwarden.authenticator

import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.authenticator.ui.platform.composition.LocalManagerProvider
import com.bitwarden.authenticator.ui.platform.feature.debugmenu.debugMenuDestination
import com.bitwarden.authenticator.ui.platform.feature.debugmenu.manager.DebugMenuLaunchManager
import com.bitwarden.authenticator.ui.platform.feature.debugmenu.navigateToDebugMenuScreen
import com.bitwarden.authenticator.ui.platform.feature.rootnav.RootNavigationRoute
import com.bitwarden.authenticator.ui.platform.feature.rootnav.rootNavDestination
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.platform.util.setupEdgeToEdge
import com.bitwarden.ui.platform.util.validate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Primary entry point for the application.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    @Inject
    lateinit var debugLaunchManager: DebugMenuLaunchManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        intent = intent.validate()
        var shouldShowSplashScreen = true
        installSplashScreen().setKeepOnScreenCondition { shouldShowSplashScreen }
        super.onCreate(savedInstanceState)
        window.decorView.filterTouchesWhenObscured = true
        if (savedInstanceState == null) {
            mainViewModel.trySendAction(
                MainAction.ReceiveFirstIntent(
                    intent = intent,
                ),
            )
        }

        AppCompatDelegate.setDefaultNightMode(settingsRepository.appTheme.osValue)
        setupEdgeToEdge(appThemeFlow = mainViewModel.stateFlow.map { it.theme })
        setContent {
            val navController = rememberNavController()
            SetupEventsEffect(navController = navController)
            val state by mainViewModel.stateFlow.collectAsStateWithLifecycle()
            updateScreenCapture(isScreenCaptureAllowed = state.isScreenCaptureAllowed)
            LocalManagerProvider {
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
        mainViewModel.trySendAction(MainAction.ReceiveNewIntent(intent = newIntent))
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        val newIntent = intent.validate()
        super.onNewIntent(newIntent, caller)
        mainViewModel.trySendAction(MainAction.ReceiveNewIntent(intent = newIntent))
    }

    @Composable
    private fun SetupEventsEffect(navController: NavHostController) {
        EventsEffect(viewModel = mainViewModel) { event ->
            when (event) {
                MainEvent.NavigateToDebugMenu -> navController.navigateToDebugMenuScreen()
                is MainEvent.UpdateAppTheme -> {
                    AppCompatDelegate.setDefaultNightMode(event.osTheme)
                }
            }
        }
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

    private fun updateScreenCapture(isScreenCaptureAllowed: Boolean) {
        if (isScreenCaptureAllowed) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
