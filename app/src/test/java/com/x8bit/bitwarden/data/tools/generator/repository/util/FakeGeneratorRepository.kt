package com.x8bit.bitwarden.data.tools.generator.repository.util

import com.bitwarden.core.PassphraseGeneratorRequest
import com.bitwarden.core.PasswordGeneratorRequest
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepository
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPassphraseResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPasswordResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.PasscodeGenerationOptions

/**
 * A fake implementation of [GeneratorRepository] for testing purposes.
 * This class provides a simplified way to set up and control responses for repository methods.
 */
class FakeGeneratorRepository : GeneratorRepository {
    private var generatePasswordResult: GeneratedPasswordResult = GeneratedPasswordResult.Success(
        generatedString = "updatedText",
    )
    private var generatePassphraseResult: GeneratedPassphraseResult =
        GeneratedPassphraseResult.Success(
            generatedString = "updatedPassphrase",
        )
    private var passcodeGenerationOptions: PasscodeGenerationOptions? = null

    override suspend fun generatePassword(
        passwordGeneratorRequest: PasswordGeneratorRequest,
    ): GeneratedPasswordResult {
        return generatePasswordResult
    }

    override suspend fun generatePassphrase(
        passphraseGeneratorRequest: PassphraseGeneratorRequest,
    ): GeneratedPassphraseResult {
        return generatePassphraseResult
    }

    override fun getPasscodeGenerationOptions(): PasscodeGenerationOptions? {
        return passcodeGenerationOptions
    }

    override fun savePasscodeGenerationOptions(options: PasscodeGenerationOptions) {
        passcodeGenerationOptions = options
    }

    /**
     * Sets the mock result for the generatePassword function.
     */
    fun setMockGeneratePasswordResult(result: GeneratedPasswordResult) {
        generatePasswordResult = result
    }

    /**
     * Sets the mock result for the generatePassphrase function.
     */
    fun setMockGeneratePassphraseResult(result: GeneratedPassphraseResult) {
        generatePassphraseResult = result
    }
}
