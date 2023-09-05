package com.x8bit.bitwarden.ui.platform.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.R.color

/**
 * The overall application theme. This can be configured to support a [darkTheme] and
 * [dynamicColor].
 */
@Composable
fun BitwardenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Get the current scheme
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkColorScheme(context)
        else -> lightColorScheme(context)
    }

    // Update status bar according to scheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    // Set overall theme based on color scheme and typography settings
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

private fun darkColorScheme(context: Context): ColorScheme =
    darkColorScheme(
        primary = Color(context.getColor(color.dark_primary)),
        secondary = Color(context.getColor(R.color.dark_primary)),
        tertiary = Color(context.getColor(R.color.dark_primary)),
    )

private fun lightColorScheme(context: Context): ColorScheme =
    lightColorScheme(
        primary = Color(context.getColor(color.primary)),
        secondary = Color(context.getColor(R.color.primary)),
        tertiary = Color(context.getColor(R.color.primary)),
    )
