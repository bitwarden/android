package com.x8bit.bitwarden.ui.tools.feature.generator.util

import com.bitwarden.generators.ForwarderServiceType
import com.bitwarden.generators.UsernameGeneratorRequest
import com.bitwarden.ui.platform.base.util.orNullIfBlank
import com.bitwarden.ui.platform.base.util.prefixHttpsIfNecessary
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias.ServiceType

/**
 * Converts a [ServiceType] to a [UsernameGeneratorRequest.Forwarded].
 */
@Suppress("LongMethod")
fun ServiceType.toUsernameGeneratorRequest(
    website: String?,
): GeneratorRequestResult {
    return when (this) {
        is ServiceType.AddyIo -> {
            val accessToken = this.apiAccessToken.orNullIfBlank()
                ?: return GeneratorRequestResult.MissingField(
                    BitwardenString.api_access_token.asText(),
                )
            val domain = this.domainName.orNullIfBlank()
                ?: return GeneratorRequestResult.MissingField(BitwardenString.domain_name.asText())
            val baseUrl = if (selfHostServerUrl.isNotBlank()) {
                selfHostServerUrl.prefixHttpsIfNecessary()
            } else {
                ServiceType.AddyIo.DEFAULT_ADDY_IO_URL
            }
            GeneratorRequestResult.Success(
                UsernameGeneratorRequest.Forwarded(
                    service = ForwarderServiceType.AddyIo(
                        apiToken = accessToken,
                        domain = domain,
                        baseUrl = baseUrl,
                    ),
                    website = website,
                ),
            )
        }

        is ServiceType.DuckDuckGo -> {
            this
                .apiKey
                .orNullIfBlank()
                ?.let {
                    GeneratorRequestResult.Success(
                        UsernameGeneratorRequest.Forwarded(
                            service = ForwarderServiceType.DuckDuckGo(token = it),
                            website = website,
                        ),
                    )
                }
                ?: GeneratorRequestResult.MissingField(BitwardenString.api_key.asText())
        }

        is ServiceType.FirefoxRelay -> {
            this
                .apiAccessToken
                .orNullIfBlank()
                ?.let {
                    GeneratorRequestResult.Success(
                        UsernameGeneratorRequest.Forwarded(
                            service = ForwarderServiceType.Firefox(apiToken = it),
                            website = website,
                        ),
                    )
                }
                ?: GeneratorRequestResult.MissingField(BitwardenString.api_access_token.asText())
        }

        is ServiceType.FastMail -> {
            this
                .apiKey
                .orNullIfBlank()
                ?.let {
                    GeneratorRequestResult.Success(
                        UsernameGeneratorRequest.Forwarded(
                            service = ForwarderServiceType.Fastmail(apiToken = it),
                            // A null `website` value here will cause an error.
                            website = website.orEmpty(),
                        ),
                    )
                }
                ?: GeneratorRequestResult.MissingField(BitwardenString.api_key.asText())
        }

        is ServiceType.ForwardEmail -> {
            val apiKey = this.apiKey.orNullIfBlank()
                ?: return GeneratorRequestResult.MissingField(BitwardenString.api_key.asText())
            val domainName = this.domainName.orNullIfBlank()
                ?: return GeneratorRequestResult.MissingField(BitwardenString.domain_name.asText())
            GeneratorRequestResult.Success(
                UsernameGeneratorRequest.Forwarded(
                    service = ForwarderServiceType.ForwardEmail(apiKey, domainName),
                    website = website,
                ),
            )
        }

        is ServiceType.SimpleLogin -> {
            val baseUrl = if (selfHostServerUrl.isNotBlank()) {
                selfHostServerUrl.prefixHttpsIfNecessary()
            } else {
                ServiceType.SimpleLogin.DEFAULT_SIMPLE_LOGIN_URL
            }
            this
                .apiKey
                .orNullIfBlank()
                ?.let {
                    GeneratorRequestResult.Success(
                        UsernameGeneratorRequest.Forwarded(
                            service = ForwarderServiceType.SimpleLogin(
                                apiKey = it,
                                baseUrl = baseUrl,
                            ),
                            website = website,
                        ),
                    )
                }
                ?: GeneratorRequestResult.MissingField(BitwardenString.api_key.asText())
        }
    }
}

/**
 * Wrapper that contains a [UsernameGeneratorRequest.Forwarded] on success of indicates what data
 * is still required to create a request.
 */
sealed class GeneratorRequestResult {
    /**
     * A request has been successfully created.
     */
    data class Success(
        val result: UsernameGeneratorRequest.Forwarded,
    ) : GeneratorRequestResult()

    /**
     * The request failed to be generated do to a missing value.
     */
    data class MissingField(
        val fieldName: Text,
    ) : GeneratorRequestResult()
}
