package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of removing a user's password.
 */
sealed class RemovePasswordResult {
    /**
     * The password was removed successfully.
     */
    data object Success : RemovePasswordResult()

    /**
     * There was an error removing the password.
     */
    data class Error(
        val error: Throwable,
    ) : RemovePasswordResult()

    /**
     * There was wrong password error removing the password.
     */
    data object WrongPasswordError : RemovePasswordResult()
}
