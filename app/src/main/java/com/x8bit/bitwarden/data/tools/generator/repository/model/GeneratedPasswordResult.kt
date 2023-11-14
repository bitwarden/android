package com.x8bit.bitwarden.data.tools.generator.repository.model

/**
 * Represents the outcome of a generator operation.
 */
sealed class GeneratedPasswordResult {

    /**
     * Operation succeeded with a value.
     */
    data class Success(val generatedString: String) : GeneratedPasswordResult()

    /**
     * There was an error during the operation.
     */
    data object InvalidRequest : GeneratedPasswordResult()
}
