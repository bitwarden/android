package com.bitwarden.data.repository.util

import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.data.repository.model.EnvironmentRegion
import java.net.URI

private const val DEFAULT_US_API_URL: String = "https://api.bitwarden.com"
private const val DEFAULT_EU_API_URL: String = "https://api.bitwarden.eu"
private const val DEFAULT_US_EVENTS_URL: String = "https://events.bitwarden.com"
private const val DEFAULT_EU_EVENTS_URL: String = "https://events.bitwarden.eu"
private const val DEFAULT_US_IDENTITY_URL: String = "https://identity.bitwarden.com"
private const val DEFAULT_EU_IDENTITY_URL: String = "https://identity.bitwarden.eu"
private const val DEFAULT_US_WEB_VAULT_URL: String = "https://vault.bitwarden.com"
private const val DEFAULT_EU_WEB_VAULT_URL: String = "https://vault.bitwarden.eu"
private const val DEFAULT_US_WEB_SEND_URL: String = "https://send.bitwarden.com/#"
private const val DEFAULT_US_ICON_URL: String = "https://icons.bitwarden.net"
private const val DEFAULT_EU_ICON_URL: String = "https://icons.bitwarden.eu"

/**
 * Returns the base api URL or the default value if one is not present.
 */
val EnvironmentUrlDataJson.baseApiUrl: String
    get() = when (this.environmentRegion) {
        EnvironmentRegion.UNITED_STATES -> DEFAULT_US_API_URL
        EnvironmentRegion.EUROPEAN_UNION -> DEFAULT_EU_API_URL
        EnvironmentRegion.SELF_HOSTED -> {
            this.api.sanitizeUrl
                ?: this.base.sanitizeUrl?.let { "$it/api" }
                ?: DEFAULT_US_API_URL
        }
    }

/**
 * Returns the base events URL or the default value if one is not present.
 */
val EnvironmentUrlDataJson.baseEventsUrl: String
    get() = when (this.environmentRegion) {
        EnvironmentRegion.UNITED_STATES -> DEFAULT_US_EVENTS_URL
        EnvironmentRegion.EUROPEAN_UNION -> DEFAULT_EU_EVENTS_URL
        EnvironmentRegion.SELF_HOSTED -> {
            this.events.sanitizeUrl
                ?: this.base.sanitizeUrl?.let { "$it/events" }
                ?: DEFAULT_US_EVENTS_URL
        }
    }

/**
 * Returns the base identity URL or the default value if one is not present.
 */
val EnvironmentUrlDataJson.baseIdentityUrl: String
    get() = when (this.environmentRegion) {
        EnvironmentRegion.UNITED_STATES -> DEFAULT_US_IDENTITY_URL
        EnvironmentRegion.EUROPEAN_UNION -> DEFAULT_EU_IDENTITY_URL
        EnvironmentRegion.SELF_HOSTED -> {
            this.identity.sanitizeUrl
                ?: this.base.sanitizeUrl?.let { "$it/identity" }
                ?: DEFAULT_US_IDENTITY_URL
        }
    }

/**
 * Returns the base web vault URL. This will check for a custom [EnvironmentUrlDataJson.webVault]
 * before falling back to the [EnvironmentUrlDataJson.base]. This can still return null if both are
 * null or blank.
 */
val EnvironmentUrlDataJson.baseWebVaultUrlOrNull: String?
    get() = when (this.environmentRegion) {
        EnvironmentRegion.UNITED_STATES -> DEFAULT_US_WEB_VAULT_URL
        EnvironmentRegion.EUROPEAN_UNION -> DEFAULT_EU_WEB_VAULT_URL
        EnvironmentRegion.SELF_HOSTED -> this.webVault.sanitizeUrl ?: this.base.sanitizeUrl
    }

/**
 * Returns the base web vault URL or the default value if one is not present.
 *
 * See [baseWebVaultUrlOrNull] for more details.
 */
val EnvironmentUrlDataJson.baseWebVaultUrlOrDefault: String
    get() = this.baseWebVaultUrlOrNull ?: DEFAULT_US_WEB_VAULT_URL

/**
 * Returns the base web send URL or the default value if one is not present.
 */
val EnvironmentUrlDataJson.baseWebSendUrl: String
    get() = when (this.environmentRegion) {
        EnvironmentRegion.UNITED_STATES -> DEFAULT_US_WEB_SEND_URL
        EnvironmentRegion.EUROPEAN_UNION,
        EnvironmentRegion.SELF_HOSTED,
            -> this.baseWebVaultUrlOrNull?.let { "$it/#/send/" } ?: DEFAULT_US_WEB_SEND_URL
    }

/**
 * Returns the base web vault import URL or the default value if one is not present.
 */
val EnvironmentUrlDataJson.toBaseWebVaultImportUrl: String
    get() =
        this
            .baseWebVaultUrlOrDefault
            .let { "$it/#/tools/import" }

/**
 * Returns a base icon url based on the environment or the default value if values are missing.
 */
val EnvironmentUrlDataJson.baseIconUrl: String
    get() = when (this.environmentRegion) {
        EnvironmentRegion.UNITED_STATES -> DEFAULT_US_ICON_URL
        EnvironmentRegion.EUROPEAN_UNION -> DEFAULT_EU_ICON_URL
        EnvironmentRegion.SELF_HOSTED -> {
            this.icon.sanitizeUrl
                ?: this.base.sanitizeUrl?.let { "$it/icons" }
                ?: DEFAULT_US_ICON_URL
        }
    }

/**
 * Returns the appropriate pre-defined labels for environments matching the known US/EU values.
 * Otherwise returns the host of the custom base URL.
 *
 * @see getSelfHostedUrlOrNull
 */
val EnvironmentUrlDataJson.labelOrBaseUrlHost: String
    get() = when (this) {
        EnvironmentUrlDataJson.DEFAULT_US -> Environment.Us.label
        EnvironmentUrlDataJson.DEFAULT_EU -> Environment.Eu.label
        else -> {
            // Grab the domain
            // Ex:
            // - "https://www.abc.com/path-1/path-1" -> "www.abc.com"
            URI
                .create(getSelfHostedUrlOrNull().orEmpty())
                .host
                .orEmpty()
        }
    }

/**
 * Returns the first self-hosted environment URL from
 * [EnvironmentUrlDataJson.webVault], [EnvironmentUrlDataJson.base],
 * [EnvironmentUrlDataJson.api], and finally [EnvironmentUrlDataJson.identity]. Returns `null` if
 * all self-host environment URLs are null.
 */
private fun EnvironmentUrlDataJson.getSelfHostedUrlOrNull(): String? =
    this.webVault.sanitizeUrl
        ?: this.base.sanitizeUrl
        ?: this.api.sanitizeUrl
        ?: this.identity.sanitizeUrl

/**
 * A helper method to filter out blank urls and remove any trailing forward slashes.
 */
private val String?.sanitizeUrl: String?
    get() = this?.trimEnd('/').takeIf { !it.isNullOrBlank() }

/**
 * Converts a raw [EnvironmentUrlDataJson] to an externally-consumable [Environment].
 */
fun EnvironmentUrlDataJson.toEnvironmentUrls(): Environment =
    when (this) {
        EnvironmentUrlDataJson.DEFAULT_US,
        EnvironmentUrlDataJson.DEFAULT_LEGACY_US,
            -> Environment.Us

        EnvironmentUrlDataJson.DEFAULT_EU,
        EnvironmentUrlDataJson.DEFAULT_LEGACY_EU,
            -> Environment.Eu

        else -> Environment.SelfHosted(environmentUrlData = this)
    }

/**
 * Converts a nullable [EnvironmentUrlDataJson] to an [Environment], where `null` values default to
 * the US environment.
 */
fun EnvironmentUrlDataJson?.toEnvironmentUrlsOrDefault(): Environment =
    this?.toEnvironmentUrls() ?: Environment.Us
