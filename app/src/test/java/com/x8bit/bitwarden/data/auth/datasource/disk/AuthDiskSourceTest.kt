package com.x8bit.bitwarden.data.auth.datasource.disk

import com.x8bit.bitwarden.data.platform.base.FakeSharedPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AuthDiskSourceTest {
    private val fakeSharedPreferences = FakeSharedPreferences()

    private val authDiskSource = AuthDiskSourceImpl(
        sharedPreferences = fakeSharedPreferences,
    )

    @Test
    fun `rememberedEmailAddress should pull from and update SharedPreferences`() {
        val rememberedEmailKey = "bwPreferencesStorage:rememberedEmail"

        // Shared preferences and the repository start with the same value.
        assertNull(authDiskSource.rememberedEmailAddress)
        assertNull(fakeSharedPreferences.getString(rememberedEmailKey, null))

        // Updating the repository updates shared preferences
        authDiskSource.rememberedEmailAddress = "remembered@gmail.com"
        assertEquals(
            "remembered@gmail.com",
            fakeSharedPreferences.getString(rememberedEmailKey, null),
        )

        // Update SharedPreferences updates the repository
        fakeSharedPreferences.edit().putString(rememberedEmailKey, null).apply()
        assertNull(authDiskSource.rememberedEmailAddress)
    }
}
