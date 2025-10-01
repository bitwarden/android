package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of updating a user's kdf settings to minimums
 */
sealed class UpdateKdfMinimumsResult {
    /**
     * Active account was not found
     */
    object ActiveAccountNotFound : UpdateKdfMinimumsResult()

    /**
     * There was an error updating user to minimum kdf settings.
     *
     * @param error the error.
     */
    data class Error(
        val error: Throwable?,
    ) : UpdateKdfMinimumsResult()

    /**
     * Updated user to minimum kdf settings successfully.
     */
    object Success : UpdateKdfMinimumsResult()
}
