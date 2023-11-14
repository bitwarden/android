package com.x8bit.bitwarden.data.tools.generator.datasource.disk

import com.x8bit.bitwarden.data.tools.generator.repository.model.PasswordGenerationOptions

/**
 * Primary access point for disk information related to generation.
 */
interface GeneratorDiskSource {

    /**
     * Retrieves a user's password generation options using a [userId].
     */
    fun getPasswordGenerationOptions(userId: String): PasswordGenerationOptions?

    /**
     * Stores a user's password generation options using a [userId].
     */
    fun storePasswordGenerationOptions(userId: String, options: PasswordGenerationOptions?)
}
