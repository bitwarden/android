package com.x8bit.bitwarden.ui.platform.util

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppThemeExtensionsTest {
    @Test
    fun `displayLabel should return the correct value for each type`() {
        mapOf(
            AppTheme.DEFAULT to R.string.default_system.asText(),
            AppTheme.DARK to R.string.dark.asText(),
            AppTheme.LIGHT to R.string.light.asText(),
        )
            .forEach { (type, label) ->
                assertEquals(
                    label,
                    type.displayLabel,
                )
            }
    }
}
