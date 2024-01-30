package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of resetting a user's password.
 */
sealed class ResetPasswordResult {
    /**
     * The password was reset successfully.
     */
    data object Success : ResetPasswordResult()

    /**
     * There was an error resetting the password.
     */
    data object Error : ResetPasswordResult()
}
