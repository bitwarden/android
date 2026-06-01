package com.x8bit.bitwarden.data.tools.generator.datasource.disk

import com.x8bit.bitwarden.data.tools.generator.repository.model.PasscodeGenerationOptions
import com.x8bit.bitwarden.data.tools.generator.repository.model.UsernameGenerationOptions

/**
 * Primary access point for disk information related to generation.
 */
interface GeneratorDiskSource {
    /**
     * Clears all the settings data for the given user.
     */
    fun clearData(userId: String)

    /**
     * Retrieves a user's passcode generation options using a [userId].
     */
    fun getPasscodeGenerationOptions(userId: String): PasscodeGenerationOptions?

    /**
     * Stores a user's passcode generation options using a [userId].
     */
    fun storePasscodeGenerationOptions(userId: String, options: PasscodeGenerationOptions?)

    /**
     * Retrieves a user's username generation options using a [userId].
     */
    fun getUsernameGenerationOptions(userId: String): UsernameGenerationOptions?

    /**
     * Stores a user's username generation options using a [userId].
     */
    fun storeUsernameGenerationOptions(userId: String, options: UsernameGenerationOptions?)
}
