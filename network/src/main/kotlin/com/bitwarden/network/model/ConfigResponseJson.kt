package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/**
 * Represents the response model for configuration data fetched from the server.
 *
 * @property type The object type, typically "config".
 * @property version The version of the configuration data.
 * @property gitHash The Git hash associated with the configuration data.
 * @property server The server information (nullable).
 * @property environment The environment information containing URLs (vault, api, identity, etc.).
 * @property featureStates A map containing various feature states.
 * @property communication The communication configuration for server bootstrap (nullable).
 */
@Serializable
data class ConfigResponseJson(
    @SerialName("object")
    val type: String?,

    @SerialName("version")
    val version: String?,

    @SerialName("gitHash")
    val gitHash: String?,

    @SerialName("server")
    val server: ServerJson?,

    @SerialName("environment")
    val environment: EnvironmentJson?,

    @SerialName("featureStates")
    val featureStates: Map<String, JsonPrimitive>?,

    @SerialName("communication")
    val communication: CommunicationJson?,
) {
    /**
     * Represents a server in the configuration response.
     *
     * @param name The name of the server.
     * @param url The URL of the server.
     */
    @Serializable
    data class ServerJson(
        @SerialName("name")
        val name: String?,

        @SerialName("url")
        val url: String?,
    )

    /**
     * Represents the environment details in the configuration response.
     *
     * @param cloudRegion The cloud region associated with the environment.
     * @param vaultUrl The URL of the vault service in the environment.
     * @param apiUrl The URL of the API service in the environment.
     * @param identityUrl The URL of the identity service in the environment.
     * @param notificationsUrl The URL of the notifications service in the environment.
     * @param ssoUrl The URL of the single sign-on (SSO) service in the environment.
     */
    @Serializable
    data class EnvironmentJson(
        @SerialName("cloudRegion")
        val cloudRegion: String?,

        @SerialName("vault")
        val vaultUrl: String?,

        @SerialName("api")
        val apiUrl: String?,

        @SerialName("identity")
        val identityUrl: String?,

        @SerialName("notifications")
        val notificationsUrl: String?,

        @SerialName("sso")
        val ssoUrl: String?,
    )

    /**
     * Represents the communication configuration in the configuration response.
     *
     * @param bootstrap The bootstrap configuration for server communication (nullable).
     */
    @Serializable
    data class CommunicationJson(
        @SerialName("bootstrap")
        val bootstrap: BootstrapJson,
    ) {
        /**
         * Represents the bootstrap configuration for SSO cookie vendor authentication.
         *
         * @param type The type of bootstrap configuration (e.g., "ssoCookieVendor").
         * @param idpLoginUrl The URL of the identity provider login page.
         * @param cookieName The name of the authentication cookie.
         * @param cookieDomain The domain for which the cookie is valid.
         */
        @Serializable
        data class BootstrapJson(
            @SerialName("type")
            val type: String,

            @SerialName("idpLoginUrl")
            val idpLoginUrl: String?,

            @SerialName("cookieName")
            val cookieName: String?,

            @SerialName("cookieDomain")
            val cookieDomain: String?,
        )
    }
}
