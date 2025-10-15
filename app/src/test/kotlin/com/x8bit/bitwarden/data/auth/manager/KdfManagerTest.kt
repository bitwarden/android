package com.x8bit.bitwarden.data.auth.manager

import com.bitwarden.core.MasterPasswordAuthenticationData
import com.bitwarden.core.MasterPasswordUnlockData
import com.bitwarden.core.UpdateKdfResponse
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.crypto.Kdf
import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.network.model.KdfTypeJson
import com.bitwarden.network.model.UserDecryptionOptionsJson
import com.bitwarden.network.service.AccountsService
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.model.UpdateKdfMinimumsResult
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class KdfManagerTest {

    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val vaultSdkSource: VaultSdkSource = mockk()
    private val accountsService: AccountsService = mockk()
    private val featureFlagManager: FeatureFlagManager = mockk {
        every { getFeatureFlag(FlagKey.ForceUpdateKdfSettings) } returns true
    }

    private val manager: KdfManager = KdfManagerImpl(
        authDiskSource = fakeAuthDiskSource,
        vaultSdkSource = vaultSdkSource,
        accountsService = accountsService,
        featureFlagManager = featureFlagManager,
    )

    @Test
    fun `needsKdfUpdateToMinimums with no active user should return false`() = runTest {
        fakeAuthDiskSource.userState = null

        val result = manager.needsKdfUpdateToMinimums()

        assertFalse(result)
    }

    @Test
    fun `needsKdfUpdateToMinimums with kdfType null should return false`() = runTest {
        val nullKdfProfile = PROFILE_1.copy(
            kdfType = null,
            kdfIterations = null,
            kdfMemory = null,
            kdfParallelism = null,
        )
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1.copy(
            accounts = mapOf(
                USER_ID_1 to ACCOUNT_1.copy(profile = nullKdfProfile),
            ),
        )

        val result = manager.needsKdfUpdateToMinimums()

        assertFalse(result)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `needsKdfUpdateToMinimums with user decryption options and without password returns false`() =
        runTest {
            val account = ACCOUNT_1.copy(
                profile = ACCOUNT_1.profile.copy(
                    userDecryptionOptions = UserDecryptionOptionsJson(
                        hasMasterPassword = false,
                        keyConnectorUserDecryptionOptions = null,
                        trustedDeviceUserDecryptionOptions = null,
                        masterPasswordUnlock = null,
                    ),
                ),
            )
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1.copy(
                accounts = mapOf(
                    USER_ID_1 to account,
                ),
            )

            val result = manager.needsKdfUpdateToMinimums()
            assertFalse(result)
        }

    @Test
    fun `needsKdfUpdateToMinimums with PBKDF2 below minimum iterations should return true`() =
        runTest {
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_2

            val result = manager.needsKdfUpdateToMinimums()

            assertTrue(result)
        }

    @Test
    fun `needsKdfUpdateToMinimums with PBKDF2 meeting minimum iterations should return false`() =
        runTest {
            val sufficientIterationsProfile = PROFILE_1.copy(
                kdfType = KdfTypeJson.PBKDF2_SHA256,
                kdfIterations = 600000, // Meets minimum
                kdfMemory = null,
                kdfParallelism = null,
            )
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1.copy(
                accounts = mapOf(
                    USER_ID_1 to ACCOUNT_1.copy(profile = sufficientIterationsProfile),
                ),
            )

            val result = manager.needsKdfUpdateToMinimums()

            assertFalse(result)
        }

    @Test
    fun `needsKdfUpdateToMinimums with Argon2id below minimum parameters should return false`() =
        runTest {
            val lowArgon2idProfile = PROFILE_1.copy(
                kdfType = KdfTypeJson.ARGON2_ID,
                kdfIterations = 1, // Below minimum of 3
                kdfMemory = 16, // Below minimum of 64
                kdfParallelism = 1, // Below minimum of 4
            )
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1.copy(
                accounts = mapOf(
                    USER_ID_1 to ACCOUNT_1.copy(profile = lowArgon2idProfile),
                ),
            )

            val result = manager.needsKdfUpdateToMinimums()

            assertFalse(result)
        }

    @Test
    fun `needsKdfUpdateToMinimums with Argon2id meeting minimum parameters should return false`() =
        runTest {
            val sufficientArgon2idProfile = PROFILE_1.copy(
                kdfType = KdfTypeJson.ARGON2_ID,
                kdfIterations = 600000, // Meets minimum
                kdfMemory = 64,
                kdfParallelism = 4,
            )
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1.copy(
                accounts = mapOf(
                    USER_ID_1 to ACCOUNT_1.copy(profile = sufficientArgon2idProfile),
                ),
            )

            val result = manager.needsKdfUpdateToMinimums()

            assertFalse(result)
        }

    @Test
    fun `updateKdfToMinimumsIfNeeded with no active user should return ActiveAccountNotFound`() =
        runTest {
            fakeAuthDiskSource.userState = null

            val result = manager.updateKdfToMinimumsIfNeeded(password = PASSWORD)

            assertEquals(
                UpdateKdfMinimumsResult.ActiveAccountNotFound,
                result,
            )
        }

    @Test
    fun `updateKdfToMinimumsIfNeeded with minimum Kdf iterations should return Success`() =
        runTest {
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1.copy(
                accounts = mapOf(
                    USER_ID_1 to ACCOUNT_1.copy(
                        profile = PROFILE_1.copy(
                            kdfType = KdfTypeJson.PBKDF2_SHA256,
                            kdfIterations = 600000,
                        ),
                    ),
                ),
            )

            val result = manager.updateKdfToMinimumsIfNeeded(password = PASSWORD)

            assertEquals(
                UpdateKdfMinimumsResult.Success,
                result,
            )
        }

    @Test
    @Suppress("MaxLineLength")
    fun `updateKdfToMinimumsIfNeeded with feature flag ForceUpdateKdfSettings to false return Success`() =
        runTest {
            every {
                featureFlagManager.getFeatureFlag(FlagKey.ForceUpdateKdfSettings)
            } returns false

            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1.copy(
                accounts = mapOf(
                    USER_ID_1 to ACCOUNT_1.copy(
                        profile = PROFILE_1.copy(
                            kdfType = KdfTypeJson.PBKDF2_SHA256,
                            kdfIterations = 1000,
                        ),
                    ),
                ),
            )

            val result = manager.updateKdfToMinimumsIfNeeded(password = PASSWORD)

            assertEquals(
                UpdateKdfMinimumsResult.Success,
                result,
            )
        }

    @Test
    fun `updateKdfToMinimumsIfNeeded if sdk throws an error should return Error`() = runTest {
        val error = Throwable("Kdf update failed")
        coEvery {
            vaultSdkSource.makeUpdateKdf(
                userId = any(),
                password = any(),
                kdf = any(),
            )
        } returns error.asFailure()

        fakeAuthDiskSource.userState = SINGLE_USER_STATE_2

        val result = manager.updateKdfToMinimumsIfNeeded(password = PASSWORD)

        assertEquals(
            UpdateKdfMinimumsResult.Error(error = error),
            result,
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `updateKdfToMinimumsIfNeeded with PBKDF2 below minimums and updateKdf API failure should return Error`() =
        runTest {
            val error = Throwable("API failed")
            coEvery {
                vaultSdkSource.makeUpdateKdf(
                    userId = any(),
                    password = any(),
                    kdf = any(),
                )
            } returns UPDATE_KDF_RESPONSE.asSuccess()

            coEvery {
                accountsService.updateKdf(any())
            } returns error.asFailure()

            fakeAuthDiskSource.userState = SINGLE_USER_STATE_2

            val result = manager.updateKdfToMinimumsIfNeeded(password = PASSWORD)

            assertEquals(UpdateKdfMinimumsResult.Error(error = error), result)
            coVerify(exactly = 1) {
                accountsService.updateKdf(any())
            }
        }

    @Test
    fun `updateKdfToMinimumsIfNeeded with PBKDF2 below minimums should return Success`() =
        runTest {
            coEvery {
                vaultSdkSource.makeUpdateKdf(
                    userId = any(),
                    password = any(),
                    kdf = any(),
                )
            } returns UPDATE_KDF_RESPONSE.asSuccess()

            coEvery {
                accountsService.updateKdf(any())
            } returns Unit.asSuccess()

            fakeAuthDiskSource.userState = SINGLE_USER_STATE_2

            val result = manager.updateKdfToMinimumsIfNeeded(password = PASSWORD)

            assertEquals(UpdateKdfMinimumsResult.Success, result)
            coVerify(exactly = 1) {
                accountsService.updateKdf(any())
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `updateKdfToMinimumsIfNeeded with PBKDF2 below minimums should update userState to minimums`() =
        runTest {
            coEvery {
                vaultSdkSource.makeUpdateKdf(
                    userId = any(),
                    password = any(),
                    kdf = any(),
                )
            } returns UPDATE_KDF_RESPONSE.asSuccess()

            coEvery {
                accountsService.updateKdf(any())
            } returns Unit.asSuccess()

            fakeAuthDiskSource.userState = SINGLE_USER_STATE_2

            val result = manager.updateKdfToMinimumsIfNeeded(password = PASSWORD)

            assertEquals(UpdateKdfMinimumsResult.Success, result)

            // Verify userState was updated with minimum KDF values
            val updatedUserState = fakeAuthDiskSource.userState
            val updatedProfile = updatedUserState?.accounts?.get(USER_ID_2)?.profile
            assertEquals(KdfTypeJson.PBKDF2_SHA256, updatedProfile?.kdfType)
            assertEquals(600000, updatedProfile?.kdfIterations)
            assertNull(updatedProfile?.kdfMemory)
            assertNull(updatedProfile?.kdfParallelism)
        }
}

private const val EMAIL = "test@bitwarden.com"
private const val EMAIL_2 = "test2@bitwarden.com"
private const val PASSWORD = "password"
private const val USER_ID_1 = "2a135b23-e1fb-42c9-bec3-573857bc8181"
private const val USER_ID_2 = "b9d32ec0-6497-4582-9798-b350f53bfa02"
private val PROFILE_1 = AccountJson.Profile(
    userId = USER_ID_1,
    email = EMAIL,
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
    userDecryptionOptions = null,
    isTwoFactorEnabled = false,
    creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
)

private val ACCOUNT_1 = AccountJson(
    profile = PROFILE_1,
    settings = AccountJson.Settings(
        environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
    ),
)

private val ACCOUNT_2 = AccountJson(
    profile = AccountJson.Profile(
        userId = USER_ID_2,
        email = EMAIL_2,
        isEmailVerified = true,
        name = "Bitwarden Tester 2",
        hasPremium = false,
        stamp = null,
        organizationId = null,
        avatarColorHex = null,
        forcePasswordResetReason = null,
        kdfType = KdfTypeJson.PBKDF2_SHA256,
        kdfIterations = 400000,
        kdfMemory = null,
        kdfParallelism = null,
        userDecryptionOptions = null,
        isTwoFactorEnabled = true,
        creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
    ),
    settings = AccountJson.Settings(
        environmentUrlData = EnvironmentUrlDataJson.DEFAULT_EU,
    ),
)

private val SINGLE_USER_STATE_1 = UserStateJson(
    activeUserId = USER_ID_1,
    accounts = mapOf(
        USER_ID_1 to ACCOUNT_1,
    ),
)

private val SINGLE_USER_STATE_2 = UserStateJson(
    activeUserId = USER_ID_2,
    accounts = mapOf(
        USER_ID_2 to ACCOUNT_2,
    ),
)

private val UPDATE_KDF_RESPONSE = UpdateKdfResponse(
    masterPasswordAuthenticationData = MasterPasswordAuthenticationData(
        kdf = mockk<Kdf>(relaxed = true),
        salt = "mockSalt",
        masterPasswordAuthenticationHash = "mockHash",
    ),
    masterPasswordUnlockData = MasterPasswordUnlockData(
        kdf = mockk<Kdf>(relaxed = true),
        masterKeyWrappedUserKey = "mockKey",
        salt = "mockSalt",
    ),
    oldMasterPasswordAuthenticationData = MasterPasswordAuthenticationData(
        kdf = mockk<Kdf>(relaxed = true),
        salt = "mockSalt",
        masterPasswordAuthenticationHash = "mockHash",
    ),
)
