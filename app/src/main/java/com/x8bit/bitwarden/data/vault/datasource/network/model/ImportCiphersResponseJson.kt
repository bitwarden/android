package com.x8bit.bitwarden.data.vault.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The response body for importing ciphers.
 */
@Serializable
sealed class ImportCiphersResponseJson {

    /**
     * Models a successful json response.
     */
    @Serializable
    object Success : ImportCiphersResponseJson()

    /**
     * Represents the json body of an invalid request.
     *
     * @param validationErrors a map where each value is a list of error messages for each key.
     * The values in the array should be used for display to the user, since the keys tend to come
     * back as nonsense. (eg: empty string key)
     */
    @Serializable
    data class Invalid(
        @SerialName("message")
        private val invalidMessage: String? = null,

        @SerialName("Message")
        private val errorMessage: String? = null,

        @SerialName("validationErrors")
        val validationErrors: Map<String, List<String>>?,
    ) : ImportCiphersResponseJson() {
        /**
         * A generic error message.
         */
        val message: String? get() = invalidMessage ?: errorMessage
    }
}
