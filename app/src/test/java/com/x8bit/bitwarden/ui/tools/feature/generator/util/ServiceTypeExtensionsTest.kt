package com.x8bit.bitwarden.ui.tools.feature.generator.util

import com.bitwarden.generators.ForwarderServiceType
import com.bitwarden.generators.UsernameGeneratorRequest
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class ServiceTypeExtensionsTest {

    @Test
    fun `toUsernameGeneratorRequest for AddyIo returns null when apiAccessToken is blank`() {
        val addyIoServiceType = ServiceType.AddyIo(
            apiAccessToken = "",
            domainName = "test.com",
            baseUrl = "http://test.com",
        )
        val request = addyIoServiceType.toUsernameGeneratorRequest(website = null)

        assertNull(request)
    }

    @Test
    fun `toUsernameGeneratorRequest for AddyIo returns null when domainName is blank`() {
        val addyIoServiceType = ServiceType.AddyIo(
            apiAccessToken = "testToken",
            domainName = "",
            baseUrl = "http://test.com",
        )
        val request = addyIoServiceType.toUsernameGeneratorRequest(website = null)

        assertNull(request)
    }

    @Test
    fun `toUsernameGeneratorRequest for AddyIo returns correct request`() {
        val addyIoServiceType = ServiceType.AddyIo(
            apiAccessToken = "testToken",
            domainName = "test.com",
            baseUrl = "http://test.com",
        )
        val website = "bitwarden.com"
        val request = addyIoServiceType.toUsernameGeneratorRequest(website)

        assertEquals(
            UsernameGeneratorRequest.Forwarded(
                service = ForwarderServiceType.AddyIo(
                    apiToken = "testToken",
                    domain = "test.com",
                    baseUrl = "http://test.com",
                ),
                website = website,
            ),
            request,
        )
    }

    @Test
    fun `toUsernameGeneratorRequest for DuckDuckGo returns null when apiKey is blank`() {
        val duckDuckGoServiceType = ServiceType.DuckDuckGo(apiKey = "")
        val request = duckDuckGoServiceType.toUsernameGeneratorRequest(website = null)

        assertNull(request)
    }

    @Test
    fun `toUsernameGeneratorRequest for DuckDuckGo returns correct request`() {
        val duckDuckGoServiceType = ServiceType.DuckDuckGo(apiKey = "testKey")
        val website = "bitwarden.com"
        val request = duckDuckGoServiceType.toUsernameGeneratorRequest(website)

        assertEquals(
            UsernameGeneratorRequest.Forwarded(
                service = ForwarderServiceType.DuckDuckGo(token = "testKey"),
                website = website,
            ),
            request,
        )
    }

    @Test
    fun `toUsernameGeneratorRequest for FirefoxRelay returns null when apiAccessToken is blank`() {
        val firefoxRelayServiceType = ServiceType.FirefoxRelay(apiAccessToken = "")
        val request = firefoxRelayServiceType.toUsernameGeneratorRequest(website = null)

        assertNull(request)
    }

    @Test
    fun `toUsernameGeneratorRequest for FirefoxRelay returns correct request`() {
        val firefoxRelayServiceType = ServiceType.FirefoxRelay(apiAccessToken = "testToken")
        val website = "bitwarden.com"
        val request = firefoxRelayServiceType.toUsernameGeneratorRequest(website)

        assertEquals(
            UsernameGeneratorRequest.Forwarded(
                service = ForwarderServiceType.Firefox(apiToken = "testToken"),
                website = website,
            ),
            request,
        )
    }

    @Test
    fun `toUsernameGeneratorRequest for FastMail returns null when apiKey is blank`() {
        val fastMailServiceType = ServiceType.FastMail(apiKey = "")
        val request = fastMailServiceType.toUsernameGeneratorRequest(website = null)

        assertNull(request)
    }

    @Test
    fun `toUsernameGeneratorRequest for FastMail returns correct request`() {
        val fastMailServiceType = ServiceType.FastMail(
            apiKey = "testKey",
        )
        val website = "bitwarden.com"
        val request = fastMailServiceType.toUsernameGeneratorRequest(website)

        assertEquals(
            UsernameGeneratorRequest.Forwarded(
                service = ForwarderServiceType.Fastmail(apiToken = "testKey"),
                website = "bitwarden.com",
            ),
            request,
        )
    }

    @Test
    fun `toUsernameGeneratorRequest for ForwardEmail returns null when apiKey is blank`() {
        val forwardMailServiceType = ServiceType.ForwardEmail(
            apiKey = "",
            domainName = "domainName",
        )
        val request = forwardMailServiceType.toUsernameGeneratorRequest(website = null)

        assertNull(request)
    }

    @Test
    fun `toUsernameGeneratorRequest for ForwardEmail returns null when domainName is blank`() {
        val forwardMailServiceType = ServiceType.ForwardEmail(
            apiKey = "apiKey",
            domainName = "",
        )
        val request = forwardMailServiceType.toUsernameGeneratorRequest(website = null)

        assertNull(request)
    }

    @Test
    fun `toUsernameGeneratorRequest for ForwardEmail returns correct request`() {
        val forwardEmailServiceType = ServiceType.ForwardEmail(
            apiKey = "apiKey",
            domainName = "domainName",
        )
        val website = "bitwarden.com"
        val request = forwardEmailServiceType.toUsernameGeneratorRequest(website)

        assertEquals(
            UsernameGeneratorRequest.Forwarded(
                service = ForwarderServiceType.ForwardEmail(
                    apiToken = "apiKey",
                    domain = "domainName",
                ),
                website = website,
            ),
            request,
        )
    }

    @Test
    fun `toUsernameGeneratorRequest for SimpleLogin returns null when apiKey is blank`() {
        val simpleLoginServiceType = ServiceType.SimpleLogin(apiKey = "")
        val request = simpleLoginServiceType.toUsernameGeneratorRequest(website = null)

        assertNull(request)
    }

    @Test
    fun `toUsernameGeneratorRequest for SimpleLogin returns correct request`() {
        val simpleLoginServiceType = ServiceType.SimpleLogin(apiKey = "testKey")
        val website = "bitwarden.com"
        val request = simpleLoginServiceType.toUsernameGeneratorRequest(website)

        assertEquals(
            UsernameGeneratorRequest.Forwarded(
                service = ForwarderServiceType.SimpleLogin(apiKey = "testKey"),
                website = website,
            ),
            request,
        )
    }
}
