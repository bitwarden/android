@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.data.tools.generator.repository

import com.bitwarden.generators.PassphraseGeneratorRequest
import com.bitwarden.generators.PasswordGeneratorRequest
import com.bitwarden.generators.UsernameGeneratorRequest
import com.bitwarden.vault.PasswordHistoryView
import com.x8bit.bitwarden.data.platform.repository.model.LocalDataState
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedCatchAllUsernameResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedForwardedServiceUsernameResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPassphraseResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPasswordResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPlusAddressedUsernameResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedRandomWordUsernameResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratorResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.PasscodeGenerationOptions
import com.x8bit.bitwarden.data.tools.generator.repository.model.UsernameGenerationOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Responsible for managing generator data.
 */
interface GeneratorRepository {

    /**
     * Retrieve all stored password history items for the current user.
     */
    val passwordHistoryStateFlow: StateFlow<LocalDataState<List<PasswordHistoryView>>>

    /**
     * Flow that represents the modal generated text.
     */
    val generatorResultFlow: Flow<GeneratorResult>

    /**
     * Emits the modal generator result flow to listeners.
     */
    fun emitGeneratorResult(generatorResult: GeneratorResult)

    /**
     * Attempt to generate a password based on specifications in [passwordGeneratorRequest].
     * The [shouldSave] flag determines if the password is saved for future reference
     * or generated for temporary use.
     */
    suspend fun generatePassword(
        passwordGeneratorRequest: PasswordGeneratorRequest,
        shouldSave: Boolean,
    ): GeneratedPasswordResult

    /**
     * Attempt to generate a passphrase.
     */
    suspend fun generatePassphrase(
        passphraseGeneratorRequest: PassphraseGeneratorRequest,
    ): GeneratedPassphraseResult

    /**
     * Attempt to generate a plus addressed email username.
     */
    suspend fun generatePlusAddressedEmail(
        plusAddressedEmailGeneratorRequest: UsernameGeneratorRequest.Subaddress,
    ): GeneratedPlusAddressedUsernameResult

    /**
     * Attempt to generate a catch-all email username.
     */
    suspend fun generateCatchAllEmail(
        catchAllEmailGeneratorRequest: UsernameGeneratorRequest.Catchall,
    ): GeneratedCatchAllUsernameResult

    /**
     * Attempt to generate a random word username.
     */
    suspend fun generateRandomWordUsername(
        randomWordGeneratorRequest: UsernameGeneratorRequest.Word,
    ): GeneratedRandomWordUsernameResult

    /**
     * Attempt to generate a forwarded service username.
     */
    suspend fun generateForwardedServiceUsername(
        forwardedServiceGeneratorRequest: UsernameGeneratorRequest.Forwarded,
    ): GeneratedForwardedServiceUsernameResult

    /**
     * Get the [PasscodeGenerationOptions] for the current user.
     */
    fun getPasscodeGenerationOptions(): PasscodeGenerationOptions?

    /**
     * Save the [PasscodeGenerationOptions] for the current user.
     */
    fun savePasscodeGenerationOptions(options: PasscodeGenerationOptions)

    /**
     * Get the [UsernameGenerationOptions] for the current user.
     */
    fun getUsernameGenerationOptions(): UsernameGenerationOptions?

    /**
     * Save the [UsernameGenerationOptions] for the current user.
     */
    fun saveUsernameGenerationOptions(options: UsernameGenerationOptions)

    /**
     * Store a password history item for the current user.
     */
    suspend fun storePasswordHistory(passwordHistoryView: PasswordHistoryView)

    /**
     * Clear all stored password history for the current user.
     */
    suspend fun clearPasswordHistory()
}
