package com.x8bit.bitwarden.data.platform.manager

import app.cash.turbine.test
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.network.model.KdfTypeJson
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.TrustedDeviceUserDecryptionOptionsJson
import com.bitwarden.network.model.UserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.autofill.manager.AutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutoFillData
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutofillStatus
import com.x8bit.bitwarden.data.platform.datasource.disk.util.FakeSettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.model.CoachMarkTourType
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class FirstTimeActionManagerTest {
    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val fakeSettingsDiskSource = FakeSettingsDiskSource()
    private val mutableCiphersListFlow = MutableStateFlow(emptyList<SyncResponseJson.Cipher>())
    private val vaultDiskSource = mockk<VaultDiskSource> {
        every { getCiphersFlow(any()) } returns mutableCiphersListFlow
    }
    private val mutableAutofillEnabledFlow = MutableStateFlow(false)
    private val autofillEnabledManager = mockk<AutofillEnabledManager> {
        every { isAutofillEnabledStateFlow } returns mutableAutofillEnabledFlow
        every { isAutofillEnabled } returns false
    }
    private val mutableThirdPartyAutofillStatusFlow = MutableStateFlow(DEFAULT_AUTOFILL_STATUS)
    private val thirdPartyAutofillEnabledManager: BrowserThirdPartyAutofillEnabledManager = mockk {
        every { browserThirdPartyAutofillStatusFlow } returns mutableThirdPartyAutofillStatusFlow
    }

    private val firstTimeActionManager = FirstTimeActionManagerImpl(
        authDiskSource = fakeAuthDiskSource,
        settingsDiskSource = fakeSettingsDiskSource,
        vaultDiskSource = vaultDiskSource,
        dispatcherManager = FakeDispatcherManager(),
        autofillEnabledManager = autofillEnabledManager,
        thirdPartyAutofillEnabledManager = thirdPartyAutofillEnabledManager,
    )

    @Suppress("MaxLineLength")
    @Test
    fun `allAutofillSettingsBadgeCountFlow should update when saved value is changed or autofill enabled state changes`() =
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

    @Test
    fun `allSecuritySettingsBadgeCountFlow should update when changed`() = runTest {
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

    @Test
    fun `allSettingsBadgeCountFlow should update when changed`() = runTest {
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
            // for the import logins count it is dependent on the cipher list being empty
            fakeSettingsDiskSource.storeShowImportLoginsSettingBadge(USER_ID, true)
            assertEquals(2, awaitItem())
        }
    }

    @Test
    fun `allVaultSettingsBadgeCountFlow should update when dependent states are changed changed`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val mockCipher = mockk<SyncResponseJson.Cipher>(relaxed = true)
            // For the import logins count to register, the cipher list must not be empty, and the
            // value saved to disk should be true.
            firstTimeActionManager.allVaultSettingsBadgeCountFlow.test {
                assertEquals(0, awaitItem())
                fakeSettingsDiskSource.storeShowImportLoginsSettingBadge(USER_ID, true)
                assertEquals(1, awaitItem())
                fakeSettingsDiskSource.storeShowImportLoginsSettingBadge(USER_ID, false)
                assertEquals(0, awaitItem())
                fakeSettingsDiskSource.storeShowImportLoginsSettingBadge(USER_ID, true)
                assertEquals(1, awaitItem())
                mutableCiphersListFlow.update { listOf(mockCipher) }
                assertEquals(0, awaitItem())
            }
        }

    @Test
    fun `firstTimeStateFlow should emit changes when items in the first time state change`() =
        runTest {
            firstTimeActionManager.firstTimeStateFlow.test {
                fakeAuthDiskSource.userState = MOCK_USER_STATE
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
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        firstTimeActionManager.storeShowAutoFillSettingBadge(showBadge = false)
        assertFalse(fakeSettingsDiskSource.getShowAutoFillSettingBadge(userId = USER_ID)!!)
    }

    @Test
    fun `storeShowAutoFillSettingBadge should store value of true to disk`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        firstTimeActionManager.storeShowAutoFillSettingBadge(showBadge = true)
        assertTrue(fakeSettingsDiskSource.getShowAutoFillSettingBadge(userId = USER_ID)!!)
    }

    @Test
    fun `storeShowBrowserAutofillSettingBadge should store value of false to disk`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        firstTimeActionManager.storeShowBrowserAutofillSettingBadge(showBadge = false)
        assertFalse(fakeSettingsDiskSource.getShowBrowserAutofillSettingBadge(userId = USER_ID)!!)
    }

    @Test
    fun `storeShowBrowserAutofillSettingBadge should store value of true to disk`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        firstTimeActionManager.storeShowBrowserAutofillSettingBadge(showBadge = true)
        assertTrue(fakeSettingsDiskSource.getShowBrowserAutofillSettingBadge(userId = USER_ID)!!)
    }

    @Test
    fun `getShowAutoFillSettingBadge should return the value saved to disk`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        firstTimeActionManager.storeShowAutoFillSettingBadge(showBadge = true)
        assertTrue(fakeSettingsDiskSource.getShowAutoFillSettingBadge(userId = USER_ID)!!)
    }

    @Test
    fun `storeShowUnlockSettingBadge should store value of false to disk`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        firstTimeActionManager.storeShowUnlockSettingBadge(showBadge = false)
        assertFalse(fakeSettingsDiskSource.getShowUnlockSettingBadge(userId = USER_ID)!!)
    }

    @Test
    fun `storeShowUnlockSettingBadge should store value of true to disk`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
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

    @Test
    fun `shouldShowAddLoginCoachMarkFlow updates when disk source updates`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        firstTimeActionManager.shouldShowAddLoginCoachMarkFlow.test {
            assertTrue(awaitItem())
            fakeSettingsDiskSource.storeShouldShowAddLoginCoachMark(shouldShow = false)
            assertFalse(awaitItem())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `if there are any login ciphers available for the active user should not show add login coach marks`() =
        runTest {
            val mockJsonWithNoLogin = mockk<SyncResponseJson.Cipher> {
                every { login } returns null
                every { organizationId } returns null
            }
            val mockJsonWithLogin = mockk<SyncResponseJson.Cipher> {
                every { login } returns mockk()
                every { organizationId } returns null
            }
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            mutableCiphersListFlow.update {
                listOf(
                    mockJsonWithNoLogin,
                    mockJsonWithNoLogin,
                )
            }
            firstTimeActionManager.shouldShowAddLoginCoachMarkFlow.test {
                assertTrue(awaitItem())
                mutableCiphersListFlow.update {
                    listOf(
                        mockJsonWithLogin,
                        mockJsonWithNoLogin,
                    )
                }
                assertFalse(awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `markCoachMarkTourCompleted for the ADD_LOGIN type sets the value to true in the disk source for should show add logins coach mark`() {
        assertNull(fakeSettingsDiskSource.getShouldShowAddLoginCoachMark())
        firstTimeActionManager.markCoachMarkTourCompleted(CoachMarkTourType.ADD_LOGIN)
        assertTrue(fakeSettingsDiskSource.getShouldShowAddLoginCoachMark() == false)
    }

    @Test
    fun `shouldShowGeneratorCoachMarkFlow updates when disk source updates`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        firstTimeActionManager.shouldShowGeneratorCoachMarkFlow.test {
            assertTrue(awaitItem())
            fakeSettingsDiskSource.storeShouldShowGeneratorCoachMark(shouldShow = false)
            assertFalse(awaitItem())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `if there are any login ciphers available for the active user should not show generator coach marks`() =
        runTest {
            val mockJsonWithNoLogin = mockk<SyncResponseJson.Cipher> {
                every { login } returns null
                every { organizationId } returns null
            }
            val mockJsonWithLogin = mockk<SyncResponseJson.Cipher> {
                every { login } returns mockk()
                every { organizationId } returns null
            }
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            mutableCiphersListFlow.update {
                listOf(
                    mockJsonWithNoLogin,
                    mockJsonWithNoLogin,
                )
            }
            firstTimeActionManager.shouldShowGeneratorCoachMarkFlow.test {
                assertTrue(awaitItem())
                mutableCiphersListFlow.update {
                    listOf(
                        mockJsonWithLogin,
                        mockJsonWithNoLogin,
                    )
                }
                assertFalse(awaitItem())
            }
        }

    @Test
    fun `if there are login ciphers attached to an organization we should show coach marks`() =
        runTest {
            val mockJsonWithLoginAndWithOrganizationId = mockk<SyncResponseJson.Cipher> {
                every { login } returns mockk()
                every { organizationId } returns "1234"
            }
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            mutableCiphersListFlow.update {
                listOf(mockJsonWithLoginAndWithOrganizationId)
            }
            firstTimeActionManager.shouldShowGeneratorCoachMarkFlow.test {
                assertTrue(awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `markCoachMarkTourCompleted for the GENERATOR type sets the value to true in the disk source for should show generator coach mark`() {
        assertNull(fakeSettingsDiskSource.getShouldShowGeneratorCoachMark())
        firstTimeActionManager.markCoachMarkTourCompleted(CoachMarkTourType.GENERATOR)
        assertTrue(fakeSettingsDiskSource.getShouldShowGeneratorCoachMark() == false)
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

private val DEFAULT_BROWSER_AUTOFILL_DATA = BrowserThirdPartyAutoFillData(
    isAvailable = false,
    isThirdPartyEnabled = false,
)

private val DEFAULT_AUTOFILL_STATUS = BrowserThirdPartyAutofillStatus(
    braveStableStatusData = DEFAULT_BROWSER_AUTOFILL_DATA,
    chromeStableStatusData = DEFAULT_BROWSER_AUTOFILL_DATA,
    chromeBetaChannelStatusData = DEFAULT_BROWSER_AUTOFILL_DATA,
)
