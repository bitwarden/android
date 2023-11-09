package com.x8bit.bitwarden.data.generator.datasource.sdk

import com.bitwarden.core.PasswordGeneratorRequest

/**
 * Source of password generation functionality from the Bitwarden SDK.
 */
interface GeneratorSdkSource {

    /**
     * Generates a password returning a [String] wrapped in a [Result].
     */
    suspend fun generatePassword(request: PasswordGeneratorRequest): Result<String>
}
