package com.x8bit.bitwarden.ui.platform.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.x8bit.bitwarden.ui.platform.theme.color.BitwardenColorScheme
import com.x8bit.bitwarden.ui.platform.theme.color.darkBitwardenColorScheme
import com.x8bit.bitwarden.ui.platform.theme.color.dynamicBitwardenColorScheme
import com.x8bit.bitwarden.ui.platform.theme.color.lightBitwardenColorScheme
import com.x8bit.bitwarden.ui.platform.theme.color.toMaterialColorScheme
import com.x8bit.bitwarden.ui.platform.theme.shape.BitwardenShapes
import com.x8bit.bitwarden.ui.platform.theme.shape.bitwardenShapes
import com.x8bit.bitwarden.ui.platform.theme.type.BitwardenTypography
import com.x8bit.bitwarden.ui.platform.theme.type.bitwardenTypography
import com.x8bit.bitwarden.ui.platform.theme.type.toMaterialTypography

/**
 * Static wrapper to make accessing the theme components easier.
 */
object BitwardenTheme {
    /**
     * Retrieves the current [BitwardenColorScheme].
     */
    val colorScheme: BitwardenColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalBitwardenColorScheme.current

    /**
     * Retrieves the current [BitwardenShapes].
     */
    val shapes: BitwardenShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalBitwardenShapes.current

    /**
     * Retrieves the current [BitwardenTypography].
     */
    val typography: BitwardenTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalBitwardenTypography.current
}

/**
 * The overall application theme. This can be configured to support a [theme] and [dynamicColor].
 */
@Composable
fun BitwardenTheme(
    theme: AppTheme = AppTheme.DEFAULT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (theme) {
        AppTheme.DEFAULT -> isSystemInDarkTheme()
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
    }

    // Get the current scheme
    val context = LocalContext.current
    val materialColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) {
                dynamicDarkColorScheme(context = context)
            } else {
                dynamicLightColorScheme(context = context)
            }
        }

        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    val bitwardenColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicBitwardenColorScheme(
                materialColorScheme = materialColorScheme,
                isDarkTheme = darkTheme,
            )
        }

        darkTheme -> darkBitwardenColorScheme
        else -> lightBitwardenColorScheme
    }

    // Update status bar according to scheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
            window.setBackgroundDrawable(
                ColorDrawable(bitwardenColorScheme.background.primary.value.toInt()),
            )
        }
    }

    CompositionLocalProvider(
        LocalBitwardenColorScheme provides bitwardenColorScheme,
        LocalBitwardenShapes provides bitwardenShapes,
        LocalBitwardenTypography provides bitwardenTypography,
    ) {
        MaterialTheme(
            colorScheme = bitwardenColorScheme.toMaterialColorScheme(
                defaultColorScheme = materialColorScheme,
            ),
            typography = bitwardenTypography.toMaterialTypography(),
            content = content,
        )
    }
}

/**
 * Provides access to the Bitwarden colors throughout the app.
 */
val LocalBitwardenColorScheme: ProvidableCompositionLocal<BitwardenColorScheme> =
    compositionLocalOf { lightBitwardenColorScheme }

/**
 * Provides access to the Bitwarden shapes throughout the app.
 */
val LocalBitwardenShapes: ProvidableCompositionLocal<BitwardenShapes> =
    compositionLocalOf { bitwardenShapes }

/**
 * Provides access to the Bitwarden typography throughout the app.
 */
val LocalBitwardenTypography: ProvidableCompositionLocal<BitwardenTypography> =
    compositionLocalOf { bitwardenTypography }
