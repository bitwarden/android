package com.x8bit.bitwarden.data.platform.repository.model

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText

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
     * Helper for a returning a human-readable label from a [Type].
     */
    val label: Text get() = type.label

    /**
     * The default US environment.
     */
    data object Us : Environment() {
        override val type: Type get() = Type.US
        override val environmentUrlData: EnvironmentUrlDataJson
            get() = EnvironmentUrlDataJson.DEFAULT_US
    }

    /**
     * The default EU environment.
     */
    data object Eu : Environment() {
        override val type: Type get() = Type.EU
        override val environmentUrlData: EnvironmentUrlDataJson
            get() = EnvironmentUrlDataJson.DEFAULT_EU
    }

    /**
     * A custom self-hosted environment with a fully configurable [environmentUrlData].
     */
    data class SelfHosted(
        override val environmentUrlData: EnvironmentUrlDataJson,
    ) : Environment() {
        override val type: Type get() = Type.SELF_HOSTED
    }

    /**
     * A summary of the various types that can be enumerated over and which contains a
     * human-readable [label].
     */
    enum class Type(val label: Text) {
        US(label = "bitwarden.com".asText()),
        EU(label = "bitwarden.eu".asText()),
        SELF_HOSTED(label = R.string.self_hosted.asText()),
    }
}
