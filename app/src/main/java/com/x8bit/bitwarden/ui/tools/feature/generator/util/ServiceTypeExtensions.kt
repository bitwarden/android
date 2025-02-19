package com.x8bit.bitwarden.ui.tools.feature.generator.util

import com.bitwarden.generators.ForwarderServiceType
import com.bitwarden.generators.UsernameGeneratorRequest
import com.x8bit.bitwarden.ui.platform.base.util.orNullIfBlank
import com.x8bit.bitwarden.ui.platform.base.util.prefixHttpsIfNecessary
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType

/**
 * Converts a [ServiceType] to a [UsernameGeneratorRequest.Forwarded].
 */
@Suppress("LongMethod")
fun ServiceType.toUsernameGeneratorRequest(
    website: String?,
    allowAddyIoSelfHostUrl: Boolean,
    allowSimpleLoginSelfHostUrl: Boolean,
): UsernameGeneratorRequest.Forwarded? {
    return when (this) {
        is ServiceType.AddyIo -> {
            val accessToken = this.apiAccessToken.orNullIfBlank() ?: return null
            val domain = this.domainName.orNullIfBlank() ?: return null
            val baseUrl = if (allowAddyIoSelfHostUrl && selfHostServerUrl.isNotBlank()) {
                selfHostServerUrl.prefixHttpsIfNecessary()
            } else {
                ServiceType.AddyIo.DEFAULT_ADDY_IO_URL
            }
            UsernameGeneratorRequest.Forwarded(
                service = ForwarderServiceType.AddyIo(
                    apiToken = accessToken,
                    domain = domain,
                    baseUrl = baseUrl,
                ),
                website = website,
            )
        }

        is ServiceType.DuckDuckGo -> {
            this
                .apiKey
                .orNullIfBlank()
                ?.let {
                    UsernameGeneratorRequest.Forwarded(
                        service = ForwarderServiceType.DuckDuckGo(token = it),
                        website = website,
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
                        website = website,
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
                        // A null `website` value here will cause an error.
                        website = website.orEmpty(),
                    )
                }
        }

        is ServiceType.ForwardEmail -> {
            val apiKey = this.apiKey.orNullIfBlank() ?: return null
            val domainName = this.domainName.orNullIfBlank() ?: return null
            UsernameGeneratorRequest.Forwarded(
                service = ForwarderServiceType.ForwardEmail(apiKey, domainName),
                website = website,
            )
        }

        is ServiceType.SimpleLogin -> {
            val baseUrl = if (allowSimpleLoginSelfHostUrl && selfHostServerUrl.isNotBlank()) {
                selfHostServerUrl.prefixHttpsIfNecessary()
            } else {
                ServiceType.SimpleLogin.DEFAULT_SIMPLE_LOGIN_URL
            }
            this
                .apiKey
                .orNullIfBlank()
                ?.let {
                    UsernameGeneratorRequest.Forwarded(
                        service = ForwarderServiceType.SimpleLogin(apiKey = it, baseUrl = baseUrl),
                        website = website,
                    )
                }
        }
    }
}
