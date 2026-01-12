package com.x8bit.bitwarden.data.vault.manager

import app.cash.turbine.test
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.createMockPolicy
import com.bitwarden.network.model.createMockSyncResponse
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.model.NetworkConnection
import com.x8bit.bitwarden.data.platform.manager.model.NetworkSignalStrength
import com.x8bit.bitwarden.data.platform.manager.util.FakeNetworkConnectionManager
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.manager.model.VaultMigrationData
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZonedDateTime

class VaultMigrationManagerTest {

    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val fakeDispatcherManager = FakeDispatcherManager()

    private val mutableHasPersonalCiphersFlow = MutableStateFlow(false)
    private val vaultDiskSource: VaultDiskSource = mockk {
        every { hasPersonalCiphersFlow(any()) } returns mutableHasPersonalCiphersFlow
    }

    private val mutableVaultUnlockDataStateFlow =
        MutableStateFlow<List<VaultUnlockData>>(emptyList())
    private val vaultLockManager: VaultLockManager = mockk {
        every { vaultUnlockDataStateFlow } returns mutableVaultUnlockDataStateFlow
    }

    private val mutableLastSyncTimeFlow =
        MutableStateFlow<Instant?>(Instant.parse("2023-10-27T12:00:00Z"))
    private val settingsDiskSource: SettingsDiskSource = mockk {
        every { getLastSyncTimeFlow(any()) } returns mutableLastSyncTimeFlow
    }

    private val policyManager: PolicyManager = mockk {
        every { getActivePolicies(any()) } returns emptyList()
        every { getPersonalOwnershipPolicyOrganizationId() } returns null
    }

    private val mockFeatureFlagManager = mockk<FeatureFlagManager>(relaxed = true) {
        every { getFeatureFlag(FlagKey.MigrateMyVaultToMyItems) } returns true
    }

    private val fakeNetworkConnectionManager = FakeNetworkConnectionManager(
        isNetworkConnected = true,
        networkConnection = NetworkConnection.Wifi(strength = NetworkSignalStrength.GOOD),
    )

    private fun createVaultMigrationManager(): VaultMigrationManager =
        VaultMigrationManagerImpl(
            authDiskSource = fakeAuthDiskSource,
            vaultDiskSource = vaultDiskSource,
            settingsDiskSource = settingsDiskSource,
            vaultLockManager = vaultLockManager,
            policyManager = policyManager,
            featureFlagManager = mockFeatureFlagManager,
            connectionManager = fakeNetworkConnectionManager,
            dispatcherManager = fakeDispatcherManager,
        )

    @Test
    fun `vaultMigrationDataStateFlow should initially emit NoMigrationRequired`() = runTest {
        val vaultMigrationManager = createVaultMigrationManager()
        assertEquals(
            VaultMigrationData.NoMigrationRequired,
            vaultMigrationManager.vaultMigrationDataStateFlow.value,
        )
    }

    @Test
    fun `should emit MigrationRequired when vault unlocks with all conditions met`() = runTest {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        // Setup conditions for migration
        val mockPolicy = createMockPolicy(number = 1, type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        every {
            policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns listOf(mockPolicy)
        every {
            policyManager.getPersonalOwnershipPolicyOrganizationId()
        } returns "mockId-1"

        // Store organizations
        val syncResponse = createMockSyncResponse(number = 1)
        fakeAuthDiskSource.storeOrganizations(
            userId = userId,
            organizations = syncResponse.profile.organizations,
        )

        val vaultMigrationManager = createVaultMigrationManager()

        vaultMigrationManager.vaultMigrationDataStateFlow.test {
            assertEquals(VaultMigrationData.NoMigrationRequired, awaitItem())

            // Simulate vault unlock
            mutableVaultUnlockDataStateFlow.value = listOf(
                VaultUnlockData(
                    userId = userId,
                    status = VaultUnlockData.Status.UNLOCKED,
                ),
            )

            // Emit personal ciphers exist
            mutableHasPersonalCiphersFlow.value = true

            assertEquals(
                VaultMigrationData.MigrationRequired(
                    organizationId = "mockId-1",
                    organizationName = "mockName-1",
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `should emit NoMigrationRequired when no personal ownership policy`() = runTest {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        every {
            policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns emptyList()

        val vaultMigrationManager = createVaultMigrationManager()

        vaultMigrationManager.vaultMigrationDataStateFlow.test {
            assertEquals(VaultMigrationData.NoMigrationRequired, awaitItem())

            // Simulate vault unlock
            mutableVaultUnlockDataStateFlow.value = listOf(
                VaultUnlockData(
                    userId = userId,
                    status = VaultUnlockData.Status.UNLOCKED,
                ),
            )

            // Emit personal ciphers exist
            mutableHasPersonalCiphersFlow.value = true

            // Should remain NoMigrationRequired since no policy
            expectNoEvents()
        }
    }

    @Test
    fun `should emit NoMigrationRequired when feature flag is disabled`() = runTest {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        val mockPolicy = createMockPolicy(number = 1, type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        every {
            policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns listOf(mockPolicy)

        every {
            mockFeatureFlagManager.getFeatureFlag(FlagKey.MigrateMyVaultToMyItems)
        } returns false

        val vaultMigrationManager = createVaultMigrationManager()

        vaultMigrationManager.vaultMigrationDataStateFlow.test {
            assertEquals(VaultMigrationData.NoMigrationRequired, awaitItem())

            // Simulate vault unlock
            mutableVaultUnlockDataStateFlow.value = listOf(
                VaultUnlockData(
                    userId = userId,
                    status = VaultUnlockData.Status.UNLOCKED,
                ),
            )

            // Emit personal ciphers exist
            mutableHasPersonalCiphersFlow.value = true

            // Should remain NoMigrationRequired since feature flag is disabled
            expectNoEvents()
        }
    }

    @Test
    fun `should emit NoMigrationRequired when no network connection`() = runTest {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        fakeNetworkConnectionManager.fakeIsNetworkConnected = false

        val mockPolicy = createMockPolicy(number = 1, type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        every {
            policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns listOf(mockPolicy)
        every {
            policyManager.getPersonalOwnershipPolicyOrganizationId()
        } returns "mockId-1"

        val syncResponse = createMockSyncResponse(number = 1)
        fakeAuthDiskSource.storeOrganizations(
            userId = userId,
            organizations = syncResponse.profile.organizations,
        )

        val vaultMigrationManager = createVaultMigrationManager()

        vaultMigrationManager.vaultMigrationDataStateFlow.test {
            assertEquals(VaultMigrationData.NoMigrationRequired, awaitItem())

            // Simulate vault unlock
            mutableVaultUnlockDataStateFlow.value = listOf(
                VaultUnlockData(
                    userId = userId,
                    status = VaultUnlockData.Status.UNLOCKED,
                ),
            )

            // Emit personal ciphers exist
            mutableHasPersonalCiphersFlow.value = true

            // Should remain NoMigrationRequired since no network
            expectNoEvents()
        }
    }

    @Test
    fun `should emit NoMigrationRequired when no personal ciphers exist`() = runTest {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        val mockPolicy = createMockPolicy(number = 1, type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        every {
            policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns listOf(mockPolicy)

        val vaultMigrationManager = createVaultMigrationManager()

        vaultMigrationManager.vaultMigrationDataStateFlow.test {
            assertEquals(VaultMigrationData.NoMigrationRequired, awaitItem())

            // Simulate vault unlock
            mutableVaultUnlockDataStateFlow.value = listOf(
                VaultUnlockData(
                    userId = userId,
                    status = VaultUnlockData.Status.UNLOCKED,
                ),
            )

            // Emit no personal ciphers
            mutableHasPersonalCiphersFlow.value = false

            // Should remain NoMigrationRequired since no personal ciphers
            expectNoEvents()
        }
    }

    @Test
    fun `should emit NoMigrationRequired when organization ID is null`() = runTest {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        val mockPolicy = createMockPolicy(number = 1, type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        every {
            policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns listOf(mockPolicy)
        every {
            policyManager.getPersonalOwnershipPolicyOrganizationId()
        } returns null

        val vaultMigrationManager = createVaultMigrationManager()

        vaultMigrationManager.vaultMigrationDataStateFlow.test {
            assertEquals(VaultMigrationData.NoMigrationRequired, awaitItem())

            // Simulate vault unlock
            mutableVaultUnlockDataStateFlow.value = listOf(
                VaultUnlockData(
                    userId = userId,
                    status = VaultUnlockData.Status.UNLOCKED,
                ),
            )

            // Emit personal ciphers exist
            mutableHasPersonalCiphersFlow.value = true

            // Should remain NoMigrationRequired since organization ID is null
            expectNoEvents()
        }
    }

    @Test
    fun `should emit NoMigrationRequired when organizations list is null`() = runTest {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        val mockPolicy = createMockPolicy(number = 1, type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        every {
            policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns listOf(mockPolicy)
        every {
            policyManager.getPersonalOwnershipPolicyOrganizationId()
        } returns "mockId-1"

        // Store organizations as null in authDiskSource
        fakeAuthDiskSource.storeOrganizations(userId = userId, organizations = null)

        val vaultMigrationManager = createVaultMigrationManager()

        vaultMigrationManager.vaultMigrationDataStateFlow.test {
            assertEquals(VaultMigrationData.NoMigrationRequired, awaitItem())

            // Simulate vault unlock
            mutableVaultUnlockDataStateFlow.value = listOf(
                VaultUnlockData(
                    userId = userId,
                    status = VaultUnlockData.Status.UNLOCKED,
                ),
            )

            // Emit personal ciphers exist
            mutableHasPersonalCiphersFlow.value = true

            // Should remain NoMigrationRequired since organizations list is null
            expectNoEvents()
        }
    }

    @Test
    fun `should emit NoMigrationRequired when organization not found`() = runTest {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        val mockPolicy = createMockPolicy(number = 1, type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        every {
            policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns listOf(mockPolicy)
        // Return a different org ID that doesn't match the one in the sync response
        every {
            policyManager.getPersonalOwnershipPolicyOrganizationId()
        } returns "non-existent-org-id"

        // Store organizations with a different ID
        val syncResponse = createMockSyncResponse(number = 1)
        fakeAuthDiskSource.storeOrganizations(
            userId = userId,
            organizations = syncResponse.profile.organizations,
        )

        val vaultMigrationManager = createVaultMigrationManager()

        vaultMigrationManager.vaultMigrationDataStateFlow.test {
            assertEquals(VaultMigrationData.NoMigrationRequired, awaitItem())

            // Simulate vault unlock
            mutableVaultUnlockDataStateFlow.value = listOf(
                VaultUnlockData(
                    userId = userId,
                    status = VaultUnlockData.Status.UNLOCKED,
                ),
            )

            // Emit personal ciphers exist
            mutableHasPersonalCiphersFlow.value = true

            // Should remain NoMigrationRequired since organization is not found
            expectNoEvents()
        }
    }

    @Test
    fun `should not emit when vault is locked`() = runTest {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        val mockPolicy = createMockPolicy(number = 1, type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        every {
            policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns listOf(mockPolicy)
        every {
            policyManager.getPersonalOwnershipPolicyOrganizationId()
        } returns "mockId-1"

        val syncResponse = createMockSyncResponse(number = 1)
        fakeAuthDiskSource.storeOrganizations(
            userId = userId,
            organizations = syncResponse.profile.organizations,
        )

        val vaultMigrationManager = createVaultMigrationManager()

        vaultMigrationManager.vaultMigrationDataStateFlow.test {
            assertEquals(VaultMigrationData.NoMigrationRequired, awaitItem())

            // Vault is locked - no unlock data
            mutableVaultUnlockDataStateFlow.value = emptyList()

            // Emit personal ciphers exist - should not trigger migration check since
            // vault is locked
            mutableHasPersonalCiphersFlow.value = true

            // Should remain NoMigrationRequired since vault is locked
            expectNoEvents()
        }
    }

    @Test
    fun `should not emit when sync has not occurred yet`() = runTest {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        // Set sync time as null (never synced)
        mutableLastSyncTimeFlow.value = null

        val mockPolicy = createMockPolicy(number = 1, type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        every {
            policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns listOf(mockPolicy)
        every {
            policyManager.getPersonalOwnershipPolicyOrganizationId()
        } returns "mockId-1"

        val syncResponse = createMockSyncResponse(number = 1)
        fakeAuthDiskSource.storeOrganizations(
            userId = userId,
            organizations = syncResponse.profile.organizations,
        )

        val vaultMigrationManager = createVaultMigrationManager()

        vaultMigrationManager.vaultMigrationDataStateFlow.test {
            assertEquals(VaultMigrationData.NoMigrationRequired, awaitItem())

            // Simulate vault unlock
            mutableVaultUnlockDataStateFlow.value = listOf(
                VaultUnlockData(
                    userId = userId,
                    status = VaultUnlockData.Status.UNLOCKED,
                ),
            )

            // Emit personal ciphers exist - should not trigger since no sync has occurred
            mutableHasPersonalCiphersFlow.value = true

            // Should remain NoMigrationRequired since sync hasn't happened
            expectNoEvents()
        }
    }

    @Test
    fun `should emit MigrationRequired when sync completes after vault unlock`() = runTest {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        // Start with sync time as null (never synced) - simulates multi-account scenario
        // where lastSyncTime was cleared without clearing cipher data
        mutableLastSyncTimeFlow.value = null

        val mockPolicy = createMockPolicy(number = 1, type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        every {
            policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns listOf(mockPolicy)
        every {
            policyManager.getPersonalOwnershipPolicyOrganizationId()
        } returns "mockId-1"

        val syncResponse = createMockSyncResponse(number = 1)
        fakeAuthDiskSource.storeOrganizations(
            userId = userId,
            organizations = syncResponse.profile.organizations,
        )

        val vaultMigrationManager = createVaultMigrationManager()

        vaultMigrationManager.vaultMigrationDataStateFlow.test {
            assertEquals(VaultMigrationData.NoMigrationRequired, awaitItem())

            // Simulate vault unlock
            mutableVaultUnlockDataStateFlow.value = listOf(
                VaultUnlockData(
                    userId = userId,
                    status = VaultUnlockData.Status.UNLOCKED,
                ),
            )

            // Emit personal ciphers exist - should not trigger yet since no sync
            mutableHasPersonalCiphersFlow.value = true

            // Should remain NoMigrationRequired since sync hasn't happened
            expectNoEvents()

            // Sync completes - should now trigger migration check
            mutableLastSyncTimeFlow.value = Instant.parse("2023-10-27T12:00:00Z")

            assertEquals(
                VaultMigrationData.MigrationRequired(
                    organizationId = "mockId-1",
                    organizationName = "mockName-1",
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `should emit MigrationRequired when network connectivity changes from offline to online`() =
        runTest {
            val userId = "mockId-1"
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            fakeNetworkConnectionManager.fakeIsNetworkConnected = false

            val mockPolicy = createMockPolicy(number = 1, type = PolicyTypeJson.PERSONAL_OWNERSHIP)
            every {
                policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
            } returns listOf(mockPolicy)
            every {
                policyManager.getPersonalOwnershipPolicyOrganizationId()
            } returns "mockId-1"

            val syncResponse = createMockSyncResponse(number = 1)
            fakeAuthDiskSource.storeOrganizations(
                userId = userId,
                organizations = syncResponse.profile.organizations,
            )

            val vaultMigrationManager = createVaultMigrationManager()

            vaultMigrationManager.vaultMigrationDataStateFlow.test {
                assertEquals(VaultMigrationData.NoMigrationRequired, awaitItem())

                // Simulate vault unlock while offline
                mutableVaultUnlockDataStateFlow.value = listOf(
                    VaultUnlockData(
                        userId = userId,
                        status = VaultUnlockData.Status.UNLOCKED,
                    ),
                )

                // Emit personal ciphers exist - still offline
                mutableHasPersonalCiphersFlow.value = true

                // Should remain NoMigrationRequired since no network
                expectNoEvents()

                // Network comes online - should now emit MigrationRequired
                fakeNetworkConnectionManager.fakeIsNetworkConnected = true

                assertEquals(
                    VaultMigrationData.MigrationRequired(
                        organizationId = "mockId-1",
                        organizationName = "mockName-1",
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `should emit NoMigrationRequired when network connectivity changes from online to offline`() =
        runTest {
            val userId = "mockId-1"
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            val mockPolicy = createMockPolicy(number = 1, type = PolicyTypeJson.PERSONAL_OWNERSHIP)
            every {
                policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
            } returns listOf(mockPolicy)
            every {
                policyManager.getPersonalOwnershipPolicyOrganizationId()
            } returns "mockId-1"

            val syncResponse = createMockSyncResponse(number = 1)
            fakeAuthDiskSource.storeOrganizations(
                userId = userId,
                organizations = syncResponse.profile.organizations,
            )

            val vaultMigrationManager = createVaultMigrationManager()

            vaultMigrationManager.vaultMigrationDataStateFlow.test {
                assertEquals(VaultMigrationData.NoMigrationRequired, awaitItem())

                // Simulate vault unlock while online
                mutableVaultUnlockDataStateFlow.value = listOf(
                    VaultUnlockData(
                        userId = userId,
                        status = VaultUnlockData.Status.UNLOCKED,
                    ),
                )

                // Emit personal ciphers exist
                mutableHasPersonalCiphersFlow.value = true

                // Should emit MigrationRequired since all conditions met
                assertEquals(
                    VaultMigrationData.MigrationRequired(
                        organizationId = "mockId-1",
                        organizationName = "mockName-1",
                    ),
                    awaitItem(),
                )

                // Network goes offline - should revert to NoMigrationRequired
                fakeNetworkConnectionManager.fakeIsNetworkConnected = false

                assertEquals(VaultMigrationData.NoMigrationRequired, awaitItem())
            }
        }
}

private val MOCK_USER_STATE = UserStateJson(
    activeUserId = "mockId-1",
    accounts = mapOf(
        "mockId-1" to AccountJson(
            profile = AccountJson.Profile(
                userId = "mockId-1",
                email = "test@bitwarden.com",
                isEmailVerified = true,
                name = "Bitwarden Tester",
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
                isTwoFactorEnabled = false,
                creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
            ),
            tokens = AccountTokensJson(
                accessToken = "accessToken",
                refreshToken = "refreshToken",
            ),
            settings = AccountJson.Settings(
                environmentUrlData = null,
            ),
        ),
    ),
)
