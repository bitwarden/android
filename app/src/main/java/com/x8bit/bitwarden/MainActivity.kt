package com.x8bit.bitwarden

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.x8bit.bitwarden.ui.platform.feature.rootnav.RootNavScreen
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Primary entry point for the application.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BitwardenTheme {
                RootNavScreen()
            }
        }
    }
}
