package com.bitwarden.authenticator.data.auth.repository

import com.bitwarden.authenticator.data.auth.datasource.disk.AuthDiskSource
import com.bitwarden.core.data.manager.realtime.RealtimeManager
import javax.inject.Inject

/**
 * Default implementation of [AuthRepository].
 */
class AuthRepositoryImpl @Inject constructor(
    private val authDiskSource: AuthDiskSource,
    private val realtimeManager: RealtimeManager,
) : AuthRepository {

    /**
     * Updates the "last active time" for the current user.
     */
    override fun updateLastActiveTime() {
        authDiskSource.storeLastActiveTimeMillis(
            lastActiveTimeMillis = realtimeManager.elapsedRealtimeMs,
        )
    }
}
