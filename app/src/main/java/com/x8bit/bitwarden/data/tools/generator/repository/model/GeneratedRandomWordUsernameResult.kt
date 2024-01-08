package com.x8bit.bitwarden.data.tools.generator.repository.model

/**
 * Represents the outcome of a generator operation.
 */
sealed class GeneratedRandomWordUsernameResult {

    /**
     * Operation succeeded with a value.
     */
    data class Success(
        val generatedUsername: String,
    ) : GeneratedRandomWordUsernameResult()

    /**
     * There was an error during the operation.
     */
    data object InvalidRequest : GeneratedRandomWordUsernameResult()
}
