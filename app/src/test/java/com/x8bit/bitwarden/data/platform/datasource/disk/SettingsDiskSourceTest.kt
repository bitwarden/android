package com.x8bit.bitwarden.data.platform.datasource.disk

import androidx.core.content.edit
import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.base.FakeSharedPreferences
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettingsDiskSourceTest {
    private val fakeSharedPreferences = FakeSharedPreferences()

    private val settingsDiskSource = SettingsDiskSourceImpl(
        sharedPreferences = fakeSharedPreferences,
    )

    @Test
    fun `appLanguage should pull from SharedPreferences`() {
        val appLanguageKey = "bwPreferencesStorage:appLocale"
        val expected = AppLanguage.AFRIKAANS

        // Verify initial value is null and disk source matches shared preferences.
        assertNull(fakeSharedPreferences.getString(appLanguageKey, null))
        assertNull(settingsDiskSource.appLanguage)

        // Updating the shared preferences should update disk source.
        fakeSharedPreferences
            .edit {
                putString(
                    appLanguageKey,
                    expected.localeName,
                )
            }
        val actual = settingsDiskSource.appLanguage
        assertEquals(
            expected,
            actual,
        )
    }

    @Test
    fun `setting appLanguage should update SharedPreferences`() {
        val appLanguageKey = "bwPreferencesStorage:appLocale"
        val appLanguage = AppLanguage.ENGLISH
        settingsDiskSource.appLanguage = appLanguage
        val actual = fakeSharedPreferences.getString(
            appLanguageKey,
            AppLanguage.DEFAULT.localeName,
        )
        assertEquals(
            appLanguage.localeName,
            actual,
        )
    }

    @Test
    fun `clearData should clear all necessary data for the given user`() {
        val userId = "userId"
        settingsDiskSource.storeVaultTimeoutInMinutes(
            userId = userId,
            vaultTimeoutInMinutes = 30,
        )
        settingsDiskSource.storeVaultTimeoutAction(
            userId = userId,
            vaultTimeoutAction = VaultTimeoutAction.LOCK,
        )
        settingsDiskSource.storePullToRefreshEnabled(
            userId = userId,
            isPullToRefreshEnabled = true,
        )
        settingsDiskSource.storeInlineAutofillEnabled(
            userId = userId,
            isInlineAutofillEnabled = true,
        )

        settingsDiskSource.clearData(userId = userId)

        assertNull(settingsDiskSource.getVaultTimeoutInMinutes(userId = userId))
        assertNull(settingsDiskSource.getVaultTimeoutAction(userId = userId))
        assertNull(settingsDiskSource.getPullToRefreshEnabled(userId = userId))
        assertNull(settingsDiskSource.getInlineAutofillEnabled(userId = userId))
    }

    @Test
    fun `isIconLoadingDisabled should pull from and update SharedPreferences`() {
        val isIconLoadingDisabled = "bwPreferencesStorage:disableFavicon"
        val expected = false

        // Assert that the default value in disk source is null
        assertNull(settingsDiskSource.isIconLoadingDisabled)

        // Updating the shared preferences should update disk source.
        fakeSharedPreferences
            .edit {
                putBoolean(
                    isIconLoadingDisabled,
                    expected,
                )
            }
        assertEquals(
            expected,
            settingsDiskSource.isIconLoadingDisabled,
        )

        // Updating the disk source updates the shared preferences
        settingsDiskSource.isIconLoadingDisabled = true
        assertTrue(
            fakeSharedPreferences.getBoolean(
                isIconLoadingDisabled, false,
            ),
        )
    }

    @Test
    fun `appTheme when values are present should pull from SharedPreferences`() {
        val appThemeBaseKey = "bwPreferencesStorage:appTheme"
        val appTheme = AppTheme.DEFAULT
        fakeSharedPreferences
            .edit {
                putString(
                    appThemeBaseKey,
                    appTheme.value,
                )
            }
        val actual = settingsDiskSource.appTheme
        assertEquals(
            appTheme,
            actual,
        )
    }

    @Test
    fun `appTheme when values are absent should return DEFAULT`() {
        assertEquals(
            AppTheme.DEFAULT,
            settingsDiskSource.appTheme,
        )
    }

    @Test
    fun `getAppThemeFlow should react to changes in getAppTheme`() = runTest {
        val appTheme = AppTheme.DARK
        settingsDiskSource.appThemeFlow.test {
            // The initial values of the Flow and the property are in sync
            assertEquals(
                AppTheme.DEFAULT,
                settingsDiskSource.appTheme,
            )
            assertEquals(
                AppTheme.DEFAULT,
                awaitItem(),
            )

            // Updating the repository updates shared preferences
            settingsDiskSource.appTheme = appTheme
            assertEquals(
                appTheme,
                awaitItem(),
            )
        }
    }

    @Test
    fun `storeAppTheme for should update SharedPreferences`() {
        val appThemeBaseKey = "bwPreferencesStorage:theme"
        val appTheme = AppTheme.DARK
        settingsDiskSource.appTheme = appTheme
        val actual = fakeSharedPreferences.getString(
            appThemeBaseKey,
            null,
        )
        assertEquals(
            appTheme.value,
            actual,
        )
    }

    @Test
    fun `getVaultTimeoutInMinutes when values are present should pull from SharedPreferences`() {
        val vaultTimeoutBaseKey = "bwPreferencesStorage:vaultTimeout"
        val mockUserId = "mockUserId"
        val vaultTimeoutInMinutes = 360
        fakeSharedPreferences
            .edit {
                putInt(
                    "${vaultTimeoutBaseKey}_$mockUserId",
                    vaultTimeoutInMinutes,
                )
            }
        val actual = settingsDiskSource.getVaultTimeoutInMinutes(userId = mockUserId)
        assertEquals(
            vaultTimeoutInMinutes,
            actual,
        )
    }

    @Test
    fun `getVaultTimeoutInMinutes when values are absent should return null`() {
        val mockUserId = "mockUserId"
        assertNull(settingsDiskSource.getVaultTimeoutInMinutes(userId = mockUserId))
    }

    @Test
    fun `getVaultTimeoutInMinutesFlow should react to changes in getVaultTimeoutInMinutes`() =
        runTest {
            val mockUserId = "mockUserId"
            val vaultTimeoutInMinutes = 360
            settingsDiskSource.getVaultTimeoutInMinutesFlow(userId = mockUserId).test {
                // The initial values of the Flow and the property are in sync
                assertNull(settingsDiskSource.getVaultTimeoutInMinutes(userId = mockUserId))
                assertNull(awaitItem())

                // Updating the repository updates shared preferences
                settingsDiskSource.storeVaultTimeoutInMinutes(
                    userId = mockUserId,
                    vaultTimeoutInMinutes = vaultTimeoutInMinutes,
                )
                assertEquals(vaultTimeoutInMinutes, awaitItem())
            }
        }

    @Test
    fun `storeVaultTimeoutInMinutes for non-null values should update SharedPreferences`() {
        val vaultTimeoutBaseKey = "bwPreferencesStorage:vaultTimeout"
        val mockUserId = "mockUserId"
        val vaultTimeoutInMinutes = 360
        settingsDiskSource.storeVaultTimeoutInMinutes(
            userId = mockUserId,
            vaultTimeoutInMinutes = vaultTimeoutInMinutes,
        )
        val actual = fakeSharedPreferences.getInt(
            "${vaultTimeoutBaseKey}_$mockUserId",
            0,
        )
        assertEquals(
            vaultTimeoutInMinutes,
            actual,
        )
    }

    @Test
    fun `storeVaultTimeoutInMinutes for null values should clear SharedPreferences`() {
        val vaultTimeoutBaseKey = "bwPreferencesStorage:vaultTimeout"
        val mockUserId = "mockUserId"
        val previousValue = 123
        val vaultTimeoutKey = "${vaultTimeoutBaseKey}_$mockUserId"
        fakeSharedPreferences.edit {
            putInt(vaultTimeoutKey, previousValue)
        }
        assertTrue(fakeSharedPreferences.contains(vaultTimeoutKey))
        settingsDiskSource.storeVaultTimeoutInMinutes(
            userId = mockUserId,
            vaultTimeoutInMinutes = null,
        )
        assertFalse(fakeSharedPreferences.contains(vaultTimeoutKey))
    }

    @Test
    fun `getVaultTimeoutAction when values are present should pull from SharedPreferences`() {
        val vaultTimeoutActionBaseKey = "bwPreferencesStorage:vaultTimeoutAction"
        val mockUserId = "mockUserId"
        val vaultTimeoutAction = VaultTimeoutAction.LOCK
        fakeSharedPreferences
            .edit {
                putString(
                    "${vaultTimeoutActionBaseKey}_$mockUserId",
                    "0",
                )
            }
        val actual = settingsDiskSource.getVaultTimeoutAction(userId = mockUserId)
        assertEquals(
            vaultTimeoutAction,
            actual,
        )
    }

    @Test
    fun `getVaultTimeoutAction when values are absent should return null`() {
        val mockUserId = "mockUserId"
        assertNull(settingsDiskSource.getVaultTimeoutAction(userId = mockUserId))
    }

    @Test
    fun `getVaultTimeoutActionFlow should react to changes in getVaultTimeoutAction`() = runTest {
        val mockUserId = "mockUserId"
        val vaultTimeoutAction = VaultTimeoutAction.LOCK
        settingsDiskSource.getVaultTimeoutActionFlow(userId = mockUserId).test {
            // The initial values of the Flow and the property are in sync
            assertNull(settingsDiskSource.getVaultTimeoutAction(userId = mockUserId))
            assertNull(awaitItem())

            // Updating the disk source updates shared preferences
            settingsDiskSource.storeVaultTimeoutAction(
                userId = mockUserId,
                vaultTimeoutAction = vaultTimeoutAction,
            )
            assertEquals(vaultTimeoutAction, awaitItem())
        }
    }

    @Test
    fun `storeVaultTimeoutAction for non-null values should update SharedPreferences`() {
        val vaultTimeoutActionBaseKey = "bwPreferencesStorage:vaultTimeoutAction"
        val mockUserId = "mockUserId"
        val vaultTimeoutAction = VaultTimeoutAction.LOCK
        settingsDiskSource.storeVaultTimeoutAction(
            userId = mockUserId,
            vaultTimeoutAction = vaultTimeoutAction,
        )
        val actual = fakeSharedPreferences.getString(
            "${vaultTimeoutActionBaseKey}_$mockUserId",
            null,
        )
        assertEquals(
            "0",
            actual,
        )
    }

    @Test
    fun `storeVaultTimeoutAction for null values should clear SharedPreferences`() {
        val vaultTimeoutActionBaseKey = "bwPreferencesStorage:vaultTimeoutAction"
        val mockUserId = "mockUserId"
        val vaultTimeoutActionKey = "${vaultTimeoutActionBaseKey}_$mockUserId"
        fakeSharedPreferences.edit {
            putString(vaultTimeoutActionKey, "0")
        }
        settingsDiskSource.storeVaultTimeoutAction(
            userId = mockUserId,
            vaultTimeoutAction = null,
        )
        assertNull(fakeSharedPreferences.getString(vaultTimeoutActionKey, null))
    }

    @Test
    fun `getPullToRefreshEnabled when values are present should pull from SharedPreferences`() {
        val pullToRefreshBaseKey = "bwPreferencesStorage:syncOnRefresh"
        val mockUserId = "mockUserId"
        val pullToRefreshKey = "${pullToRefreshBaseKey}_$mockUserId"
        fakeSharedPreferences
            .edit {
                putBoolean(pullToRefreshKey, true)
            }
        assertEquals(true, settingsDiskSource.getPullToRefreshEnabled(userId = mockUserId))
    }

    @Test
    fun `getPullToRefreshEnabled when values are absent should return null`() {
        val mockUserId = "mockUserId"
        assertNull(settingsDiskSource.getPullToRefreshEnabled(userId = mockUserId))
    }

    @Test
    fun `getPullToRefreshEnabledFlow should react to changes in getPullToRefreshEnabled`() =
        runTest {
            val mockUserId = "mockUserId"
            settingsDiskSource.getPullToRefreshEnabledFlow(userId = mockUserId).test {
                // The initial values of the Flow and the property are in sync
                assertNull(settingsDiskSource.getPullToRefreshEnabled(userId = mockUserId))
                assertNull(awaitItem())

                // Updating the disk source updates shared preferences
                settingsDiskSource.storePullToRefreshEnabled(
                    userId = mockUserId,
                    isPullToRefreshEnabled = true,
                )
                assertEquals(true, awaitItem())
            }
        }

    @Test
    fun `storePullToRefreshEnabled for non-null values should update SharedPreferences`() {
        val pullToRefreshBaseKey = "bwPreferencesStorage:syncOnRefresh"
        val mockUserId = "mockUserId"
        val pullToRefreshKey = "${pullToRefreshBaseKey}_$mockUserId"
        settingsDiskSource.storePullToRefreshEnabled(
            userId = mockUserId,
            isPullToRefreshEnabled = true,
        )
        assertTrue(fakeSharedPreferences.getBoolean(pullToRefreshKey, false))
    }

    @Test
    fun `storePullToRefreshEnabled for null values should clear SharedPreferences`() {
        val pullToRefreshBaseKey = "bwPreferencesStorage:syncOnRefresh"
        val mockUserId = "mockUserId"
        val pullToRefreshKey = "${pullToRefreshBaseKey}_$mockUserId"
        fakeSharedPreferences.edit { putBoolean(pullToRefreshKey, false) }
        settingsDiskSource.storePullToRefreshEnabled(
            userId = mockUserId,
            isPullToRefreshEnabled = null,
        )
        assertFalse(fakeSharedPreferences.contains(pullToRefreshKey))
    }

    @Test
    fun `getInlineAutofillEnabled when values are present should pull from SharedPreferences`() {
        val inlineAutofillEnabledBaseKey = "bwPreferencesStorage:inlineAutofillEnabled"
        val mockUserId = "mockUserId"
        val inlineAutofillEnabledKey = "${inlineAutofillEnabledBaseKey}_$mockUserId"
        fakeSharedPreferences
            .edit {
                putBoolean(inlineAutofillEnabledKey, true)
            }
        assertEquals(true, settingsDiskSource.getInlineAutofillEnabled(userId = mockUserId))
    }

    @Test
    fun `getInlineAutofillEnabled when values are absent should return null`() {
        val mockUserId = "mockUserId"
        assertNull(settingsDiskSource.getInlineAutofillEnabled(userId = mockUserId))
    }

    @Test
    fun `storeInlineAutofillEnabled for non-null values should update SharedPreferences`() {
        val inlineAutofillEnabledBaseKey = "bwPreferencesStorage:inlineAutofillEnabled"
        val mockUserId = "mockUserId"
        val inlineAutofillEnabledKey = "${inlineAutofillEnabledBaseKey}_$mockUserId"
        settingsDiskSource.storeInlineAutofillEnabled(
            userId = mockUserId,
            isInlineAutofillEnabled = true,
        )
        assertTrue(fakeSharedPreferences.getBoolean(inlineAutofillEnabledKey, false))
    }

    @Test
    fun `storeInlineAutofillEnabled for null values should clear SharedPreferences`() {
        val inlineAutofillEnabledBaseKey = "bwPreferencesStorage:inlineAutofillEnabled"
        val mockUserId = "mockUserId"
        val inlineAutofillEnabledKey = "${inlineAutofillEnabledBaseKey}_$mockUserId"
        fakeSharedPreferences.edit { putBoolean(inlineAutofillEnabledKey, false) }
        settingsDiskSource.storeInlineAutofillEnabled(
            userId = mockUserId,
            isInlineAutofillEnabled = null,
        )
        assertFalse(fakeSharedPreferences.contains(inlineAutofillEnabledKey))
    }
}
