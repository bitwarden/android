package com.x8bit.bitwarden.data.tools.generator.datasource.disk

import android.content.SharedPreferences
import com.x8bit.bitwarden.data.platform.datasource.disk.BaseDiskSource
import com.x8bit.bitwarden.data.tools.generator.repository.model.PasswordGenerationOptions
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

    override fun getPasswordGenerationOptions(userId: String): PasswordGenerationOptions? {
        val key = getPasswordGenerationOptionsKey(userId)
        return getString(key)?.let { json.decodeFromString(it) }
    }

    override fun storePasswordGenerationOptions(
        userId: String,
        options: PasswordGenerationOptions?,
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
