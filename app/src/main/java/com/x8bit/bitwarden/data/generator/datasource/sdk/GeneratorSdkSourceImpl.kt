package com.x8bit.bitwarden.data.generator.datasource.sdk

import com.bitwarden.core.PasswordGeneratorRequest
import com.bitwarden.sdk.ClientGenerators

/**
 * Implementation of [GeneratorSdkSource] that delegates password generation.
 *
 * @property clientGenerator An instance of [ClientGenerators] provided by the Bitwarden SDK.
 */
class GeneratorSdkSourceImpl(
    private val clientGenerator: ClientGenerators,
) : GeneratorSdkSource {

    override suspend fun generatePassword(
        request: PasswordGeneratorRequest,
    ): Result<String> = runCatching {
        clientGenerator.password(request)
    }
}
