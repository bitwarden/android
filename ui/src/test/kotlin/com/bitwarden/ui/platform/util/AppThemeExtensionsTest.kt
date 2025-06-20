package com.bitwarden.ui.platform.util

import com.bitwarden.ui.platform.base.BaseComposeTest
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppThemeExtensionsTest : BaseComposeTest() {
    @Test
    fun `isDarkMode with Dark AppTheme should return true regardless of system mode`() {
        val appTheme = AppTheme.DARK
        assertTrue(appTheme.isDarkMode(isSystemDarkMode = false))
        assertTrue(appTheme.isDarkMode(isSystemDarkMode = true))
    }

    @Test
    fun `isDarkMode with Light AppTheme should return false regardless of system mode`() {
        val appTheme = AppTheme.LIGHT
        assertFalse(appTheme.isDarkMode(isSystemDarkMode = false))
        assertFalse(appTheme.isDarkMode(isSystemDarkMode = true))
    }

    @Test
    fun `isDarkMode with default AppTheme should return correct value based on system mode`() {
        val appTheme = AppTheme.DEFAULT
        assertFalse(appTheme.isDarkMode(isSystemDarkMode = false))
        assertTrue(appTheme.isDarkMode(isSystemDarkMode = true))
    }
}
