package com.x8bit.bitwarden.data.auth.manager.model

/**
 * Models result of migrating existing user to key connector.
 * */
sealed class MigrateExistingUserToKeyConnectorResult {
    /**
     * Operation succeeded.
     */
    data object Success : MigrateExistingUserToKeyConnectorResult()

    /**
     * There was an error.
     */
    data class Error(
        val error: Throwable,
    ) : MigrateExistingUserToKeyConnectorResult()

    /**
     * Incorrect password provided.
     */
    data object WrongPasswordError : MigrateExistingUserToKeyConnectorResult()
}
