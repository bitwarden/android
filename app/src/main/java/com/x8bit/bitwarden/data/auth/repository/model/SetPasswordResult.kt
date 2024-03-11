package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of setting a user's password.
 */
sealed class SetPasswordResult {
    /**
     * The password was set successfully.
     */
    data object Success : SetPasswordResult()

    /**
     * There was an error setting the password.
     */
    data object Error : SetPasswordResult()
}
