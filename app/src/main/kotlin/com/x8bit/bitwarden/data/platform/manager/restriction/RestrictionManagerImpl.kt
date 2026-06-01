package com.x8bit.bitwarden.data.platform.manager.restriction

import android.content.RestrictionsManager
import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.data.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository

/**
 * The default implementation of the [RestrictionManager].
 */
class RestrictionManagerImpl(
    private val environmentRepository: EnvironmentRepository,
    private val restrictionsManager: RestrictionsManager,
) : RestrictionManager {

    override fun initialize() {
        updatePreconfiguredRestrictionSettings()
    }

    private fun updatePreconfiguredRestrictionSettings() {
        restrictionsManager
            .applicationRestrictions
            ?.takeUnless { it.isEmpty }
            ?.getString(BASE_ENVIRONMENT_URL_RESTRICTION_KEY)
            ?.let { url -> setPreconfiguredUrl(baseEnvironmentUrl = url) }
    }

    private fun setPreconfiguredUrl(baseEnvironmentUrl: String) {
        environmentRepository.environment = when (baseEnvironmentUrl) {
            // If the baseEnvironmentUrl matches the predefined US environment, assume it is the
            // default US environment.
            Environment.Us.environmentUrlData.base -> Environment.Us
            // If the baseEnvironmentUrl matches the predefined EU environment, assume it is the
            // default EU environment.
            Environment.Eu.environmentUrlData.base -> Environment.Eu
            // Otherwise make a custom self-host environment.
            else -> Environment.SelfHosted(EnvironmentUrlDataJson(baseEnvironmentUrl))
        }
    }
}

private const val BASE_ENVIRONMENT_URL_RESTRICTION_KEY: String = "baseEnvironmentUrl"
