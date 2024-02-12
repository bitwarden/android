package com.x8bit.bitwarden.ui.platform.feature.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * Splash screen with empty composable content so that the Activity window background is shown.
 */
@OmitFromCoverage
@Composable
fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize())
}
