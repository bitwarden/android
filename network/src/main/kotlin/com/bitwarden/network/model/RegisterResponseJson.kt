package com.bitwarden.network.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * Models response bodies for the register request.
 */
@Serializable
sealed class RegisterResponseJson {

    /**
     * Models a successful json response of the register request.
     */
    @Serializable
    data object Success : RegisterResponseJson()

    /**
     * Represents the json body of an invalid register request.
     *
     * @param validationErrors a map where each value is a list of error messages for each key.
     * The values in the array should be used for display to the user, since the keys tend to come
     * back as nonsense. (eg: empty string key)
     */
    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class Invalid(
        @JsonNames("message")
        @SerialName("Message")
        private val invalidMessage: String? = null,

        @SerialName("validationErrors")
        private val validationErrors: Map<String, List<String>>?,
    ) : RegisterResponseJson() {
        /**
         * A generic error message.
         */
        val message: String?
            get() = validationErrors
                ?.values
                ?.firstOrNull()
                ?.firstOrNull()
                ?: invalidMessage
    }
}
