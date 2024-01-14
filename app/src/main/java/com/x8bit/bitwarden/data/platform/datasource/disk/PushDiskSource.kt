package com.x8bit.bitwarden.data.platform.datasource.disk

import java.time.ZonedDateTime

/**
 * Primary access point for push notification information.
 */
interface PushDiskSource {
    /**
     * The currently registered GCM push token. A single token will be registered for the device,
     * regardless of the user.
     */
    var registeredPushToken: String?

    /**
     * Clears all the data for the given user.
     */
    fun clearData(userId: String)

    /**
     * Retrieves the last stored token for a user.
     */
    fun getCurrentPushToken(userId: String): String?

    /**
     * Retrieves the last time a push token was registered for a user.
     */
    fun getLastPushTokenRegistrationDate(userId: String): ZonedDateTime?

    /**
     * Sets the current token for a user.
     */
    fun storeCurrentPushToken(userId: String, pushToken: String?)

    /**
     * Sets the last push token registration date for a user.
     */
    fun storeLastPushTokenRegistrationDate(userId: String, registrationDate: ZonedDateTime?)
}
