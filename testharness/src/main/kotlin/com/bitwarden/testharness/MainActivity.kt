package com.bitwarden.testharness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.bitwarden.testharness.ui.platform.feature.rootnav.RootNavScreen
import com.bitwarden.testharness.ui.platform.feature.rootnav.RootNavigationRoute
import com.bitwarden.testharness.ui.platform.feature.rootnav.rootNavDestination
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.platform.util.setupEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map

/**
 * Primary entry point for the Credential Manager test harness application.
 *
 * Delegates navigation to [RootNavScreen] following the same pattern as @app and @authenticator
 * modules. Handles Activity-level concerns like theme and splash screen.
 *
 * The root navigation is managed by [RootNavScreen] which orchestrates all test screen flows:
 * - Landing screen with test category selection (Autofill, Credential Manager)
 * - Individual test screens for each Credential Manager API operation
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        var shouldShowSplashScreen = true
        installSplashScreen().setKeepOnScreenCondition { shouldShowSplashScreen }
        super.onCreate(savedInstanceState)

        setupEdgeToEdge(appThemeFlow = mainViewModel.stateFlow.map { it.theme })

        setContent {
            val navController = rememberNavController()
            val state by mainViewModel.stateFlow.collectAsStateWithLifecycle()

            BitwardenTheme(
                theme = state.theme,
            ) {
                NavHost(
                    navController = navController,
                    startDestination = RootNavigationRoute,
                ) {
                    rootNavDestination { shouldShowSplashScreen = false }
                }
            }
        }
    }
}
