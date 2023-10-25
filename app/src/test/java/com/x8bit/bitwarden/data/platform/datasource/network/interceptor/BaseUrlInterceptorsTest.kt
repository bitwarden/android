package com.x8bit.bitwarden.data.platform.datasource.network.interceptor

import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BaseUrlInterceptorsTest {
    private val baseUrlInterceptors = BaseUrlInterceptors()

    @Test
    fun `the default environment should be US and all interceptors should have the correct URLs`() {
        assertEquals(
            Environment.Us,
            baseUrlInterceptors.environment,
        )
        assertEquals(
            "https://vault.bitwarden.com/api",
            baseUrlInterceptors.apiInterceptor.baseUrl,
        )
        assertEquals(
            "https://vault.bitwarden.com/identity",
            baseUrlInterceptors.identityInterceptor.baseUrl,
        )
        assertEquals(
            "https://vault.bitwarden.com/events",
            baseUrlInterceptors.eventsInterceptor.baseUrl,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `setting the environment should update all the interceptors correctly for a non-blank base URL`() {
        baseUrlInterceptors.environment = Environment.Eu

        assertEquals(
            "https://vault.bitwarden.eu/api",
            baseUrlInterceptors.apiInterceptor.baseUrl,
        )
        assertEquals(
            "https://vault.bitwarden.eu/identity",
            baseUrlInterceptors.identityInterceptor.baseUrl,
        )
        assertEquals(
            "https://vault.bitwarden.eu/events",
            baseUrlInterceptors.eventsInterceptor.baseUrl,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `setting the environment should update all the interceptors correctly for a blank base URL and all URLs filled`() {
        baseUrlInterceptors.environment = Environment.SelfHosted(
            environmentUrlData = EnvironmentUrlDataJson(
                base = "   ",
                api = "https://api.com",
                identity = "https://identity.com",
                events = "https://events.com",
            ),
        )

        assertEquals(
            "https://api.com",
            baseUrlInterceptors.apiInterceptor.baseUrl,
        )
        assertEquals(
            "https://identity.com",
            baseUrlInterceptors.identityInterceptor.baseUrl,
        )
        assertEquals(
            "https://events.com",
            baseUrlInterceptors.eventsInterceptor.baseUrl,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `setting the environment should update all the interceptors correctly for a blank base URL and some or all URLs absent`() {
        baseUrlInterceptors.environment = Environment.SelfHosted(
            environmentUrlData = EnvironmentUrlDataJson(
                base = "   ",
                api = "",
                identity = "",
                icon = "   ",
            ),
        )

        assertEquals(
            "https://api.bitwarden.com",
            baseUrlInterceptors.apiInterceptor.baseUrl,
        )
        assertEquals(
            "https://identity.bitwarden.com",
            baseUrlInterceptors.identityInterceptor.baseUrl,
        )
        assertEquals(
            "https://events.bitwarden.com",
            baseUrlInterceptors.eventsInterceptor.baseUrl,
        )
    }
}
