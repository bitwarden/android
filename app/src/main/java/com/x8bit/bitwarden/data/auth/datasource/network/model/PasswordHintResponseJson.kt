package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models response bodies from password hint response.
 */
@Serializable
sealed class PasswordHintResponseJson {

    /**
     * The success body of the password hint response
     */
    @Serializable
    data object Success : PasswordHintResponseJson()

    /**
     * The error body of an invalid request containing a message.
     */
    @Serializable
    data class Error(
        @SerialName("message")
        val errorMessage: String?,
    ) : PasswordHintResponseJson()
}
