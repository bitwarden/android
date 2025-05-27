package com.x8bit.bitwarden.data.auth.manager

import com.x8bit.bitwarden.data.auth.manager.model.LogoutEvent
import com.x8bit.bitwarden.data.auth.repository.model.LogoutReason
import kotlinx.coroutines.flow.SharedFlow

/**
 * Manages the logging out of users and clearing of their data.
 */
interface UserLogoutManager {

    /**
     * Observable flow of [LogoutEvent]s
     */
    val logoutEventFlow: SharedFlow<LogoutEvent>

    /**
     * Completely logs out the given [userId], removing all data. The [reason] indicates why the
     * user is being logged out.
     */
    fun logout(userId: String, reason: LogoutReason)

    /**
     * Partially logs out the given [userId]. All data for the given [userId] will be removed with
     * the exception of basic account data. The [reason] indicates why the user is being logged out.
     */
    fun softLogout(userId: String, reason: LogoutReason)
}
