package com.x8bit.bitwarden.data.auth.datasource.network.model

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
     *
     * @param validationErrors a map where each value is a list of error messages for each key.
     * The values in the array should be used for display to the user, since the keys tend to come
     * back as nonsense. (eg: empty string key)
     */
    @Serializable
    data class Invalid(
        @SerialName("validationErrors")
        val validationErrors: Map<String, List<String?>>?,
    ) : DeleteAccountResponseJson()
}
