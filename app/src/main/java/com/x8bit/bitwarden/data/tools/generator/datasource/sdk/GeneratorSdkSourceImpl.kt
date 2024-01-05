package com.x8bit.bitwarden.data.tools.generator.datasource.sdk

import com.bitwarden.core.PassphraseGeneratorRequest
import com.bitwarden.core.PasswordGeneratorRequest
import com.bitwarden.core.UsernameGeneratorRequest
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

    override suspend fun generateForwardedServiceEmail(
        request: UsernameGeneratorRequest.Forwarded,
    ): Result<String> = runCatching {
        clientGenerator.username(request)
    }
}
