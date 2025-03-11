package com.x8bit.bitwarden.ui.platform.util

import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import java.util.Locale

class LocaleExtensionsTest {

    @Test
    fun `locale with Espanol language returns AppLanguage SPANISH`() {
        val locale = Locale("es")
        assertEquals(
            AppLanguage.SPANISH,
            locale.appLanguage,
        )
    }

    @Test
    fun `locale with GB english returns AppLanguage ENGLISH_BRITISH`() {
        val locale = Locale("en-GB")
        assertEquals(
            AppLanguage.ENGLISH_BRITISH,
            locale.appLanguage,
        )
    }

    @Test
    fun `locale with non existent app language returns null`() {
        val locale = Locale("😅😅😅")
        assertNull(locale.appLanguage)
    }
}
