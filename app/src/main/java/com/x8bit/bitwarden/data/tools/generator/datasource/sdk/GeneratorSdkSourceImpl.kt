package com.x8bit.bitwarden.data.tools.generator.datasource.sdk

import com.bitwarden.generators.PassphraseGeneratorRequest
import com.bitwarden.generators.PasswordGeneratorRequest
import com.bitwarden.generators.UsernameGeneratorRequest
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

    override suspend fun generatePassphrase(
        request: PassphraseGeneratorRequest,
    ): Result<String> = runCatching {
        clientGenerator.passphrase(request)
    }

    override suspend fun generatePlusAddressedEmail(
        request: UsernameGeneratorRequest.Subaddress,
    ): Result<String> = runCatching {
        clientGenerator.username(request)
    }

    override suspend fun generateCatchAllEmail(
        request: UsernameGeneratorRequest.Catchall,
    ): Result<String> = runCatching {
        clientGenerator.username(request)
    }

    override suspend fun generateRandomWord(
        request: UsernameGeneratorRequest.Word,
    ): Result<String> = runCatching {
        clientGenerator.username(request)
    }

    override suspend fun generateForwardedServiceEmail(
        request: UsernameGeneratorRequest.Forwarded,
    ): Result<String> = runCatching {
        clientGenerator.username(request)
    }
}
