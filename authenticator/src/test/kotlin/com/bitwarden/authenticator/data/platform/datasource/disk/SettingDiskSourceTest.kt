package com.bitwarden.authenticator.data.platform.datasource.disk

import androidx.core.content.edit
import app.cash.turbine.test
import com.bitwarden.authenticator.ui.platform.feature.settings.data.model.DefaultSaveOption
import com.bitwarden.data.datasource.disk.base.FakeSharedPreferences
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettingDiskSourceTest {

    private val sharedPreferences: FakeSharedPreferences = FakeSharedPreferences()

    private val settingDiskSource: SettingsDiskSource = SettingsDiskSourceImpl(
        sharedPreferences = sharedPreferences,
        flightRecorderDiskSource = mockk(),
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

    @Test
    fun `hasUserDismissedSyncWithBitwardenCard should read and write from shared preferences`() {
        val sharedPrefsKey = "bwPreferencesStorage:hasUserDismissedSyncWithBitwardenCard"

        // Shared preferences and the disk source start with the same value:
        assertNull(settingDiskSource.hasUserDismissedSyncWithBitwardenCard)
        assertNull(sharedPreferences.getString(sharedPrefsKey, null))

        // Updating the disk source updates shared preferences:
        settingDiskSource.hasUserDismissedSyncWithBitwardenCard = false
        assertFalse(sharedPreferences.getBoolean(sharedPrefsKey, true))

        sharedPreferences.edit {
            putBoolean(sharedPrefsKey, true)
        }
        assertTrue(settingDiskSource.hasUserDismissedSyncWithBitwardenCard!!)
    }

    @Test
    fun `defaultSaveOption should read and write from shared preferences`() = runTest {
        val sharedPrefsKey = "bwPreferencesStorage:defaultSaveOption"

        settingDiskSource.defaultSaveOptionFlow.test {
            // Verify initial value is null and disk source should default to NONE
            assertNull(sharedPreferences.getString(sharedPrefsKey, null))
            assertEquals(
                DefaultSaveOption.NONE,
                settingDiskSource.defaultSaveOption,
            )
            assertEquals(
                DefaultSaveOption.NONE,
                awaitItem(),
            )

            // Updating the shared preferences should update disk source
            sharedPreferences.edit {
                putString(
                    sharedPrefsKey,
                    DefaultSaveOption.BITWARDEN_APP.value,
                )
            }
            assertEquals(
                DefaultSaveOption.BITWARDEN_APP,
                settingDiskSource.defaultSaveOption,
            )

            // Updating the disk source should update shared preferences
            settingDiskSource.defaultSaveOption = DefaultSaveOption.LOCAL
            assertEquals(
                DefaultSaveOption.LOCAL.value,
                sharedPreferences.getString(sharedPrefsKey, null),
            )
            assertEquals(
                DefaultSaveOption.LOCAL,
                awaitItem(),
            )

            // Incorrect value should default to DefaultSaveOption.NONE
            sharedPreferences.edit {
                putString(
                    sharedPrefsKey,
                    "invalid",
                )
            }
            assertEquals(
                DefaultSaveOption.NONE,
                settingDiskSource.defaultSaveOption,
            )
        }
    }

    @Test
    fun `previouslySyncedBitwardenAccountIds should read and write from shared preferences`() {
        val sharedPrefsKey = "bwPreferencesStorage:previouslySyncedBitwardenAccountIds"

        // Disk source should read value from shared preferences:
        sharedPreferences.edit {
            putStringSet(sharedPrefsKey, setOf("a"))
        }
        assertEquals(
            setOf("a"),
            settingDiskSource.previouslySyncedBitwardenAccountIds,
        )

        // Updating the disk source should update shared preferences:
        settingDiskSource.previouslySyncedBitwardenAccountIds = setOf("1", "2")
        assertEquals(
            setOf("1", "2"),
            settingDiskSource.previouslySyncedBitwardenAccountIds,
        )
    }
}
