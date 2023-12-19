package com.x8bit.bitwarden.data.tools.generator.repository.util

import com.bitwarden.core.PassphraseGeneratorRequest
import com.bitwarden.core.PasswordGeneratorRequest
import com.bitwarden.core.PasswordHistoryView
import com.x8bit.bitwarden.data.platform.repository.model.LocalDataState
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepository
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPassphraseResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPasswordResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.PasscodeGenerationOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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

    private val mutablePasswordHistoryStateFlow =
        MutableStateFlow<LocalDataState<List<PasswordHistoryView>>>(LocalDataState.Loading)

    override val passwordHistoryStateFlow: StateFlow<LocalDataState<List<PasswordHistoryView>>>
        get() = mutablePasswordHistoryStateFlow

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

    override suspend fun storePasswordHistory(passwordHistoryView: PasswordHistoryView) {
        val currentList = mutablePasswordHistoryStateFlow.value.data.orEmpty()
        val updatedList = currentList + passwordHistoryView
        mutablePasswordHistoryStateFlow.value = LocalDataState.Loaded(updatedList)
    }

    override suspend fun clearPasswordHistory() {
        mutablePasswordHistoryStateFlow.value = LocalDataState.Loaded(emptyList())
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

    /**
     * Checks if a specific password is in the history.
     */
    fun isPasswordStoredInHistory(password: String): Boolean {
        val passwordHistoryList = mutablePasswordHistoryStateFlow.value.data.orEmpty()
        return passwordHistoryList.any { it.password == password }
    }

    /**
     * Emits specified state to the passwordHistoryStateFlow.
     */
    fun emitPasswordHistoryState(state: LocalDataState<List<PasswordHistoryView>>) {
        mutablePasswordHistoryStateFlow.value = state
    }
}
