package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Describes the result of attempting to switch user accounts locally.
 */
sealed class SwitchAccountResult {

    /**
     * The user account was switched successfully.
     */
    data object AccountSwitched : SwitchAccountResult()

    /**
     * There was no change in accounts when attempting to switch users.
     */
    data object NoChange : SwitchAccountResult()
}
