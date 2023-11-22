package com.x8bit.bitwarden.data.tools.generator.datasource.disk

import android.content.SharedPreferences
import com.x8bit.bitwarden.data.platform.datasource.disk.BaseDiskSource
import com.x8bit.bitwarden.data.tools.generator.repository.model.PasscodeGenerationOptions
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val PASSWORD_GENERATION_OPTIONS_KEY = "passwordGenerationOptions"

/**
 * Primary implementation of [GeneratorDiskSource].
 */
class GeneratorDiskSourceImpl(
    sharedPreferences: SharedPreferences,
    private val json: Json,
) : BaseDiskSource(sharedPreferences),
    GeneratorDiskSource {

    override fun getPasscodeGenerationOptions(userId: String): PasscodeGenerationOptions? {
        val key = getPasswordGenerationOptionsKey(userId)
        return getString(key)?.let { json.decodeFromString(it) }
    }

    override fun storePasscodeGenerationOptions(
        userId: String,
        options: PasscodeGenerationOptions?,
    ) {
        val key = getPasswordGenerationOptionsKey(userId)
        putString(
            key,
            options?.let { json.encodeToString(options) },
        )
    }

    private fun getPasswordGenerationOptionsKey(userId: String): String =
        "${BASE_KEY}_${PASSWORD_GENERATION_OPTIONS_KEY}_$userId"
}
