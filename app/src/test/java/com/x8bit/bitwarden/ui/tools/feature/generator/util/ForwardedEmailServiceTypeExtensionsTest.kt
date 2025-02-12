package com.x8bit.bitwarden.ui.tools.feature.generator.util

import com.x8bit.bitwarden.data.tools.generator.repository.model.UsernameGenerationOptions
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

class ForwardedEmailServiceTypeExtensionsTest {

    @Test
    fun `toServiceType should map to correct service type`() {
        val options = UsernameGenerationOptions(
            type = UsernameGenerationOptions.UsernameType.RANDOM_WORD,
            serviceType = UsernameGenerationOptions.ForwardedEmailServiceType.NONE,
            capitalizeRandomWordUsername = true,
            includeNumberRandomWordUsername = false,
            plusAddressedEmail = "example+plus@gmail.com",
            catchAllEmailDomain = "example.com",
            firefoxRelayApiAccessToken = "access_token_firefox_relay",
            simpleLoginApiKey = "api_key_simple_login",
            duckDuckGoApiKey = "api_key_duck_duck_go",
            fastMailApiKey = "api_key_fast_mail",
            anonAddyApiAccessToken = "access_token_anon_addy",
            anonAddyDomainName = "anonaddy.com",
            forwardEmailApiAccessToken = "access_token_forward_email",
            forwardEmailDomainName = "forwardemail.net",
            emailWebsite = "email.example.com",
        )
        UsernameGenerationOptions.ForwardedEmailServiceType.entries
            .forEach {
                val expected = createMockForwardedEmailAliasServiceType(it)
                assertEquals(
                    expected,
                    it.toServiceType(options),
                )
            }
    }

    @Suppress("MaxLineLength")
    private fun createMockForwardedEmailAliasServiceType(
        serviceTypeOption: UsernameGenerationOptions.ForwardedEmailServiceType,
    ): ServiceType? = when (serviceTypeOption) {
        UsernameGenerationOptions.ForwardedEmailServiceType.NONE -> null

        UsernameGenerationOptions.ForwardedEmailServiceType.ANON_ADDY -> {
            ServiceType.AddyIo(
                apiAccessToken = "access_token_anon_addy",
                domainName = "anonaddy.com",
            )
        }

        UsernameGenerationOptions.ForwardedEmailServiceType.FIREFOX_RELAY -> {
            ServiceType.FirefoxRelay(apiAccessToken = "access_token_firefox_relay")
        }

        UsernameGenerationOptions.ForwardedEmailServiceType.SIMPLE_LOGIN -> {
            ServiceType.SimpleLogin(apiKey = "api_key_simple_login")
        }

        UsernameGenerationOptions.ForwardedEmailServiceType.DUCK_DUCK_GO -> {
            ServiceType.DuckDuckGo(apiKey = "api_key_duck_duck_go")
        }

        UsernameGenerationOptions.ForwardedEmailServiceType.FASTMAIL -> {
            ServiceType.FastMail(apiKey = "api_key_fast_mail")
        }

        UsernameGenerationOptions.ForwardedEmailServiceType.FORWARD_EMAIL -> {
            ServiceType.ForwardEmail(
                apiKey = "access_token_forward_email",
                domainName = "forwardemail.net",
            )
        }
    }
}
