package com.x8bit.bitwarden.data.platform.repository.util

import com.bitwarden.servercommunicationconfig.AcquiredCookie
import com.x8bit.bitwarden.data.platform.datasource.disk.model.CookieConfigurationData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AcquiredCookieExtensionsTest {

    @Test
    fun `toConfigurationCookie should map AcquiredCookie to CookieConfigurationData Cookie`() {
        val acquiredCookie = AcquiredCookie(
            name = "BW_SESSION",
            value = "session_value",
        )

        val result = acquiredCookie.toConfigurationCookie()

        assertEquals(
            CookieConfigurationData.Cookie(
                name = "BW_SESSION",
                value = "session_value",
            ),
            result,
        )
    }

    @Test
    fun `toConfigurationDataCookies should map list of AcquiredCookie to list of Cookie`() {
        val acquiredCookies = listOf(
            AcquiredCookie(name = "SESSION", value = "session_val"),
            AcquiredCookie(name = "REFRESH", value = "refresh_val"),
        )

        val result = acquiredCookies.toConfigurationDataCookies()

        assertEquals(
            listOf(
                CookieConfigurationData.Cookie(name = "SESSION", value = "session_val"),
                CookieConfigurationData.Cookie(name = "REFRESH", value = "refresh_val"),
            ),
            result,
        )
    }
}
