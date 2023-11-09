package com.x8bit.bitwarden.data.generator.repository

import com.bitwarden.core.PasswordGeneratorRequest
import com.x8bit.bitwarden.data.generator.datasource.sdk.GeneratorSdkSource
import com.x8bit.bitwarden.data.generator.repository.model.GeneratedPasswordResult
import javax.inject.Singleton

/**
 * Default implementation of [GeneratorRepository].
 */
@Singleton
class GeneratorRepositoryImpl constructor(
    private val generatorSdkSource: GeneratorSdkSource,
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
}
