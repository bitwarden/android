package com.x8bit.bitwarden.data.tools.generator.datasource.disk

import com.x8bit.bitwarden.data.tools.generator.repository.model.PasscodeGenerationOptions

/**
 * Primary access point for disk information related to generation.
 */
interface GeneratorDiskSource {

    /**
     * Retrieves a user's passcode generation options using a [userId].
     */
    fun getPasscodeGenerationOptions(userId: String): PasscodeGenerationOptions?

    /**
     * Stores a user's passcode generation options using a [userId].
     */
    fun storePasscodeGenerationOptions(userId: String, options: PasscodeGenerationOptions?)
}
