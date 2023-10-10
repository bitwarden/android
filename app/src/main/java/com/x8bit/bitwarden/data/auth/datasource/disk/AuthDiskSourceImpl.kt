package com.x8bit.bitwarden.data.auth.datasource.disk

import android.content.SharedPreferences

private const val REMEMBERED_EMAIL_ADDRESS_KEY = "bwPreferencesStorage:rememberedEmail"

/**
 * Primary implementation of [AuthDiskSource].
 */
class AuthDiskSourceImpl(
    private val sharedPreferences: SharedPreferences,
) : AuthDiskSource {
    override var rememberedEmailAddress: String?
        get() = sharedPreferences.getString(REMEMBERED_EMAIL_ADDRESS_KEY, null)
        set(value) {
            sharedPreferences
                .edit()
                .putString(REMEMBERED_EMAIL_ADDRESS_KEY, value)
                .apply()
        }
}
