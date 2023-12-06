package com.x8bit.bitwarden.data.tools.generator.datasource.disk

import com.x8bit.bitwarden.data.platform.base.FakeSharedPreferences
import com.x8bit.bitwarden.data.tools.generator.repository.model.PasscodeGenerationOptions
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GeneratorDiskSourceTest {
    private val fakeSharedPreferences = FakeSharedPreferences()

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val generatorDiskSource = GeneratorDiskSourceImpl(
        sharedPreferences = fakeSharedPreferences,
        json = json,
    )

    @Test
    fun `getPasscodeGenerationOptions should return correct options when available`() {
        val userId = "user123"
        val options = PasscodeGenerationOptions(
            length = 14,
            allowAmbiguousChar = false,
            hasNumbers = true,
            minNumber = 0,
            hasUppercase = true,
            minUppercase = null,
            hasLowercase = false,
            minLowercase = null,
            allowSpecial = false,
            minSpecial = 1,
            allowCapitalize = false,
            allowIncludeNumber = false,
            wordSeparator = "-",
            numWords = 3,
        )

        val key = "bwPreferencesStorage_passwordGenerationOptions_$userId"
        fakeSharedPreferences.edit().putString(key, json.encodeToString(options)).apply()

        val result = generatorDiskSource.getPasscodeGenerationOptions(userId)

        assertEquals(options, result)
    }

    @Test
    fun `getPasscodeGenerationOptions should return null when options are not available`() {
        val userId = "user123"

        val result = generatorDiskSource.getPasscodeGenerationOptions(userId)

        assertNull(result)
    }

    @Test
    fun `storePasscodeGenerationOptions should correctly store options`() {
        val userId = "user123"
        val options = PasscodeGenerationOptions(
            length = 14,
            allowAmbiguousChar = false,
            hasNumbers = true,
            minNumber = 0,
            hasUppercase = true,
            minUppercase = null,
            hasLowercase = false,
            minLowercase = null,
            allowSpecial = false,
            minSpecial = 1,
            allowCapitalize = false,
            allowIncludeNumber = false,
            wordSeparator = "-",
            numWords = 3,
        )

        val key = "bwPreferencesStorage_passwordGenerationOptions_$userId"

        generatorDiskSource.storePasscodeGenerationOptions(userId, options)

        val storedValue = fakeSharedPreferences.getString(key, null)
        assertNotNull(storedValue)
        assertEquals(json.encodeToString(options), storedValue)
    }
}
