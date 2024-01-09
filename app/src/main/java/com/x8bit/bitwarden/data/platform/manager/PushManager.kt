package com.x8bit.bitwarden.data.platform.manager

/**
 * Manager to handle push notification registration.
 */
interface PushManager {
    /**
     * Registers a [token] for the current user with Bitwarden's server if needed.
     */
    fun registerPushTokenIfNecessary(token: String)

    /**
     * Attempts to register a push token for the current user retrieved from storage if needed.
     */
    fun registerStoredPushTokenIfNecessary()
}
