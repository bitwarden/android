package com.x8bit.bitwarden.data.tools.generator.repository

import com.bitwarden.core.PassphraseGeneratorRequest
import com.bitwarden.core.PasswordGeneratorRequest
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.GeneratorDiskSource
import com.x8bit.bitwarden.data.tools.generator.datasource.sdk.GeneratorSdkSource
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPassphraseResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPasswordResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.PasscodeGenerationOptions
import javax.inject.Singleton

/**
 * Default implementation of [GeneratorRepository].
 */
@Singleton
class GeneratorRepositoryImpl constructor(
    private val generatorSdkSource: GeneratorSdkSource,
    private val generatorDiskSource: GeneratorDiskSource,
    private val authDiskSource: AuthDiskSource,
) : GeneratorRepository {

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
}
