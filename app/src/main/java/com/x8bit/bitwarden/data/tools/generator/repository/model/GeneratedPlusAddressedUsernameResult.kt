package com.x8bit.bitwarden.data.tools.generator.repository.model

/**
 * Represents the outcome of a generator operation.
 */
sealed class GeneratedPlusAddressedUsernameResult {
    /**
     * Operation succeeded with a value.
     */
    data class Success(
        val generatedEmailAddress: String,
    ) : GeneratedPlusAddressedUsernameResult()

    /**
     * There was an error during the operation.
     */
    data object InvalidRequest : GeneratedPlusAddressedUsernameResult()
}
