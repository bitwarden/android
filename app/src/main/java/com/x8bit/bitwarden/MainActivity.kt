package com.x8bit.bitwarden

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.x8bit.bitwarden.ui.feature.rootnav.RootNavScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Primary entry point for the application.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RootNavScreen() }
    }
}
