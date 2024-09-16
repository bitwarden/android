package com.x8bit.bitwarden.data.tools.generator.repository.util

import com.bitwarden.generators.PassphraseGeneratorRequest
import com.bitwarden.generators.PasswordGeneratorRequest
import com.bitwarden.generators.UsernameGeneratorRequest
import com.bitwarden.vault.PasswordHistoryView
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.platform.repository.model.LocalDataState
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepository
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.junit.jupiter.api.Assertions.assertEquals

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

    private var usernameGenerationOptions: UsernameGenerationOptions? = null

    private var passwordGeneratorRequest: PasswordGeneratorRequest? = null

    private val mutablePasswordHistoryStateFlow =
        MutableStateFlow<LocalDataState<List<PasswordHistoryView>>>(LocalDataState.Loading)

    private val mutableGeneratorResultFlow = bufferedMutableSharedFlow<GeneratorResult>()

    private var generatePlusAddressedEmailResult: GeneratedPlusAddressedUsernameResult =
        GeneratedPlusAddressedUsernameResult.Success(
            generatedEmailAddress = "email+abcd1234@address.com",
        )

    private var generateCatchAllEmailResult: GeneratedCatchAllUsernameResult =
        GeneratedCatchAllUsernameResult.Success(
            generatedEmailAddress = "user@domain",
        )

    private var generateRandomWordUsernameResult: GeneratedRandomWordUsernameResult =
        GeneratedRandomWordUsernameResult.Success(
            generatedUsername = "randomWord",
        )

    private var generateForwardedServiceResult: GeneratedForwardedServiceUsernameResult =
        GeneratedForwardedServiceUsernameResult.Success(
            generatedEmailAddress = "updatedUsername",
        )

    private var passwordGeneratorPolicy: PolicyInformation.PasswordGenerator? = null

    override val passwordHistoryStateFlow: StateFlow<LocalDataState<List<PasswordHistoryView>>>
        get() = mutablePasswordHistoryStateFlow

    override val generatorResultFlow: Flow<GeneratorResult>
        get() = mutableGeneratorResultFlow.asSharedFlow()

    override fun emitGeneratorResult(generatorResult: GeneratorResult) {
        mutableGeneratorResultFlow.tryEmit(generatorResult)
    }

    override suspend fun generatePassword(
        passwordGeneratorRequest: PasswordGeneratorRequest,
        shouldSave: Boolean,
    ): GeneratedPasswordResult {
        this.passwordGeneratorRequest = passwordGeneratorRequest
        return generatePasswordResult
    }

    override suspend fun generatePassphrase(
        passphraseGeneratorRequest: PassphraseGeneratorRequest,
    ): GeneratedPassphraseResult {
        return generatePassphraseResult
    }

    override suspend fun generatePlusAddressedEmail(
        plusAddressedEmailGeneratorRequest: UsernameGeneratorRequest.Subaddress,
    ): GeneratedPlusAddressedUsernameResult {
        return generatePlusAddressedEmailResult
    }

    override suspend fun generateCatchAllEmail(
        catchAllEmailGeneratorRequest: UsernameGeneratorRequest.Catchall,
    ): GeneratedCatchAllUsernameResult {
        return generateCatchAllEmailResult
    }

    override suspend fun generateRandomWordUsername(
        randomWordGeneratorRequest: UsernameGeneratorRequest.Word,
    ): GeneratedRandomWordUsernameResult {
        return generateRandomWordUsernameResult
    }

    override suspend fun generateForwardedServiceUsername(
        forwardedServiceGeneratorRequest: UsernameGeneratorRequest.Forwarded,
    ): GeneratedForwardedServiceUsernameResult {
        return generateForwardedServiceResult
    }

    override fun getPasscodeGenerationOptions(): PasscodeGenerationOptions? {
        return passcodeGenerationOptions
    }

    override fun savePasscodeGenerationOptions(options: PasscodeGenerationOptions) {
        passcodeGenerationOptions = options
    }

    override fun getUsernameGenerationOptions(): UsernameGenerationOptions? {
        return usernameGenerationOptions
    }

    override fun saveUsernameGenerationOptions(options: UsernameGenerationOptions) {
        usernameGenerationOptions = options
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

    /**
     * Sets the mock result for the generateForwardedService function.
     */
    fun setMockGenerateForwardedServiceResult(result: GeneratedForwardedServiceUsernameResult) {
        generateForwardedServiceResult = result
    }

    /**
     * Sets the mock result for the generateCatchAll function.
     */
    fun setMockCatchAllResult(result: GeneratedCatchAllUsernameResult) {
        generateCatchAllEmailResult = result
    }

    /**
     * Sets the mock result for the generateRandomWord function.
     */
    fun setMockRandomWordResult(result: GeneratedRandomWordUsernameResult) {
        generateRandomWordUsernameResult = result
    }

    /**
     * Asserts that the passed in request matches the stored request.
     */
    fun assertEqualsStoredRequest(request: PasswordGeneratorRequest) {
        assertEquals(request, passwordGeneratorRequest)
    }
}
