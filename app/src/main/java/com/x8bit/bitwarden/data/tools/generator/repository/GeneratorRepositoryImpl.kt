package com.x8bit.bitwarden.data.tools.generator.repository

import com.bitwarden.core.PasswordGeneratorRequest
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.GeneratorDiskSource
import com.x8bit.bitwarden.data.tools.generator.datasource.sdk.GeneratorSdkSource
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPasswordResult
import com.x8bit.bitwarden.data.tools.generator.repository.model.PasswordGenerationOptions
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

    override fun getPasswordGenerationOptions(): PasswordGenerationOptions? {
        val userId = authDiskSource.userState?.activeUserId
        return userId?.let { generatorDiskSource.getPasswordGenerationOptions(it) }
    }

    override fun savePasswordGenerationOptions(options: PasswordGenerationOptions) {
        val userId = authDiskSource.userState?.activeUserId
        userId?.let { generatorDiskSource.storePasswordGenerationOptions(it, options) }
    }
}
