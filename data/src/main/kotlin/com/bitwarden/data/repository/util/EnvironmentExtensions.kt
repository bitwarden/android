package com.bitwarden.data.repository.util

import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.data.repository.model.Environment
import java.net.URI

private const val DEFAULT_US_API_URL: String = "https://api.bitwarden.com"
private const val DEFAULT_EU_API_URL: String = "https://api.bitwarden.eu"
private const val DEFAULT_FED_RAMP_API_URL: String = "https://api.bitwarden-gov.com"
private const val DEFAULT_US_EVENTS_URL: String = "https://events.bitwarden.com"
private const val DEFAULT_EU_EVENTS_URL: String = "https://events.bitwarden.eu"
private const val DEFAULT_FED_RAMP_EVENTS_URL: String = "https://events.bitwarden-gov.com"
private const val DEFAULT_US_IDENTITY_URL: String = "https://identity.bitwarden.com"
private const val DEFAULT_EU_IDENTITY_URL: String = "https://identity.bitwarden.eu"
private const val DEFAULT_FED_RAMP_IDENTITY_URL: String = "https://identity.bitwarden-gov.com"
private const val DEFAULT_US_WEB_VAULT_URL: String = "https://vault.bitwarden.com"
private const val DEFAULT_EU_WEB_VAULT_URL: String = "https://vault.bitwarden.eu"
private const val DEFAULT_FED_RAMP_WEB_VAULT_URL: String = "https://vault.bitwarden-gov.com"
private const val DEFAULT_US_WEB_SEND_URL: String = "https://send.bitwarden.com/#"
private const val DEFAULT_FED_RAMP_WEB_SEND_URL: String = "https://send.bitwarden-gov.com/#"
private const val DEFAULT_US_ICON_URL: String = "https://icons.bitwarden.net"
private const val DEFAULT_EU_ICON_URL: String = "https://icons.bitwarden.eu"
private const val DEFAULT_FED_RAMP_ICON_URL: String = "https://icons.bitwarden-gov.com"

/**
 * Returns the base api URL or the default value if one is not present.
 */
val Environment.baseApiUrl: String
    get() = when (this) {
        is Environment.Prod.Us -> DEFAULT_US_API_URL
        is Environment.Prod.Eu -> DEFAULT_EU_API_URL
        is Environment.Prod.FedRamp -> DEFAULT_FED_RAMP_API_URL
        is Environment.SelfHosted -> {
            this.environmentUrlData.api.sanitizeUrl
                ?: this.environmentUrlData.base.sanitizeUrl?.let { "$it/api" }
                ?: DEFAULT_US_API_URL
        }
    }

/**
 * Returns the base events URL or the default value if one is not present.
 */
val Environment.baseEventsUrl: String
    get() = when (this) {
        is Environment.Prod.Us -> DEFAULT_US_EVENTS_URL
        is Environment.Prod.Eu -> DEFAULT_EU_EVENTS_URL
        is Environment.Prod.FedRamp -> DEFAULT_FED_RAMP_EVENTS_URL
        is Environment.SelfHosted -> {
            this.environmentUrlData.events.sanitizeUrl
                ?: this.environmentUrlData.base.sanitizeUrl?.let { "$it/events" }
                ?: DEFAULT_US_EVENTS_URL
        }
    }

/**
 * Returns the base identity URL or the default value if one is not present.
 */
val Environment.baseIdentityUrl: String
    get() = when (this) {
        is Environment.Prod.Us -> DEFAULT_US_IDENTITY_URL
        is Environment.Prod.Eu -> DEFAULT_EU_IDENTITY_URL
        is Environment.Prod.FedRamp -> DEFAULT_FED_RAMP_IDENTITY_URL
        is Environment.SelfHosted -> {
            this.environmentUrlData.identity.sanitizeUrl
                ?: this.environmentUrlData.base.sanitizeUrl?.let { "$it/identity" }
                ?: DEFAULT_US_IDENTITY_URL
        }
    }

/**
 * Returns the base web vault URL. This will check for a custom [EnvironmentUrlDataJson.webVault]
 * before falling back to the [EnvironmentUrlDataJson.base]. This can still return null if both are
 * null or blank.
 */
val Environment.baseWebVaultUrlOrNull: String?
    get() = when (this) {
        is Environment.Prod.Us -> DEFAULT_US_WEB_VAULT_URL
        is Environment.Prod.Eu -> DEFAULT_EU_WEB_VAULT_URL
        is Environment.Prod.FedRamp -> DEFAULT_FED_RAMP_WEB_VAULT_URL
        is Environment.SelfHosted -> {
            this.environmentUrlData.webVault.sanitizeUrl ?: this.environmentUrlData.base.sanitizeUrl
        }
    }

/**
 * Returns the base web vault URL or the default value if one is not present.
 *
 * See [baseWebVaultUrlOrNull] for more details.
 */
val Environment.baseWebVaultUrlOrDefault: String
    get() = this.baseWebVaultUrlOrNull ?: DEFAULT_US_WEB_VAULT_URL

/**
 * Returns the base web send URL or the default value if one is not present.
 */
val Environment.baseWebSendUrl: String
    get() = when (this) {
        is Environment.Prod.Us -> DEFAULT_US_WEB_SEND_URL
        is Environment.Prod.FedRamp -> DEFAULT_FED_RAMP_WEB_SEND_URL
        is Environment.Prod.Eu,
        is Environment.SelfHosted,
            -> this.baseWebVaultUrlOrNull?.let { "$it/#/send/" } ?: DEFAULT_US_WEB_SEND_URL
    }

/**
 * Returns the base web vault import URL or the default value if one is not present.
 */
val Environment.toBaseWebVaultImportUrl: String
    get() = this
        .baseWebVaultUrlOrDefault
        .let { "$it/#/tools/import" }

/**
 * Returns a base icon url based on the environment or the default value if values are missing.
 */
val Environment.baseIconUrl: String
    get() = when (this) {
        is Environment.Prod.Us -> DEFAULT_US_ICON_URL
        is Environment.Prod.Eu -> DEFAULT_EU_ICON_URL
        is Environment.Prod.FedRamp -> DEFAULT_FED_RAMP_ICON_URL
        is Environment.SelfHosted -> {
            this.environmentUrlData.icon.sanitizeUrl
                ?: this.environmentUrlData.base.sanitizeUrl?.let { "$it/icons" }
                ?: DEFAULT_US_ICON_URL
        }
    }

/**
 * Returns the appropriate pre-defined labels for environments matching the known US/EU values.
 * Otherwise, returns the host of the custom base URL.
 *
 * @see selfHostedUrlOrNull
 */
val Environment.labelOrBaseUrlHost: String
    get() = when (this) {
        is Environment.Prod -> this.label
        is Environment.SelfHosted -> {
            // Grab the domain
            // Ex: "https://www.abc.com/path-1/path-1" -> "www.abc.com"
            URI
                .create(this.environmentUrlData.selfHostedUrlOrNull.orEmpty())
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
private val EnvironmentUrlDataJson.selfHostedUrlOrNull: String?
    get() = this.webVault.sanitizeUrl
        ?: this.base.sanitizeUrl
        ?: this.api.sanitizeUrl
        ?: this.identity.sanitizeUrl

/**
 * A helper method to filter out blank urls and remove any trailing forward slashes.
 */
private val String?.sanitizeUrl: String?
    get() = this?.trimEnd('/').takeIf { !it.isNullOrBlank() }
