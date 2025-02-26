package com.bitwarden.authenticator.data.platform.datasource.disk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/**
 * Models the state of feature flags.
 */
@Serializable
data class FeatureFlagsConfiguration(
    @SerialName("featureFlags")
    val featureFlags: Map<String, JsonPrimitive>,
)
