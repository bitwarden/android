package com.x8bit.bitwarden.data.auth.manager

import com.x8bit.bitwarden.data.auth.manager.model.LogoutEvent
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
     * Completely logs out the given [userId], removing all data. If [isExpired] is true, a toast
     * will be displayed letting the user know the session has expired.
     */
    fun logout(userId: String, isExpired: Boolean = false)

    /**
     * Partially logs out the given [userId]. All data for the given [userId] will be removed with
     * the exception of basic account data. If [isExpired] is true, a toast will be displayed
     * letting the user know the session has expired.
     */
    fun softLogout(userId: String, isExpired: Boolean = false)
}
