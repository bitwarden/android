package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models response bodies from the delete account request.
 */
sealed class DeleteAccountResponseJson {

    /**
     * Models a successful deletion response.
     */
    data object Success : DeleteAccountResponseJson()

    /**
     * Models the json body of a deletion error.
     * @param message a human readable error message.
     * @param validationErrors a map where each value is a list of error messages for each key.
     * The values in the array should be used for display to the user, since the keys tend to come
     * back as nonsense. (eg: empty string key)
     */
    @Serializable
    @Suppress("MaxLineLength")
    data class Invalid(
        @SerialName("message")
        override val message: String,

        @SerialName("validationErrors")
        override val validationErrors: Map<String, List<String>>?,
    ) : DeleteAccountResponseJson(), InvalidJsonResponse {
        /**
         * The type of invalid responses that can be received.
         */
        sealed class InvalidType {
            /**
             * Represents cannot delete accounts owned by an organization invalid response
             */
            data object CannotDeleteAccountOwnedByOrg : InvalidType()

            /**
             * Represents generic invalid response
             */
            data object GenericInvalid : InvalidType()
        }

        val invalidType: InvalidType
            get() = if (message == "Cannot delete accounts owned by an organization. Contact your organization administrator for additional details.") {
                InvalidType.CannotDeleteAccountOwnedByOrg
            } else {
                InvalidType.GenericInvalid
            }
    }
}
