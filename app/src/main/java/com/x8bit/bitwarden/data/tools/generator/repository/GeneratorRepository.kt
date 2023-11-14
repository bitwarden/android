package com.x8bit.bitwarden.data.tools.generator.repository

import com.bitwarden.core.PasswordGeneratorRequest
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPasswordResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.PasswordGenerationOptions

/**
 * Responsible for managing generator data.
 */
interface GeneratorRepository {

    /**
     * Attempt to generate a password.
     */
    suspend fun generatePassword(
        passwordGeneratorRequest: PasswordGeneratorRequest,
    ): GeneratedPasswordResult

    /**
     * Get the [PasswordGenerationOptions] for the current user.
     */
    fun getPasswordGenerationOptions(): PasswordGenerationOptions?

    /**
     * Save the [PasswordGenerationOptions] for the current user.
     */
    fun savePasswordGenerationOptions(options: PasswordGenerationOptions)
}
