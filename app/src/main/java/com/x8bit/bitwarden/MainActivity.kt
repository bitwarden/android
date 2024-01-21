package com.x8bit.bitwarden

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.feature.rootnav.RootNavScreen
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Primary entry point for the application.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    @Inject
    lateinit var settingsRepository: SettingsRepository

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
            BitwardenTheme(
                theme = state.theme,
            ) {
                RootNavScreen(
                    onSplashScreenRemoved = { shouldShowSplashScreen = false },
                )
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
}
