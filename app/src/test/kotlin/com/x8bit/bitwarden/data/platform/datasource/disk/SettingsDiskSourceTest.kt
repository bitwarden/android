package com.x8bit.bitwarden.data.platform.datasource.disk

import androidx.core.content.edit
import app.cash.turbine.test
import com.bitwarden.core.data.util.decodeFromStringOrNull
import com.bitwarden.core.di.CoreModule
import com.bitwarden.data.datasource.disk.base.FakeSharedPreferences
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.x8bit.bitwarden.data.platform.manager.model.AppResumeScreenData
import com.x8bit.bitwarden.data.platform.repository.model.ClearClipboardFrequency
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

@Suppress("LargeClass")
class SettingsDiskSourceTest {
    private val fakeSharedPreferences = FakeSharedPreferences()
    private val json = CoreModule.providesJson()

    private val settingsDiskSource: SettingsDiskSource = SettingsDiskSourceImpl(
        sharedPreferences = fakeSharedPreferences,
        json = json,
        flightRecorderDiskSource = mockk(),
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
    fun `appLanguageFlow should react to changes in appLanguage`() = runTest {
        val appLanguage = AppLanguage.ENGLISH_BRITISH
        settingsDiskSource.appLanguageFlow.test {
            // The initial values of the Flow and the property are in sync
            assertNull(settingsDiskSource.appLanguage)
            assertNull(awaitItem())

            // Updating the repository updates shared preferences
            settingsDiskSource.appLanguage = appLanguage
            assertEquals(appLanguage, awaitItem())
        }
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
    fun `systemBiometricIntegritySource should pull from SharedPreferences`() {
        val biometricIntegritySource = "bwPreferencesStorage:biometricIntegritySource"
        val expected = "mockBiometricIntegritySource"

        // Verify initial value is null and disk source matches shared preferences.
        assertNull(fakeSharedPreferences.getString(biometricIntegritySource, null))
        assertNull(settingsDiskSource.systemBiometricIntegritySource)

        // Updating the shared preferences should update disk source.
        fakeSharedPreferences.edit {
            putString(biometricIntegritySource, expected)
        }
        val actual = settingsDiskSource.systemBiometricIntegritySource
        assertEquals(expected, actual)
    }

    @Test
    fun `setting systemBiometricIntegritySource should update SharedPreferences`() {
        val biometricIntegritySource = "bwPreferencesStorage:biometricIntegritySource"
        val expected = "mockBiometricIntegritySource"
        settingsDiskSource.systemBiometricIntegritySource = expected
        val actual = fakeSharedPreferences.getString(biometricIntegritySource, null)
        assertEquals(expected, actual)
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
        settingsDiskSource.storeAutoCopyTotpDisabled(
            userId = userId,
            isAutomaticallyCopyTotpDisabled = true,
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
        settingsDiskSource.storeLastSyncTime(
            userId = userId,
            lastSyncTime = Instant.parse("2023-10-27T12:00:00Z"),
        )
        settingsDiskSource.storeClearClipboardFrequencySeconds(userId = userId, frequency = 5)
        val systemBioIntegrityState = "system_biometrics_integrity_state"
        settingsDiskSource.storeAccountBiometricIntegrityValidity(
            userId = userId,
            systemBioIntegrityState = systemBioIntegrityState,
            value = true,
        )

        settingsDiskSource.storeShowUnlockSettingBadge(userId = userId, showBadge = true)
        settingsDiskSource.storeShowBrowserAutofillSettingBadge(userId = userId, showBadge = true)
        settingsDiskSource.storeShowAutoFillSettingBadge(userId = userId, showBadge = true)

        settingsDiskSource.clearData(userId = userId)

        // We do not clear these even when you call clear storage
        assertTrue(settingsDiskSource.getShowUnlockSettingBadge(userId = userId) ?: false)
        assertTrue(settingsDiskSource.getShowBrowserAutofillSettingBadge(userId = userId) ?: false)
        assertTrue(settingsDiskSource.getShowAutoFillSettingBadge(userId = userId) ?: false)

        // These should be cleared
        assertNull(settingsDiskSource.getVaultTimeoutInMinutes(userId = userId))
        assertNull(settingsDiskSource.getVaultTimeoutAction(userId = userId))
        assertNull(settingsDiskSource.getDefaultUriMatchType(userId = userId))
        assertNull(settingsDiskSource.getAutoCopyTotpDisabled(userId = userId))
        assertNull(settingsDiskSource.getAutofillSavePromptDisabled(userId = userId))
        assertNull(settingsDiskSource.getPullToRefreshEnabled(userId = userId))
        assertNull(settingsDiskSource.getInlineAutofillEnabled(userId = userId))
        assertNull(settingsDiskSource.getBlockedAutofillUris(userId = userId))
        assertNull(settingsDiskSource.getLastSyncTime(userId = userId))
        assertNull(settingsDiskSource.getClearClipboardFrequencySeconds(userId = userId))
        assertNull(
            settingsDiskSource.getAccountBiometricIntegrityValidity(
                userId = userId,
                systemBioIntegrityState = systemBioIntegrityState,
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getAccountBiometricIntegrityValidity should pull from and update SharedPreferences`() {
        val userId = "userId-1234"
        val systemBiometricIntegritySource = "systemValidity"
        val accountBioIntegrityValid =
            "bwPreferencesStorage:accountBiometricIntegrityValid_${userId}_$systemBiometricIntegritySource"
        val isValid = true

        // Assert that the default value in disk source is null
        assertNull(
            settingsDiskSource.getAccountBiometricIntegrityValidity(
                userId = userId,
                systemBioIntegrityState = systemBiometricIntegritySource,
            ),
        )

        // Updating the shared preferences should update disk source.
        fakeSharedPreferences.edit { putBoolean(accountBioIntegrityValid, isValid) }
        assertEquals(
            isValid,
            settingsDiskSource.getAccountBiometricIntegrityValidity(
                userId = userId,
                systemBioIntegrityState = systemBiometricIntegritySource,
            ),
        )

        // Updating the disk source updates the shared preferences
        settingsDiskSource.storeAccountBiometricIntegrityValidity(
            userId = userId,
            systemBioIntegrityState = systemBiometricIntegritySource,
            value = isValid,
        )
        assertEquals(
            fakeSharedPreferences.getBoolean(accountBioIntegrityValid, false),
            isValid,
        )
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
    fun `isCrashLoggingEnabled should pull from and update SharedPreferences`() {
        val isCrashLoggingEnabled = "bwPreferencesStorage:crashLoggingEnabled"
        val expected = false

        assertNull(settingsDiskSource.isCrashLoggingEnabled)

        fakeSharedPreferences
            .edit {
                putBoolean(
                    isCrashLoggingEnabled,
                    expected,
                )
            }
        assertEquals(
            expected,
            settingsDiskSource.isCrashLoggingEnabled,
        )

        settingsDiskSource.isCrashLoggingEnabled = true
        assertTrue(
            fakeSharedPreferences.getBoolean(
                isCrashLoggingEnabled, false,
            ),
        )
    }

    @Test
    fun `hasUserLoggedInOrCreatedAccount should pull from and update SharedPreferences`() {
        val hasUserLoggedInOrCreatedAccount = "bwPreferencesStorage:hasUserLoggedInOrCreatedAccount"
        val expected = false

        assertNull(settingsDiskSource.hasUserLoggedInOrCreatedAccount)

        fakeSharedPreferences
            .edit {
                putBoolean(
                    hasUserLoggedInOrCreatedAccount,
                    expected,
                )
            }
        assertEquals(
            expected,
            settingsDiskSource.hasUserLoggedInOrCreatedAccount,
        )

        settingsDiskSource.hasUserLoggedInOrCreatedAccount = true
        assertTrue(
            fakeSharedPreferences.getBoolean(
                hasUserLoggedInOrCreatedAccount, false,
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
    fun `isDynamicColorsEnabled should pull from and update SharedPreferences`() {
        val isDynamicColorsEnabled = "bwPreferencesStorage:isDynamicColorsEnabled"
        val expected = false

        assertNull(settingsDiskSource.isDynamicColorsEnabled)

        fakeSharedPreferences
            .edit {
                putBoolean(
                    isDynamicColorsEnabled,
                    expected,
                )
            }

        assertEquals(
            expected,
            settingsDiskSource.isDynamicColorsEnabled,
        )

        settingsDiskSource.isDynamicColorsEnabled = true
        assertTrue(
            fakeSharedPreferences.getBoolean(
                isDynamicColorsEnabled, false,
            ),
        )
    }

    @Test
    fun `isDynamicColorsEnabledFlow should react to changes in isDynamicColorsEnabled`() = runTest {
        settingsDiskSource.isDynamicColorsEnabledFlow.test {
            // The initial values of the Flow and the property are in sync
            assertNull(settingsDiskSource.isDynamicColorsEnabled)
            assertNull(awaitItem())
            settingsDiskSource.isDynamicColorsEnabled = true
            assertTrue(awaitItem() ?: false)
            settingsDiskSource.isDynamicColorsEnabled = false
            assertFalse(awaitItem() ?: true)
        }
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
                    4,
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
            4,
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
    fun `getAutoCopyTotpDisabled when values are present should pull from SharedPreferences`() {
        val disableAutoTotpCopyBaseKey = "bwPreferencesStorage:disableAutoTotpCopy"
        val mockUserId = "mockUserId"
        val disableAutoTotpCopyKey = "${disableAutoTotpCopyBaseKey}_$mockUserId"
        fakeSharedPreferences
            .edit {
                putBoolean(disableAutoTotpCopyKey, true)
            }
        assertEquals(true, settingsDiskSource.getAutoCopyTotpDisabled(userId = mockUserId))
    }

    @Test
    fun `getAutoCopyTotpDisabled when values are absent should return null`() {
        val mockUserId = "mockUserId"
        assertNull(settingsDiskSource.getAutoCopyTotpDisabled(userId = mockUserId))
    }

    @Test
    fun `storeAutoCopyTotpDisabled for non-null values should update SharedPreferences`() {
        val disableAutoTotpCopyBaseKey = "bwPreferencesStorage:disableAutoTotpCopy"
        val mockUserId = "mockUserId"
        val disableAutoTotpCopyKey = "${disableAutoTotpCopyBaseKey}_$mockUserId"
        settingsDiskSource.storeAutoCopyTotpDisabled(
            userId = mockUserId,
            isAutomaticallyCopyTotpDisabled = true,
        )
        assertTrue(fakeSharedPreferences.getBoolean(disableAutoTotpCopyKey, false))
    }

    @Test
    fun `storeAutoCopyTotpDisabled for null values should clear SharedPreferences`() {
        val disableAutoTotpCopyBaseKey = "bwPreferencesStorage:disableAutoTotpCopy"
        val mockUserId = "mockUserId"
        val disableAutoTotpCopyKey = "${disableAutoTotpCopyBaseKey}_$mockUserId"
        fakeSharedPreferences.edit { putBoolean(disableAutoTotpCopyKey, false) }
        settingsDiskSource.storeAutoCopyTotpDisabled(
            userId = mockUserId,
            isAutomaticallyCopyTotpDisabled = null,
        )
        assertFalse(fakeSharedPreferences.contains(disableAutoTotpCopyKey))
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

    @Test
    fun `getScreenCaptureAllowed should pull from SharedPreferences`() {
        val screenCaptureAllowKey = "bwPreferencesStorage:screenCaptureAllowed"
        val isScreenCaptureAllowed = true
        fakeSharedPreferences.edit {
            putBoolean(screenCaptureAllowKey, isScreenCaptureAllowed)
        }
        val actual = settingsDiskSource.screenCaptureAllowed
        assertEquals(isScreenCaptureAllowed, actual)
    }

    @Test
    fun `storeScreenCaptureAllowed for non-null values should update SharedPreferences`() {
        val screenCaptureAllowKey = "bwPreferencesStorage:screenCaptureAllowed"
        val isScreenCaptureAllowed = true
        settingsDiskSource.screenCaptureAllowed = isScreenCaptureAllowed
        val actual = fakeSharedPreferences.getBoolean(screenCaptureAllowKey, false)
        assertEquals(isScreenCaptureAllowed, actual)
    }

    @Test
    fun `storeScreenCaptureAllowed for null values should clear SharedPreferences`() {
        val screenCaptureAllowKey = "bwPreferencesStorage:screenCaptureAllowed"
        fakeSharedPreferences.edit {
            putBoolean(screenCaptureAllowKey, true)
        }
        settingsDiskSource.screenCaptureAllowed = null
        assertFalse(fakeSharedPreferences.contains(screenCaptureAllowKey))
    }

    @Test
    fun `storeClearClipboardFrequency should update SharedPreferences`() {
        val clearClipboardBaseKey = "bwPreferencesStorage:clearClipboard"
        val mockUserId = "mockUserId"
        val clearClipboardKey = "${clearClipboardBaseKey}_$mockUserId"

        assertNull(settingsDiskSource.getClearClipboardFrequencySeconds(mockUserId))
        assertEquals(fakeSharedPreferences.getInt(mockUserId, 0), 0)

        // Updating the disk source updates shared preferences
        settingsDiskSource.storeClearClipboardFrequencySeconds(
            mockUserId,
            ClearClipboardFrequency.ONE_MINUTE.frequencySeconds,
        )

        assertEquals(
            ClearClipboardFrequency.ONE_MINUTE.frequencySeconds,
            fakeSharedPreferences.getInt(clearClipboardKey, 0),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `storeClearClipboardFrequency should clear the SharedPreferences value if the value is null`() {
        val clearClipboardBaseKey = "bwPreferencesStorage:clearClipboard"
        val mockUserId = "mockUserId"
        val clearClipboardKey = "${clearClipboardBaseKey}_$mockUserId"

        assertNull(settingsDiskSource.getClearClipboardFrequencySeconds(mockUserId))
        assertEquals(fakeSharedPreferences.getInt(mockUserId, 0), 0)

        // Updating the disk source updates shared preferences
        settingsDiskSource.storeClearClipboardFrequencySeconds(
            mockUserId,
            null,
        )

        assertFalse(fakeSharedPreferences.contains(clearClipboardKey))
    }

    @Test
    fun `getClearClipboardFrequency should pull from SharedPreferences`() {
        val clearClipboardBaseKey = "bwPreferencesStorage:clearClipboard"
        val mockUserId = "mockUserId"
        val expectedValue = 20
        val clearClipboardKey = "${clearClipboardBaseKey}_$mockUserId"

        assertNull(settingsDiskSource.getClearClipboardFrequencySeconds(mockUserId))
        assertEquals(fakeSharedPreferences.getInt(mockUserId, 0), 0)

        // Update SharedPreferences updates the disk source
        fakeSharedPreferences.edit {
            putInt(clearClipboardKey, expectedValue)
        }
        assertEquals(
            expectedValue,
            settingsDiskSource.getClearClipboardFrequencySeconds(mockUserId),
        )
    }

    @Test
    fun `initialAutofillDialogShown should pull from and update SharedPreferences`() {
        val initialAutofillDialogShownKey = "bwPreferencesStorage:addSitePromptShown"
        val expectedValue = true

        assertEquals(null, settingsDiskSource.initialAutofillDialogShown)
        assertFalse(fakeSharedPreferences.getBoolean(initialAutofillDialogShownKey, false))

        // Update SharedPreferences updates the disk source
        fakeSharedPreferences.edit {
            putBoolean(initialAutofillDialogShownKey, expectedValue)
        }

        assertEquals(
            expectedValue,
            settingsDiskSource.initialAutofillDialogShown,
        )

        // Updating the disk source updates shared preferences
        settingsDiskSource.initialAutofillDialogShown = false

        assertFalse(fakeSharedPreferences.getBoolean(initialAutofillDialogShownKey, true))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `initialAutofillDialogShown should clear the SharedPreferences value if the value is null `() {
        val initialAutofillDialogShownKey = "bwPreferencesStorage:addSitePromptShown"
        val expectedValue = true

        assertEquals(null, settingsDiskSource.initialAutofillDialogShown)
        assertFalse(fakeSharedPreferences.getBoolean(initialAutofillDialogShownKey, false))

        // Update SharedPreferences updates the disk source
        fakeSharedPreferences.edit {
            putBoolean(initialAutofillDialogShownKey, expectedValue)
        }

        assertEquals(
            expectedValue,
            settingsDiskSource.initialAutofillDialogShown,
        )

        // Updating the disk source updates shared preferences
        settingsDiskSource.initialAutofillDialogShown = null

        assertFalse(fakeSharedPreferences.contains(initialAutofillDialogShownKey))
    }

    @Test
    fun `record user sign in adds value of true to shared preferences`() {
        val keyPrefix = "bwPreferencesStorage:hasUserLoggedInOrCreatedAccount"
        val mockUserId = "mockUserId"
        settingsDiskSource.storeUseHasLoggedInPreviously(userId = mockUserId)

        val actual = fakeSharedPreferences.getBoolean(
            key = "${keyPrefix}_$mockUserId",
            defaultValue = false,
        )

        assertTrue(actual)
    }

    @Test
    fun `hasUserSignedInPreviously returns true if value is present in shared preferences`() {
        val mockUserId = "mockUserId"
        fakeSharedPreferences.edit {
            putBoolean("bwPreferencesStorage:hasUserLoggedInOrCreatedAccount_$mockUserId", true)
        }

        assertTrue(settingsDiskSource.getUserHasSignedInPreviously(userId = mockUserId))
    }

    @Test
    fun `hasUserSignedInPreviously returns false if value is not present in shared preferences`() {
        assertFalse(settingsDiskSource.getUserHasSignedInPreviously(userId = "haveNotSignedIn"))
    }

    @Test
    fun `storeShowAutoFillSettingBadge should update SharedPreferences`() {
        val mockUserId = "mockUserId"
        val showAutofillSettingBadgeKey =
            "bwPreferencesStorage:showAutofillSettingBadge_$mockUserId"
        settingsDiskSource.storeShowAutoFillSettingBadge(
            userId = mockUserId,
            showBadge = true,
        )
        assertTrue(fakeSharedPreferences.getBoolean(showAutofillSettingBadgeKey, false))
    }

    @Test
    fun `getShowAutoFillSettingBadge should pull value from shared preferences`() {
        val mockUserId = "mockUserId"
        val showAutofillSettingBadgeKey =
            "bwPreferencesStorage:showAutofillSettingBadge_$mockUserId"
        fakeSharedPreferences.edit {
            putBoolean(showAutofillSettingBadgeKey, true)
        }

        assertTrue(settingsDiskSource.getShowAutoFillSettingBadge(userId = mockUserId)!!)
    }

    @Test
    fun `storeShowAutoFillSettingBadge should update the flow value`() = runTest {
        val mockUserId = "mockUserId"
        settingsDiskSource.getShowAutoFillSettingBadgeFlow(userId = mockUserId).test {
            // The initial values of the Flow are in sync
            assertFalse(awaitItem() ?: false)
            settingsDiskSource.storeShowAutoFillSettingBadge(mockUserId, true)
            assertTrue(awaitItem() ?: false)

            // update the value to false
            settingsDiskSource.storeShowAutoFillSettingBadge(
                userId = mockUserId, false,
            )
            assertFalse(awaitItem() ?: true)
        }
    }

    @Test
    fun `storeShowBrowserAutofillSettingBadge should update SharedPreferences`() {
        val mockUserId = "mockUserId"
        val showBrowserAutofillSettingBadgeKey =
            "bwPreferencesStorage:showBrowserAutofillSettingBadge_$mockUserId"
        settingsDiskSource.storeShowBrowserAutofillSettingBadge(
            userId = mockUserId,
            showBadge = true,
        )
        assertTrue(fakeSharedPreferences.getBoolean(showBrowserAutofillSettingBadgeKey, false))
    }

    @Test
    fun `getShowBrowserAutofillSettingBadge should pull value from shared preferences`() {
        val mockUserId = "mockUserId"
        val showBrowserAutofillSettingBadgeKey =
            "bwPreferencesStorage:showBrowserAutofillSettingBadge_$mockUserId"
        fakeSharedPreferences.edit {
            putBoolean(showBrowserAutofillSettingBadgeKey, true)
        }

        assertTrue(settingsDiskSource.getShowBrowserAutofillSettingBadge(userId = mockUserId)!!)
    }

    @Test
    fun `storeShowBrowserAutofillSettingBadge should update the flow value`() = runTest {
        val mockUserId = "mockUserId"
        settingsDiskSource.getShowBrowserAutofillSettingBadgeFlow(userId = mockUserId).test {
            // The initial values of the Flow are in sync
            assertFalse(awaitItem() ?: false)
            settingsDiskSource.storeShowBrowserAutofillSettingBadge(
                userId = mockUserId,
                showBadge = true,
            )
            assertTrue(awaitItem() ?: false)

            // update the value to false
            settingsDiskSource.storeShowBrowserAutofillSettingBadge(
                userId = mockUserId,
                showBadge = false,
            )
            assertFalse(awaitItem() ?: true)
        }
    }

    @Test
    fun `storeShowUnlockSettingBadge should update SharedPreferences`() {
        val mockUserId = "mockUserId"
        val showUnlockSettingBadgeKey =
            "bwPreferencesStorage:showUnlockSettingBadge_$mockUserId"
        settingsDiskSource.storeShowUnlockSettingBadge(
            userId = mockUserId,
            showBadge = true,
        )
        assertTrue(fakeSharedPreferences.getBoolean(showUnlockSettingBadgeKey, false))
    }

    @Test
    fun `getShowUnlockSettingBadge should pull value from shared preferences`() {
        val mockUserId = "mockUserId"
        val showUnlockSettingBadgeKey =
            "bwPreferencesStorage:showUnlockSettingBadge_$mockUserId"
        fakeSharedPreferences.edit {
            putBoolean(showUnlockSettingBadgeKey, true)
        }

        assertTrue(settingsDiskSource.getShowUnlockSettingBadge(userId = mockUserId)!!)
    }

    @Test
    fun `storeShowUnlockSettingsBadge should update the flow value`() = runTest {
        val mockUserId = "mockUserId"
        settingsDiskSource.getShowUnlockSettingBadgeFlow(userId = mockUserId).test {
            // The initial values of the Flow are in sync
            assertFalse(awaitItem() ?: false)
            settingsDiskSource.storeShowUnlockSettingBadge(mockUserId, true)
            assertTrue(awaitItem() ?: false)

            // update the value to false
            settingsDiskSource.storeShowUnlockSettingBadge(
                userId = mockUserId, false,
            )
            assertFalse(awaitItem() ?: true)
        }
    }

    @Test
    fun `getShowImportLoginsSettingBadge should pull from shared preferences`() {
        val mockUserId = "mockUserId"
        val showImportLoginsSettingBadgeKey =
            "bwPreferencesStorage:showImportLoginsSettingBadge_$mockUserId"
        fakeSharedPreferences.edit {
            putBoolean(showImportLoginsSettingBadgeKey, true)
        }
        assertTrue(
            settingsDiskSource.getShowImportLoginsSettingBadge(userId = mockUserId)!!,
        )
    }

    @Test
    fun `storeShowImportLoginsSettingBadge should update SharedPreferences`() {
        val mockUserId = "mockUserId"
        val showImportLoginsSettingBadgeKey =
            "bwPreferencesStorage:showImportLoginsSettingBadge_$mockUserId"
        settingsDiskSource.storeShowImportLoginsSettingBadge(mockUserId, true)
        assertTrue(
            fakeSharedPreferences.getBoolean(showImportLoginsSettingBadgeKey, false),
        )
    }

    @Test
    fun `storeShowImportLoginsSettingBadge should update the flow value`() = runTest {
        val mockUserId = "mockUserId"
        settingsDiskSource.getShowImportLoginsSettingBadgeFlow(mockUserId).test {
            // The initial values of the Flow are in sync
            assertFalse(awaitItem() ?: false)
            settingsDiskSource.storeShowImportLoginsSettingBadge(
                userId = mockUserId,
                showBadge = true,
            )
            assertTrue(awaitItem() ?: false)
            // update the value to false
            settingsDiskSource.storeShowImportLoginsSettingBadge(
                userId = mockUserId, false,
            )
            assertFalse(awaitItem() ?: true)
        }
    }

    @Test
    fun `getAppRegisteredForExport should pull from SharedPreferences`() {
        val vaultRegisteredForExportKey = "bwPreferencesStorage:isVaultRegisteredForExport"
        fakeSharedPreferences.edit {
            putBoolean(vaultRegisteredForExportKey, true)
        }
        assertTrue(settingsDiskSource.getAppRegisteredForExport()!!)
    }

    @Test
    fun `storeAppRegisteredForExport should update SharedPreferences`() {
        val vaultRegisteredForExportKey = "bwPreferencesStorage:isVaultRegisteredForExport"
        settingsDiskSource.storeAppRegisteredForExport(true)
        assertTrue(fakeSharedPreferences.getBoolean(vaultRegisteredForExportKey, false))
    }

    @Test
    fun `storeAppRegisteredForExport should update the flow value`() = runTest {
        val mockUserId = "mockUserId"
        settingsDiskSource.getAppRegisteredForExportFlow(mockUserId).test {
            // The initial values of the Flow are in sync
            assertFalse(awaitItem() ?: false)
            settingsDiskSource.storeAppRegisteredForExport(true)
            assertTrue(awaitItem() ?: false)
            // Update the value to false
            settingsDiskSource.storeAppRegisteredForExport(false)
            assertFalse(awaitItem() ?: true)
        }
    }

    @Test
    fun `getAddCipherActionCount should pull from SharedPreferences`() {
        val addActionCountKey = "bwPreferencesStorage:addActionCount"
        fakeSharedPreferences.edit { putInt(addActionCountKey, 1) }
        assertEquals(
            1, settingsDiskSource.getAddCipherActionCount(),
        )
    }

    @Test
    fun `storeAddCipherActionCount should update SharedPreferences`() {
        val addActionCountKey = "bwPreferencesStorage:addActionCount"
        settingsDiskSource.storeAddCipherActionCount(count = 1)
        assertEquals(1, fakeSharedPreferences.getInt(addActionCountKey, 0))
    }

    @Test
    fun `getCopyGeneratedResultActionCount should pull from SharedPreferences`() {
        val copyActionCountKey = "bwPreferencesStorage:copyActionCount"
        fakeSharedPreferences.edit { putInt(copyActionCountKey, 1) }
        assertEquals(
            1, settingsDiskSource.getGeneratedResultActionCount(),
        )
    }

    @Test
    fun `storeCopyGeneratedResultCount should update SharedPreferences`() {
        val copyActionCountKey = "bwPreferencesStorage:copyActionCount"
        settingsDiskSource.storeGeneratedResultActionCount(count = 1)
        assertEquals(1, fakeSharedPreferences.getInt(copyActionCountKey, 0))
    }

    @Test
    fun `getCreateSendActionCount should pull from SharedPreferences`() {
        val createActionCountKey = "bwPreferencesStorage:createActionCount"
        fakeSharedPreferences.edit { putInt(createActionCountKey, 1) }
        assertEquals(1, settingsDiskSource.getCreateSendActionCount())
    }

    @Test
    fun `storeCreateSendActionCount should update SharedPreferences`() {
        val createActionCountKey = "bwPreferencesStorage:createActionCount"
        settingsDiskSource.storeCreateSendActionCount(count = 1)
        assertEquals(1, fakeSharedPreferences.getInt(createActionCountKey, 0))
    }

    @Test
    fun `getShouldShowAddLoginCoachMark should pull value from SharedPreferences`() {
        val hasSeenAddLoginCoachMarkKey = "bwPreferencesStorage:shouldShowAddLoginCoachMark"
        fakeSharedPreferences.edit { putBoolean(hasSeenAddLoginCoachMarkKey, true) }
        assertTrue(settingsDiskSource.getShouldShowAddLoginCoachMark() == true)
    }

    @Test
    fun `storeShouldShowAddLoginCoachMark should update SharedPreferences`() {
        val hasSeenAddLoginCoachMarkKey = "bwPreferencesStorage:shouldShowAddLoginCoachMark"
        settingsDiskSource.storeShouldShowAddLoginCoachMark(shouldShow = true)
        assertTrue(
            fakeSharedPreferences.getBoolean(
                key = hasSeenAddLoginCoachMarkKey,
                defaultValue = false,
            ),
        )
    }

    @Test
    fun `getShouldShowAddLoginCoachMarkFlow emits changes to stored value`() = runTest {
        settingsDiskSource.getShouldShowAddLoginCoachMarkFlow().test {
            assertNull(awaitItem())
            settingsDiskSource.storeShouldShowAddLoginCoachMark(shouldShow = false)
            assertFalse(awaitItem() ?: true)
            settingsDiskSource.storeShouldShowAddLoginCoachMark(shouldShow = true)
            assertTrue(awaitItem() ?: false)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getShouldShowGeneratorCoachMarkGeneratorCoachMark should pull value from SharedPreferences`() {
        val hasSeenGeneratorCoachMarkKey = "bwPreferencesStorage:shouldShowGeneratorCoachMark"
        fakeSharedPreferences.edit { putBoolean(hasSeenGeneratorCoachMarkKey, true) }
        assertTrue(settingsDiskSource.getShouldShowGeneratorCoachMark() == true)
    }

    @Test
    fun `storeShouldShowGeneratorCoachMarkGeneratorCoachMark should update SharedPreferences`() {
        val hasSeenGeneratorCoachMarkKey = "bwPreferencesStorage:shouldShowGeneratorCoachMark"
        settingsDiskSource.storeShouldShowGeneratorCoachMark(shouldShow = true)
        assertTrue(
            fakeSharedPreferences.getBoolean(
                key = hasSeenGeneratorCoachMarkKey,
                defaultValue = false,
            ),
        )
    }

    @Test
    fun `getShouldShowGeneratorCoachMarkFlow emits changes to stored value`() = runTest {
        settingsDiskSource.getShouldShowGeneratorCoachMarkFlow().test {
            assertNull(awaitItem())
            settingsDiskSource.storeShouldShowGeneratorCoachMark(shouldShow = false)
            assertFalse(awaitItem() ?: true)
            settingsDiskSource.storeShouldShowGeneratorCoachMark(shouldShow = true)
            assertTrue(awaitItem() ?: false)
        }
    }

    @Test
    fun `getAppResumeScreen should pull from SharedPreferences`() {
        val mockUserId = "mockUserId"
        val resumeScreenKey = "bwPreferencesStorage:resumeScreen_$mockUserId"
        val expectedData = AppResumeScreenData.GeneratorScreen
        fakeSharedPreferences.edit {
            putString(
                resumeScreenKey,
                json.encodeToString<AppResumeScreenData>(expectedData),
            )
        }
        assertEquals(expectedData, settingsDiskSource.getAppResumeScreen(mockUserId))
    }

    @Test
    fun `storeAppResumeScreen should update SharedPreferences`() {
        val mockUserId = "mockUserId"
        val resumeScreenKey = "bwPreferencesStorage:resumeScreen_$mockUserId"
        val expectedData = AppResumeScreenData.GeneratorScreen
        settingsDiskSource.storeAppResumeScreen(mockUserId, expectedData)
        assertEquals(
            expectedData,
            fakeSharedPreferences.getString(resumeScreenKey, "")?.let {
                Json.decodeFromStringOrNull<AppResumeScreenData>(it)
            },
        )
    }

    @Test
    fun `storeAppResumeScreen should save null when passed`() {
        val mockUserId = "mockUserId"
        val resumeScreenKey = "bwPreferencesStorage:resumeScreen_$mockUserId"
        val expectedData = AppResumeScreenData.GeneratorScreen
        settingsDiskSource.storeAppResumeScreen(mockUserId, expectedData)
        assertEquals(
            expectedData,
            fakeSharedPreferences.getString(resumeScreenKey, "")?.let {
                Json.decodeFromStringOrNull<AppResumeScreenData>(it)
            },
        )
        settingsDiskSource.storeAppResumeScreen(mockUserId, null)
        assertNull(
            fakeSharedPreferences.getString(resumeScreenKey, "")?.let {
                Json.decodeFromStringOrNull<AppResumeScreenData>(it)
            },
        )
    }

    @Test
    fun `browserAutofillDialogReshowTime should pull from SharedPreferences`() {
        val browserAutofillDialogReshowTimeKey =
            "bwPreferencesStorage:browserAutofillDialogReshowTime"
        val expected = 11111L

        // Verify initial value is null and disk source matches shared preferences.
        assertNull(fakeSharedPreferences.getString(browserAutofillDialogReshowTimeKey, null))
        assertNull(settingsDiskSource.browserAutofillDialogReshowTime)

        // Updating the shared preferences should update disk source.
        fakeSharedPreferences.edit {
            putLong(browserAutofillDialogReshowTimeKey, expected)
        }
        val actual = settingsDiskSource.browserAutofillDialogReshowTime
        assertEquals(Instant.ofEpochMilli(expected), actual)
    }

    @Test
    fun `setting browserAutofillDialogReshowTime should update SharedPreferences`() {
        val browserAutofillDialogReshowTimeKey =
            "bwPreferencesStorage:browserAutofillDialogReshowTime"
        val timeMs = 1111L
        val timeInstant = Instant.ofEpochMilli(timeMs)
        settingsDiskSource.browserAutofillDialogReshowTime = timeInstant
        val actual = fakeSharedPreferences.getLong(browserAutofillDialogReshowTimeKey, 0L)
        assertEquals(timeMs, actual)
    }
}
