package com.x8bit.bitwarden.data.platform.repository.util

import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import java.net.URI

/**
 * Returns the appropriate pre-defined labels for environments matching the known US/EU values.
 * Otherwise returns the host of the custom base URL.
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
                .create(this.base)
                .host
                .orEmpty()
        }
    }

/**
 * Converts a raw [EnvironmentUrlDataJson] to an externally-consumable [Environment].
 */
fun EnvironmentUrlDataJson.toEnvironmentUrls(): Environment =
    when (this) {
        Environment.Us.environmentUrlData -> Environment.Us
        Environment.Eu.environmentUrlData -> Environment.Eu
        else -> Environment.SelfHosted(environmentUrlData = this)
    }

/**
 * Converts a nullable [EnvironmentUrlDataJson] to an [Environment], where `null` values default to
 * the US environment.
 */
fun EnvironmentUrlDataJson?.toEnvironmentUrlsOrDefault(): Environment =
    this?.toEnvironmentUrls() ?: Environment.Us
