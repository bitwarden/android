package com.bitwarden.authenticator.data.platform.datasource.disk

import androidx.core.content.edit
import com.bitwarden.authenticator.data.platform.base.FakeSharedPreferences
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettingDiskSourceTest {

    private val sharedPreferences: FakeSharedPreferences = FakeSharedPreferences()

    private val settingDiskSource = SettingsDiskSourceImpl(
        sharedPreferences,
    )

    @Test
    fun `hasUserDismissedDownloadBitwardenCard should read and write from shared preferences`() {
        val sharedPrefsKey = "bwPreferencesStorage:hasUserDismissedDownloadBitwardenCard"

        // Shared preferences and the disk source start with the same value:
        assertNull(settingDiskSource.hasUserDismissedDownloadBitwardenCard)
        assertNull(sharedPreferences.getString(sharedPrefsKey, null))

        // Updating the disk source updates shared preferences:
        settingDiskSource.hasUserDismissedDownloadBitwardenCard = false
        assertFalse(sharedPreferences.getBoolean(sharedPrefsKey, true))

        sharedPreferences.edit {
            putBoolean(sharedPrefsKey, true)
        }
        assertTrue(settingDiskSource.hasUserDismissedDownloadBitwardenCard!!)
    }
}
