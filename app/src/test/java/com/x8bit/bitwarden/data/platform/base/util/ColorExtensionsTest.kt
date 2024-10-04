package com.x8bit.bitwarden.data.platform.base.util

import androidx.compose.ui.graphics.Color
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.isLightOverlayRequired
import com.x8bit.bitwarden.ui.platform.base.util.toSafeOverlayColor
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorExtensionsTest : BaseComposeTest() {
    @Suppress("MaxLineLength")
    @Test
    fun `isLightOverlayRequired for a color with luminance below the light threshold should return true`() {
        assertTrue(Color.Blue.isLightOverlayRequired)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isLightOverlayRequired for a color with luminance above the light threshold should return false`() {
        assertFalse(Color.Yellow.isLightOverlayRequired)
    }

    @Test
    fun `toSafeOverlayColor for a dark color in light mode should use the surface color`() =
        runTestWithTheme(theme = AppTheme.LIGHT) {
            assertEquals(
                BitwardenTheme.colorScheme.background.primary,
                Color.Blue.toSafeOverlayColor(),
            )
        }

    @Test
    fun `toSafeOverlayColor for a dark color in dark mode should use the onSurface color`() =
        runTestWithTheme(theme = AppTheme.DARK) {
            assertEquals(
                BitwardenTheme.colorScheme.text.primary,
                Color.Blue.toSafeOverlayColor(),
            )
        }

    @Test
    fun `toSafeOverlayColor for a light color in light mode should use the onSurface color`() =
        runTestWithTheme(theme = AppTheme.LIGHT) {
            assertEquals(
                BitwardenTheme.colorScheme.text.primary,
                Color.Yellow.toSafeOverlayColor(),
            )
        }

    @Test
    fun `toSafeOverlayColor for a light color in dark mode should use the surface color`() =
        runTestWithTheme(theme = AppTheme.DARK) {
            assertEquals(
                BitwardenTheme.colorScheme.background.primary,
                Color.Yellow.toSafeOverlayColor(),
            )
        }
}
