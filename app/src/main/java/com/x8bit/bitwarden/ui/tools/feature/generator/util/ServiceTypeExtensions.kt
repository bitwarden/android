package com.x8bit.bitwarden.ui.tools.feature.generator.util

import com.bitwarden.core.ForwarderServiceType
import com.bitwarden.core.UsernameGeneratorRequest
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType

/**
 * Converts a [ServiceType] to a [UsernameGeneratorRequest.Forwarded].
 */
fun ServiceType.toUsernameGeneratorRequest(): UsernameGeneratorRequest.Forwarded {
    return when (this) {
        is ServiceType.AddyIo -> {
            UsernameGeneratorRequest.Forwarded(
                service = ForwarderServiceType.AddyIo(
                    apiToken = this.apiAccessToken,
                    domain = this.domainName,
                    baseUrl = this.baseUrl,
                ),
                website = null,
            )
        }
        is ServiceType.DuckDuckGo -> {
            UsernameGeneratorRequest.Forwarded(
                service = ForwarderServiceType.DuckDuckGo(token = this.apiKey),
                website = null,
            )
        }
        is ServiceType.FirefoxRelay -> {
            UsernameGeneratorRequest.Forwarded(
                service = ForwarderServiceType.Firefox(apiToken = this.apiAccessToken),
                website = null,
            )
        }
        is ServiceType.FastMail -> {
            UsernameGeneratorRequest.Forwarded(
                service = ForwarderServiceType.Fastmail(apiToken = this.apiKey),
                website = null,
            )
        }
        is ServiceType.SimpleLogin -> {
            UsernameGeneratorRequest.Forwarded(
                service = ForwarderServiceType.SimpleLogin(apiKey = this.apiKey),
                website = null,
            )
        }
    }
}
