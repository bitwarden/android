package com.bitwarden.authenticator.data.platform.repository.util

import com.bitwarden.authenticator.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.authenticator.data.platform.repository.model.Environment
import java.net.URI

private const val DEFAULT_API_URL: String = "https://api.bitwarden.com"
private const val DEFAULT_WEB_VAULT_URL: String = "https://vault.bitwarden.com"
private const val DEFAULT_WEB_SEND_URL: String = "https://send.bitwarden.com/#"
private const val DEFAULT_ICON_URL: String = "https://icons.bitwarden.net"

/**
 * Returns the base api URL or the default value if one is not present.
 */
val EnvironmentUrlDataJson.baseApiUrl: String
    get() = this.base.sanitizeUrl?.let { "$it/api" }
        ?: this.api.sanitizeUrl
        ?: DEFAULT_API_URL

/**
 * Returns the base web vault URL. This will check for a custom [EnvironmentUrlDataJson.webVault]
 * before falling back to the [EnvironmentUrlDataJson.base]. This can still return null if both are
 * null or blank.
 */
val EnvironmentUrlDataJson.baseWebVaultUrlOrNull: String?
    get() = this.webVault.sanitizeUrl
        ?: this.base.sanitizeUrl

/**
 * Returns the base web vault URL or the default value if one is not present.
 *
 * See [baseWebVaultUrlOrNull] for more details.
 */
val EnvironmentUrlDataJson.baseWebVaultUrlOrDefault: String
    get() = this.baseWebVaultUrlOrNull ?: DEFAULT_WEB_VAULT_URL

/**
 * Returns the base web send URL or the default value if one is not present.
 */
val EnvironmentUrlDataJson.baseWebSendUrl: String
    get() =
        this
            .baseWebVaultUrlOrNull
            ?.let { "$it/#/send/" }
            ?: DEFAULT_WEB_SEND_URL

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
    get() = this.icon.sanitizeUrl
        ?: this.base.sanitizeUrl?.let { "$it/icons" }
        ?: DEFAULT_ICON_URL

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
            -> Environment.Us

        EnvironmentUrlDataJson.DEFAULT_EU,
            -> Environment.Eu

        else -> Environment.SelfHosted(environmentUrlData = this)
    }

/**
 * Converts a nullable [EnvironmentUrlDataJson] to an [Environment], where `null` values default to
 * the US environment.
 */
fun EnvironmentUrlDataJson?.toEnvironmentUrlsOrDefault(): Environment =
    this?.toEnvironmentUrls() ?: Environment.Us
