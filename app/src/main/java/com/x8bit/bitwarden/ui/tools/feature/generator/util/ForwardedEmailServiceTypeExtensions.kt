package com.x8bit.bitwarden.ui.tools.feature.generator.util

import com.x8bit.bitwarden.data.tools.generator.repository.model.UsernameGenerationOptions
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType.AddyIo
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType.DuckDuckGo
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType.FastMail
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType.FirefoxRelay
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType.ForwardEmail
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType.SimpleLogin

/**
 * Converts this [UsernameGenerationOptions.ForwardedEmailServiceType] into a corresponding
 * [ForwardedEmailAlias.ServiceType] with the given [options], or `null`.
 */
fun UsernameGenerationOptions.ForwardedEmailServiceType?.toServiceType(
    options: UsernameGenerationOptions,
): ForwardedEmailAlias.ServiceType? {
    this ?: return null
    return when (this) {
        UsernameGenerationOptions.ForwardedEmailServiceType.FIREFOX_RELAY -> {
            FirefoxRelay(apiAccessToken = options.firefoxRelayApiAccessToken.orEmpty())
        }

        UsernameGenerationOptions.ForwardedEmailServiceType.SIMPLE_LOGIN -> {
            SimpleLogin(apiKey = options.simpleLoginApiKey.orEmpty())
        }

        UsernameGenerationOptions.ForwardedEmailServiceType.DUCK_DUCK_GO -> {
            DuckDuckGo(apiKey = options.duckDuckGoApiKey.orEmpty())
        }

        UsernameGenerationOptions.ForwardedEmailServiceType.FASTMAIL -> {
            FastMail(apiKey = options.fastMailApiKey.orEmpty())
        }

        UsernameGenerationOptions.ForwardedEmailServiceType.ANON_ADDY -> {
            AddyIo(
                apiAccessToken = options.anonAddyApiAccessToken.orEmpty(),
                domainName = options.anonAddyDomainName.orEmpty(),
            )
        }

        UsernameGenerationOptions.ForwardedEmailServiceType.FORWARD_EMAIL -> {
            ForwardEmail(
                apiKey = options.forwardEmailApiAccessToken.orEmpty(),
                domainName = options.forwardEmailDomainName.orEmpty(),
            )
        }

        UsernameGenerationOptions.ForwardedEmailServiceType.NONE -> null
    }
}
