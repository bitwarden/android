package com.x8bit.bitwarden.data.auth.manager

import com.x8bit.bitwarden.data.auth.repository.model.UserState
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the global state of all users.
 */
interface UserStateManager {
    /**
     * Emits updates for changes to the [UserState].
     */
    val userStateFlow: StateFlow<UserState?>

    /**
     * Tracks whether there is an additional account that is pending login/registration in order to
     * have multiple accounts available.
     *
     * This allows a direct view into and modification of [UserState.hasPendingAccountAddition].
     * Note that this call has no effect when there is no [UserState] information available.
     */
    var hasPendingAccountAddition: Boolean

    /**
     * Emits updates for changes to the [UserState.hasPendingAccountAddition] flag.
     */
    val hasPendingAccountAdditionStateFlow: StateFlow<Boolean>

    /**
     * Tracks whether there is an account that is pending deletion in order to allow the account to
     * remain active until the deletion is finalized.
     */
    var hasPendingAccountDeletion: Boolean

    /**
     * Run the given [block] while preventing any updates to [UserState]. This is useful in cases
     * where many individual changes might occur that would normally affect the [UserState] but we
     * only want a single final emission. In the rare case that multiple threads are running
     * transactions simultaneously, there will be no [UserState] updates until the last
     * transaction completes.
     */
    suspend fun <T> userStateTransaction(block: suspend () -> T): T
}
