package com.x8bit.bitwarden.data.tools.generator.datasource.sdk

import com.bitwarden.generators.PassphraseGeneratorRequest
import com.bitwarden.generators.PasswordGeneratorRequest
import com.bitwarden.generators.UsernameGeneratorRequest
import com.bitwarden.sdk.ClientGenerators
import com.x8bit.bitwarden.data.platform.datasource.sdk.BaseSdkSource
import com.x8bit.bitwarden.data.platform.manager.SdkClientManager

/**
 * Implementation of [GeneratorSdkSource] that delegates password generation.
 *
 * @property sdkClientManager The [SdkClientManager] used to retrieve an instance of the
 * [ClientGenerators] provided by the Bitwarden SDK.
 */
class GeneratorSdkSourceImpl(
    sdkClientManager: SdkClientManager,
) : BaseSdkSource(sdkClientManager = sdkClientManager),
    GeneratorSdkSource {

    override suspend fun generatePassword(
        request: PasswordGeneratorRequest,
    ): Result<String> = runCatchingWithLogs {
        getClient().generators().password(request)
    }

    override suspend fun generatePassphrase(
        request: PassphraseGeneratorRequest,
    ): Result<String> = runCatchingWithLogs {
        getClient().generators().passphrase(request)
    }

    override suspend fun generatePlusAddressedEmail(
        request: UsernameGeneratorRequest.Subaddress,
    ): Result<String> = runCatchingWithLogs {
        getClient().generators().username(request)
    }

    override suspend fun generateCatchAllEmail(
        request: UsernameGeneratorRequest.Catchall,
    ): Result<String> = runCatchingWithLogs {
        getClient().generators().username(request)
    }

    override suspend fun generateRandomWord(
        request: UsernameGeneratorRequest.Word,
    ): Result<String> = runCatchingWithLogs {
        getClient().generators().username(request)
    }

    override suspend fun generateForwardedServiceEmail(
        request: UsernameGeneratorRequest.Forwarded,
    ): Result<String> = runCatchingWithLogs {
        getClient().generators().username(request)
    }
}
