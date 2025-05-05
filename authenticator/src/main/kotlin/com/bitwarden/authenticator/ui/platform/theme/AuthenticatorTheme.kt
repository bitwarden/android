package com.bitwarden.authenticator.ui.platform.theme

import android.app.Activity
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.annotation.ColorRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.authenticator.ui.platform.manager.biometrics.BiometricsManager
import com.bitwarden.authenticator.ui.platform.manager.biometrics.BiometricsManagerImpl
import com.bitwarden.authenticator.ui.platform.manager.exit.ExitManager
import com.bitwarden.authenticator.ui.platform.manager.exit.ExitManagerImpl
import com.bitwarden.authenticator.ui.platform.manager.intent.IntentManager
import com.bitwarden.authenticator.ui.platform.manager.intent.IntentManagerImpl
import com.bitwarden.authenticator.ui.platform.manager.permissions.PermissionsManager
import com.bitwarden.authenticator.ui.platform.manager.permissions.PermissionsManagerImpl

/**
 * The overall application theme. This can be configured to support a [theme] and [dynamicColor].
 */
@Composable
fun AuthenticatorTheme(
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
    val activity = context as Activity
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
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
            window.setBackgroundDrawable(ColorDrawable(colorScheme.surface.value.toInt()))
        }
    }

    val nonMaterialColors = if (darkTheme) {
        darkNonMaterialColors(context)
    } else {
        lightNonMaterialColors(context)
    }

    CompositionLocalProvider(
        LocalNonMaterialColors provides nonMaterialColors,
        LocalNonMaterialTypography provides nonMaterialTypography,
        LocalPermissionsManager provides PermissionsManagerImpl(activity),
        LocalIntentManager provides IntentManagerImpl(context),
        LocalExitManager provides ExitManagerImpl(activity),
        LocalBiometricsManager provides BiometricsManagerImpl(activity),
    ) {
        // Set overall theme based on color scheme and typography settings
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}

private fun darkColorScheme(context: Context): ColorScheme =
    androidx.compose.material3.darkColorScheme(
        primary = R.color.dark_primary.toColor(context),
        onPrimary = R.color.dark_on_primary.toColor(context),
        primaryContainer = R.color.dark_primary_container.toColor(context),
        onPrimaryContainer = R.color.dark_on_primary_container.toColor(context),
        secondary = R.color.dark_secondary.toColor(context),
        onSecondary = R.color.dark_on_secondary.toColor(context),
        secondaryContainer = R.color.dark_secondary_container.toColor(context),
        onSecondaryContainer = R.color.dark_on_secondary_container.toColor(context),
        tertiary = R.color.dark_tertiary.toColor(context),
        onTertiary = R.color.dark_on_tertiary.toColor(context),
        tertiaryContainer = R.color.dark_tertiary_container.toColor(context),
        onTertiaryContainer = R.color.dark_on_tertiary_container.toColor(context),
        error = R.color.dark_error.toColor(context),
        onError = R.color.dark_on_error.toColor(context),
        errorContainer = R.color.dark_error_container.toColor(context),
        onErrorContainer = R.color.dark_on_error_container.toColor(context),
        surface = R.color.dark_surface.toColor(context),
        surfaceBright = R.color.dark_surface_bright.toColor(context),
        surfaceContainer = R.color.dark_surface_container.toColor(context),
        surfaceContainerHigh = R.color.dark_surface_container_high.toColor(context),
        surfaceContainerHighest = R.color.dark_surface_container_highest.toColor(context),
        surfaceContainerLow = R.color.dark_surface_container_low.toColor(context),
        surfaceContainerLowest = R.color.dark_surface_container_lowest.toColor(context),
        surfaceVariant = R.color.dark_surface_variant.toColor(context),
        surfaceDim = R.color.dark_surface_dim.toColor(context),
        onSurface = R.color.dark_on_surface.toColor(context),
        onSurfaceVariant = R.color.dark_on_surface_variant.toColor(context),
        outline = R.color.dark_outline.toColor(context),
        outlineVariant = R.color.dark_outline_variant.toColor(context),
        inverseSurface = R.color.dark_inverse_surface.toColor(context),
        inverseOnSurface = R.color.dark_inverse_on_surface.toColor(context),
        inversePrimary = R.color.dark_inverse_primary.toColor(context),
        scrim = R.color.dark_scrim.toColor(context),
    )

private fun lightColorScheme(context: Context): ColorScheme =
    androidx.compose.material3.lightColorScheme(
        primary = R.color.primary.toColor(context),
        onPrimary = R.color.on_primary.toColor(context),
        primaryContainer = R.color.primary_container.toColor(context),
        onPrimaryContainer = R.color.on_primary_container.toColor(context),
        secondary = R.color.secondary.toColor(context),
        onSecondary = R.color.on_secondary.toColor(context),
        secondaryContainer = R.color.secondary_container.toColor(context),
        onSecondaryContainer = R.color.on_secondary_container.toColor(context),
        tertiary = R.color.tertiary.toColor(context),
        onTertiary = R.color.on_tertiary.toColor(context),
        tertiaryContainer = R.color.tertiary_container.toColor(context),
        onTertiaryContainer = R.color.on_tertiary_container.toColor(context),
        error = R.color.error.toColor(context),
        onError = R.color.on_error.toColor(context),
        errorContainer = R.color.error_container.toColor(context),
        onErrorContainer = R.color.on_error_container.toColor(context),
        surface = R.color.surface.toColor(context),
        surfaceBright = R.color.surface_bright.toColor(context),
        surfaceContainer = R.color.surface_container.toColor(context),
        surfaceContainerHigh = R.color.surface_container_high.toColor(context),
        surfaceContainerHighest = R.color.surface_container_highest.toColor(context),
        surfaceContainerLow = R.color.surface_container_low.toColor(context),
        surfaceContainerLowest = R.color.surface_container_lowest.toColor(context),
        surfaceVariant = R.color.surface_variant.toColor(context),
        surfaceDim = R.color.surface_dim.toColor(context),
        onSurface = R.color.on_surface.toColor(context),
        onSurfaceVariant = R.color.on_surface_variant.toColor(context),
        outline = R.color.outline.toColor(context),
        outlineVariant = R.color.outline_variant.toColor(context),
        inverseSurface = R.color.inverse_surface.toColor(context),
        inverseOnSurface = R.color.inverse_on_surface.toColor(context),
        inversePrimary = R.color.inverse_primary.toColor(context),
        scrim = R.color.scrim.toColor(context),
    )

@ColorRes
private fun Int.toColor(context: Context): Color =
    Color(context.getColor(this))

/**
 * Provides access to the biometrics manager throughout the app.
 */
val LocalBiometricsManager: ProvidableCompositionLocal<BiometricsManager> = compositionLocalOf {
    error("CompositionLocal BiometricsManager not present")
}

/**
 * Provides access to the exit manager throughout the app.
 */
val LocalExitManager: ProvidableCompositionLocal<ExitManager> = compositionLocalOf {
    error("CompositionLocal ExitManager not present")
}

/**
 * Provides access to the intent manager throughout the app.
 */
val LocalIntentManager: ProvidableCompositionLocal<IntentManager> = compositionLocalOf {
    error("CompositionLocal LocalIntentManager not present")
}

/**
 * Provides access to the permission manager throughout the app.
 */
val LocalPermissionsManager: ProvidableCompositionLocal<PermissionsManager> = compositionLocalOf {
    error("CompositionLocal LocalPermissionsManager not present")
}

/**
 * Provides access to non material theme typography throughout the app.
 */
val LocalNonMaterialTypography: ProvidableCompositionLocal<NonMaterialTypography> =
    compositionLocalOf { nonMaterialTypography }

/**
 * Provides access to non material theme colors throughout the app.
 */
val LocalNonMaterialColors: ProvidableCompositionLocal<NonMaterialColors> =
    compositionLocalOf {
        // Default value here will immediately be overridden in BitwardenTheme, similar
        // to how MaterialTheme works.
        NonMaterialColors(
            fingerprint = Color.Transparent,
            qrCodeClickableText = Color.Transparent,
        )
    }

/**
 * Models colors that live outside of the Material Theme spec.
 */
data class NonMaterialColors(
    val fingerprint: Color,
    val qrCodeClickableText: Color,
)

private fun lightNonMaterialColors(context: Context): NonMaterialColors =
    NonMaterialColors(
        fingerprint = R.color.light_fingerprint.toColor(context),
        qrCodeClickableText = R.color.qr_code_clickable_text.toColor(context),
    )

private fun darkNonMaterialColors(context: Context): NonMaterialColors =
    NonMaterialColors(
        fingerprint = R.color.dark_fingerprint.toColor(context),
        qrCodeClickableText = R.color.qr_code_clickable_text.toColor(context),
    )
