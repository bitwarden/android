package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of creating a new user via SSO.
 */
sealed class NewSsoUserResult {
    /**
     * A new user has successfully been created.
     */
    data object Success : NewSsoUserResult()

    /**
     * There was an error while truing to create the new user.
     */
    data object Failure : NewSsoUserResult()
}
