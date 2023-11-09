package com.x8bit.bitwarden.data.generator.repository

import com.bitwarden.core.PasswordGeneratorRequest
import com.x8bit.bitwarden.data.generator.repository.model.GeneratedPasswordResult

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
}
