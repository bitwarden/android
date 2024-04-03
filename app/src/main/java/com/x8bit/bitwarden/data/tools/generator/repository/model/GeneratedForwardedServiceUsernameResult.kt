package com.x8bit.bitwarden.data.tools.generator.repository.model

/**
 * Represents the outcome of a generator operation.
 */
sealed class GeneratedForwardedServiceUsernameResult {
    /**
     * Operation succeeded with a value.
     */
    data class Success(
        val generatedEmailAddress: String,
    ) : GeneratedForwardedServiceUsernameResult()

    /**
     * There was an error during the operation.
     */
    data class InvalidRequest(val message: String?) : GeneratedForwardedServiceUsernameResult()
}
