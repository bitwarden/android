package com.x8bit.bitwarden.data.platform.datasource.disk

import android.content.SharedPreferences
import com.x8bit.bitwarden.data.platform.util.getBinaryLongFromZoneDateTime
import com.x8bit.bitwarden.data.platform.util.getZoneDateTimeFromBinaryLong
import java.time.ZonedDateTime

private const val CURRENT_PUSH_TOKEN_KEY = "pushCurrentToken"
private const val LAST_REGISTRATION_DATE_KEY = "pushLastRegistrationDate"
private const val REGISTERED_PUSH_TOKEN_KEY = "pushRegisteredToken"

/**
 * Primary implementation of [PushDiskSource].
 */
class PushDiskSourceImpl(
    sharedPreferences: SharedPreferences,
) : BaseDiskSource(sharedPreferences = sharedPreferences),
    PushDiskSource {
    override var registeredPushToken: String?
        get() = getString(key = REGISTERED_PUSH_TOKEN_KEY)
        set(value) {
            putString(
                key = REGISTERED_PUSH_TOKEN_KEY,
                value = value,
            )
        }

    override fun clearData(userId: String) {
        storeCurrentPushToken(userId = userId, pushToken = null)
        storeLastPushTokenRegistrationDate(userId = userId, registrationDate = null)
    }

    override fun getCurrentPushToken(userId: String): String? {
        return getString(CURRENT_PUSH_TOKEN_KEY.appendIdentifier(userId))
    }

    override fun getLastPushTokenRegistrationDate(userId: String): ZonedDateTime? {
        return getLong(LAST_REGISTRATION_DATE_KEY.appendIdentifier(userId))
            ?.let { getZoneDateTimeFromBinaryLong(it) }
    }

    override fun storeCurrentPushToken(userId: String, pushToken: String?) {
        putString(
            key = CURRENT_PUSH_TOKEN_KEY.appendIdentifier(userId),
            value = pushToken,
        )
    }

    override fun storeLastPushTokenRegistrationDate(
        userId: String,
        registrationDate: ZonedDateTime?,
    ) {
        putLong(
            key = LAST_REGISTRATION_DATE_KEY.appendIdentifier(userId),
            value = registrationDate?.let { getBinaryLongFromZoneDateTime(registrationDate) },
        )
    }
}
