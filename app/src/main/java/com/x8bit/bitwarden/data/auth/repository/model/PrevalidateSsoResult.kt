package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Possible SSO prevalidation results.
 */
sealed class PrevalidateSsoResult {
    /**
     * Prevalidation was successful and returned [token].
     */
    data class Success(
        val token: String,
    ) : PrevalidateSsoResult()

    /**
     * There was an error in prevalidation.
     */
    data object Failure : PrevalidateSsoResult()
}
