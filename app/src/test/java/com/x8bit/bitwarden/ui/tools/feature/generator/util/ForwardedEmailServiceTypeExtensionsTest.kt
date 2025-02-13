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
            simpleLoginSelfHostServerUrl = "https://simplelogin.local",
            duckDuckGoApiKey = "api_key_duck_duck_go",
            fastMailApiKey = "api_key_fast_mail",
            anonAddyApiAccessToken = "access_token_anon_addy",
            anonAddyDomainName = "anonaddy.com",
            anonAddySelfHostServerUrl = "https://anonaddy.local",
            forwardEmailApiAccessToken = "access_token_forward_email",
            forwardEmailDomainName = "forwardemail.net",
            emailWebsite = "email.example.com",
        )
        UsernameGenerationOptions.ForwardedEmailServiceType.entries
            .forEach {
                val expected = createMockForwardedEmailAliasServiceType(
                    serviceTypeOption = it,
                    useEmptyValues = false,
                )
                assertEquals(
                    expected,
                    it.toServiceType(options),
                )
            }
    }

    @Test
    fun `toServiceType should map to correct service type with empty values`() {
        val options = UsernameGenerationOptions(
            type = UsernameGenerationOptions.UsernameType.RANDOM_WORD,
            serviceType = UsernameGenerationOptions.ForwardedEmailServiceType.NONE,
            capitalizeRandomWordUsername = true,
            includeNumberRandomWordUsername = false,
            plusAddressedEmail = null,
            catchAllEmailDomain = null,
            firefoxRelayApiAccessToken = null,
            simpleLoginApiKey = null,
            simpleLoginSelfHostServerUrl = null,
            duckDuckGoApiKey = null,
            fastMailApiKey = null,
            anonAddyApiAccessToken = null,
            anonAddyDomainName = null,
            anonAddySelfHostServerUrl = null,
            forwardEmailApiAccessToken = null,
            forwardEmailDomainName = null,
            emailWebsite = null,
        )
        UsernameGenerationOptions.ForwardedEmailServiceType.entries
            .forEach {
                assertEquals(
                    createMockForwardedEmailAliasServiceType(
                        serviceTypeOption = it,
                        useEmptyValues = true,
                    ),
                    it.toServiceType(options),
                )
            }
    }

    private fun createMockForwardedEmailAliasServiceType(
        serviceTypeOption: UsernameGenerationOptions.ForwardedEmailServiceType,
        useEmptyValues: Boolean = false,
    ): ServiceType? = when (serviceTypeOption) {
        UsernameGenerationOptions.ForwardedEmailServiceType.NONE -> null

        UsernameGenerationOptions.ForwardedEmailServiceType.ANON_ADDY -> {
            ServiceType.AddyIo(
                apiAccessToken = "access_token_anon_addy".takeUnless { useEmptyValues }.orEmpty(),
                domainName = "anonaddy.com".takeUnless { useEmptyValues }.orEmpty(),
                selfHostServerUrl = "https://anonaddy.local"
                    .takeUnless { useEmptyValues }
                    .orEmpty(),
            )
        }

        UsernameGenerationOptions.ForwardedEmailServiceType.FIREFOX_RELAY -> {
            ServiceType.FirefoxRelay(
                apiAccessToken = "access_token_firefox_relay"
                    .takeUnless { useEmptyValues }
                    .orEmpty(),
            )
        }

        UsernameGenerationOptions.ForwardedEmailServiceType.SIMPLE_LOGIN -> {
            ServiceType.SimpleLogin(
                apiKey = "api_key_simple_login"
                    .takeUnless { useEmptyValues }
                    .orEmpty(),
                selfHostServerUrl = "https://simplelogin.local"
                    .takeUnless { useEmptyValues }
                    .orEmpty(),
            )
        }

        UsernameGenerationOptions.ForwardedEmailServiceType.DUCK_DUCK_GO -> {
            ServiceType.DuckDuckGo(
                apiKey = "api_key_duck_duck_go"
                    .takeUnless { useEmptyValues }
                    .orEmpty(),
            )
        }

        UsernameGenerationOptions.ForwardedEmailServiceType.FASTMAIL -> {
            ServiceType.FastMail(
                apiKey = "api_key_fast_mail"
                    .takeUnless { useEmptyValues }
                    .orEmpty(),
            )
        }

        UsernameGenerationOptions.ForwardedEmailServiceType.FORWARD_EMAIL -> {
            ServiceType.ForwardEmail(
                apiKey = "access_token_forward_email".takeUnless { useEmptyValues }.orEmpty(),
                domainName = "forwardemail.net".takeUnless { useEmptyValues }.orEmpty(),
            )
        }
    }
}
