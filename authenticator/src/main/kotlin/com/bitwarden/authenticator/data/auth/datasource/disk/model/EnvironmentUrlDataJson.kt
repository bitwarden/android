package com.bitwarden.authenticator.data.auth.datasource.disk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents URLs for various Bitwarden domains.
 *
 * @property base The overall base URL.
 * @property api Separate base URL for the "/api" domain (if applicable).
 * @property identity Separate base URL for the "/identity" domain (if applicable).
 * @property icon Separate base URL for the icon domain (if applicable).
 * @property notifications Separate base URL for the notifications domain (if applicable).
 * @property webVault Separate base URL for the web vault domain (if applicable).
 * @property events Separate base URL for the events domain (if applicable).
 */
@Serializable
data class EnvironmentUrlDataJson(
    @SerialName("base")
    val base: String,

    @SerialName("api")
    val api: String? = null,

    @SerialName("identity")
    val identity: String? = null,

    @SerialName("icons")
    val icon: String? = null,

    @SerialName("notifications")
    val notifications: String? = null,

    @SerialName("webVault")
    val webVault: String? = null,

    @SerialName("events")
    val events: String? = null,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Default [EnvironmentUrlDataJson] for the US region.
         */
        val DEFAULT_US: EnvironmentUrlDataJson =
            EnvironmentUrlDataJson(base = "https://vault.bitwarden.com")

        /**
         * Default [EnvironmentUrlDataJson] for the EU region.
         */
        val DEFAULT_EU: EnvironmentUrlDataJson =
            EnvironmentUrlDataJson(base = "https://vault.bitwarden.eu")
    }
}
