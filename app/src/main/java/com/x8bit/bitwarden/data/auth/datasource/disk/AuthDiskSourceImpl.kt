package com.x8bit.bitwarden.data.auth.datasource.disk

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.core.content.edit
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val BASE_KEY = "bwPreferencesStorage"
private const val REMEMBERED_EMAIL_ADDRESS_KEY = "$BASE_KEY:rememberedEmail"
private const val STATE_KEY = "$BASE_KEY:state"

/**
 * Primary implementation of [AuthDiskSource].
 */
class AuthDiskSourceImpl(
    private val sharedPreferences: SharedPreferences,
    private val json: Json,
) : AuthDiskSource {
    override var rememberedEmailAddress: String?
        get() = getString(key = REMEMBERED_EMAIL_ADDRESS_KEY)
        set(value) {
            putString(
                key = REMEMBERED_EMAIL_ADDRESS_KEY,
                value = value,
            )
        }

    override var userState: UserStateJson?
        get() = getString(key = STATE_KEY)?.let { json.decodeFromString(it) }
        set(value) {
            putString(
                key = STATE_KEY,
                value = value?.let { json.encodeToString(value) },
            )
        }

    override val userStateFlow: Flow<UserStateJson?>
        get() = mutableUserStateFlow
            .onSubscription { emit(userState) }

    private val mutableUserStateFlow = MutableSharedFlow<UserStateJson?>(
        replay = 1,
        extraBufferCapacity = Int.MAX_VALUE,
    )

    private val onSharedPreferenceChangeListener =
        OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                STATE_KEY -> mutableUserStateFlow.tryEmit(userState)
            }
        }

    init {
        sharedPreferences
            .registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
    }

    private fun getString(
        key: String,
        default: String? = null,
    ): String? = sharedPreferences.getString(key, default)

    private fun putString(
        key: String,
        value: String?,
    ): Unit = sharedPreferences.edit { putString(key, value) }
}
