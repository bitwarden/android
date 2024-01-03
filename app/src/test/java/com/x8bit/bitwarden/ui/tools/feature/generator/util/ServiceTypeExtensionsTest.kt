package com.x8bit.bitwarden.ui.tools.feature.generator.util

import com.bitwarden.core.ForwarderServiceType
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ServiceTypeExtensionsTest {

    @Test
    fun `toUsernameGeneratorRequest for AddyIo returns correct request`() {
        val addyIoServiceType = ServiceType.AddyIo(
            apiAccessToken = "testToken",
            domainName = "test.com",
            baseUrl = "http://test.com",
        )
        val request = addyIoServiceType.toUsernameGeneratorRequest()

        assertEquals(
            ForwarderServiceType.AddyIo(
                apiToken = "testToken",
                domain = "test.com",
                baseUrl = "http://test.com",
            ),
            request.service,
        )
        assertEquals(null, request.website)
    }

    @Test
    fun `toUsernameGeneratorRequest for DuckDuckGo returns correct request`() {
        val duckDuckGoServiceType = ServiceType.DuckDuckGo(apiKey = "testKey")
        val request = duckDuckGoServiceType.toUsernameGeneratorRequest()

        assertEquals(ForwarderServiceType.DuckDuckGo("testKey"), request.service)
        assertEquals(null, request.website)
    }

    @Test
    fun `toUsernameGeneratorRequest for FirefoxRelay returns correct request`() {
        val firefoxRelayServiceType = ServiceType.FirefoxRelay(apiAccessToken = "testToken")
        val request = firefoxRelayServiceType.toUsernameGeneratorRequest()

        assertEquals(ForwarderServiceType.Firefox(apiToken = "testToken"), request.service)
        assertEquals(null, request.website)
    }

    @Test
    fun `toUsernameGeneratorRequest for FastMail returns correct request`() {
        val fastMailServiceType = ServiceType.FastMail(apiKey = "testKey")
        val request = fastMailServiceType.toUsernameGeneratorRequest()

        assertEquals(ForwarderServiceType.Fastmail(apiToken = "testKey"), request.service)
        assertEquals(null, request.website)
    }

    @Test
    fun `toUsernameGeneratorRequest for SimpleLogin returns correct request`() {
        val simpleLoginServiceType = ServiceType.SimpleLogin(apiKey = "testKey")
        val request = simpleLoginServiceType.toUsernameGeneratorRequest()

        assertEquals(ForwarderServiceType.SimpleLogin(apiKey = "testKey"), request.service)
        assertEquals(null, request.website)
    }
}
