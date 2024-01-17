package com.x8bit.bitwarden.data.tools.generator.repository.model

/**
 * A result from the Generator.
 */
sealed class GeneratorResult {
    /**
     * A generated username.
     */
    data class Username(val username: String) : GeneratorResult()

    /**
     * A generated password.
     */
    data class Password(val password: String) : GeneratorResult()
}
