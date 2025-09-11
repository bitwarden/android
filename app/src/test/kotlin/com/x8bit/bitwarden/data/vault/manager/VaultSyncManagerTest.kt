package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.createMockCipher
import com.bitwarden.network.model.createMockOrganization
import com.bitwarden.network.model.createMockOrganizationKeys
import com.bitwarden.network.model.createMockPolicy
import com.bitwarden.network.model.createMockProfile
import com.bitwarden.network.model.createMockSyncResponse
import com.bitwarden.network.service.SyncService
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.manager.UserLogoutManager
import com.x8bit.bitwarden.data.auth.repository.model.LogoutReason
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult
import com.x8bit.bitwarden.data.vault.manager.model.SyncVaultDataResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class VaultSyncManagerTest {

    private val clock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )
    private val syncService: SyncService = mockk {
        coEvery {
            getAccountRevisionDateMillis()
        } returns clock.instant().plus(1, ChronoUnit.MINUTES).toEpochMilli().asSuccess()
    }
    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val settingsDiskSource = mockk<SettingsDiskSource> {
        every { getLastSyncTime(userId = any()) } returns clock.instant()
        every { storeLastSyncTime(userId = any(), lastSyncTime = any()) } just runs
    }
    private val mutableGetCiphersFlow: MutableStateFlow<List<SyncResponseJson.Cipher>> =
        MutableStateFlow(listOf(createMockCipher(1)))
    private val vaultDiskSource: VaultDiskSource = mockk {
        coEvery { resyncVaultData(any()) } just runs
        every { getCiphersFlow(any()) } returns mutableGetCiphersFlow
    }
    private val vaultSdkSource: VaultSdkSource = mockk {
        every { clearCrypto(userId = any()) } just runs
    }
    private val userLogoutManager: UserLogoutManager = mockk {
        every { softLogout(any(), any()) } just runs
    }
    private val vaultSyncManager = VaultSyncManagerImpl(
        syncService = syncService,
        settingsDiskSource = settingsDiskSource,
        authDiskSource = fakeAuthDiskSource,
        vaultDiskSource = vaultDiskSource,
        vaultSdkSource = vaultSdkSource,
        userLogoutManager = userLogoutManager,
        clock = clock,
    )

    @Test
    fun `sync with forced should skip checks and call the syncService sync`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery { syncService.sync() } returns Throwable("failure").asFailure()

        vaultSyncManager.sync(userId = "mockId-1", forced = true)

        coVerify(exactly = 0) {
            syncService.getAccountRevisionDateMillis()
        }
        coVerify(exactly = 1) {
            syncService.sync()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `sync with syncService Success should unlock the vault for orgs if necessary and update AuthDiskSource and VaultDiskSource`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockSyncResponse = createMockSyncResponse(number = 1)
            coEvery { syncService.sync() } returns mockSyncResponse.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(
                        organizationKeys = createMockOrganizationKeys(1),
                    ),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                vaultDiskSource.replaceVaultData(
                    userId = MOCK_USER_STATE.activeUserId,
                    vault = mockSyncResponse,
                )
            } just runs
            every {
                settingsDiskSource.storeLastSyncTime(MOCK_USER_STATE.activeUserId, clock.instant())
            } just runs

            vaultSyncManager.sync(
                userId = MOCK_USER_STATE.activeUserId,
                forced = false,
            )

            val updatedUserState = MOCK_USER_STATE
                .copy(
                    accounts = mapOf(
                        "mockId-1" to MOCK_ACCOUNT.copy(
                            profile = MOCK_PROFILE.copy(
                                avatarColorHex = "mockAvatarColor-1",
                                stamp = "mockSecurityStamp-1",
                            ),
                        ),
                    ),
                )
            fakeAuthDiskSource.assertUserState(
                userState = updatedUserState,
            )
            fakeAuthDiskSource.assertUserKey(
                userId = "mockId-1",
                userKey = "mockKey-1",
            )
            fakeAuthDiskSource.assertPrivateKey(
                userId = "mockId-1",
                privateKey = "mockPrivateKey-1",
            )
            fakeAuthDiskSource.assertOrganizationKeys(
                userId = "mockId-1",
                organizationKeys = mapOf("mockId-1" to "mockKey-1"),
            )
            fakeAuthDiskSource.assertOrganizations(
                userId = "mockId-1",
                organizations = listOf(createMockOrganization(number = 1)),
            )
            fakeAuthDiskSource.assertPolicies(
                userId = "mockId-1",
                policies = listOf(createMockPolicy(number = 1)),
            )
            fakeAuthDiskSource.assertShouldUseKeyConnector(
                userId = "mockId-1",
                shouldUseKeyConnector = false,
            )
            coVerify {
                vaultDiskSource.replaceVaultData(
                    userId = MOCK_USER_STATE.activeUserId,
                    vault = mockSyncResponse,
                )
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(
                        organizationKeys = createMockOrganizationKeys(1),
                    ),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `sync with syncService Success with a different security stamp should logout and return early`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockSyncResponse = createMockSyncResponse(number = 1)
            coEvery { syncService.sync() } returns mockSyncResponse
                .copy(profile = createMockProfile(number = 1).copy(securityStamp = "newStamp"))
                .asSuccess()

            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(
                        organizationKeys = createMockOrganizationKeys(1),
                    ),
                )
            } returns InitializeCryptoResult.Success.asSuccess()

            vaultSyncManager.sync(
                userId = MOCK_USER_STATE.activeUserId,
                forced = false,
            )

            coVerify(exactly = 1) {
                userLogoutManager.softLogout(userId = userId, reason = LogoutReason.SecurityStamp)
            }

            coVerify(exactly = 0) {
                vaultDiskSource.replaceVaultData(
                    userId = MOCK_USER_STATE.activeUserId,
                    vault = any(),
                )
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(
                        organizationKeys = createMockOrganizationKeys(1),
                    ),
                )
            }
        }

    @Test
    fun `sync should return error when getAccountRevisionDateMillis fails`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val throwable = Throwable()
            coEvery {
                syncService.getAccountRevisionDateMillis()
            } returns throwable.asFailure()
            val syncResult = vaultSyncManager.sync(
                userId = MOCK_USER_STATE.activeUserId,
                forced = false,
            )
            assertEquals(
                SyncVaultDataResult.Error(throwable = throwable),
                syncResult,
            )
        }

    @Test
    fun `sync when the last sync time is more recent than the revision date should not sync `() =
        runTest {
            val userId = "mockId-1"
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            every {
                settingsDiskSource.getLastSyncTime(userId = userId)
            } returns clock.instant().plus(2, ChronoUnit.MINUTES)

            vaultSyncManager.sync(
                userId = userId,
                forced = false,
            )

            coVerify(exactly = 0) { syncService.sync() }
        }
}

private val MOCK_PROFILE = AccountJson.Profile(
    userId = "mockId-1",
    email = "email",
    isEmailVerified = true,
    name = null,
    stamp = "mockSecurityStamp-1",
    organizationId = null,
    avatarColorHex = null,
    hasPremium = false,
    forcePasswordResetReason = null,
    kdfType = null,
    kdfIterations = null,
    kdfMemory = null,
    kdfParallelism = null,
    userDecryptionOptions = null,
    isTwoFactorEnabled = false,
    creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
)

private val MOCK_ACCOUNT = AccountJson(
    profile = MOCK_PROFILE,
    tokens = AccountTokensJson(
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
