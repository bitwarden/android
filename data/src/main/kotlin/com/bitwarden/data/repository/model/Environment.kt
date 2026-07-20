package com.bitwarden.data.repository.model

import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.data.repository.util.labelOrBaseUrlHost

/**
 * A higher-level wrapper around [EnvironmentUrlDataJson] that provides type-safety, enumerability,
 * and human-readable labels.
 */
sealed class Environment {
    /**
     * A type that can be enumerated over indicating the high-level type of environment.
     */
    abstract val type: Type

    /**
     * The raw [EnvironmentUrlDataJson] that contains specific base URLs for each relevant domain.
     */
    abstract val environmentUrlData: EnvironmentUrlDataJson

    /**
     * Indicates if the environment is used for FedRamp.
     */
    abstract val isFedRamp: Boolean

    /**
     * A human-readable label for the environment based in some way on its base URL.
     */
    abstract val label: String

    /**
     * Represents a production environment.
     */
    sealed class Prod : Environment() {
        /**
         * The production US environment.
         */
        data object Us : Prod() {
            override val type: Type get() = Type.US
            override val label: String get() = "bitwarden.com"
            override val isFedRamp: Boolean get() = false
            override val environmentUrlData: EnvironmentUrlDataJson
                get() = EnvironmentUrlDataJson.DEFAULT_US
        }

        /**
         * The production EU environment.
         */
        data object Eu : Prod() {
            override val type: Type get() = Type.EU
            override val label: String get() = "bitwarden.eu"
            override val isFedRamp: Boolean get() = false
            override val environmentUrlData: EnvironmentUrlDataJson
                get() = EnvironmentUrlDataJson.DEFAULT_EU
        }

        /**
         * The production FedRamp environment.
         */
        data object FedRamp : Prod() {
            override val type: Type get() = Type.FED_RAMP
            override val label: String get() = "bitwarden-gov.com"
            override val isFedRamp: Boolean get() = true
            override val environmentUrlData: EnvironmentUrlDataJson
                get() = EnvironmentUrlDataJson.DEFAULT_FED_RAMP
        }
    }

    /**
     * A custom self-hosted environment with a fully configurable [environmentUrlData].
     */
    data class SelfHosted(
        override val environmentUrlData: EnvironmentUrlDataJson,
    ) : Environment() {
        override val type: Type get() = Type.SELF_HOSTED
        override val label: String get() = this.labelOrBaseUrlHost
        override val isFedRamp: Boolean get() = environmentUrlData.isFedRamp
        val isInternal: Boolean get() = environmentUrlData.isInternal
    }

    /**
     * A summary of the various types that can be enumerated over.
     */
    enum class Type {
        US,
        EU,
        FED_RAMP,
        SELF_HOSTED,
    }
}
