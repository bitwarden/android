package com.x8bit.bitwarden.data.platform.repository

import android.view.autofill.AutofillManager
import app.cash.turbine.test
import com.bitwarden.authenticatorbridge.util.generateSecretKey
import com.bitwarden.core.EnrollPinResponse
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.data.manager.flightrecorder.FlightRecorderManager
import com.bitwarden.network.model.KdfTypeJson
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.TrustedDeviceUserDecryptionOptionsJson
import com.bitwarden.network.model.UserDecryptionOptionsJson
import com.bitwarden.network.model.createMockPolicy
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.auth.repository.model.UserFingerprintResult
import com.x8bit.bitwarden.data.auth.repository.model.createMockVaultTimeoutPolicy
import com.x8bit.bitwarden.data.auth.repository.model.createMockVaultTimeoutPolicyJsonObject
import com.x8bit.bitwarden.data.autofill.accessibility.manager.FakeAccessibilityEnabledManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillEnabledManagerImpl
import com.x8bit.bitwarden.data.platform.datasource.disk.util.FakeSettingsDiskSource
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.repository.model.BiometricsKeyResult
import com.x8bit.bitwarden.data.platform.repository.model.ClearClipboardFrequency
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkConstructor
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZonedDateTime
import javax.crypto.Cipher

@Suppress("LargeClass")
class SettingsRepositoryTest {
    private val autofillManager: AutofillManager = mockk {
        every { disableAutofillServices() } just runs
    }
    private val autofillEnabledManager: AutofillEnabledManager = AutofillEnabledManagerImpl()
    private val fakeAccessibilityEnabledManager = FakeAccessibilityEnabledManager()
    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val fakeSettingsDiskSource = FakeSettingsDiskSource()
    private val vaultSdkSource: VaultSdkSource = mockk()
    private val mutableActivePolicyFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Policy>>()
    private val policyManager: PolicyManager = mockk {
        every {
            getActivePoliciesFlow(type = PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT)
        } returns mutableActivePolicyFlow
    }
    private val flightRecorderManager = mockk<FlightRecorderManager>()

    private val settingsRepository = SettingsRepositoryImpl(
        autofillManager = autofillManager,
        autofillEnabledManager = autofillEnabledManager,
        authDiskSource = fakeAuthDiskSource,
        settingsDiskSource = fakeSettingsDiskSource,
        vaultSdkSource = vaultSdkSource,
        accessibilityEnabledManager = fakeAccessibilityEnabledManager,
        dispatcherManager = FakeDispatcherManager(),
        policyManager = policyManager,
        flightRecorderManager = flightRecorderManager,
    )

    @BeforeEach
    fun setup() {
        mockkConstructor(NoActiveUserException::class)
        every {
            anyConstructed<NoActiveUserException>() == any<NoActiveUserException>()
        } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkConstructor(NoActiveUserException::class)
    }

    @Test
    fun `setDefaultsIfNecessary should set LOCK default values for the given user if necessary`() {
        assertNull(fakeSettingsDiskSource.getVaultTimeoutInMinutes(userId = USER_ID))
        assertNull(fakeSettingsDiskSource.getVaultTimeoutAction(userId = USER_ID))

        settingsRepository.setDefaultsIfNecessary(userId = USER_ID)

        // Calling once sets values
        assertEquals(
            15,
            fakeSettingsDiskSource.getVaultTimeoutInMinutes(userId = USER_ID),
        )
        assertEquals(
            VaultTimeoutAction.LOCK,
            fakeSettingsDiskSource.getVaultTimeoutAction(userId = USER_ID),
        )

        // Updating the Vault settings values and calling setDefaultsIfNecessary again has no effect
        // on the currently stored values.
        fakeSettingsDiskSource.apply {
            storeVaultTimeoutInMinutes(
                userId = USER_ID,
                vaultTimeoutInMinutes = 240,
            )
            storeVaultTimeoutAction(
                userId = USER_ID,
                vaultTimeoutAction = VaultTimeoutAction.LOGOUT,
            )
        }
        settingsRepository.setDefaultsIfNecessary(userId = USER_ID)
        assertEquals(
            240,
            fakeSettingsDiskSource.getVaultTimeoutInMinutes(userId = USER_ID),
        )
        assertEquals(
            VaultTimeoutAction.LOGOUT,
            fakeSettingsDiskSource.getVaultTimeoutAction(userId = USER_ID),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `setDefaultsIfNecessary should set LOCK default values for the given user with a password if necessary`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE.copy(
            accounts = mapOf(
                USER_ID to MOCK_ACCOUNT.copy(
                    profile = MOCK_PROFILE.copy(
                        userDecryptionOptions = MOCK_USER_DECRYPTION_OPTIONS.copy(
                            hasMasterPassword = true,
                        ),
                    ),
                ),
            ),
        )
        assertNull(fakeSettingsDiskSource.getVaultTimeoutInMinutes(userId = USER_ID))
        assertNull(fakeSettingsDiskSource.getVaultTimeoutAction(userId = USER_ID))

        settingsRepository.setDefaultsIfNecessary(userId = USER_ID)

        // Calling once sets values
        assertEquals(15, fakeSettingsDiskSource.getVaultTimeoutInMinutes(userId = USER_ID))
        assertEquals(
            VaultTimeoutAction.LOCK,
            fakeSettingsDiskSource.getVaultTimeoutAction(userId = USER_ID),
        )

        // Updating the Vault settings values and calling setDefaultsIfNecessary again has no
        // effect on the currently stored values.
        fakeSettingsDiskSource.apply {
            storeVaultTimeoutInMinutes(
                userId = USER_ID,
                vaultTimeoutInMinutes = 240,
            )
            storeVaultTimeoutAction(
                userId = USER_ID,
                vaultTimeoutAction = VaultTimeoutAction.LOCK,
            )
        }
        settingsRepository.setDefaultsIfNecessary(userId = USER_ID)
        assertEquals(240, fakeSettingsDiskSource.getVaultTimeoutInMinutes(userId = USER_ID))
        assertEquals(
            VaultTimeoutAction.LOCK,
            fakeSettingsDiskSource.getVaultTimeoutAction(userId = USER_ID),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `setDefaultsIfNecessary should set LOGOUT default values for the given user without a password if necessary`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        assertNull(fakeSettingsDiskSource.getVaultTimeoutInMinutes(userId = USER_ID))
        assertNull(fakeSettingsDiskSource.getVaultTimeoutAction(userId = USER_ID))

        settingsRepository.setDefaultsIfNecessary(userId = USER_ID)

        // Calling once sets values
        assertEquals(15, fakeSettingsDiskSource.getVaultTimeoutInMinutes(userId = USER_ID))
        assertEquals(
            VaultTimeoutAction.LOGOUT,
            fakeSettingsDiskSource.getVaultTimeoutAction(userId = USER_ID),
        )

        // Updating the Vault settings values and calling setDefaultsIfNecessary again has no
        // effect on the currently stored values since we have a way to unlock the vault.
        fakeAuthDiskSource.storePinProtectedUserKeyEnvelope(
            userId = USER_ID,
            pinProtectedUserKeyEnvelope = "pinProtectedKey",
        )
        fakeSettingsDiskSource.apply {
            storeVaultTimeoutInMinutes(
                userId = USER_ID,
                vaultTimeoutInMinutes = 240,
            )
            storeVaultTimeoutAction(
                userId = USER_ID,
                vaultTimeoutAction = VaultTimeoutAction.LOCK,
            )
        }
        settingsRepository.setDefaultsIfNecessary(userId = USER_ID)
        assertEquals(240, fakeSettingsDiskSource.getVaultTimeoutInMinutes(userId = USER_ID))
        assertEquals(
            VaultTimeoutAction.LOCK,
            fakeSettingsDiskSource.getVaultTimeoutAction(userId = USER_ID),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `setDefaultsIfNecessary should reset default values to LOGOUT for the given user without a password if necessary`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        assertNull(fakeSettingsDiskSource.getVaultTimeoutInMinutes(userId = USER_ID))
        assertNull(fakeSettingsDiskSource.getVaultTimeoutAction(userId = USER_ID))

        settingsRepository.setDefaultsIfNecessary(userId = USER_ID)

        // Calling once sets values
        assertEquals(15, fakeSettingsDiskSource.getVaultTimeoutInMinutes(userId = USER_ID))
        assertEquals(
            VaultTimeoutAction.LOGOUT,
            fakeSettingsDiskSource.getVaultTimeoutAction(userId = USER_ID),
        )

        // Updating the Vault settings values and calling setDefaultsIfNecessary again has no
        // effect on the currently stored values.
        fakeSettingsDiskSource.apply {
            storeVaultTimeoutInMinutes(
                userId = USER_ID,
                vaultTimeoutInMinutes = 240,
            )
            storeVaultTimeoutAction(
                userId = USER_ID,
                vaultTimeoutAction = VaultTimeoutAction.LOCK,
            )
        }
        // This will reset the setting because the user does not have a method to unlock the vault
        // so you cannot use the "lock" timeout action, it must be "logout".
        settingsRepository.setDefaultsIfNecessary(userId = USER_ID)
        assertEquals(15, fakeSettingsDiskSource.getVaultTimeoutInMinutes(userId = USER_ID))
        assertEquals(
            VaultTimeoutAction.LOGOUT,
            fakeSettingsDiskSource.getVaultTimeoutAction(userId = USER_ID),
        )
    }

    @Test
    fun `appLanguage should pull from and update SettingsDiskSource`() {
        assertEquals(
            AppLanguage.DEFAULT,
            settingsRepository.appLanguage,
        )

        // Updates to the disk source change the repository value.
        fakeSettingsDiskSource.appLanguage = AppLanguage.ENGLISH
        assertEquals(
            AppLanguage.ENGLISH,
            settingsRepository.appLanguage,
        )

        // Updates to the repository value change the disk source.
        settingsRepository.appLanguage = AppLanguage.DUTCH
        assertEquals(
            AppLanguage.DUTCH,
            fakeSettingsDiskSource.appLanguage,
        )
    }

    @Test
    fun `appLanguageStateFlow should react to changes in SettingsDiskSource`() = runTest {
        settingsRepository.appLanguageStateFlow.test {
            assertEquals(AppLanguage.DEFAULT, awaitItem())
            fakeSettingsDiskSource.appLanguage = AppLanguage.DUTCH
            assertEquals(AppLanguage.DUTCH, awaitItem())
        }
    }

    @Test
    fun `vaultLastSync should pull from and update SettingsDiskSource`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        assertNull(settingsRepository.vaultLastSync)
        val instant = Instant.ofEpochMilli(1_698_408_000_000L)

        // Updates to the disk source change the repository value.
        fakeSettingsDiskSource.storeLastSyncTime(
            userId = MOCK_USER_STATE.activeUserId,
            lastSyncTime = instant,
        )
        assertEquals(instant, settingsRepository.vaultLastSync)
    }

    @Test
    fun `vaultLastSyncStateFlow should react to changes in SettingsDiskSource`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val instant = Instant.ofEpochMilli(1_698_408_000_000L)
        settingsRepository
            .vaultLastSyncStateFlow
            .test {
                assertNull(awaitItem())
                fakeSettingsDiskSource.storeLastSyncTime(
                    userId = MOCK_USER_STATE.activeUserId,
                    lastSyncTime = instant,
                )
                assertEquals(instant, awaitItem())
            }
    }

    @Test
    fun `isIconLoadingDisabled should pull from and update SettingsDiskSource`() {
        assertFalse(settingsRepository.isIconLoadingDisabled)

        // Updates to the disk source change the repository value.
        fakeSettingsDiskSource.isIconLoadingDisabled = true
        assertTrue(settingsRepository.isIconLoadingDisabled)

        // Updates to the repository change the disk source value
        settingsRepository.isIconLoadingDisabled = false
        assertFalse(fakeSettingsDiskSource.isIconLoadingDisabled!!)
    }

    @Test
    fun `isCrashLoggingEnabled should pull from and update SettingsDiskSource`() {
        assertTrue(settingsRepository.isCrashLoggingEnabled)

        // Updates to the disk source change the repository value.
        fakeSettingsDiskSource.isCrashLoggingEnabled = false
        assertFalse(settingsRepository.isCrashLoggingEnabled)

        // Updates to the repository change the disk source value
        settingsRepository.isCrashLoggingEnabled = true
        assertTrue(fakeSettingsDiskSource.isCrashLoggingEnabled!!)
    }

    @Test
    fun `hasUserLoggedInOrCreatedAccount should pull from and update SettingsDiskSource`() {
        assertFalse(settingsRepository.hasUserLoggedInOrCreatedAccount)

        // Updates to the disk source change the repository value.
        fakeSettingsDiskSource.hasUserLoggedInOrCreatedAccount = false
        assertFalse(settingsRepository.hasUserLoggedInOrCreatedAccount)

        // Updates to the repository change the disk source value
        settingsRepository.hasUserLoggedInOrCreatedAccount = true
        assertTrue(fakeSettingsDiskSource.hasUserLoggedInOrCreatedAccount!!)
    }

    @Test
    fun `appTheme should pull from and update SettingsDiskSource`() {
        fakeAuthDiskSource.userState = null
        assertEquals(
            AppTheme.DEFAULT,
            settingsRepository.appTheme,
        )

        fakeAuthDiskSource.userState = MOCK_USER_STATE

        // Updates to the disk source change the repository value
        fakeSettingsDiskSource.appTheme = AppTheme.DARK
        assertEquals(
            AppTheme.DARK,
            settingsRepository.appTheme,
        )

        // Updates to the repository value change the disk source
        settingsRepository.appTheme = AppTheme.LIGHT
        assertEquals(
            AppTheme.LIGHT,
            fakeSettingsDiskSource.appTheme,
        )
    }

    @Test
    fun `getAppThemeFlow should react to changes in SettingsDiskSource`() = runTest {
        settingsRepository
            .appThemeStateFlow
            .test {
                assertEquals(
                    AppTheme.DEFAULT,
                    awaitItem(),
                )
                fakeSettingsDiskSource.appTheme = AppTheme.DARK
                assertEquals(
                    AppTheme.DARK,
                    awaitItem(),
                )
            }
    }

    @Test
    fun `storeAppTheme should properly update SettingsDiskSource`() {
        settingsRepository.appTheme = AppTheme.DARK
        assertEquals(
            AppTheme.DARK,
            fakeSettingsDiskSource.appTheme,
        )
    }

    @Test
    fun `isDynamicColorsEnabled should pull from and update SettingsDiskSource`() {
        assertFalse(settingsRepository.isDynamicColorsEnabled)

        // Updates to the disk source change the repository value
        fakeSettingsDiskSource.isDynamicColorsEnabled = true
        assertTrue(settingsRepository.isDynamicColorsEnabled)

        // Updates to the repository value change the disk source
        settingsRepository.isDynamicColorsEnabled = false
        assertFalse(fakeSettingsDiskSource.isDynamicColorsEnabled!!)
    }

    @Test
    fun `isDynamicColorsEnabled should react to changes in SettingsDiskSource`() = runTest {
        settingsRepository
            .isDynamicColorsEnabledFlow
            .test {
                assertFalse(awaitItem())
                fakeSettingsDiskSource.isDynamicColorsEnabled = true
                assertTrue(awaitItem())
            }
    }

    @Test
    fun `isDynamicColorsEnabled should properly update SettingsDiskSource`() {
        settingsRepository.isDynamicColorsEnabled = true
        assertTrue(fakeSettingsDiskSource.isDynamicColorsEnabled!!)
    }

    @Test
    fun `vaultTimeout should pull from and update SettingsDiskSource for the current user`() {
        fakeAuthDiskSource.userState = null
        assertEquals(
            VaultTimeout.Never,
            settingsRepository.vaultTimeout,
        )

        fakeAuthDiskSource.userState = MOCK_USER_STATE

        // Updates to the disk source change the repository value
        VAULT_TIMEOUT_MAP.forEach { (vaultTimeout, vaultTimeoutInMinutes) ->
            fakeSettingsDiskSource.storeVaultTimeoutInMinutes(
                userId = USER_ID,
                vaultTimeoutInMinutes = vaultTimeoutInMinutes,
            )
            assertEquals(
                vaultTimeout,
                settingsRepository.vaultTimeout,
            )
        }

        // Updates to the repository value change the disk source
        VAULT_TIMEOUT_MAP.forEach { (vaultTimeout, vaultTimeoutInMinutes) ->
            settingsRepository.vaultTimeout = vaultTimeout
            assertEquals(
                vaultTimeoutInMinutes,
                fakeSettingsDiskSource.getVaultTimeoutInMinutes(userId = USER_ID),
            )
        }
    }

    @Test
    fun `vaultTimeoutAction should pull from and update SettingsDiskSource`() {
        fakeAuthDiskSource.userState = null
        assertEquals(
            VaultTimeoutAction.LOCK,
            settingsRepository.vaultTimeoutAction,
        )

        fakeAuthDiskSource.userState = MOCK_USER_STATE

        // Updates to the disk source change the repository value
        VAULT_TIMEOUT_ACTIONS.forEach { vaultTimeoutAction ->
            fakeSettingsDiskSource.storeVaultTimeoutAction(
                userId = USER_ID,
                vaultTimeoutAction = vaultTimeoutAction,
            )
            assertEquals(
                vaultTimeoutAction,
                settingsRepository.vaultTimeoutAction,
            )
        }

        // Updates to the repository value change the disk source
        VAULT_TIMEOUT_ACTIONS.forEach { vaultTimeoutAction ->
            settingsRepository.vaultTimeoutAction = vaultTimeoutAction
            assertEquals(
                vaultTimeoutAction,
                fakeSettingsDiskSource.getVaultTimeoutAction(userId = USER_ID),
            )
        }
    }

    @Test
    fun `getVaultTimeoutStateFlow should react to changes in SettingsDiskSource`() = runTest {
        settingsRepository
            .getVaultTimeoutStateFlow(userId = USER_ID)
            .test {
                assertEquals(
                    VaultTimeout.Never,
                    awaitItem(),
                )
                VAULT_TIMEOUT_MAP.forEach { (vaultTimeout, vaultTimeoutInMinutes) ->
                    fakeSettingsDiskSource.storeVaultTimeoutInMinutes(
                        userId = USER_ID,
                        vaultTimeoutInMinutes = vaultTimeoutInMinutes,
                    )
                    assertEquals(
                        vaultTimeout,
                        awaitItem(),
                    )
                }
            }
    }

    @Test
    fun `storeVaultTimeout should properly update SettingsDiskSource`() {
        VAULT_TIMEOUT_MAP.forEach { (vaultTimeout, vaultTimeoutInMinutes) ->
            settingsRepository.storeVaultTimeout(
                userId = USER_ID,
                vaultTimeout = vaultTimeout,
            )
            assertEquals(
                vaultTimeoutInMinutes,
                fakeSettingsDiskSource.getVaultTimeoutInMinutes(userId = USER_ID),
            )
        }
    }

    @Test
    fun `getVaultTimeoutActionStateFlow should react to changes in SettingsDiskSource`() = runTest {
        settingsRepository
            .getVaultTimeoutActionStateFlow(userId = USER_ID)
            .test {
                assertEquals(
                    VaultTimeoutAction.LOCK,
                    awaitItem(),
                )
                VAULT_TIMEOUT_ACTIONS.forEach { vaultTimeoutAction ->
                    fakeSettingsDiskSource.storeVaultTimeoutAction(
                        userId = USER_ID,
                        vaultTimeoutAction = vaultTimeoutAction,
                    )
                    assertEquals(
                        vaultTimeoutAction,
                        awaitItem(),
                    )
                }
            }
    }

    @Test
    fun `isVaultTimeoutActionSet when no value is persisted should return false`() {
        assertFalse(settingsRepository.isVaultTimeoutActionSet(userId = USER_ID))
    }

    @Test
    fun `isVaultTimeoutActionSet when a value is persisted should return true`() {
        fakeSettingsDiskSource.storeVaultTimeoutAction(
            userId = USER_ID,
            vaultTimeoutAction = VaultTimeoutAction.LOGOUT,
        )
        assertTrue(
            settingsRepository.isVaultTimeoutActionSet(userId = USER_ID),
        )
    }

    @Test
    fun `storeVaultTimeoutAction should properly update SettingsDiskSource`() {
        VAULT_TIMEOUT_ACTIONS.forEach { vaultTimeoutAction ->
            settingsRepository.storeVaultTimeoutAction(
                userId = USER_ID,
                vaultTimeoutAction = vaultTimeoutAction,
            )
            assertEquals(
                vaultTimeoutAction,
                fakeSettingsDiskSource.getVaultTimeoutAction(userId = USER_ID),
            )
        }
    }

    @Test
    fun `defaultUriMatchType should pull from and update SettingsDiskSource`() {
        fakeAuthDiskSource.userState = null
        assertEquals(
            UriMatchType.DOMAIN,
            settingsRepository.defaultUriMatchType,
        )

        fakeAuthDiskSource.userState = MOCK_USER_STATE

        // Updates to the disk source change the repository value
        UriMatchType.entries.forEach { uriMatchType ->
            fakeSettingsDiskSource.storeDefaultUriMatchType(
                userId = USER_ID,
                uriMatchType = uriMatchType,
            )
            assertEquals(
                uriMatchType,
                settingsRepository.defaultUriMatchType,
            )
        }

        // Updates to the repository value change the disk source
        UriMatchType.entries.forEach { uriMatchType ->
            settingsRepository.defaultUriMatchType = uriMatchType
            assertEquals(
                uriMatchType,
                fakeSettingsDiskSource.getDefaultUriMatchType(userId = USER_ID),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isUnlockWithBiometricsEnabled should return a value that tracks the existence of a biometrics key for the current user`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        fakeAuthDiskSource.storeUserBiometricUnlockKey(
            userId = USER_ID,
            biometricsKey = null,
        )
        assertFalse(settingsRepository.isUnlockWithBiometricsEnabled)

        fakeAuthDiskSource.storeUserBiometricUnlockKey(
            userId = USER_ID,
            biometricsKey = "biometricsKey",
        )
        assertTrue(settingsRepository.isUnlockWithBiometricsEnabled)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isUnlockWithPinEnabled should return a value that tracks the existence of an encrypted PIN for the current user`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        fakeAuthDiskSource.storeEncryptedPin(
            userId = USER_ID,
            encryptedPin = null,
        )
        assertFalse(settingsRepository.isUnlockWithPinEnabled)

        fakeAuthDiskSource.storeEncryptedPin(
            userId = USER_ID,
            encryptedPin = "encryptedPin",
        )
        assertTrue(settingsRepository.isUnlockWithPinEnabled)
    }

    @Test
    fun `isInlineAutofillEnabled should pull from and update SettingsDiskSource`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        assertTrue(settingsRepository.isInlineAutofillEnabled)

        // Updates to the disk source change the repository value.
        fakeSettingsDiskSource.isIconLoadingDisabled = false
        assertFalse(settingsRepository.isUnlockWithPinEnabled)

        // Updates to the repository change the disk source value
        settingsRepository.isInlineAutofillEnabled = true
        assertTrue(fakeSettingsDiskSource.getInlineAutofillEnabled(userId = USER_ID)!!)
    }

    @Test
    fun `isAutoCopyTotpDisabled should pull from and update SettingsDiskSource`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        assertFalse(settingsRepository.isAutoCopyTotpDisabled)

        // Updates to the disk source change the repository value.
        fakeSettingsDiskSource.storeAutoCopyTotpDisabled(
            userId = USER_ID,
            isAutomaticallyCopyTotpDisabled = true,
        )
        assertTrue(settingsRepository.isAutoCopyTotpDisabled)

        // Updates to the repository change the disk source value
        settingsRepository.isAutoCopyTotpDisabled = false
        assertFalse(fakeSettingsDiskSource.getAutoCopyTotpDisabled(userId = USER_ID)!!)
    }

    @Test
    fun `isAutofillSavePromptDisabled should pull from and update SettingsDiskSource`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        assertFalse(settingsRepository.isAutofillSavePromptDisabled)

        // Updates to the disk source change the repository value.
        fakeSettingsDiskSource.storeAutofillSavePromptDisabled(
            userId = USER_ID,
            isAutofillSavePromptDisabled = true,
        )
        assertTrue(settingsRepository.isAutofillSavePromptDisabled)

        // Updates to the repository change the disk source value
        settingsRepository.isAutofillSavePromptDisabled = false
        assertFalse(fakeSettingsDiskSource.getAutofillSavePromptDisabled(userId = USER_ID)!!)
    }

    @Test
    fun `blockedAutofillUris should pull from and update SettingsDiskSource`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        assertEquals(
            emptyList<String>(),
            settingsRepository.blockedAutofillUris,
        )

        // Updates to the disk source change the repository value.
        fakeSettingsDiskSource.storeBlockedAutofillUris(
            userId = USER_ID,
            blockedAutofillUris = listOf(
                "https://www.example1.com",
                "https://www.example2.com",
            ),
        )
        assertEquals(
            listOf(
                "https://www.example1.com",
                "https://www.example2.com",
            ),
            settingsRepository.blockedAutofillUris,
        )

        // Updates to the repository change the disk source value
        settingsRepository.blockedAutofillUris = emptyList()
        assertEquals(
            emptyList<String>(),
            fakeSettingsDiskSource.getBlockedAutofillUris(userId = USER_ID),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isAccessibilityEnabledStateFlow should emit whenever the accessibilityEnabledManager does`() =
        runTest {
            settingsRepository.isAccessibilityEnabledStateFlow.test {
                assertFalse(awaitItem())

                fakeAccessibilityEnabledManager.isAccessibilityEnabled = true
                assertTrue(awaitItem())

                fakeAccessibilityEnabledManager.isAccessibilityEnabled = false
                assertFalse(awaitItem())
            }
        }

    @Test
    fun `isAutofillEnabledStateFlow should emit whenever the AutofillEnabledManager does`() =
        runTest {
            settingsRepository.isAutofillEnabledStateFlow.test {
                assertFalse(awaitItem())

                autofillEnabledManager.isAutofillEnabled = true
                assertTrue(awaitItem())

                autofillEnabledManager.isAutofillEnabled = false
                assertFalse(awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `disableAutofill should trigger an emission of false from isAutofillEnabledStateFlow and disable autofill with the OS`() =
        runTest {
            // Start in a state where autofill is enabled
            autofillEnabledManager.isAutofillEnabled = true
            settingsRepository.isAutofillEnabledStateFlow.test {
                assertTrue(awaitItem())
                expectNoEvents()
            }

            settingsRepository.disableAutofill()

            assertFalse(settingsRepository.isAutofillEnabledStateFlow.value)
            assertFalse(autofillEnabledManager.isAutofillEnabled)
            verify { autofillManager.disableAutofillServices() }
        }

    @Test
    fun `getUserFingerprint should return failure with no active user`() = runTest {
        fakeAuthDiskSource.userState = null

        val result = settingsRepository.getUserFingerprint()

        assertEquals(UserFingerprintResult.Error(error = NoActiveUserException()), result)
    }

    @Test
    fun `getUserFingerprint should return failure with active user when source returns failure`() =
        runTest {
            val error = Throwable("Fail!")
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultSdkSource.getUserFingerprint(userId = USER_ID)
            } returns error.asFailure()

            val result = settingsRepository.getUserFingerprint()

            coVerify(exactly = 1) {
                vaultSdkSource.getUserFingerprint(userId = USER_ID)
            }
            assertEquals(UserFingerprintResult.Error(error = error), result)
        }

    @Test
    fun `getUserFingerprint should return success with active user when source returns success`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val fingerprint = "fingerprint"
            coEvery {
                vaultSdkSource.getUserFingerprint(userId = USER_ID)
            } returns fingerprint.asSuccess()

            val result = settingsRepository.getUserFingerprint()

            coVerify(exactly = 1) {
                vaultSdkSource.getUserFingerprint(userId = USER_ID)
            }
            assertEquals(UserFingerprintResult.Success(fingerprint), result)
        }

    @Test
    fun `getPullToRefreshEnabledFlow should react to changes in SettingsDiskSource`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        settingsRepository
            .getPullToRefreshEnabledFlow()
            .test {
                assertFalse(awaitItem())
                fakeSettingsDiskSource.storePullToRefreshEnabled(
                    userId = USER_ID,
                    isPullToRefreshEnabled = true,
                )
                assertTrue(awaitItem())
                fakeSettingsDiskSource.storePullToRefreshEnabled(
                    userId = USER_ID,
                    isPullToRefreshEnabled = false,
                )
                assertFalse(awaitItem())
            }
    }

    @Test
    fun `storePullToRefreshEnabled should properly update SettingsDiskSource`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        settingsRepository.storePullToRefreshEnabled(true)
        assertEquals(true, fakeSettingsDiskSource.getPullToRefreshEnabled(userId = USER_ID))
    }

    @Test
    fun `setupBiometricsKey with missing user state should return BiometricsKeyResult Error`() =
        runTest {
            val cipher = mockk<Cipher>()
            fakeAuthDiskSource.userState = null

            val result = settingsRepository.setupBiometricsKey(cipher = cipher)

            assertEquals(BiometricsKeyResult.Error(error = NoActiveUserException()), result)
            coVerify(exactly = 0) {
                vaultSdkSource.getUserEncryptionKey(userId = any())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `setupBiometricsKey with getUserEncryptionKey failure should return BiometricsKeyResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val cipher = mockk<Cipher>()
            val error = Throwable("Fail")
            coEvery {
                vaultSdkSource.getUserEncryptionKey(userId = USER_ID)
            } returns error.asFailure()

            val result = settingsRepository.setupBiometricsKey(cipher = cipher)

            assertEquals(BiometricsKeyResult.Error(error = error), result)
            coVerify(exactly = 1) {
                vaultSdkSource.getUserEncryptionKey(userId = USER_ID)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `setupBiometricsKey with getUserEncryptionKey success should return BiometricsKeyResult Success`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val encryptedKey = "asdf1234"
            val encryptedBytes = byteArrayOf(1, 1)
            val initVector = byteArrayOf(2, 2)
            val cipher = mockk<Cipher> {
                every { doFinal(any()) } returns encryptedBytes
                every { iv } returns initVector
            }
            coEvery {
                vaultSdkSource.getUserEncryptionKey(userId = USER_ID)
            } returns encryptedKey.asSuccess()

            val result = settingsRepository.setupBiometricsKey(cipher = cipher)

            assertEquals(BiometricsKeyResult.Success, result)
            fakeAuthDiskSource.assertBiometricsKey(
                userId = USER_ID,
                biometricsKey = encryptedBytes.toString(Charsets.ISO_8859_1),
            )
            fakeAuthDiskSource.assertBiometricInitVector(userId = USER_ID, iv = initVector)
            coVerify(exactly = 1) {
                vaultSdkSource.getUserEncryptionKey(userId = USER_ID)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `storeUnlockPin when the master password on restart is required should only save an encrypted PIN to disk`() {
        val pin = "1234"
        val userKeyEncryptedPin = "encryptedPin"
        val pinProtectedUserKeyEnvelope = "pinProtectedUserKeyEnvelope"
        val enrollResponse = EnrollPinResponse(
            pinProtectedUserKeyEnvelope = pinProtectedUserKeyEnvelope,
            userKeyEncryptedPin = userKeyEncryptedPin,
        )
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery {
            vaultSdkSource.enrollPin(
                userId = USER_ID,
                pin = pin,
            )
        } returns enrollResponse.asSuccess()

        settingsRepository.storeUnlockPin(
            pin = pin,
            shouldRequireMasterPasswordOnRestart = true,
        )

        fakeAuthDiskSource.apply {
            assertEncryptedPin(
                userId = USER_ID,
                encryptedPin = userKeyEncryptedPin,
            )
            assertPinProtectedUserKeyEnvelope(
                userId = USER_ID,
                pinProtectedUserKeyEnvelope = pinProtectedUserKeyEnvelope,
                inMemoryOnly = true,
            )
        }
        coVerify {
            vaultSdkSource.enrollPin(
                userId = USER_ID,
                pin = pin,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `storeUnlockPin when the master password on restart is not required should save all PIN data to disk`() {
        val pin = "1234"
        val userKeyEncryptedPin = "encryptedPin"
        val pinProtectedUserKeyEnvelope = "pinProtectedUserKeyEnvelope"
        val enrollResponse = EnrollPinResponse(
            pinProtectedUserKeyEnvelope = pinProtectedUserKeyEnvelope,
            userKeyEncryptedPin = userKeyEncryptedPin,
        )
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery {
            vaultSdkSource.enrollPin(
                userId = USER_ID,
                pin = pin,
            )
        } returns enrollResponse.asSuccess()

        settingsRepository.storeUnlockPin(
            pin = pin,
            shouldRequireMasterPasswordOnRestart = false,
        )

        fakeAuthDiskSource.apply {
            assertEncryptedPin(
                userId = USER_ID,
                encryptedPin = userKeyEncryptedPin,
            )
            assertPinProtectedUserKeyEnvelope(
                userId = USER_ID,
                pinProtectedUserKeyEnvelope = pinProtectedUserKeyEnvelope,
                inMemoryOnly = false,
            )
        }
        coVerify {
            vaultSdkSource.enrollPin(
                userId = USER_ID,
                pin = pin,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clearUnlockPin should clear any previously stored PIN-related values for the current user`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        fakeAuthDiskSource.apply {
            storeEncryptedPin(
                userId = USER_ID,
                encryptedPin = "encryptedPin",
            )
            storePinProtectedUserKeyEnvelope(
                userId = USER_ID,
                pinProtectedUserKeyEnvelope = "pinProtectedUserKeyEnvelope",
            )
        }

        settingsRepository.clearUnlockPin()

        fakeAuthDiskSource.apply {
            assertEncryptedPin(
                userId = USER_ID,
                encryptedPin = null,
            )
            assertPinProtectedUserKeyEnvelope(
                userId = USER_ID,
                pinProtectedUserKeyEnvelope = null,
            )
        }
    }

    @Test
    fun `isScreenCaptureAllowed property should update SettingsDiskSource and emit changes`() =
        runTest {
            fakeSettingsDiskSource.screenCaptureAllowed = false
            settingsRepository.isScreenCaptureAllowedStateFlow.test {
                assertFalse(awaitItem())

                settingsRepository.isScreenCaptureAllowed = true
                assertTrue(awaitItem())

                assertEquals(true, fakeSettingsDiskSource.screenCaptureAllowed)

                settingsRepository.isScreenCaptureAllowed = false
                assertFalse(awaitItem())

                assertEquals(false, fakeSettingsDiskSource.screenCaptureAllowed)
            }
        }

    @Test
    fun `clearClipboardFrequency should pull from and update SettingsDiskSource`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        fakeSettingsDiskSource.storeClearClipboardFrequencySeconds(
            USER_ID,
            ClearClipboardFrequency.ONE_MINUTE.frequencySeconds,
        )

        assertEquals(
            ClearClipboardFrequency.ONE_MINUTE,
            settingsRepository.clearClipboardFrequency,
        )

        settingsRepository.clearClipboardFrequency = ClearClipboardFrequency.TEN_SECONDS

        assertEquals(
            ClearClipboardFrequency.TEN_SECONDS,
            settingsRepository.clearClipboardFrequency,
        )
    }

    @Test
    fun `initialAutofillDialogShown should pull from and update SettingsDiskSource`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        fakeSettingsDiskSource.initialAutofillDialogShown = true
        assertTrue(settingsRepository.initialAutofillDialogShown)

        settingsRepository.initialAutofillDialogShown = false
        assertEquals(false, fakeSettingsDiskSource.initialAutofillDialogShown)
    }

    @Test
    fun `isAuthenticatorSyncEnabled should default to false`() {
        assertFalse(settingsRepository.isAuthenticatorSyncEnabled)
    }

    @Test
    fun `getUserHasLoggedInValue should default to false if no value exists`() {
        assertFalse(settingsRepository.getUserHasLoggedInValue(userId = "userId"))
    }

    @Test
    fun `getUserHasLoggedInValue should return true if it exists`() {
        val userId = "userId"
        fakeSettingsDiskSource.storeUseHasLoggedInPreviously(userId = userId)
        assertTrue(settingsRepository.getUserHasLoggedInValue(userId = userId))
    }

    @Test
    fun `storeUserHasLoggedInValue should store value of true to disk`() {
        val userId = "userId"
        settingsRepository.storeUserHasLoggedInValue(userId = userId)
        assertTrue(fakeSettingsDiskSource.getUserHasSignedInPreviously(userId = userId))
    }

    @Test
    @Suppress("MaxLineLength")
    fun `isAuthenticatorSyncEnabled set to true should generate an authenticator sync key and also a symmetric key if none exists`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery { vaultSdkSource.getUserEncryptionKey(USER_ID) }
                .returns(AUTHENTICATION_SYNC_KEY.asSuccess())
            fakeAuthDiskSource.authenticatorSyncSymmetricKey = null
            assertNull(fakeAuthDiskSource.getAuthenticatorSyncUnlockKey(USER_ID))

            settingsRepository.isAuthenticatorSyncEnabled = true

            assertTrue(settingsRepository.isAuthenticatorSyncEnabled)
            assertEquals(
                AUTHENTICATION_SYNC_KEY,
                fakeAuthDiskSource.getAuthenticatorSyncUnlockKey(USER_ID),
            )
            assertNotNull(fakeAuthDiskSource.authenticatorSyncSymmetricKey)
            coVerify { vaultSdkSource.getUserEncryptionKey(USER_ID) }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `isAuthenticatorSyncEnabled set to true should generate an authenticator sync key and leave symmetric key untouched if already set`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery { vaultSdkSource.getUserEncryptionKey(USER_ID) }
                .returns(AUTHENTICATION_SYNC_KEY.asSuccess())
            val symmetricKey = generateSecretKey().getOrThrow().encoded
            fakeAuthDiskSource.authenticatorSyncSymmetricKey = symmetricKey
            assertNull(fakeAuthDiskSource.getAuthenticatorSyncUnlockKey(USER_ID))

            settingsRepository.isAuthenticatorSyncEnabled = true

            assertTrue(settingsRepository.isAuthenticatorSyncEnabled)
            assertEquals(
                AUTHENTICATION_SYNC_KEY,
                fakeAuthDiskSource.getAuthenticatorSyncUnlockKey(USER_ID),
            )
            fakeAuthDiskSource.authenticatorSyncSymmetricKey.contentEquals(symmetricKey)
            coVerify { vaultSdkSource.getUserEncryptionKey(USER_ID) }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `isAuthenticatorSyncEnabled set to false should clear authenticator sync key and leave symmetric sync key untouched`() =
        runTest {
            val syncSymmetricKey = generateSecretKey().getOrThrow().encoded
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            fakeAuthDiskSource.authenticatorSyncSymmetricKey = syncSymmetricKey
            fakeAuthDiskSource.storeAuthenticatorSyncUnlockKey(USER_ID, AUTHENTICATION_SYNC_KEY)

            assertTrue(settingsRepository.isAuthenticatorSyncEnabled)

            settingsRepository.isAuthenticatorSyncEnabled = false

            assertFalse(settingsRepository.isAuthenticatorSyncEnabled)
            assertNull(fakeAuthDiskSource.getAuthenticatorSyncUnlockKey(USER_ID))
            assertTrue(
                fakeAuthDiskSource.authenticatorSyncSymmetricKey.contentEquals(syncSymmetricKey),
            )
        }

    @Test
    fun `isAuthenticatorSyncEnabled should be true when there exists an authenticator sync key`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            assertFalse(settingsRepository.isAuthenticatorSyncEnabled)
            fakeAuthDiskSource.storeAuthenticatorSyncUnlockKey(
                userId = USER_ID,
                authenticatorSyncUnlockKey = "fakeKey",
            )
            assertTrue(settingsRepository.isAuthenticatorSyncEnabled)
        }

    @Test
    fun `isAuthenticatorSyncEnabled should be false when there is no active user`() =
        runTest {
            fakeAuthDiskSource.userState = null
            assertFalse(settingsRepository.isAuthenticatorSyncEnabled)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `isAuthenticatorSyncEnabled should be false when the active user has no authenticator sync key set`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            assertFalse(settingsRepository.isAuthenticatorSyncEnabled)
        }

    @Test
    fun `isUnlockWithBiometricsEnabledFlow should react to changes in AuthDiskSource`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        settingsRepository.isUnlockWithBiometricsEnabledFlow.test {
            assertFalse(awaitItem())
            fakeAuthDiskSource.storeUserBiometricUnlockKey(
                userId = USER_ID,
                biometricsKey = "biometricsKey",
            )
            assertTrue(awaitItem())
            fakeAuthDiskSource.storeUserBiometricUnlockKey(
                userId = USER_ID,
                biometricsKey = null,
            )
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `isUnlockWithPinEnabledFlow should react to changes in AuthDiskSource`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        settingsRepository.isUnlockWithPinEnabledFlow.test {
            assertFalse(awaitItem())
            fakeAuthDiskSource.storePinProtectedUserKey(
                userId = USER_ID,
                pinProtectedUserKey = "pinProtectedUserKey",
            )
            assertTrue(awaitItem())
            fakeAuthDiskSource.storePinProtectedUserKey(
                userId = USER_ID,
                pinProtectedUserKey = null,
            )
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `isAppRegisteredForExport should return false if no value exists`() {
        assertFalse(settingsRepository.isAppRegisteredForExport())
    }

    @Test
    fun `isAppRegisteredForExport should return true if it exists`() {
        fakeSettingsDiskSource.storeAppRegisteredForExport(isRegistered = true)
        assertTrue(settingsRepository.isAppRegisteredForExport())
    }

    @Test
    fun `storeAppRegisteredForExport should store value of true to disk`() {
        settingsRepository.storeAppRegisteredForExport(isRegistered = true)
        assertTrue(fakeSettingsDiskSource.getAppRegisteredForExport())
    }

    @Test
    fun `getAppRegisteredForExportFlow should react to changes in SettingsDiskSource`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            settingsRepository
                .getAppRegisteredForExportFlow(userId = USER_ID)
                .test {
                    assertFalse(awaitItem())
                    fakeSettingsDiskSource.storeAppRegisteredForExport(
                        isRegistered = true,
                    )
                    assertTrue(awaitItem())
                    fakeSettingsDiskSource.storeAppRegisteredForExport(
                        isRegistered = false,
                    )
                    assertFalse(awaitItem())
                }
        }

    @Test
    fun `mutableActivePolicyFlow without vault timeout policy should do nothing`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            fakeSettingsDiskSource.assertVaultTimeoutAction(userId = USER_ID, expected = null)
            fakeSettingsDiskSource.assertVaultTimeoutInMinutes(userId = USER_ID, expected = null)

            mutableActivePolicyFlow.emit(listOf(createMockPolicy()))
            fakeSettingsDiskSource.assertVaultTimeoutAction(userId = USER_ID, expected = null)
            fakeSettingsDiskSource.assertVaultTimeoutInMinutes(userId = USER_ID, expected = null)
        }

    @Test
    fun `mutableActivePolicyFlow emissions should update the vault timeout action accordingly`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            fakeSettingsDiskSource.assertVaultTimeoutAction(userId = USER_ID, expected = null)

            val lockPolicy = createMockPolicy(
                type = PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT,
                data = createMockVaultTimeoutPolicyJsonObject(
                    vaultTimeout = createMockVaultTimeoutPolicy(
                        action = PolicyInformation.VaultTimeout.Action.LOCK,
                    ),
                ),
            )
            mutableActivePolicyFlow.emit(listOf(lockPolicy))
            fakeSettingsDiskSource.assertVaultTimeoutAction(
                userId = USER_ID,
                expected = VaultTimeoutAction.LOCK,
            )

            val logoutPolicy = createMockPolicy(
                type = PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT,
                data = createMockVaultTimeoutPolicyJsonObject(
                    vaultTimeout = createMockVaultTimeoutPolicy(
                        action = PolicyInformation.VaultTimeout.Action.LOGOUT,
                    ),
                ),
            )
            mutableActivePolicyFlow.emit(listOf(logoutPolicy))
            fakeSettingsDiskSource.assertVaultTimeoutAction(
                userId = USER_ID,
                expected = VaultTimeoutAction.LOGOUT,
            )
        }

    @Test
    fun `mutableActivePolicyFlow emissions should update the vault timeout accordingly`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            fakeSettingsDiskSource.assertVaultTimeoutInMinutes(userId = USER_ID, expected = null)
            // Storing a larger value so we can verify it changes properly
            fakeSettingsDiskSource.storeVaultTimeoutInMinutes(
                userId = USER_ID,
                vaultTimeoutInMinutes = 100,
            )

            val nullTypeWithMinutesPolicy = createMockPolicy(
                type = PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT,
                data = createMockVaultTimeoutPolicyJsonObject(
                    vaultTimeout = createMockVaultTimeoutPolicy(minutes = 5),
                ),
            )
            mutableActivePolicyFlow.emit(listOf(nullTypeWithMinutesPolicy))
            fakeSettingsDiskSource.assertVaultTimeoutInMinutes(userId = USER_ID, expected = 5)

            val customMinutesPolicy = createMockPolicy(
                type = PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT,
                data = createMockVaultTimeoutPolicyJsonObject(
                    vaultTimeout = createMockVaultTimeoutPolicy(
                        minutes = 10,
                        type = PolicyInformation.VaultTimeout.Type.CUSTOM,
                    ),
                ),
            )
            mutableActivePolicyFlow.emit(listOf(customMinutesPolicy))
            // No change since 5 is within the range
            fakeSettingsDiskSource.assertVaultTimeoutInMinutes(userId = USER_ID, expected = 5)

            val onSystemLockPolicy = createMockPolicy(
                type = PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT,
                data = createMockVaultTimeoutPolicyJsonObject(
                    vaultTimeout = createMockVaultTimeoutPolicy(
                        minutes = 10,
                        type = PolicyInformation.VaultTimeout.Type.ON_SYSTEM_LOCK,
                    ),
                ),
            )
            mutableActivePolicyFlow.emit(listOf(onSystemLockPolicy))
            // Set to on app restart
            fakeSettingsDiskSource.assertVaultTimeoutInMinutes(userId = USER_ID, expected = -1)

            val onAppRestartPolicy = createMockPolicy(
                type = PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT,
                data = createMockVaultTimeoutPolicyJsonObject(
                    vaultTimeout = createMockVaultTimeoutPolicy(
                        minutes = 10,
                        type = PolicyInformation.VaultTimeout.Type.ON_APP_RESTART,
                    ),
                ),
            )
            mutableActivePolicyFlow.emit(listOf(onAppRestartPolicy))
            // No change, ON_APP_RESTART is treated the same as ON_SYSTEM_RESTART
            fakeSettingsDiskSource.assertVaultTimeoutInMinutes(userId = USER_ID, expected = -1)

            val immediatePolicy = createMockPolicy(
                type = PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT,
                data = createMockVaultTimeoutPolicyJsonObject(
                    vaultTimeout = createMockVaultTimeoutPolicy(
                        minutes = 10,
                        type = PolicyInformation.VaultTimeout.Type.IMMEDIATELY,
                    ),
                ),
            )
            mutableActivePolicyFlow.emit(listOf(immediatePolicy))
            // Set to Immediate
            fakeSettingsDiskSource.assertVaultTimeoutInMinutes(userId = USER_ID, expected = 0)

            val neverPolicy = createMockPolicy(
                type = PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT,
                data = createMockVaultTimeoutPolicyJsonObject(
                    vaultTimeout = createMockVaultTimeoutPolicy(
                        minutes = 10,
                        type = PolicyInformation.VaultTimeout.Type.NEVER,
                    ),
                ),
            )
            mutableActivePolicyFlow.emit(listOf(neverPolicy))
            // Clear the value completely
            fakeSettingsDiskSource.assertVaultTimeoutInMinutes(userId = USER_ID, expected = null)
        }
}

private const val USER_ID: String = "userId"
private const val AUTHENTICATION_SYNC_KEY = "authSyncKey"

private val MOCK_TRUSTED_DEVICE_USER_DECRYPTION_OPTIONS =
    TrustedDeviceUserDecryptionOptionsJson(
        encryptedPrivateKey = null,
        encryptedUserKey = null,
        hasAdminApproval = false,
        hasLoginApprovingDevice = false,
        hasManageResetPasswordPermission = false,
    )

private val MOCK_USER_DECRYPTION_OPTIONS: UserDecryptionOptionsJson = UserDecryptionOptionsJson(
    hasMasterPassword = false,
    trustedDeviceUserDecryptionOptions = MOCK_TRUSTED_DEVICE_USER_DECRYPTION_OPTIONS,
    keyConnectorUserDecryptionOptions = null,
    masterPasswordUnlock = null,
)

private val MOCK_PROFILE = AccountJson.Profile(
    userId = USER_ID,
    email = "test@bitwarden.com",
    isEmailVerified = true,
    name = "Bitwarden Tester",
    hasPremium = false,
    stamp = null,
    organizationId = null,
    avatarColorHex = null,
    forcePasswordResetReason = null,
    kdfType = KdfTypeJson.ARGON2_ID,
    kdfIterations = 600000,
    kdfMemory = 16,
    kdfParallelism = 4,
    userDecryptionOptions = MOCK_USER_DECRYPTION_OPTIONS,
    isTwoFactorEnabled = false,
    creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
)

private val MOCK_ACCOUNT = AccountJson(
    profile = MOCK_PROFILE,
    settings = AccountJson.Settings(
        environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
    ),
)

private val MOCK_USER_STATE = UserStateJson(
    activeUserId = USER_ID,
    accounts = mapOf(
        USER_ID to MOCK_ACCOUNT,
    ),
)

/**
 * A list of all [VaultTimeoutAction].
 *
 * The order is reversed here in order to ensure that the first value differs from the default.
 */
private val VAULT_TIMEOUT_ACTIONS = VaultTimeoutAction.entries.reversed()

/**
 * Maps a VaultTimeout to its expected vaultTimeoutInMinutes value.
 */
private val VAULT_TIMEOUT_MAP =
    mapOf(
        VaultTimeout.OneMinute to 1,
        VaultTimeout.FiveMinutes to 5,
        VaultTimeout.FifteenMinutes to 15,
        VaultTimeout.ThirtyMinutes to 30,
        VaultTimeout.OneHour to 60,
        VaultTimeout.FourHours to 240,
        VaultTimeout.OnAppRestart to -1,
        VaultTimeout.Never to null,
        VaultTimeout.Custom(vaultTimeoutInMinutes = 123) to 123,
    )
