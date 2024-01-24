package com.x8bit.bitwarden.data.platform.datasource.disk

import androidx.core.content.edit
import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.base.FakeSharedPreferences
import com.x8bit.bitwarden.data.platform.datasource.network.di.PlatformNetworkModule
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

@Suppress("LargeClass")
class SettingsDiskSourceTest {
    private val fakeSharedPreferences = FakeSharedPreferences()
    private val json = PlatformNetworkModule.providesJson()

    private val settingsDiskSource = SettingsDiskSourceImpl(
        sharedPreferences = fakeSharedPreferences,
        json = json,
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
        settingsDiskSource.storeDefaultUriMatchType(
            userId = userId,
            uriMatchType = UriMatchType.REGULAR_EXPRESSION,
        )
        settingsDiskSource.storeAutofillSavePromptDisabled(
            userId = userId,
            isAutofillSavePromptDisabled = true,
        )
        settingsDiskSource.storePullToRefreshEnabled(
            userId = userId,
            isPullToRefreshEnabled = true,
        )
        settingsDiskSource.storeInlineAutofillEnabled(
            userId = userId,
            isInlineAutofillEnabled = true,
        )
        settingsDiskSource.storeBlockedAutofillUris(
            userId = userId,
            blockedAutofillUris = listOf("www.example.com"),
        )
        settingsDiskSource.storeApprovePasswordlessLoginsEnabled(
            userId = userId,
            isApprovePasswordlessLoginsEnabled = true,
        )
        settingsDiskSource.storeLastSyncTime(
            userId = userId,
            lastSyncTime = Instant.parse("2023-10-27T12:00:00Z"),
        )

        settingsDiskSource.clearData(userId = userId)

        assertNull(settingsDiskSource.getVaultTimeoutInMinutes(userId = userId))
        assertNull(settingsDiskSource.getVaultTimeoutAction(userId = userId))
        assertNull(settingsDiskSource.getDefaultUriMatchType(userId = userId))
        assertNull(settingsDiskSource.getAutofillSavePromptDisabled(userId = userId))
        assertNull(settingsDiskSource.getPullToRefreshEnabled(userId = userId))
        assertNull(settingsDiskSource.getInlineAutofillEnabled(userId = userId))
        assertNull(settingsDiskSource.getBlockedAutofillUris(userId = userId))
        assertNull(settingsDiskSource.getApprovePasswordlessLoginsEnabled(userId = userId))
        assertNull(settingsDiskSource.getLastSyncTime(userId = userId))
    }

    @Test
    fun `getLastSyncTime should pull from and update SharedPreferences`() {
        val userId = "userId-1234"
        val lastVaultSync = "bwPreferencesStorage:vaultLastSyncTime_$userId"
        val instantLong = 1_698_408_000_000L
        val instant = Instant.ofEpochMilli(instantLong)

        // Assert that the default value in disk source is null
        assertNull(settingsDiskSource.getLastSyncTime(userId = userId))

        // Updating the shared preferences should update disk source.
        fakeSharedPreferences.edit { putLong(lastVaultSync, instantLong) }
        assertEquals(instant, settingsDiskSource.getLastSyncTime(userId = userId))

        // Updating the disk source updates the shared preferences
        settingsDiskSource.storeLastSyncTime(userId = userId, lastSyncTime = instant)
        assertEquals(
            fakeSharedPreferences.getLong(lastVaultSync, 0L),
            instantLong,
        )
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
    fun `getDefaultUriMatchType when values are present should pull from SharedPreferences`() {
        val defaultUriMatchTypeBaseKey = "bwPreferencesStorage:defaultUriMatch"
        val mockUserId = "mockUserId"
        val uriMatchType = UriMatchType.REGULAR_EXPRESSION
        fakeSharedPreferences
            .edit {
                putInt(
                    "${defaultUriMatchTypeBaseKey}_$mockUserId",
                    3,
                )
            }
        val actual = settingsDiskSource.getDefaultUriMatchType(userId = mockUserId)
        assertEquals(
            uriMatchType,
            actual,
        )
    }

    @Test
    fun `getDefaultUriMatchType when values are absent should return null`() {
        val mockUserId = "mockUserId"
        assertNull(settingsDiskSource.getDefaultUriMatchType(userId = mockUserId))
    }

    @Test
    fun `storeDefaultUriMatchType for non-null values should update SharedPreferences`() {
        val defaultUriMatchTypeBaseKey = "bwPreferencesStorage:defaultUriMatch"
        val mockUserId = "mockUserId"
        val uriMatchType = UriMatchType.REGULAR_EXPRESSION
        settingsDiskSource.storeDefaultUriMatchType(
            userId = mockUserId,
            uriMatchType = uriMatchType,
        )
        val actual = fakeSharedPreferences.getInt(
            "${defaultUriMatchTypeBaseKey}_$mockUserId",
            0,
        )
        assertEquals(
            3,
            actual,
        )
    }

    @Test
    fun `storeDefaultUriMatchType for null values should clear SharedPreferences`() {
        val defaultUriMatchTypeBaseKey = "bwPreferencesStorage:defaultUriMatch"
        val mockUserId = "mockUserId"
        val defaultUriMatchTypeKey = "${defaultUriMatchTypeBaseKey}_$mockUserId"
        fakeSharedPreferences.edit {
            putInt(defaultUriMatchTypeKey, 3)
        }
        assertTrue(fakeSharedPreferences.contains(defaultUriMatchTypeKey))
        settingsDiskSource.storeDefaultUriMatchType(
            userId = mockUserId,
            uriMatchType = null,
        )
        assertFalse(fakeSharedPreferences.contains(defaultUriMatchTypeKey))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getAutofillSavePromptDisabled when values are present should pull from SharedPreferences`() {
        val disableAutofillSavePromptBaseKey = "bwPreferencesStorage:autofillDisableSavePrompt"
        val mockUserId = "mockUserId"
        val disableAutofillSavePromptKey = "${disableAutofillSavePromptBaseKey}_$mockUserId"
        fakeSharedPreferences
            .edit {
                putBoolean(disableAutofillSavePromptKey, true)
            }
        assertEquals(true, settingsDiskSource.getAutofillSavePromptDisabled(userId = mockUserId))
    }

    @Test
    fun `getAutofillSavePromptDisabled when values are absent should return null`() {
        val mockUserId = "mockUserId"
        assertNull(settingsDiskSource.getAutofillSavePromptDisabled(userId = mockUserId))
    }

    @Test
    fun `storeAutofillSavePromptDisabled for non-null values should update SharedPreferences`() {
        val disableAutofillSavePromptBaseKey = "bwPreferencesStorage:autofillDisableSavePrompt"
        val mockUserId = "mockUserId"
        val disableAutofillSavePromptKey = "${disableAutofillSavePromptBaseKey}_$mockUserId"
        settingsDiskSource.storeAutofillSavePromptDisabled(
            userId = mockUserId,
            isAutofillSavePromptDisabled = true,
        )
        assertTrue(fakeSharedPreferences.getBoolean(disableAutofillSavePromptKey, false))
    }

    @Test
    fun `storeAutofillSavePromptDisabled for null values should clear SharedPreferences`() {
        val disableAutofillSavePromptBaseKey = "bwPreferencesStorage:autofillDisableSavePrompt"
        val mockUserId = "mockUserId"
        val disableAutofillSavePromptKey = "${disableAutofillSavePromptBaseKey}_$mockUserId"
        fakeSharedPreferences.edit { putBoolean(disableAutofillSavePromptKey, false) }
        settingsDiskSource.storeAutofillSavePromptDisabled(
            userId = mockUserId,
            isAutofillSavePromptDisabled = null,
        )
        assertFalse(fakeSharedPreferences.contains(disableAutofillSavePromptKey))
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

    @Test
    fun `getBlockedAutofillUris should pull from SharedPreferences`() {
        val blockedAutofillUrisBaseKey = "bwPreferencesStorage:autofillBlacklistedUris"
        val mockUserId = "mockUserId"
        val mockBlockedAutofillUris = listOf(
            "https://www.example1.com",
            "https://www.example2.com",
        )
        fakeSharedPreferences
            .edit {
                putString(
                    "${blockedAutofillUrisBaseKey}_$mockUserId",
                    """
                    [
                      "https://www.example1.com",
                      "https://www.example2.com"
                    ]
                    """
                        .trimIndent(),
                )
            }
        val actual = settingsDiskSource.getBlockedAutofillUris(userId = mockUserId)
        assertEquals(
            mockBlockedAutofillUris,
            actual,
        )
    }

    @Test
    fun `storeBlockedAutofillUris should update SharedPreferences`() {
        val blockedAutofillUrisBaseKey = "bwPreferencesStorage:autofillBlacklistedUris"
        val mockUserId = "mockUserId"
        val mockBlockedAutofillUris = listOf(
            "https://www.example1.com",
            "https://www.example2.com",
        )
        settingsDiskSource.storeBlockedAutofillUris(
            userId = mockUserId,
            blockedAutofillUris = mockBlockedAutofillUris,
        )
        val actual = fakeSharedPreferences.getString(
            "${blockedAutofillUrisBaseKey}_$mockUserId",
            null,
        )
        assertEquals(
            json.parseToJsonElement(
                """
                [
                  "https://www.example1.com",
                  "https://www.example2.com"
                ]
                """
                    .trimIndent(),
            ),
            json.parseToJsonElement(requireNotNull(actual)),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getApprovePasswordlessLoginsEnabled when values are present should pull from SharedPreferences`() {
        val approvePasswordlessLoginsBaseKey = "bwPreferencesStorage:approvePasswordlessLogins"
        val mockUserId = "mockUserId"
        val isEnabled = true
        fakeSharedPreferences
            .edit {
                putBoolean(
                    "${approvePasswordlessLoginsBaseKey}_$mockUserId",
                    isEnabled,
                )
            }
        val actual = settingsDiskSource.getApprovePasswordlessLoginsEnabled(userId = mockUserId)
        assertEquals(
            isEnabled,
            actual,
        )
    }

    @Test
    fun `getApprovePasswordlessLoginsEnabled when values are absent should return null`() {
        val mockUserId = "mockUserId"
        assertNull(settingsDiskSource.getApprovePasswordlessLoginsEnabled(userId = mockUserId))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `storeApprovePasswordlessLoginsEnabled for non-null values should update SharedPreferences`() {
        val approvePasswordlessLoginsBaseKey = "bwPreferencesStorage:approvePasswordlessLogins"
        val mockUserId = "mockUserId"
        val isEnabled = true
        settingsDiskSource.storeApprovePasswordlessLoginsEnabled(
            userId = mockUserId,
            isApprovePasswordlessLoginsEnabled = isEnabled,
        )
        val actual = fakeSharedPreferences.getBoolean(
            "${approvePasswordlessLoginsBaseKey}_$mockUserId",
            false,
        )
        assertEquals(
            isEnabled,
            actual,
        )
    }

    @Test
    fun `storeApprovePasswordlessLoginsEnabled for null values should clear SharedPreferences`() {
        val approvePasswordlessLoginsBaseKey = "bwPreferencesStorage:approvePasswordlessLogins"
        val mockUserId = "mockUserId"
        val approvePasswordlessLoginsKey = "${approvePasswordlessLoginsBaseKey}_$mockUserId"
        fakeSharedPreferences.edit {
            putBoolean(approvePasswordlessLoginsKey, true)
        }
        settingsDiskSource.storeApprovePasswordlessLoginsEnabled(
            userId = mockUserId,
            isApprovePasswordlessLoginsEnabled = null,
        )
        assertFalse(fakeSharedPreferences.contains(approvePasswordlessLoginsKey))
    }
}
