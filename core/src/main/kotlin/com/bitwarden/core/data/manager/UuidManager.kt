package com.bitwarden.core.data.manager

/**
 * Manager for generating unique identifiers.
 */
interface UuidManager {
    /**
     * Generates a random UUID string.
     *
     * @return A string representation of a randomly generated UUID.
     */
    fun generateUuid(): String
}
