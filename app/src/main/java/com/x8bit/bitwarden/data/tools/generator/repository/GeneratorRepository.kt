package com.x8bit.bitwarden.data.tools.generator.repository

import com.bitwarden.core.PassphraseGeneratorRequest
import com.bitwarden.core.PasswordGeneratorRequest
import com.bitwarden.core.PasswordHistoryView
import com.x8bit.bitwarden.data.platform.repository.model.LocalDataState
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPassphraseResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPasswordResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.PasscodeGenerationOptions
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
     * Get the [PasscodeGenerationOptions] for the current user.
     */
    fun getPasscodeGenerationOptions(): PasscodeGenerationOptions?

    /**
     * Save the [PasscodeGenerationOptions] for the current user.
     */
    fun savePasscodeGenerationOptions(options: PasscodeGenerationOptions)

    /**
     * Store a password history item for the current user.
     */
    suspend fun storePasswordHistory(passwordHistoryView: PasswordHistoryView)

    /**
     * Clear all stored password history for the current user.
     */
    suspend fun clearPasswordHistory()
}
