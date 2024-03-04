package com.x8bit.bitwarden.ui.tools.feature.generator.util

import com.bitwarden.generators.ForwarderServiceType
import com.bitwarden.generators.UsernameGeneratorRequest
import com.x8bit.bitwarden.ui.platform.base.util.orNullIfBlank
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType

/**
 * Converts a [ServiceType] to a [UsernameGeneratorRequest.Forwarded].
 */
@Suppress("ReturnCount", "LongMethod")
fun ServiceType.toUsernameGeneratorRequest(): UsernameGeneratorRequest.Forwarded? {
    return when (this) {
        is ServiceType.AddyIo -> {
            val accessToken = this.apiAccessToken.orNullIfBlank() ?: return null
            val domain = this.domainName.orNullIfBlank() ?: return null
            UsernameGeneratorRequest.Forwarded(
                service = ForwarderServiceType.AddyIo(
                    apiToken = accessToken,
                    domain = domain,
                    baseUrl = this.baseUrl,
                ),
                website = null,
            )
        }

        is ServiceType.DuckDuckGo -> {
            this
                .apiKey
                .orNullIfBlank()
                ?.let {
                    UsernameGeneratorRequest.Forwarded(
                        service = ForwarderServiceType.DuckDuckGo(token = it),
                        website = null,
                    )
                }
        }

        is ServiceType.FirefoxRelay -> {
            this
                .apiAccessToken
                .orNullIfBlank()
                ?.let {
                    UsernameGeneratorRequest.Forwarded(
                        service = ForwarderServiceType.Firefox(apiToken = it),
                        website = null,
                    )
                }
        }

        is ServiceType.FastMail -> {
            this
                .apiKey
                .orNullIfBlank()
                ?.let {
                    UsernameGeneratorRequest.Forwarded(
                        service = ForwarderServiceType.Fastmail(apiToken = it),
                        website = null,
                    )
                }
        }

        is ServiceType.ForwardEmail -> {
            val apiKey = this.apiKey.orNullIfBlank() ?: return null
            val domainName = this.domainName.orNullIfBlank() ?: return null
            UsernameGeneratorRequest.Forwarded(
                service = ForwarderServiceType.ForwardEmail(apiKey, domainName),
                website = null,
            )
        }

        is ServiceType.SimpleLogin -> {
            this
                .apiKey
                .orNullIfBlank()
                ?.let {
                    UsernameGeneratorRequest.Forwarded(
                        service = ForwarderServiceType.SimpleLogin(apiKey = it),
                        website = null,
                    )
                }
        }
    }
}
