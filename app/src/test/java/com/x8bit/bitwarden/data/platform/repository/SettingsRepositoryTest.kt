package com.x8bit.bitwarden.data.platform.repository

import android.view.autofill.AutofillManager
import app.cash.turbine.test
import com.bitwarden.core.DerivePinKeyResponse
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.model.UserFingerprintResult
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.datasource.disk.util.FakeSettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.AppForegroundManager
import com.x8bit.bitwarden.data.platform.manager.model.AppForegroundState
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

@Suppress("LargeClass")
class SettingsRepositoryTest {
    private val autofillManager: AutofillManager = mockk {
        every { hasEnabledAutofillServices() } answers { isAutofillEnabledAndSupported }
        every { isAutofillSupported } answers { isAutofillEnabledAndSupported }
        every { isEnabled } answers { isAutofillEnabledAndSupported }
        every { disableAutofillServices() } just runs
    }
    private val mutableAppForegroundStateFlow = MutableStateFlow(AppForegroundState.BACKGROUNDED)
    private val appForegroundManager: AppForegroundManager = mockk {
        every { appForegroundStateFlow } returns mutableAppForegroundStateFlow
    }
    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val fakeSettingsDiskSource = FakeSettingsDiskSource()
    private val vaultSdkSource: VaultSdkSource = mockk()

    private var isAutofillEnabledAndSupported = false

    private val settingsRepository = SettingsRepositoryImpl(
        autofillManager = autofillManager,
        appForegroundManager = appForegroundManager,
        authDiskSource = fakeAuthDiskSource,
        settingsDiskSource = fakeSettingsDiskSource,
        vaultSdkSource = vaultSdkSource,
        dispatcherManager = FakeDispatcherManager(),
    )

    @Test
    fun `setDefaultsIfNecessary should set default values for the given user if necessary`() {
        val userId = "userId"
        assertNull(fakeSettingsDiskSource.getVaultTimeoutInMinutes(userId = userId))
        assertNull(fakeSettingsDiskSource.getVaultTimeoutAction(userId = userId))

        settingsRepository.setDefaultsIfNecessary(userId = userId)

        // Calling once sets values
        assertEquals(
            30,
            fakeSettingsDiskSource.getVaultTimeoutInMinutes(userId = userId),
        )
        assertEquals(
            VaultTimeoutAction.LOCK,
            fakeSettingsDiskSource.getVaultTimeoutAction(userId = userId),
        )

        // Updating the Vault settings values and calling setDefaultsIfNecessary again has no effect
        // on the currently stored values.
        fakeSettingsDiskSource.apply {
            storeVaultTimeoutInMinutes(
                userId = userId,
                vaultTimeoutInMinutes = 240,
            )
            storeVaultTimeoutAction(
                userId = userId,
                vaultTimeoutAction = VaultTimeoutAction.LOGOUT,
            )
        }
        settingsRepository.setDefaultsIfNecessary(userId = userId)
        assertEquals(
            240,
            fakeSettingsDiskSource.getVaultTimeoutInMinutes(userId = userId),
        )
        assertEquals(
            VaultTimeoutAction.LOGOUT,
            fakeSettingsDiskSource.getVaultTimeoutAction(userId = userId),
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
    fun `vaultTimeout should pull from and update SettingsDiskSource for the current user`() {
        fakeAuthDiskSource.userState = null
        assertEquals(
            VaultTimeout.Never,
            settingsRepository.vaultTimeout,
        )

        val userId = "userId"
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        // Updates to the disk source change the repository value
        VAULT_TIMEOUT_MAP.forEach { (vaultTimeout, vaultTimeoutInMinutes) ->
            fakeSettingsDiskSource.storeVaultTimeoutInMinutes(
                userId = userId,
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
                fakeSettingsDiskSource.getVaultTimeoutInMinutes(userId = userId),
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

        val userId = "userId"
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        // Updates to the disk source change the repository value
        VAULT_TIMEOUT_ACTIONS.forEach { vaultTimeoutAction ->
            fakeSettingsDiskSource.storeVaultTimeoutAction(
                userId = userId,
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
                fakeSettingsDiskSource.getVaultTimeoutAction(userId = userId),
            )
        }
    }

    @Test
    fun `getVaultTimeoutStateFlow should react to changes in SettingsDiskSource`() = runTest {
        val userId = "userId"
        settingsRepository
            .getVaultTimeoutStateFlow(userId = userId)
            .test {
                assertEquals(
                    VaultTimeout.Never,
                    awaitItem(),
                )
                VAULT_TIMEOUT_MAP.forEach { (vaultTimeout, vaultTimeoutInMinutes) ->
                    fakeSettingsDiskSource.storeVaultTimeoutInMinutes(
                        userId = userId,
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
        val userId = "userId"
        VAULT_TIMEOUT_MAP.forEach { (vaultTimeout, vaultTimeoutInMinutes) ->
            settingsRepository.storeVaultTimeout(
                userId = userId,
                vaultTimeout = vaultTimeout,
            )
            assertEquals(
                vaultTimeoutInMinutes,
                fakeSettingsDiskSource.getVaultTimeoutInMinutes(userId = userId),
            )
        }
    }

    @Test
    fun `getVaultTimeoutActionStateFlow should react to changes in SettingsDiskSource`() = runTest {
        val userId = "userId"
        settingsRepository
            .getVaultTimeoutActionStateFlow(userId = userId)
            .test {
                assertEquals(
                    VaultTimeoutAction.LOCK,
                    awaitItem(),
                )
                VAULT_TIMEOUT_ACTIONS.forEach { vaultTimeoutAction ->
                    fakeSettingsDiskSource.storeVaultTimeoutAction(
                        userId = userId,
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
        val userId = "userId"
        assertFalse(
            settingsRepository.isVaultTimeoutActionSet(userId = userId),
        )
    }

    @Test
    fun `isVaultTimeoutActionSet when a value is persisted should return true`() {
        val userId = "userId"
        fakeSettingsDiskSource.storeVaultTimeoutAction(
            userId = userId,
            vaultTimeoutAction = VaultTimeoutAction.LOGOUT,
        )
        assertTrue(
            settingsRepository.isVaultTimeoutActionSet(userId = userId),
        )
    }

    @Test
    fun `storeVaultTimeoutAction should properly update SettingsDiskSource`() {
        val userId = "userId"
        VAULT_TIMEOUT_ACTIONS.forEach { vaultTimeoutAction ->
            settingsRepository.storeVaultTimeoutAction(
                userId = userId,
                vaultTimeoutAction = vaultTimeoutAction,
            )
            assertEquals(
                vaultTimeoutAction,
                fakeSettingsDiskSource.getVaultTimeoutAction(userId = userId),
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

        val userId = "userId"
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        // Updates to the disk source change the repository value
        UriMatchType.entries.forEach { uriMatchType ->
            fakeSettingsDiskSource.storeDefaultUriMatchType(
                userId = userId,
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
                fakeSettingsDiskSource.getDefaultUriMatchType(userId = userId),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isUnlockWithPinEnabled should return a value that tracks the existence of an encrypted PIN for the current user`() {
        val userId = "userId"
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        fakeAuthDiskSource.storeEncryptedPin(
            userId = userId,
            encryptedPin = null,
        )
        assertFalse(settingsRepository.isUnlockWithPinEnabled)

        fakeAuthDiskSource.storeEncryptedPin(
            userId = userId,
            encryptedPin = "encryptedPin",
        )
        assertTrue(settingsRepository.isUnlockWithPinEnabled)
    }

    @Test
    fun `isInlineAutofillEnabled should pull from and update SettingsDiskSource`() {
        val userId = "userId"
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        assertTrue(settingsRepository.isInlineAutofillEnabled)

        // Updates to the disk source change the repository value.
        fakeSettingsDiskSource.isIconLoadingDisabled = false
        assertFalse(settingsRepository.isUnlockWithPinEnabled)

        // Updates to the repository change the disk source value
        settingsRepository.isInlineAutofillEnabled = true
        assertTrue(fakeSettingsDiskSource.getInlineAutofillEnabled(userId = userId)!!)
    }

    @Test
    fun `isAutofillSavePromptDisabled should pull from and update SettingsDiskSource`() {
        val userId = "userId"
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        assertFalse(settingsRepository.isAutofillSavePromptDisabled)

        // Updates to the disk source change the repository value.
        fakeSettingsDiskSource.storeAutofillSavePromptDisabled(
            userId = userId,
            isAutofillSavePromptDisabled = true,
        )
        assertTrue(settingsRepository.isAutofillSavePromptDisabled)

        // Updates to the repository change the disk source value
        settingsRepository.isAutofillSavePromptDisabled = false
        assertFalse(fakeSettingsDiskSource.getAutofillSavePromptDisabled(userId = userId)!!)
    }

    @Test
    fun `blockedAutofillUris should pull from and update SettingsDiskSource`() {
        val userId = "userId"
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        assertEquals(
            emptyList<String>(),
            settingsRepository.blockedAutofillUris,
        )

        // Updates to the disk source change the repository value.
        fakeSettingsDiskSource.storeBlockedAutofillUris(
            userId = userId,
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
            fakeSettingsDiskSource.getBlockedAutofillUris(userId = userId),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isAutofillEnabledStateFlow should emit updates if necessary when the app foreground state changes and disableAutofill is called`() =
        runTest {
            // Updates are not received without an isAutofillEnabledStateFlow subscriber.
            isAutofillEnabledAndSupported = true
            mutableAppForegroundStateFlow.value = AppForegroundState.FOREGROUNDED
            assertFalse(settingsRepository.isAutofillEnabledStateFlow.value)

            // Revert back to initial state.
            isAutofillEnabledAndSupported = false
            mutableAppForegroundStateFlow.value = AppForegroundState.BACKGROUNDED

            settingsRepository.isAutofillEnabledStateFlow.test {
                assertFalse(awaitItem())

                // An update is received when both the autofill state and foreground state change
                isAutofillEnabledAndSupported = true
                mutableAppForegroundStateFlow.value = AppForegroundState.FOREGROUNDED
                assertTrue(awaitItem())

                // An update is not received when only the foreground state changes
                mutableAppForegroundStateFlow.value = AppForegroundState.BACKGROUNDED
                expectNoEvents()

                // An update is not received when only the autofill state changes
                isAutofillEnabledAndSupported = false
                expectNoEvents()

                // An update is received after both states have changed
                mutableAppForegroundStateFlow.value = AppForegroundState.FOREGROUNDED
                assertFalse(awaitItem())

                // Calling disableAutofill will result in an emission of false
                isAutofillEnabledAndSupported = true
                mutableAppForegroundStateFlow.value = AppForegroundState.BACKGROUNDED
                assertTrue(awaitItem())
                settingsRepository.disableAutofill()
                assertFalse(awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `disableAutofill should trigger an emission of false from isAutofillEnabledStateFlow and disable autofill with the OS`() =
        runTest {
            // Start in a state where autofill is enabled
            isAutofillEnabledAndSupported = true
            mutableAppForegroundStateFlow.value = AppForegroundState.FOREGROUNDED
            settingsRepository.isAutofillEnabledStateFlow.test {
                assertTrue(awaitItem())
                expectNoEvents()
            }

            settingsRepository.disableAutofill()

            assertFalse(settingsRepository.isAutofillEnabledStateFlow.value)
            verify { autofillManager.disableAutofillServices() }
        }

    @Test
    fun `getUserFingerprint should return failure with no active user`() = runTest {
        fakeAuthDiskSource.userState = null

        val result = settingsRepository.getUserFingerprint()

        assertEquals(UserFingerprintResult.Error, result)
    }

    @Test
    fun `getUserFingerprint should return failure with active user when source returns failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultSdkSource.getUserFingerprint(
                    userId = MOCK_USER_STATE.activeUserId,
                )
            } returns Result.failure(Throwable())

            val result = settingsRepository.getUserFingerprint()

            coVerify(exactly = 1) {
                vaultSdkSource.getUserFingerprint(
                    userId = MOCK_USER_STATE.activeUserId,
                )
            }
            assertEquals(UserFingerprintResult.Error, result)
        }

    @Test
    fun `getUserFingerprint should return success with active user when source returns success`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val fingerprint = "fingerprint"
            coEvery {
                vaultSdkSource.getUserFingerprint(
                    userId = MOCK_USER_STATE.activeUserId,
                )
            } returns Result.success(fingerprint)

            val result = settingsRepository.getUserFingerprint()

            coVerify(exactly = 1) {
                vaultSdkSource.getUserFingerprint(
                    userId = MOCK_USER_STATE.activeUserId,
                )
            }
            assertEquals(UserFingerprintResult.Success(fingerprint), result)
        }

    @Test
    fun `getPullToRefreshEnabledFlow should react to changes in SettingsDiskSource`() = runTest {
        val userId = "userId"
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        settingsRepository
            .getPullToRefreshEnabledFlow()
            .test {
                assertFalse(awaitItem())
                fakeSettingsDiskSource.storePullToRefreshEnabled(
                    userId = userId,
                    isPullToRefreshEnabled = true,
                )
                assertTrue(awaitItem())
                fakeSettingsDiskSource.storePullToRefreshEnabled(
                    userId = userId,
                    isPullToRefreshEnabled = false,
                )
                assertFalse(awaitItem())
            }
    }

    @Test
    fun `storePullToRefreshEnabled should properly update SettingsDiskSource`() {
        val userId = "userId"
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        settingsRepository.storePullToRefreshEnabled(true)
        assertEquals(true, fakeSettingsDiskSource.getPullToRefreshEnabled(userId = userId))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `storeUnlockPin when the master password on restart is required should only save an encrypted PIN to disk`() {
        val userId = "userId"
        val pin = "1234"
        val encryptedPin = "encryptedPin"
        val pinProtectedUserKey = "pinProtectedUserKey"
        val derivePinKeyResponse = DerivePinKeyResponse(
            pinProtectedUserKey = pinProtectedUserKey,
            encryptedPin = encryptedPin,
        )
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery {
            vaultSdkSource.derivePinKey(
                userId = userId,
                pin = pin,
            )
        } returns derivePinKeyResponse.asSuccess()

        settingsRepository.storeUnlockPin(
            pin = pin,
            shouldRequireMasterPasswordOnRestart = true,
        )

        fakeAuthDiskSource.apply {
            assertEncryptedPin(
                userId = userId,
                encryptedPin = encryptedPin,
            )
            assertPinProtectedUserKey(
                userId = userId,
                pinProtectedUserKey = pinProtectedUserKey,
                inMemoryOnly = true,
            )
        }
        coVerify {
            vaultSdkSource.derivePinKey(
                userId = userId,
                pin = pin,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `storeUnlockPin when the master password on restart is not required should save all PIN data to disk`() {
        val userId = "userId"
        val pin = "1234"
        val encryptedPin = "encryptedPin"
        val pinProtectedUserKey = "pinProtectedUserKey"
        val derivePinKeyResponse = DerivePinKeyResponse(
            pinProtectedUserKey = pinProtectedUserKey,
            encryptedPin = encryptedPin,
        )
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery {
            vaultSdkSource.derivePinKey(
                userId = userId,
                pin = pin,
            )
        } returns derivePinKeyResponse.asSuccess()

        settingsRepository.storeUnlockPin(
            pin = pin,
            shouldRequireMasterPasswordOnRestart = false,
        )

        fakeAuthDiskSource.apply {
            assertEncryptedPin(
                userId = userId,
                encryptedPin = encryptedPin,
            )
            assertPinProtectedUserKey(
                userId = userId,
                pinProtectedUserKey = pinProtectedUserKey,
                inMemoryOnly = false,
            )
        }
        coVerify {
            vaultSdkSource.derivePinKey(
                userId = userId,
                pin = pin,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clearUnlockPin should clear any previously stored PIN-related values for the current user`() {
        val userId = "userId"
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        fakeAuthDiskSource.apply {
            storeEncryptedPin(
                userId = userId,
                encryptedPin = "encryptedPin",
            )
            storePinProtectedUserKey(
                userId = userId,
                pinProtectedUserKey = "pinProtectedUserKey",
            )
        }

        settingsRepository.clearUnlockPin()

        fakeAuthDiskSource.apply {
            assertEncryptedPin(
                userId = userId,
                encryptedPin = null,
            )
            assertPinProtectedUserKey(
                userId = userId,
                pinProtectedUserKey = null,
            )
        }
    }

    @Test
    fun `isApprovePasswordlessLoginsEnabled should properly update SettingsDiskSource`() {
        fakeAuthDiskSource.userState = null
        assertFalse(settingsRepository.isApprovePasswordlessLoginsEnabled)

        val userId = "userId"
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        // Updates to the disk source change the repository value
        fakeSettingsDiskSource.storeApprovePasswordlessLoginsEnabled(
            userId = userId,
            isApprovePasswordlessLoginsEnabled = true,
        )
        assertEquals(
            true,
            settingsRepository.isApprovePasswordlessLoginsEnabled,
        )

        // Updates to the repository value change the disk source
        settingsRepository.isApprovePasswordlessLoginsEnabled = false
        assertEquals(
            false,
            fakeSettingsDiskSource.getApprovePasswordlessLoginsEnabled(userId = userId),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isScreenCaptureAllowed property should update SettingsDiskSource and emit changes`() =
        runTest {
            val userId = "userId"
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            fakeSettingsDiskSource.storeScreenCaptureAllowed(userId, false)

            settingsRepository.isScreenCaptureAllowedStateFlow.test {
                assertFalse(awaitItem())

                settingsRepository.isScreenCaptureAllowed = true
                assertTrue(awaitItem())

                assertEquals(true, fakeSettingsDiskSource.getScreenCaptureAllowed(userId))

                settingsRepository.isScreenCaptureAllowed = false
                assertFalse(awaitItem())

                assertEquals(false, fakeSettingsDiskSource.getScreenCaptureAllowed(userId))
            }
        }

    @Test
    fun `validatePassword returns the validate password result`() = runTest {
        val userId = "userId"
        val password = "password"
        val passwordHash = "passwordHash"
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        fakeAuthDiskSource.storeMasterPasswordHash(userId = userId, passwordHash = passwordHash)
        coEvery {
            vaultSdkSource.validatePassword(
                userId = userId,
                password = password,
                passwordHash = passwordHash,
            )
        } returns true

        val result = settingsRepository
            .validatePassword(
                password = password,
            )
        assertTrue(result)
    }

    @Test
    fun `validatePassword returns false if there's no stored password hash`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val password = "password"

        val result = settingsRepository
            .validatePassword(
                password = password,
            )
        assertFalse(result)
    }
}

private val MOCK_USER_STATE =
    UserStateJson(
        activeUserId = "userId",
        accounts = mapOf("userId" to mockk()),
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
