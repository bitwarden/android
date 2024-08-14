package com.x8bit.bitwarden.data.tools.generator.repository.model

import androidx.annotation.Keep
import com.x8bit.bitwarden.data.platform.datasource.network.serializer.BaseEnumeratedIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A data class representing the configuration options for generating usernames.
 *
 * @property type The type of username to be generated, as defined in UsernameType.
 * @property serviceType The type of email forwarding service to be used,
 * as defined in ForwardedEmailServiceType.
 * @property capitalizeRandomWordUsername Indicates whether to capitalize the username.
 * @property includeNumberRandomWordUsername Indicates whether to include a number in the username.
 * @property plusAddressedEmail The email address to be used for plus-addressing.
 * @property catchAllEmailDomain The domain name to be used for catch-all email addresses.
 * @property firefoxRelayApiAccessToken The API access token for Firefox Relay.
 * @property simpleLoginApiKey The API key for SimpleLogin.
 * @property duckDuckGoApiKey The API key for DuckDuckGo.
 * @property fastMailApiKey The API key for FastMail.
 * @property anonAddyApiAccessToken The API access token for AnonAddy.
 * @property anonAddyDomainName The domain name associated with AnonAddy.
 * @property forwardEmailApiAccessToken The API access token for Forward Email.
 * @property forwardEmailDomainName The domain name associated with Forward Email.
 * @property emailWebsite The website associated with the email service.
 */
@Serializable
data class UsernameGenerationOptions(
    @SerialName("type")
    val type: UsernameType,

    @SerialName("serviceType")
    val serviceType: ForwardedEmailServiceType? = null,

    @SerialName("capitalizeRandomWordUsername")
    val capitalizeRandomWordUsername: Boolean? = null,

    @SerialName("includeNumberRandomWordUsername")
    val includeNumberRandomWordUsername: Boolean? = null,

    @SerialName("plusAddressedEmail")
    val plusAddressedEmail: String? = null,

    @SerialName("catchAllEmailDomain")
    val catchAllEmailDomain: String? = null,

    @SerialName("firefoxRelayApiAccessToken")
    val firefoxRelayApiAccessToken: String? = null,

    @SerialName("simpleLoginApiKey")
    val simpleLoginApiKey: String? = null,

    @SerialName("duckDuckGoApiKey")
    val duckDuckGoApiKey: String? = null,

    @SerialName("fastMailApiKey")
    val fastMailApiKey: String? = null,

    @SerialName("anonAddyApiAccessToken")
    val anonAddyApiAccessToken: String? = null,

    @SerialName("anonAddyDomainName")
    val anonAddyDomainName: String? = null,

    @SerialName("forwardEmailApiAccessToken")
    val forwardEmailApiAccessToken: String? = null,

    @SerialName("forwardEmailDomainName")
    val forwardEmailDomainName: String? = null,

    @SerialName("emailWebsite")
    val emailWebsite: String? = null,
) {

    /**
     * Represents different Username Types.
     */
    @Serializable(with = UsernameTypeSerializer::class)
    enum class UsernameType {
        @SerialName("0")
        PLUS_ADDRESSED_EMAIL,

        @SerialName("1")
        CATCH_ALL_EMAIL,

        @SerialName("2")
        FORWARDED_EMAIL_ALIAS,

        @SerialName("3")
        RANDOM_WORD,
    }

    /**
     * Represents different Service Types within the ForwardedEmailAlias Username Type.
     */
    @Serializable(with = ForwardedEmailServiceTypeSerializer::class)
    enum class ForwardedEmailServiceType {
        @SerialName("-1")
        NONE,

        @SerialName("0")
        ANON_ADDY,

        @SerialName("1")
        FIREFOX_RELAY,

        @SerialName("2")
        SIMPLE_LOGIN,

        @SerialName("3")
        DUCK_DUCK_GO,

        @SerialName("4")
        FASTMAIL,

        @SerialName("5")
        FORWARD_EMAIL,
    }
}

@Keep
private class UsernameTypeSerializer :
    BaseEnumeratedIntSerializer<UsernameGenerationOptions.UsernameType>(
        UsernameGenerationOptions.UsernameType.entries.toTypedArray(),
    )

@Keep
private class ForwardedEmailServiceTypeSerializer :
    BaseEnumeratedIntSerializer<UsernameGenerationOptions.ForwardedEmailServiceType>(
        UsernameGenerationOptions.ForwardedEmailServiceType.entries.toTypedArray(),
    )
