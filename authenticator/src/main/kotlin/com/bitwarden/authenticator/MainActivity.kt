package com.bitwarden.authenticator

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.bitwarden.authenticator.data.platform.util.isSuspicious
import com.bitwarden.authenticator.ui.platform.feature.debugmenu.manager.DebugMenuLaunchManager
import com.bitwarden.authenticator.ui.platform.feature.debugmenu.navigateToDebugMenuScreen
import com.bitwarden.authenticator.ui.platform.feature.rootnav.RootNavScreen
import com.bitwarden.authenticator.ui.platform.theme.AuthenticatorTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Primary entry point for the application.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    @Inject
    lateinit var debugLaunchManager: DebugMenuLaunchManager

    override fun onCreate(savedInstanceState: Bundle?) {
        sanitizeIntent()
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

        setContent {
            val state by mainViewModel.stateFlow.collectAsStateWithLifecycle()
            val navController = rememberNavController()
            observeViewModelEvents(navController)
            AuthenticatorTheme(
                theme = state.theme,
            ) {
                RootNavScreen(
                    navController = navController,
                    onSplashScreenRemoved = { shouldShowSplashScreen = false },
                    onExitApplication = { finishAffinity() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        sanitizeIntent()
        mainViewModel.trySendAction(
            MainAction.ReceiveNewIntent(intent = intent),
        )
    }

    private fun sanitizeIntent() {
        if (intent.isSuspicious) {
            intent = Intent(
                /* packageContext = */ this,
                /* cls = */ MainActivity::class.java,
            )
        }
    }

    private fun observeViewModelEvents(navController: NavHostController) {
        mainViewModel
            .eventFlow
            .onEach { event ->
                when (event) {
                    is MainEvent.ScreenCaptureSettingChange -> {
                        handleScreenCaptureSettingChange(event)
                    }

                    MainEvent.NavigateToDebugMenu -> navController.navigateToDebugMenuScreen()
                }
            }
            .launchIn(lifecycleScope)
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

    private fun handleScreenCaptureSettingChange(event: MainEvent.ScreenCaptureSettingChange) {
        if (event.isAllowed) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
