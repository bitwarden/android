package com.bitwarden.authenticator.data.platform.repository.model

import com.bitwarden.authenticator.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.authenticator.data.platform.repository.util.labelOrBaseUrlHost

/**
 * A higher-level wrapper around [EnvironmentUrlDataJson] that provides type-safety, enumerability,
 * and human-readable labels.
 */
sealed class Environment {
    /**
     * The [Type] of the environment.
     */
    abstract val type: Type

    /**
     * The raw [environmentUrlData] that contains specific base URLs for each relevant domain.
     */
    abstract val environmentUrlData: EnvironmentUrlDataJson

    /**
     * A human-readable label for the environment based in some way on its base URL.
     */
    abstract val label: String

    /**
     * The default US environment.
     */
    data object Us : Environment() {
        override val type: Type get() = Type.US
        override val environmentUrlData: EnvironmentUrlDataJson
            get() = EnvironmentUrlDataJson.DEFAULT_US
        override val label: String
            get() = "bitwarden.com"
    }

    /**
     * The default EU environment.
     */
    data object Eu : Environment() {
        override val type: Type get() = Type.EU
        override val environmentUrlData: EnvironmentUrlDataJson
            get() = EnvironmentUrlDataJson.DEFAULT_EU
        override val label: String
            get() = "bitwarden.eu"
    }

    /**
     * A custom self-hosted environment with a fully configurable [environmentUrlData].
     */
    data class SelfHosted(
        override val environmentUrlData: EnvironmentUrlDataJson,
    ) : Environment() {
        override val type: Type get() = Type.SELF_HOSTED
        override val label: String
            get() = environmentUrlData.labelOrBaseUrlHost
    }

    /**
     * A summary of the various types that can be enumerated over and which contains a
     * human-readable [label].
     */
    enum class Type {
        US,
        EU,
        SELF_HOSTED,
    }
}
