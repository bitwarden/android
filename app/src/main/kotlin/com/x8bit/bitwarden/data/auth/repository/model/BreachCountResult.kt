package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of determining if a password has been breached.
 */
sealed class BreachCountResult {
    /**
     * Contains the number of breaches.
     */
    data class Success(val breachCount: Int) : BreachCountResult()

    /**
     * There was an error determining if the password has been breached.
     */
    data class Error(
        val error: Throwable,
        val message: String? = null,
    ) : BreachCountResult()
}
