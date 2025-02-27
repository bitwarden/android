package com.bitwarden.authenticator.data.platform.datasource.network.interceptor

import com.bitwarden.authenticator.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.authenticator.data.platform.repository.model.Environment
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
    }

    @Suppress("MaxLineLength")
    @Test
    fun `setting the environment should update all the interceptors correctly for a non-blank base URL`() {
        baseUrlInterceptors.environment = Environment.Eu

        assertEquals(
            "https://vault.bitwarden.eu/api",
            baseUrlInterceptors.apiInterceptor.baseUrl,
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
    }
}
