package com.bitwarden.authenticator.data.auth.repository

import android.os.SystemClock
import com.bitwarden.authenticator.data.auth.datasource.disk.AuthDiskSource
import javax.inject.Inject

/**
 * Default implementation of [AuthRepository].
 */
class AuthRepositoryImpl @Inject constructor(
    private val authDiskSource: AuthDiskSource,
    private val elapsedRealtimeMillisProvider: () -> Long = { SystemClock.elapsedRealtime() },
) : AuthRepository {

    /**
     * Updates the "last active time" for the current user.
     */
    override fun updateLastActiveTime() {
        authDiskSource.storeLastActiveTimeMillis(
            lastActiveTimeMillis = elapsedRealtimeMillisProvider(),
        )
    }
}
