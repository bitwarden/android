package com.x8bit.bitwarden.data.tools.generator.datasource.sdk

import com.bitwarden.generators.PassphraseGeneratorRequest
import com.bitwarden.generators.PasswordGeneratorRequest
import com.bitwarden.generators.UsernameGeneratorRequest

/**
 * Source of password generation functionality from the Bitwarden SDK.
 */
interface GeneratorSdkSource {

    /**
     * Generates a password returning a [String] wrapped in a [Result].
     */
    suspend fun generatePassword(request: PasswordGeneratorRequest): Result<String>

    /**
     * Generates a passphrase returning a [String] wrapped in a [Result].
     */
    suspend fun generatePassphrase(request: PassphraseGeneratorRequest): Result<String>

    /**
     * Generates a plus addressed email returning a [String] wrapped in a [Result].
     */
    suspend fun generatePlusAddressedEmail(
        request: UsernameGeneratorRequest.Subaddress,
    ): Result<String>

    /**
     * Generates a catch all email returning a [String] wrapped in a [Result].
     */
    suspend fun generateCatchAllEmail(
        request: UsernameGeneratorRequest.Catchall,
    ): Result<String>

    /**
     * Generates a random word username returning a [String] wrapped in a [Result].
     */
    suspend fun generateRandomWord(
        request: UsernameGeneratorRequest.Word,
    ): Result<String>

    /**
     * Generates a forwarded service email returning a [String] wrapped in a [Result].
     */
    suspend fun generateForwardedServiceEmail(
        request: UsernameGeneratorRequest.Forwarded,
    ): Result<String>
}
