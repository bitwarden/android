package com.bitwarden.ui.platform.theme

import android.content.Context
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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.ui.platform.components.field.interceptor.IncognitoInput
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.ui.platform.theme.color.BitwardenColorScheme
import com.bitwarden.ui.platform.theme.color.darkBitwardenColorScheme
import com.bitwarden.ui.platform.theme.color.dynamicBitwardenColorScheme
import com.bitwarden.ui.platform.theme.color.lightBitwardenColorScheme
import com.bitwarden.ui.platform.theme.color.toMaterialColorScheme
import com.bitwarden.ui.platform.theme.shape.BitwardenShapes
import com.bitwarden.ui.platform.theme.shape.bitwardenShapes
import com.bitwarden.ui.platform.theme.type.BitwardenTypography
import com.bitwarden.ui.platform.theme.type.bitwardenTypography
import com.bitwarden.ui.platform.theme.type.toMaterialTypography
import com.bitwarden.ui.platform.util.isDarkMode

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
    context: Context = LocalContext.current,
    theme: AppTheme = AppTheme.DEFAULT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isDarkTheme = theme.isDarkMode(isSystemDarkMode = isSystemInDarkTheme())
    val isDynamicEnabled = dynamicColor && isBuildVersionAtLeast(version = Build.VERSION_CODES.S)
    // Get the material color schemes from the OS.
    val darkMaterialColorScheme = if (isDynamicEnabled) {
        dynamicDarkColorScheme(context = context)
    } else {
        darkColorScheme()
    }
    val lightMaterialColorScheme = if (isDynamicEnabled) {
        dynamicLightColorScheme(context = context)
    } else {
        lightColorScheme()
    }
    val materialColorScheme = if (isDarkTheme) darkMaterialColorScheme else lightMaterialColorScheme
    // Convert the material color schemes to Bitwarden color schemes.
    val lightDynamicColorScheme = dynamicBitwardenColorScheme(
        materialColorScheme = lightMaterialColorScheme,
        isDarkTheme = false,
    )
    val darkDynamicColorScheme = dynamicBitwardenColorScheme(
        materialColorScheme = darkMaterialColorScheme,
        isDarkTheme = true,
    )
    val bitwardenColorScheme = when {
        isDynamicEnabled -> if (isDarkTheme) darkDynamicColorScheme else lightDynamicColorScheme
        isDarkTheme -> darkBitwardenColorScheme
        else -> lightBitwardenColorScheme
    }
    // Provide the selected color scheme as well as the dynamic ones for override support.
    CompositionLocalProvider(
        LocalBitwardenColorScheme provides bitwardenColorScheme,
        LocalBitwardenDynamicLightColorScheme provides lightDynamicColorScheme,
        LocalBitwardenDynamicDarkColorScheme provides darkDynamicColorScheme,
        LocalBitwardenShapes provides bitwardenShapes,
        LocalBitwardenTypography provides bitwardenTypography,
    ) {
        MaterialTheme(
            colorScheme = bitwardenColorScheme.toMaterialColorScheme(
                defaultColorScheme = materialColorScheme,
            ),
            typography = bitwardenTypography.toMaterialTypography(),
        ) { IncognitoInput(content = content) }
    }
}

/**
 * Provides access to the Bitwarden colors throughout the app.
 */
val LocalBitwardenColorScheme: ProvidableCompositionLocal<BitwardenColorScheme> =
    compositionLocalOf { lightBitwardenColorScheme }

/**
 * Provides access to the Bitwarden dynamic light-mode colors throughout the app.
 * This is only to be used for scenarios that require the UI to have its colors overridden.
 */
val LocalBitwardenDynamicLightColorScheme: ProvidableCompositionLocal<BitwardenColorScheme> =
    compositionLocalOf { lightBitwardenColorScheme }

/**
 * Provides access to the Bitwarden default dark-mode colors throughout the app.
 * This is only to be used for dynamic that require the UI to have its colors overridden.
 */
val LocalBitwardenDynamicDarkColorScheme: ProvidableCompositionLocal<BitwardenColorScheme> =
    compositionLocalOf { darkBitwardenColorScheme }

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
