package com.x8bit.bitwarden.data.tools.generator.repository

import com.bitwarden.core.PassphraseGeneratorRequest
import com.bitwarden.core.PasswordGeneratorRequest
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPassphraseResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPasswordResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.PasscodeGenerationOptions

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
     * Attempt to generate a passphrase.
     */
    suspend fun generatePassphrase(
        passphraseGeneratorRequest: PassphraseGeneratorRequest,
    ): GeneratedPassphraseResult

    /**
     * Get the [PasscodeGenerationOptions] for the current user.
     */
    fun getPasscodeGenerationOptions(): PasscodeGenerationOptions?

    /**
     * Save the [PasscodeGenerationOptions] for the current user.
     */
    fun savePasscodeGenerationOptions(options: PasscodeGenerationOptions)
}
