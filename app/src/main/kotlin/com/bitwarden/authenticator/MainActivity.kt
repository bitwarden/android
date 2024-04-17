package com.bitwarden.authenticator

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.authenticator.ui.platform.feature.rootnav.RootNavScreen
import com.bitwarden.authenticator.ui.platform.theme.AuthenticatorTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var shouldShowSplashScreen = true
        installSplashScreen().setKeepOnScreenCondition { shouldShowSplashScreen }

        if (savedInstanceState == null) {
            mainViewModel.trySendAction(
                MainAction.ReceiveFirstIntent(
                    intent = intent
                )
            )
        }

        setContent {
            val state by mainViewModel.stateFlow.collectAsStateWithLifecycle()

            AuthenticatorTheme(
                theme = state.theme
            ) {
                RootNavScreen(
                    onSplashScreenRemoved = { shouldShowSplashScreen = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        mainViewModel.trySendAction(
            MainAction.ReceiveNewIntent(intent = intent)
        )
    }
}
