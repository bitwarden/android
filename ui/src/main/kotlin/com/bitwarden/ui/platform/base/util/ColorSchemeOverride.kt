package com.bitwarden.ui.platform.base.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.platform.theme.LocalBitwardenColorScheme
import com.bitwarden.ui.platform.theme.LocalBitwardenDynamicDarkColorScheme
import com.bitwarden.ui.platform.theme.LocalBitwardenDynamicLightColorScheme
import com.bitwarden.ui.platform.theme.color.BitwardenColorScheme
import com.bitwarden.ui.platform.theme.color.darkBitwardenColorScheme
import com.bitwarden.ui.platform.theme.color.lightBitwardenColorScheme

/**
 * Overrides the [BitwardenColorScheme] for the [content] based on the provided [AppTheme].
 */
@Composable
fun ColorSchemeOverride(
    appTheme: AppTheme,
    content: @Composable () -> Unit,
) {
    val colorSchemeOverride = when (appTheme) {
        AppTheme.DEFAULT -> BitwardenTheme.colorScheme
        AppTheme.DARK -> {
            if (BitwardenTheme.colorScheme.isDynamicTheme) {
                LocalBitwardenDynamicDarkColorScheme.current
            } else {
                darkBitwardenColorScheme
            }
        }

        AppTheme.LIGHT -> {
            if (BitwardenTheme.colorScheme.isDynamicTheme) {
                LocalBitwardenDynamicLightColorScheme.current
            } else {
                lightBitwardenColorScheme
            }
        }
    }
    CompositionLocalProvider(
        value = LocalBitwardenColorScheme provides colorSchemeOverride,
        content = content,
    )
}
