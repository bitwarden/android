package com.bitwarden.data.repository.util

import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.data.repository.model.Environment

/**
 * Converts a raw [EnvironmentUrlDataJson] to an externally-consumable [Environment].
 */
fun EnvironmentUrlDataJson.toEnvironmentUrls(): Environment =
    when (this) {
        EnvironmentUrlDataJson.DEFAULT_US,
        EnvironmentUrlDataJson.DEFAULT_LEGACY_US,
            -> Environment.Prod.Us

        EnvironmentUrlDataJson.DEFAULT_EU,
        EnvironmentUrlDataJson.DEFAULT_LEGACY_EU,
            -> Environment.Prod.Eu

        EnvironmentUrlDataJson.DEFAULT_FED_RAMP -> Environment.Prod.FedRamp
        else -> Environment.SelfHosted(environmentUrlData = this)
    }

/**
 * Converts a nullable [EnvironmentUrlDataJson] to an [Environment], where `null` values default to
 * the US environment.
 */
fun EnvironmentUrlDataJson?.toEnvironmentUrlsOrDefault(): Environment =
    this?.toEnvironmentUrls() ?: Environment.Prod.Us
