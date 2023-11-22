package com.x8bit.bitwarden.data.platform.base.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.isLightOverlayRequired
import com.x8bit.bitwarden.ui.platform.base.util.toSafeOverlayColor
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
        runTestWithTheme(isDarkTheme = false) {
            assertEquals(
                MaterialTheme.colorScheme.surface,
                Color.Blue.toSafeOverlayColor(),
            )
        }

    @Test
    fun `toSafeOverlayColor for a dark color in dark mode should use the onSurface color`() =
        runTestWithTheme(isDarkTheme = true) {
            assertEquals(
                MaterialTheme.colorScheme.onSurface,
                Color.Blue.toSafeOverlayColor(),
            )
        }

    @Test
    fun `toSafeOverlayColor for a light color in light mode should use the onSurface color`() =
        runTestWithTheme(isDarkTheme = false) {
            assertEquals(
                MaterialTheme.colorScheme.onSurface,
                Color.Yellow.toSafeOverlayColor(),
            )
        }

    @Test
    fun `toSafeOverlayColor for a light color in dark mode should use the surface color`() =
        runTestWithTheme(isDarkTheme = true) {
            assertEquals(
                MaterialTheme.colorScheme.surface,
                Color.Yellow.toSafeOverlayColor(),
            )
        }
}
