@file:OmitFromCoverage

package com.bitwarden.ui.platform.util

import android.content.res.Configuration
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.ColorInt
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@ColorInt
private val SCRIM_COLOR: Int = Color.TRANSPARENT

/**
 * Helper method to handle edge-to-edge logic for dark mode.
 *
 * This logic is from the Now-In-Android app found [here](https://github.com/android/nowinandroid/blob/689ef92e41427ab70f82e2c9fe59755441deae92/app/src/main/kotlin/com/google/samples/apps/nowinandroid/MainActivity.kt#L94).
 */
@Suppress("MaxLineLength")
fun ComponentActivity.setupEdgeToEdge(
    appThemeFlow: Flow<AppTheme>,
) {
    lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(state = Lifecycle.State.STARTED) {
            combine(
                isSystemInDarkModeFlow(),
                appThemeFlow,
            ) { isSystemDarkMode, appTheme ->
                appTheme.isDarkMode(isSystemDarkMode = isSystemDarkMode)
            }
                .distinctUntilChanged()
                .collect { isDarkMode ->
                    // This handles all the settings to go edge-to-edge. We are using a transparent
                    // scrim for system bars and switching between "light" and "dark" based on the
                    // system and internal app theme settings.
                    val style = if (isDarkMode) {
                        SystemBarStyle.dark(scrim = SCRIM_COLOR)
                    } else {
                        SystemBarStyle.light(scrim = SCRIM_COLOR, darkScrim = SCRIM_COLOR)
                    }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                }
        }
    }
}

/**
 * Adds a configuration change listener to retrieve whether system is in dark theme or not.
 * This will emit current status immediately and then will emit changes as needed.
 */
private fun ComponentActivity.isSystemInDarkModeFlow(): Flow<Boolean> =
    callbackFlow {
        channel.trySend(element = resources.configuration.isSystemInDarkMode)
        val listener = Consumer<Configuration> {
            channel.trySend(element = it.isSystemInDarkMode)
        }
        addOnConfigurationChangedListener(listener = listener)
        awaitClose { removeOnConfigurationChangedListener(listener = listener) }
    }
        .distinctUntilChanged()
        .conflate()
