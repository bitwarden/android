package com.x8bit.bitwarden.data.auth.datasource.network.service

/**
 * Defines methods for interacting with the have I been pwned API.
 */
interface HaveIBeenPwnedService {

    /**
     * Check to see if the given password has been breached. Returns true if breached.
     */
    suspend fun hasPasswordBeenBreached(
        password: String,
    ): Result<Boolean>
}
