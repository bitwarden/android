package com.bitwarden.ui.platform.base.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.bitwarden.ui.platform.base.BaseComposeTest
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.ui.platform.theme.BitwardenTheme
import org.junit.Assert
import org.junit.Test

class ColorExtensionsTest : BaseComposeTest() {

    @Suppress("MaxLineLength")
    @Test
    fun `isLightOverlayRequired for a color with luminance below the light threshold should return true`() {
        Assert.assertTrue(Color.Companion.Blue.isLightOverlayRequired)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isLightOverlayRequired for a color with luminance above the light threshold should return false`() {
        Assert.assertFalse(Color.Companion.Yellow.isLightOverlayRequired)
    }

    @Test
    fun `toSafeOverlayColor for a dark color in light mode should use the surface color`() =
        setContent(theme = AppTheme.LIGHT) {
            Assert.assertEquals(
                BitwardenTheme.colorScheme.background.primary,
                Color.Companion.Blue.toSafeOverlayColor(),
            )
        }

    @Test
    fun `toSafeOverlayColor for a dark color in dark mode should use the onSurface color`() =
        setContent(theme = AppTheme.DARK) {
            Assert.assertEquals(
                BitwardenTheme.colorScheme.text.primary,
                Color.Companion.Blue.toSafeOverlayColor(),
            )
        }

    @Test
    fun `toSafeOverlayColor for a light color in light mode should use the onSurface color`() =
        setContent(theme = AppTheme.LIGHT) {
            Assert.assertEquals(
                BitwardenTheme.colorScheme.text.primary,
                Color.Companion.Yellow.toSafeOverlayColor(),
            )
        }

    @Test
    fun `toSafeOverlayColor for a light color in dark mode should use the surface color`() =
        setContent(theme = AppTheme.DARK) {
            Assert.assertEquals(
                BitwardenTheme.colorScheme.background.primary,
                Color.Companion.Yellow.toSafeOverlayColor(),
            )
        }

    @Suppress("LongParameterList")
    fun setContent(
        theme: AppTheme = AppTheme.DEFAULT,
        test: @Composable () -> Unit,
    ) {
        setTestContent {
            BitwardenTheme(
                theme = theme,
                content = test,
            )
        }
    }
}
