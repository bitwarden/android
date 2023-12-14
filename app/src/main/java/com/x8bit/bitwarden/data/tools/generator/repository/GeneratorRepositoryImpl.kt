package com.x8bit.bitwarden.data.tools.generator.repository

import com.bitwarden.core.PassphraseGeneratorRequest
import com.bitwarden.core.PasswordGeneratorRequest
import com.bitwarden.core.PasswordHistoryView
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.repository.model.LocalDataState
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.GeneratorDiskSource
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.PasswordHistoryDiskSource
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.entity.toPasswordHistory
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.entity.toPasswordHistoryEntity
import com.x8bit.bitwarden.data.tools.generator.datasource.sdk.GeneratorSdkSource
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPassphraseResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPasswordResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.PasscodeGenerationOptions
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Singleton

/**
 * Default implementation of [GeneratorRepository].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class GeneratorRepositoryImpl constructor(
    private val generatorSdkSource: GeneratorSdkSource,
    private val generatorDiskSource: GeneratorDiskSource,
    private val authDiskSource: AuthDiskSource,
    private val vaultSdkSource: VaultSdkSource,
    private val passwordHistoryDiskSource: PasswordHistoryDiskSource,
) : GeneratorRepository {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutablePasswordHistoryStateFlow =
        MutableStateFlow<LocalDataState<List<PasswordHistoryView>>>(LocalDataState.Loading)

    override val passwordHistoryStateFlow: StateFlow<LocalDataState<List<PasswordHistoryView>>>
        get() = mutablePasswordHistoryStateFlow.asStateFlow()

    private var passwordHistoryJob: Job? = null

    init {
        mutablePasswordHistoryStateFlow
            .subscriptionCount
            .flatMapLatest { subscriberCount ->
                if (subscriberCount > 0) {
                    authDiskSource
                        .userStateFlow
                        .map { it?.activeUserId }
                        .distinctUntilChanged()
                } else {
                    flow { awaitCancellation() }
                }
            }
            .onEach { activeUserId ->
                observePasswordHistoryForUser(activeUserId)
            }
            .launchIn(scope)
    }

    private fun observePasswordHistoryForUser(userId: String?) {
        passwordHistoryJob?.cancel()
        userId ?: return

        mutablePasswordHistoryStateFlow.value = LocalDataState.Loading

        passwordHistoryJob = passwordHistoryDiskSource
            .getPasswordHistoriesForUser(userId)
            .map { encryptedPasswordHistoryList ->
                val passwordHistories =
                    encryptedPasswordHistoryList.map { it.toPasswordHistory() }
                vaultSdkSource
                    .decryptPasswordHistoryList(passwordHistories)
            }
            .onEach { encryptedPasswordHistoryListResult ->
                encryptedPasswordHistoryListResult
                    .fold(
                        onSuccess = {
                            mutablePasswordHistoryStateFlow.value = LocalDataState.Loaded(it)
                        },
                        onFailure = {
                            mutablePasswordHistoryStateFlow.value = LocalDataState.Error(it)
                        },
                    )
            }
            .launchIn(scope)
    }

    override suspend fun generatePassword(
        passwordGeneratorRequest: PasswordGeneratorRequest,
    ): GeneratedPasswordResult =
        generatorSdkSource
            .generatePassword(passwordGeneratorRequest)
            .fold(
                onSuccess = { GeneratedPasswordResult.Success(it) },
                onFailure = { GeneratedPasswordResult.InvalidRequest },
            )

    override suspend fun generatePassphrase(
        passphraseGeneratorRequest: PassphraseGeneratorRequest,
    ): GeneratedPassphraseResult =
        generatorSdkSource
            .generatePassphrase(passphraseGeneratorRequest)
            .fold(
                onSuccess = { GeneratedPassphraseResult.Success(it) },
                onFailure = { GeneratedPassphraseResult.InvalidRequest },
            )

    override fun getPasscodeGenerationOptions(): PasscodeGenerationOptions? {
        val userId = authDiskSource.userState?.activeUserId
        return userId?.let { generatorDiskSource.getPasscodeGenerationOptions(it) }
    }

    override fun savePasscodeGenerationOptions(options: PasscodeGenerationOptions) {
        val userId = authDiskSource.userState?.activeUserId
        userId?.let { generatorDiskSource.storePasscodeGenerationOptions(it, options) }
    }

    override suspend fun storePasswordHistory(passwordHistoryView: PasswordHistoryView) {
        val userId = authDiskSource.userState?.activeUserId ?: return
        val encryptedPasswordHistory = vaultSdkSource
            .encryptPasswordHistory(passwordHistoryView)
            .getOrNull() ?: return
        passwordHistoryDiskSource.insertPasswordHistory(
            encryptedPasswordHistory.toPasswordHistoryEntity(userId),
        )
    }

    override suspend fun clearPasswordHistory() {
        val userId = authDiskSource.userState?.activeUserId ?: return
        passwordHistoryDiskSource.clearPasswordHistories(userId)
    }
}
