package com.x8bit.bitwarden.data.platform.manager

import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.network.model.KdfTypeJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TrustedDeviceUserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.UserDecryptionOptionsJson
import com.x8bit.bitwarden.data.autofill.manager.AutofillEnabledManager
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.datasource.disk.util.FakeSettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class FirstTimeActionManagerTest {
    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val fakeSettingsDiskSource = FakeSettingsDiskSource()
    private val mutableCiphersListFlow = MutableStateFlow(emptyList<SyncResponseJson.Cipher>())
    private val vaultDiskSource = mockk<VaultDiskSource> {
        every { getCiphers(any()) } returns mutableCiphersListFlow
    }

    private val mutableImportLoginsFlow = MutableStateFlow(false)
    private val featureFlagManager = mockk<FeatureFlagManager> {
        every { getFeatureFlagFlow(FlagKey.ImportLoginsFlow) } returns mutableImportLoginsFlow
    }

    private val mutableAutofillEnabledFlow = MutableStateFlow(false)
    private val autofillEnabledManager = mockk<AutofillEnabledManager> {
        every { isAutofillEnabledStateFlow } returns mutableAutofillEnabledFlow
        every { isAutofillEnabled } returns false
    }

    private val firstTimeActionManager = FirstTimeActionManagerImpl(
        authDiskSource = fakeAuthDiskSource,
        settingsDiskSource = fakeSettingsDiskSource,
        vaultDiskSource = vaultDiskSource,
        featureFlagManager = featureFlagManager,
        dispatcherManager = FakeDispatcherManager(),
        autofillEnabledManager = autofillEnabledManager,
    )

    @Suppress("MaxLineLength")
    @Test
    fun `allAutoFillSettingsBadgeCountFlow should emit the value of flags set to true and update when saved value is changed or autofill enabled state changes`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            firstTimeActionManager.allAutofillSettingsBadgeCountFlow.test {
                assertEquals(0, awaitItem())
                fakeSettingsDiskSource.storeShowAutoFillSettingBadge(
                    userId = USER_ID,
                    showBadge = true,
                )
                assertEquals(1, awaitItem())
                fakeSettingsDiskSource.storeShowAutoFillSettingBadge(
                    userId = USER_ID,
                    showBadge = false,
                )
                assertEquals(0, awaitItem())
                fakeSettingsDiskSource.storeShowAutoFillSettingBadge(
                    userId = USER_ID,
                    showBadge = true,
                )
                assertEquals(1, awaitItem())
                mutableAutofillEnabledFlow.update { true }
                assertEquals(0, awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `allSecuritySettingsBadgeCountFlow should emit the value of flags set to true and update when changed`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            firstTimeActionManager.allSecuritySettingsBadgeCountFlow.test {
                assertEquals(0, awaitItem())
                fakeSettingsDiskSource.storeShowUnlockSettingBadge(
                    userId = USER_ID,
                    showBadge = true,
                )
                assertEquals(1, awaitItem())
                fakeSettingsDiskSource.storeShowUnlockSettingBadge(
                    userId = USER_ID,
                    showBadge = false,
                )
                assertEquals(0, awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `allSettingsBadgeCountFlow should emit the value of all flags set to true and update when changed`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            firstTimeActionManager.allSettingsBadgeCountFlow.test {
                assertEquals(0, awaitItem())
                fakeSettingsDiskSource.storeShowAutoFillSettingBadge(
                    userId = USER_ID,
                    showBadge = true,
                )
                assertEquals(1, awaitItem())
                fakeSettingsDiskSource.storeShowUnlockSettingBadge(
                    userId = USER_ID,
                    showBadge = true,
                )
                assertEquals(2, awaitItem())
                fakeSettingsDiskSource.storeShowAutoFillSettingBadge(
                    userId = USER_ID,
                    showBadge = false,
                )
                assertEquals(1, awaitItem())
                // for the import logins count it is dependent on the feature flag state and
                // cipher list being empty
                mutableImportLoginsFlow.update { true }
                fakeSettingsDiskSource.storeShowImportLoginsSettingBadge(USER_ID, true)
                assertEquals(2, awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `allVaultSettingsBadgeCountFlow should emit the value of all flags set to true and update when dependent states are changed changed`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val mockCipher = mockk<SyncResponseJson.Cipher>(relaxed = true)
            // For the import logins count to register, the feature flag for ImportLoginsFlow must
            // be enabled, the cipher list must not be empty, and the value saved to disk should be
            // true.
            firstTimeActionManager.allVaultSettingsBadgeCountFlow.test {
                assertEquals(0, awaitItem())
                mutableImportLoginsFlow.update { true }
                fakeSettingsDiskSource.storeShowImportLoginsSettingBadge(USER_ID, true)
                assertEquals(1, awaitItem())
                mutableImportLoginsFlow.update { false }
                assertEquals(0, awaitItem())
                mutableImportLoginsFlow.update { true }
                assertEquals(1, awaitItem())
                fakeSettingsDiskSource.storeShowImportLoginsSettingBadge(USER_ID, false)
                assertEquals(0, awaitItem())
                fakeSettingsDiskSource.storeShowImportLoginsSettingBadge(USER_ID, true)
                assertEquals(1, awaitItem())
                mutableCiphersListFlow.update {
                    listOf(mockCipher)
                }
                assertEquals(0, awaitItem())
            }
        }

    @Test
    fun `firstTimeStateFlow should emit changes when items in the first time state change`() =
        runTest {
            firstTimeActionManager.firstTimeStateFlow.test {
                fakeAuthDiskSource.userState =
                    MOCK_USER_STATE
                assertEquals(
                    FirstTimeState(
                        showImportLoginsCard = true,
                    ),
                    awaitItem(),
                )
                firstTimeActionManager.storeShowImportLogins(false)
                assertEquals(
                    FirstTimeState(
                        showImportLoginsCard = false,
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `currentOrDefaultUserFirstTimeState should return the current first time state or a default state`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        // Assert default state when no values set
        assertEquals(
            FirstTimeState(
                showImportLoginsCard = true,
            ),
            firstTimeActionManager.currentOrDefaultUserFirstTimeState,
        )
        fakeAuthDiskSource.storeShowImportLogins(USER_ID, false)

        assertEquals(
            FirstTimeState(
                showImportLoginsCard = false,
            ),
            firstTimeActionManager.currentOrDefaultUserFirstTimeState,
        )
    }

    @Test
    fun `storeShowAutoFillSettingBadge should store value of false to disk`() {
        fakeAuthDiskSource.userState =
            MOCK_USER_STATE
        firstTimeActionManager.storeShowAutoFillSettingBadge(showBadge = false)
        assertFalse(fakeSettingsDiskSource.getShowAutoFillSettingBadge(userId = USER_ID)!!)
    }

    @Test
    fun `storeShowAutoFillSettingBadge should store value of true to disk`() {
        fakeAuthDiskSource.userState =
            MOCK_USER_STATE
        firstTimeActionManager.storeShowAutoFillSettingBadge(showBadge = true)
        assertTrue(fakeSettingsDiskSource.getShowAutoFillSettingBadge(userId = USER_ID)!!)
    }

    @Test
    fun `getShowAutoFillSettingBadge should return the value saved to disk`() {
        fakeAuthDiskSource.userState =
            MOCK_USER_STATE
        firstTimeActionManager.storeShowAutoFillSettingBadge(showBadge = true)
        assertTrue(fakeSettingsDiskSource.getShowAutoFillSettingBadge(userId = USER_ID)!!)
    }

    @Test
    fun `storeShowUnlockSettingBadge should store value of false to disk`() {
        fakeAuthDiskSource.userState =
            MOCK_USER_STATE
        firstTimeActionManager.storeShowUnlockSettingBadge(showBadge = false)
        assertFalse(fakeSettingsDiskSource.getShowUnlockSettingBadge(userId = USER_ID)!!)
    }

    @Test
    fun `storeShowUnlockSettingBadge should store value of true to disk`() {
        fakeAuthDiskSource.userState =
            MOCK_USER_STATE
        firstTimeActionManager.storeShowUnlockSettingBadge(showBadge = true)
        assertTrue(fakeSettingsDiskSource.getShowUnlockSettingBadge(userId = USER_ID)!!)
    }

    @Test
    fun `storeShowImportLogins should store value of false to disk`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        firstTimeActionManager.storeShowImportLogins(showImportLogins = true)
        assertTrue(fakeAuthDiskSource.getShowImportLogins(userId = USER_ID)!!)
    }

    @Test
    fun `storeShowImportLoginsSettingsBadge should store value of true to disk`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        firstTimeActionManager.storeShowImportLoginsSettingsBadge(showBadge = false)
        assertFalse(fakeSettingsDiskSource.getShowImportLoginsSettingBadge(userId = USER_ID)!!)
    }

    @Test
    fun `show autofill badge when autofill is already enabled should be false`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        every { autofillEnabledManager.isAutofillEnabled } returns true
        assertFalse(firstTimeActionManager.currentOrDefaultUserFirstTimeState.showSetupAutofillCard)
    }

    @Test
    fun `first time state flow should update when autofill is enabled`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        firstTimeActionManager.firstTimeStateFlow.test {
            assertEquals(
                FirstTimeState(
                    showImportLoginsCard = true,
                ),
                awaitItem(),
            )
            fakeSettingsDiskSource.storeShowAutoFillSettingBadge(
                userId = MOCK_USER_STATE.activeUserId,
                showBadge = true,
            )
            assertEquals(
                FirstTimeState(
                    showImportLoginsCard = true,
                    showSetupAutofillCard = true,
                ),
                awaitItem(),
            )
            mutableAutofillEnabledFlow.update { true }
            assertEquals(
                FirstTimeState(
                    showImportLoginsCard = true,
                    showSetupAutofillCard = false,
                ),
                awaitItem(),
            )
        }
    }
}

private const val USER_ID: String = "userId"

private val MOCK_TRUSTED_DEVICE_USER_DECRYPTION_OPTIONS = TrustedDeviceUserDecryptionOptionsJson(
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
