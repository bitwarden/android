package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.core.InitUserCryptoRequest
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.util.asFailure
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultState
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import io.mockk.awaits
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@Suppress("LargeClass")
class VaultLockManagerTest {
    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val vaultSdkSource: VaultSdkSource = mockk {
        every { clearCrypto(userId = any()) } just runs
    }
    private val mutableVaultTimeoutStateFlow =
        MutableStateFlow<VaultTimeout>(VaultTimeout.ThirtyMinutes)
    private val settingsRepository: SettingsRepository = mockk {
        every { getVaultTimeoutStateFlow(any()) } returns mutableVaultTimeoutStateFlow
    }

    private val vaultLockManager: VaultLockManager = VaultLockManagerImpl(
        authDiskSource = fakeAuthDiskSource,
        vaultSdkSource = vaultSdkSource,
        settingsRepository = settingsRepository,
        dispatcherManager = FakeDispatcherManager(),
    )

    @Test
    fun `vaultTimeout updates to non-Never should clear the user's auto-unlock key`() = runTest {
        val userId = "mockId-1"
        val userAutoUnlockKey = "userAutoUnlockKey"

        // Initialize Never state
        coEvery {
            vaultSdkSource.getUserEncryptionKey(userId = userId)
        } returns userAutoUnlockKey.asSuccess()
        verifyUnlockedVault(userId = userId)
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        mutableVaultTimeoutStateFlow.value = VaultTimeout.Never

        fakeAuthDiskSource.assertUserAutoUnlockKey(
            userId = userId,
            userAutoUnlockKey = userAutoUnlockKey,
        )

        mutableVaultTimeoutStateFlow.value = VaultTimeout.ThirtyMinutes

        fakeAuthDiskSource.assertUserAutoUnlockKey(
            userId = userId,
            userAutoUnlockKey = null,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultTimeout update to Never for an unlocked account should store the user's encrypted key`() =
        runTest {
            val userId = "mockId-1"
            val userAutoUnlockKey = "userAutoUnlockKey"
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultSdkSource.getUserEncryptionKey(userId = userId)
            } returns userAutoUnlockKey.asSuccess()

            verifyUnlockedVault(userId = userId)

            fakeAuthDiskSource.assertUserAutoUnlockKey(
                userId = userId,
                userAutoUnlockKey = null,
            )

            mutableVaultTimeoutStateFlow.value = VaultTimeout.Never

            fakeAuthDiskSource.assertUserAutoUnlockKey(
                userId = userId,
                userAutoUnlockKey = userAutoUnlockKey,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultTimeout update to Never for a locked account when there is no stored private key should do nothing`() {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        assertFalse(vaultLockManager.isVaultUnlocked(userId = userId))

        mutableVaultTimeoutStateFlow.value = VaultTimeout.Never

        assertFalse(vaultLockManager.isVaultUnlocked(userId = userId))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultTimeout update to Never for a locked account when there is no stored auto-unlock key should do nothing`() {
        val userId = "mockId-1"
        val privateKey = "privateKey"
        fakeAuthDiskSource.apply {
            userState = MOCK_USER_STATE
            storePrivateKey(
                userId = userId,
                privateKey = privateKey,
            )
        }
        assertFalse(vaultLockManager.isVaultUnlocked(userId = userId))

        mutableVaultTimeoutStateFlow.value = VaultTimeout.Never

        assertFalse(vaultLockManager.isVaultUnlocked(userId = userId))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultTimeout update to Never for a locked account when there is a stored auto-unlock key should unlock the vault`() {
        val userId = "mockId-1"
        val privateKey = "privateKey"
        val userAutoUnlockKey = "userAutoUnlockKey"
        fakeAuthDiskSource.apply {
            userState = MOCK_USER_STATE
            storePrivateKey(
                userId = userId,
                privateKey = privateKey,
            )
            storeUserAutoUnlockKey(
                userId = userId,
                userAutoUnlockKey = userAutoUnlockKey,
            )
        }
        coEvery {
            vaultSdkSource.initializeCrypto(
                userId = userId,
                request = InitUserCryptoRequest(
                    kdfParams = MOCK_PROFILE.toSdkParams(),
                    email = MOCK_PROFILE.email,
                    privateKey = privateKey,
                    method = InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = userAutoUnlockKey,
                    ),
                ),
            )
        } returns InitializeCryptoResult.Success.asSuccess()

        assertFalse(vaultLockManager.isVaultUnlocked(userId = userId))

        mutableVaultTimeoutStateFlow.value = VaultTimeout.Never

        assertTrue(vaultLockManager.isVaultUnlocked(userId = userId))

        coVerify {
            vaultSdkSource.initializeCrypto(
                userId = userId,
                request = InitUserCryptoRequest(
                    kdfParams = MOCK_PROFILE.toSdkParams(),
                    email = MOCK_PROFILE.email,
                    privateKey = privateKey,
                    method = InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = userAutoUnlockKey,
                    ),
                ),
            )
        }
    }

    @Test
    fun `isVaultUnlocked should return the correct value based on the vault lock state`() =
        runTest {
            val userId = "userId"
            assertFalse(vaultLockManager.isVaultUnlocked(userId = userId))

            verifyUnlockedVault(userId = userId)

            assertTrue(vaultLockManager.isVaultUnlocked(userId = userId))
        }

    @Test
    fun `isVaultLocking should return the correct value based on the vault unlocking state`() =
        runTest {
            val userId = "userId"
            assertFalse(vaultLockManager.isVaultUnlocking(userId = userId))

            val unlockingJob = async {
                verifyUnlockingVault(userId = userId)
            }
            this.testScheduler.advanceUntilIdle()

            assertTrue(vaultLockManager.isVaultUnlocking(userId = userId))

            unlockingJob.cancel()
            this.testScheduler.advanceUntilIdle()

            assertFalse(vaultLockManager.isVaultUnlocking(userId = userId))
        }

    @Test
    fun `lockVaultIfNecessary should lock the given account if it is currently unlocked`() =
        runTest {
            val userId = "userId"
            verifyUnlockedVault(userId = userId)

            assertEquals(
                VaultState(
                    unlockedVaultUserIds = setOf(userId),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )

            vaultLockManager.lockVaultIfNecessary(userId = userId)

            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )
            verify { vaultSdkSource.clearCrypto(userId = userId) }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `lockVaultForCurrentUser should lock the vault for the current user if it is currently unlocked`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            verifyUnlockedVault(userId = userId)

            assertEquals(
                VaultState(
                    unlockedVaultUserIds = setOf(userId),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )

            vaultLockManager.lockVaultForCurrentUser()

            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )
            verify { vaultSdkSource.clearCrypto(userId = userId) }
        }

    @Test
    fun `unlockVault with initializeCrypto success should return Success`() = runTest {
        val userId = "userId"
        val kdf = MOCK_PROFILE.toSdkParams()
        val email = MOCK_PROFILE.email
        val masterPassword = "drowssap"
        val userKey = "12345"
        val privateKey = "54321"
        val organizationKeys = mapOf("orgId1" to "orgKey1")
        coEvery {
            vaultSdkSource.initializeCrypto(
                userId = userId,
                request = InitUserCryptoRequest(
                    kdfParams = kdf,
                    email = email,
                    privateKey = privateKey,
                    method = InitUserCryptoMethod.Password(
                        password = masterPassword,
                        userKey = userKey,
                    ),
                ),
            )
        } returns InitializeCryptoResult.Success.asSuccess()
        coEvery {
            vaultSdkSource.initializeOrganizationCrypto(
                userId = userId,
                request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
            )
        } returns InitializeCryptoResult.Success.asSuccess()
        assertEquals(
            VaultState(
                unlockedVaultUserIds = emptySet(),
                unlockingVaultUserIds = emptySet(),
            ),
            vaultLockManager.vaultStateFlow.value,
        )

        val result = vaultLockManager.unlockVault(
            userId = userId,
            kdf = kdf,
            email = email,
            initUserCryptoMethod = InitUserCryptoMethod.Password(
                password = masterPassword,
                userKey = userKey,
            ),
            privateKey = privateKey,
            organizationKeys = organizationKeys,
        )

        assertEquals(VaultUnlockResult.Success, result)
        assertEquals(
            VaultState(
                unlockedVaultUserIds = setOf(userId),
                unlockingVaultUserIds = emptySet(),
            ),
            vaultLockManager.vaultStateFlow.value,
        )
        coVerify(exactly = 1) {
            vaultSdkSource.initializeCrypto(
                userId = userId,
                request = InitUserCryptoRequest(
                    kdfParams = kdf,
                    email = email,
                    privateKey = privateKey,
                    method = InitUserCryptoMethod.Password(
                        password = masterPassword,
                        userKey = userKey,
                    ),
                ),
            )
        }
        coVerify(exactly = 1) {
            vaultSdkSource.initializeOrganizationCrypto(
                userId = userId,
                request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVault with initializeCrypto authentication failure for users should return AuthenticationError`() =
        runTest {
            val userId = "userId"
            val kdf = MOCK_PROFILE.toSdkParams()
            val email = MOCK_PROFILE.email
            val masterPassword = "drowssap"
            val userKey = "12345"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
            coEvery {
                vaultSdkSource.initializeCrypto(
                    userId = userId,
                    request = InitUserCryptoRequest(
                        kdfParams = kdf,
                        email = email,
                        privateKey = privateKey,
                        method = InitUserCryptoMethod.Password(
                            password = masterPassword,
                            userKey = userKey,
                        ),
                    ),
                )
            } returns InitializeCryptoResult.AuthenticationError.asSuccess()

            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )
            val result = vaultLockManager.unlockVault(
                userId = userId,
                kdf = kdf,
                email = email,
                initUserCryptoMethod = InitUserCryptoMethod.Password(
                    password = masterPassword,
                    userKey = userKey,
                ),
                privateKey = privateKey,
                organizationKeys = organizationKeys,
            )

            assertEquals(VaultUnlockResult.AuthenticationError, result)
            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )
            coVerify(exactly = 1) {
                vaultSdkSource.initializeCrypto(
                    userId = userId,
                    request = InitUserCryptoRequest(
                        kdfParams = kdf,
                        email = email,
                        privateKey = privateKey,
                        method = InitUserCryptoMethod.Password(
                            password = masterPassword,
                            userKey = userKey,
                        ),
                    ),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVault with initializeCrypto authentication failure for orgs should return AuthenticationError`() =
        runTest {
            val userId = "userId"
            val kdf = MOCK_PROFILE.toSdkParams()
            val email = MOCK_PROFILE.email
            val masterPassword = "drowssap"
            val userKey = "12345"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
            coEvery {
                vaultSdkSource.initializeCrypto(
                    userId = userId,
                    request = InitUserCryptoRequest(
                        kdfParams = kdf,
                        email = email,
                        privateKey = privateKey,
                        method = InitUserCryptoMethod.Password(
                            password = masterPassword,
                            userKey = userKey,
                        ),
                    ),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
            } returns InitializeCryptoResult.AuthenticationError.asSuccess()

            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )

            val result = vaultLockManager.unlockVault(
                userId = userId,
                kdf = kdf,
                email = email,
                initUserCryptoMethod = InitUserCryptoMethod.Password(
                    password = masterPassword,
                    userKey = userKey,
                ),
                privateKey = privateKey,
                organizationKeys = organizationKeys,
            )

            assertEquals(VaultUnlockResult.AuthenticationError, result)
            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )
            coVerify(exactly = 1) {
                vaultSdkSource.initializeCrypto(
                    userId = userId,
                    request = InitUserCryptoRequest(
                        kdfParams = kdf,
                        email = email,
                        privateKey = privateKey,
                        method = InitUserCryptoMethod.Password(
                            password = masterPassword,
                            userKey = userKey,
                        ),
                    ),
                )
            }
            coVerify(exactly = 1) {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
            }
        }

    @Test
    fun `unlockVault with initializeCrypto failure for users should return GenericError`() =
        runTest {
            val userId = "userId"
            val kdf = MOCK_PROFILE.toSdkParams()
            val email = MOCK_PROFILE.email
            val masterPassword = "drowssap"
            val userKey = "12345"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
            coEvery {
                vaultSdkSource.initializeCrypto(
                    userId = userId,
                    request = InitUserCryptoRequest(
                        kdfParams = kdf,
                        email = email,
                        privateKey = privateKey,
                        method = InitUserCryptoMethod.Password(
                            password = masterPassword,
                            userKey = userKey,
                        ),
                    ),
                )
            } returns Throwable("Fail").asFailure()
            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )

            val result = vaultLockManager.unlockVault(
                userId = userId,
                kdf = kdf,
                email = email,
                initUserCryptoMethod = InitUserCryptoMethod.Password(
                    password = masterPassword,
                    userKey = userKey,
                ),
                privateKey = privateKey,
                organizationKeys = organizationKeys,
            )

            assertEquals(VaultUnlockResult.GenericError, result)
            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )
            coVerify(exactly = 1) {
                vaultSdkSource.initializeCrypto(
                    userId = userId,
                    request = InitUserCryptoRequest(
                        kdfParams = kdf,
                        email = email,
                        privateKey = privateKey,
                        method = InitUserCryptoMethod.Password(
                            password = masterPassword,
                            userKey = userKey,
                        ),
                    ),
                )
            }
        }

    @Test
    fun `unlockVault with initializeCrypto failure for orgs should return GenericError`() =
        runTest {
            val userId = "userId"
            val kdf = MOCK_PROFILE.toSdkParams()
            val email = MOCK_PROFILE.email
            val masterPassword = "drowssap"
            val userKey = "12345"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
            coEvery {
                vaultSdkSource.initializeCrypto(
                    userId = userId,
                    request = InitUserCryptoRequest(
                        kdfParams = kdf,
                        email = email,
                        privateKey = privateKey,
                        method = InitUserCryptoMethod.Password(
                            password = masterPassword,
                            userKey = userKey,
                        ),
                    ),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
            } returns Throwable("Fail").asFailure()
            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )

            val result = vaultLockManager.unlockVault(
                userId = userId,
                kdf = kdf,
                email = email,
                initUserCryptoMethod = InitUserCryptoMethod.Password(
                    password = masterPassword,
                    userKey = userKey,
                ),
                privateKey = privateKey,
                organizationKeys = organizationKeys,
            )

            assertEquals(VaultUnlockResult.GenericError, result)
            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )
            coVerify(exactly = 1) {
                vaultSdkSource.initializeCrypto(
                    userId = userId,
                    request = InitUserCryptoRequest(
                        kdfParams = kdf,
                        email = email,
                        privateKey = privateKey,
                        method = InitUserCryptoMethod.Password(
                            password = masterPassword,
                            userKey = userKey,
                        ),
                    ),
                )
            }
            coVerify(exactly = 1) {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
            }
        }

    /**
     * Helper to ensures that the vault for the user with the given [userId] is actively unlocking.
     * Note that this call will actively hang.
     */
    private suspend fun verifyUnlockingVault(userId: String) {
        val kdf = MOCK_PROFILE.toSdkParams()
        val email = MOCK_PROFILE.email
        val masterPassword = "drowssap"
        val userKey = "12345"
        val privateKey = "54321"
        val organizationKeys = null
        coEvery {
            vaultSdkSource.initializeCrypto(
                userId = userId,
                request = InitUserCryptoRequest(
                    kdfParams = kdf,
                    email = email,
                    privateKey = privateKey,
                    method = InitUserCryptoMethod.Password(
                        password = masterPassword,
                        userKey = userKey,
                    ),
                ),
            )
        } just awaits

        vaultLockManager.unlockVault(
            userId = userId,
            kdf = kdf,
            email = email,
            privateKey = privateKey,
            initUserCryptoMethod = InitUserCryptoMethod.Password(
                password = masterPassword,
                userKey = userKey,
            ),
            organizationKeys = organizationKeys,
        )
    }

    /**
     * Helper to ensures that the vault for the user with the given [userId] is unlocked.
     */
    private suspend fun verifyUnlockedVault(userId: String) {
        val kdf = MOCK_PROFILE.toSdkParams()
        val email = MOCK_PROFILE.email
        val masterPassword = "drowssap"
        val userKey = "12345"
        val privateKey = "54321"
        val organizationKeys = null
        coEvery {
            vaultSdkSource.initializeCrypto(
                userId = userId,
                request = InitUserCryptoRequest(
                    kdfParams = kdf,
                    email = email,
                    privateKey = privateKey,
                    method = InitUserCryptoMethod.Password(
                        password = masterPassword,
                        userKey = userKey,
                    ),
                ),
            )
        } returns InitializeCryptoResult.Success.asSuccess()

        val result = vaultLockManager.unlockVault(
            userId = userId,
            kdf = kdf,
            email = email,
            privateKey = privateKey,
            initUserCryptoMethod = InitUserCryptoMethod.Password(
                password = masterPassword,
                userKey = userKey,
            ),
            organizationKeys = organizationKeys,
        )

        assertEquals(VaultUnlockResult.Success, result)
        coVerify(exactly = 1) {
            vaultSdkSource.initializeCrypto(
                userId = userId,
                request = InitUserCryptoRequest(
                    kdfParams = kdf,
                    email = email,
                    privateKey = privateKey,
                    method = InitUserCryptoMethod.Password(
                        password = masterPassword,
                        userKey = userKey,
                    ),
                ),
            )
        }
    }
}

private val MOCK_PROFILE = AccountJson.Profile(
    userId = "mockId-1",
    email = "email",
    isEmailVerified = true,
    name = null,
    stamp = null,
    organizationId = null,
    avatarColorHex = null,
    hasPremium = false,
    forcePasswordResetReason = null,
    kdfType = null,
    kdfIterations = null,
    kdfMemory = null,
    kdfParallelism = null,
    userDecryptionOptions = null,
)

private val MOCK_ACCOUNT = AccountJson(
    profile = MOCK_PROFILE,
    tokens = AccountJson.Tokens(
        accessToken = "accessToken",
        refreshToken = "refreshToken",
    ),
    settings = AccountJson.Settings(
        environmentUrlData = null,
    ),
)

private val MOCK_USER_STATE = UserStateJson(
    activeUserId = "mockId-1",
    accounts = mapOf(
        "mockId-1" to MOCK_ACCOUNT,
    ),
)
