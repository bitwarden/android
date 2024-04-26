package com.x8bit.bitwarden.data.tools.generator.datasource.disk

import android.content.SharedPreferences
import com.x8bit.bitwarden.data.platform.datasource.disk.BaseDiskSource
import com.x8bit.bitwarden.data.platform.util.decodeFromStringOrNull
import com.x8bit.bitwarden.data.tools.generator.repository.model.PasscodeGenerationOptions
import com.x8bit.bitwarden.data.tools.generator.repository.model.UsernameGenerationOptions
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val PASSWORD_GENERATION_OPTIONS_KEY = "passwordGenerationOptions"
private const val USERNAME_GENERATION_OPTIONS_KEY = "usernameGenerationOptions"

/**
 * Primary implementation of [GeneratorDiskSource].
 */
class GeneratorDiskSourceImpl(
    sharedPreferences: SharedPreferences,
    private val json: Json,
) : BaseDiskSource(sharedPreferences),
    GeneratorDiskSource {

    override fun clearData(userId: String) {
        storePasscodeGenerationOptions(userId = userId, options = null)
        storeUsernameGenerationOptions(userId = userId, options = null)
    }

    override fun getPasscodeGenerationOptions(userId: String): PasscodeGenerationOptions? =
        getString(PASSWORD_GENERATION_OPTIONS_KEY.appendIdentifier(userId))
            ?.let { json.decodeFromStringOrNull(it) }

    override fun storePasscodeGenerationOptions(
        userId: String,
        options: PasscodeGenerationOptions?,
    ) {
        putString(
            PASSWORD_GENERATION_OPTIONS_KEY.appendIdentifier(userId),
            options?.let { json.encodeToString(options) },
        )
    }

    override fun getUsernameGenerationOptions(userId: String): UsernameGenerationOptions? =
        getString(USERNAME_GENERATION_OPTIONS_KEY.appendIdentifier(userId))
            ?.let { json.decodeFromStringOrNull(it) }

    override fun storeUsernameGenerationOptions(
        userId: String,
        options: UsernameGenerationOptions?,
    ) {
        putString(
            USERNAME_GENERATION_OPTIONS_KEY.appendIdentifier(userId),
            options?.let { json.encodeToString(it) },
        )
    }
}
