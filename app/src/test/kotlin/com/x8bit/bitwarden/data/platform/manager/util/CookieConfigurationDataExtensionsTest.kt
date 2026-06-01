package com.x8bit.bitwarden.data.platform.manager.util

import com.bitwarden.network.model.NetworkCookie
import com.x8bit.bitwarden.data.platform.datasource.disk.model.CookieConfigurationData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CookieConfigurationDataExtensionsTest {

    @Test
    fun `toNetworkCookieList should return empty list when receiver is null`() {
        val cookies: List<CookieConfigurationData.Cookie>? = null

        val result = cookies.toNetworkCookieList()

        assertEquals(emptyList<NetworkCookie>(), result)
    }

    @Test
    fun `toNetworkCookieList should return empty list when list is empty`() {
        val cookies: List<CookieConfigurationData.Cookie> = emptyList()

        val result = cookies.toNetworkCookieList()

        assertEquals(emptyList<NetworkCookie>(), result)
    }

    @Test
    fun `toNetworkCookieList should map single cookie correctly`() {
        val cookies = listOf(
            CookieConfigurationData.Cookie(name = "awselb", value = "session123"),
        )

        val result = cookies.toNetworkCookieList()

        assertEquals(
            listOf(NetworkCookie(name = "awselb", value = "session123")),
            result,
        )
    }

    @Test
    fun `toNetworkCookieList should map multiple cookies correctly`() {
        val cookies = listOf(
            CookieConfigurationData.Cookie(name = "awselb", value = "session123"),
            CookieConfigurationData.Cookie(name = "awselbcors", value = "cors456"),
        )

        val result = cookies.toNetworkCookieList()

        assertEquals(
            listOf(
                NetworkCookie(name = "awselb", value = "session123"),
                NetworkCookie(name = "awselbcors", value = "cors456"),
            ),
            result,
        )
    }

    @Test
    fun `toNetworkCookie should map name and value correctly`() {
        val cookie = CookieConfigurationData.Cookie(
            name = "awselb",
            value = "session123",
        )

        val result = cookie.toNetworkCookie()

        assertEquals(NetworkCookie(name = "awselb", value = "session123"), result)
    }
}
