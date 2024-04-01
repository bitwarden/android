package com.x8bit.bitwarden.data.vault.datasource.network.model

/**
 * Represents the json body of an invalid send json request.
 */
sealed interface InvalidJsonResponse {

    /**
     * A general, user-displayable error message.
     */
    val message: String

    /**
     * a map where each value is a list of error messages for each key.
     * The values in the array should be used for display to the user, since the keys tend to come
     * back as nonsense. (eg: empty string key)
     */
    val validationErrors: Map<String, List<String>>?

    /**
     * Returns the first error message found in [validationErrors], or [message] if there are no
     * [validationErrors] present.
     */
    val firstValidationErrorMessage: String?
        get() = validationErrors
            ?.flatMap { it.value }
            ?.first()
}
