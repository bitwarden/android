package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Models result of verifying the master password.
 */
sealed class VerifyPasswordResult {

    /**
     * Master password is successfully verified.
     */
    data class Success(
        val isVerified: Boolean,
    ) : VerifyPasswordResult()

    /**
     * An error occurred while trying to verify the master password.
     */
    data object Error : VerifyPasswordResult()
}
