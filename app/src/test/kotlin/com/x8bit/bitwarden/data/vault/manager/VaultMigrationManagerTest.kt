package com.x8bit.bitwarden.data.vault.manager

import app.cash.turbine.test
import com.bitwarden.collections.CollectionView
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.createMockCipher
import com.bitwarden.network.model.createMockCipherMiniResponseJson
import com.bitwarden.network.model.createMockPolicy
import com.bitwarden.network.model.createMockSyncResponse
import com.bitwarden.send.SendView
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.FolderView
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
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockDecryptCipherListResult
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockEncryptionContext
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkCipher
import com.x8bit.bitwarden.data.vault.manager.model.GetCipherResult
import com.x8bit.bitwarden.data.vault.manager.model.VaultMigrationData
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.MigratePersonalVaultResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZonedDateTime

@Suppress("LargeClass")
class VaultMigrationManagerTest {

    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val fakeDispatcherManager = FakeDispatcherManager()

    private val mutableHasPersonalCiphersFlow = MutableStateFlow(false)
    private val vaultDiskSource: VaultDiskSource = mockk(relaxed = true) {
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

    private val mutableVaultDataFlow = MutableStateFlow<DataState<VaultData>>(
        value = DataState.Loading,
    )

    private val vaultRepository: VaultRepository = mockk {
        every { vaultDataStateFlow } returns mutableVaultDataFlow
        coEvery {
            getCipher(any())
        } returns GetCipherResult.Success(
            cipherView = createMockCipherView(number = 1),
        )
    }

    private val vaultSdkSource: VaultSdkSource =
        mockk(relaxed = true)

    private val ciphersService: com.bitwarden.network.service.CiphersService =
        mockk(relaxed = true)

    private fun createVaultMigrationManager(): VaultMigrationManager =
        VaultMigrationManagerImpl(
            authDiskSource = fakeAuthDiskSource,
            vaultDiskSource = vaultDiskSource,
            vaultRepository = vaultRepository,
            vaultSdkSource = vaultSdkSource,
            ciphersService = ciphersService,
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

    @Test
    fun `migratePersonalVault should succeed and call migrateAttachments for each cipher`() =
        runTest {
            val userId = "mockId-1"
            val organizationId = "mockOrganizationId-1"

            mutableVaultDataFlow.value = DataState.Loaded(
                data = createVaultData(
                    cipherListView = createMockCipherListView(number = 1, organizationId = null),
                    collectionViewList = listOf(
                        createMockCollectionView(
                            number = 1,
                            type = com.bitwarden.collections.CollectionType.DEFAULT_USER_COLLECTION,
                        ),
                    ),
                ),
            )

            coEvery {
                vaultRepository.getCipher(any())
            } returns GetCipherResult.Success(createMockCipherView(number = 1))
            coEvery {
                vaultDiskSource.getSelectedCiphers(userId, any())
            } returns listOf(
                createMockCipher(number = 1, organizationId = null),
            )
            coEvery {
                vaultRepository.migrateAttachments(any(), any())
            } returns Result.success(
                createMockCipherView(number = 1),
            )
            coEvery {
                vaultSdkSource.bulkMoveToOrganization(
                    userId = any(),
                    organizationId = any(),
                    cipherViews = any(),
                    collectionIds = any(),
                )
            } returns Result.success(
                listOf(
                    createMockEncryptionContext(
                        number = 1,
                        cipher = createMockSdkCipher(number = 1),
                    ),
                ),
            )
            coEvery {
                ciphersService.bulkShareCiphers(any())
            } returns Result.success(createMockCipherMiniResponseJson(1))

            val vaultMigrationManager = createVaultMigrationManager()
            val result = vaultMigrationManager.migratePersonalVault(userId, organizationId)

            assertTrue(result is MigratePersonalVaultResult.Success)
            coVerify(exactly = 1) { vaultRepository.migrateAttachments(userId, any()) }
            coVerify(exactly = 1) { vaultDiskSource.saveCipher(userId, any()) }
        }

    @Test
    fun `migratePersonalVault should fail when attachment migration fails`() = runTest {
        val userId = "mockId-1"
        val organizationId = "mockOrganizationId-1"

        mutableVaultDataFlow.value = DataState.Loaded(
            data = createVaultData(
                cipherListView = createMockCipherListView(number = 1, organizationId = null),
                collectionViewList = listOf(
                    createMockCollectionView(
                        number = 1,
                        type = com.bitwarden.collections.CollectionType.DEFAULT_USER_COLLECTION,
                    ),
                ),
            ),
        )

        coEvery {
            vaultRepository.getCipher(any())
        } returns GetCipherResult.Success(createMockCipherView(number = 1))
        coEvery {
            vaultDiskSource.getSelectedCiphers(userId, any())
        } returns listOf(
            createMockCipher(number = 1, organizationId = null),
        )

        val attachmentError = IllegalStateException("Attachment migration failed")
        coEvery {
            vaultRepository.migrateAttachments(any(), any())
        } returns Result.failure(attachmentError)

        val vaultMigrationManager = createVaultMigrationManager()
        val result = vaultMigrationManager.migratePersonalVault(userId, organizationId)

        assertTrue(result is MigratePersonalVaultResult.Failure)
        assertEquals(
            attachmentError,
            (result as MigratePersonalVaultResult.Failure).error,
        )
        coVerify { vaultRepository.migrateAttachments(userId, any()) }
        coVerify(exactly = 0) {
            vaultSdkSource.bulkMoveToOrganization(
                userId = any(),
                organizationId = any(),
                cipherViews = any(),
                collectionIds = any(),
            )
        }
    }

    @Test
    fun `migratePersonalVault should fail when vault data is not available`() = runTest {
        val userId = "mockId-1"
        val organizationId = "mockOrganizationId-1"

        // Setup mocks - vault data is null
        val mockDataState = mockk<DataState<VaultData>> {
            every { data } returns null
        }
        every { vaultRepository.vaultDataStateFlow } returns MutableStateFlow(mockDataState)

        val vaultMigrationManager = createVaultMigrationManager()
        val result = vaultMigrationManager.migratePersonalVault(userId, organizationId)

        assertTrue(result is MigratePersonalVaultResult.Failure)
        assertTrue((result as MigratePersonalVaultResult.Failure).error is IllegalStateException)
        assertEquals(
            "Vault data not available",
            (result as MigratePersonalVaultResult.Failure).error?.message,
        )
    }

    @Test
    fun `migratePersonalVault should fail when default collection not found`() = runTest {
        val userId = "mockId-1"
        val organizationId = "mockOrganizationId-1"
        val cipherListView =
            createMockCipherListView(number = 2, organizationId = "mockOrganizationId-fail")

        mutableVaultDataFlow.value = DataState.Loaded(
            data = createVaultData(cipherListView = cipherListView),
        )

        val vaultMigrationManager = createVaultMigrationManager()
        val result = vaultMigrationManager.migratePersonalVault(userId, organizationId)

        assertTrue(result is MigratePersonalVaultResult.Failure)
        assertTrue((result as MigratePersonalVaultResult.Failure).error is IllegalStateException)
        assertEquals(
            "Default user collection not found for organization",
            (result as MigratePersonalVaultResult.Failure).error?.message,
        )
    }

    @Test
    fun `migratePersonalVault should succeed immediately when no personal ciphers exist`() =
        runTest {
            val userId = "mockId-1"
            val organizationId = "mockOrganizationId-1"
            mutableVaultDataFlow.value = DataState.Loaded(
                data = createVaultData(
                    collectionViewList = listOf(
                        createMockCollectionView(
                            number = 1,
                            type = com.bitwarden.collections.CollectionType.DEFAULT_USER_COLLECTION,
                        ),
                    ),
                ),
            )

            val vaultMigrationManager = createVaultMigrationManager()
            val result = vaultMigrationManager.migratePersonalVault(userId, organizationId)

            assertTrue(result is MigratePersonalVaultResult.Success)
            coVerify(exactly = 0) {
                vaultRepository.migrateAttachments(
                    userId = any(),
                    cipherView = any(),
                )
            }
            coVerify(exactly = 0) {
                vaultSdkSource.bulkMoveToOrganization(
                    userId = any(),
                    organizationId = any(),
                    cipherViews = any(),
                    collectionIds = any(),
                )
            }
        }

    @Test
    fun `migratePersonalVault should fail when cipher decryption fails`() = runTest {
        val userId = "mockId-1"
        val organizationId = "mockOrganizationId-1"

        mutableVaultDataFlow.value = DataState.Loaded(
            data = createVaultData(
                cipherListView = createMockCipherListView(number = 1, organizationId = null),
                collectionViewList = listOf(
                    createMockCollectionView(
                        number = 1,
                        type = com.bitwarden.collections.CollectionType.DEFAULT_USER_COLLECTION,
                    ),
                ),
            ),
        )

        coEvery {
            vaultRepository.getCipher(any())
        } returns GetCipherResult.Failure(IllegalStateException("Decryption failed"))

        val vaultMigrationManager = createVaultMigrationManager()
        val result = vaultMigrationManager.migratePersonalVault(userId, organizationId)

        // Should fail when decryption fails (fail-fast behavior)
        assertTrue(result is MigratePersonalVaultResult.Failure)
        assertEquals(
            "Decryption failed",
            (result as MigratePersonalVaultResult.Failure).error?.message,
        )
        coVerify(exactly = 0) { vaultRepository.migrateAttachments(any(), any()) }
        coVerify(exactly = 0) { vaultDiskSource.getSelectedCiphers(any(), any()) }
    }

    @Test
    fun `migratePersonalVault should fail when bulkMoveToOrganization fails`() = runTest {
        val userId = "mockId-1"
        val organizationId = "mockOrganizationId-1"
        mutableVaultDataFlow.value = DataState.Loaded(
            data = createVaultData(
                cipherListView = createMockCipherListView(number = 1, organizationId = null),
                collectionViewList = listOf(
                    createMockCollectionView(
                        number = 1,
                        type = com.bitwarden.collections.CollectionType.DEFAULT_USER_COLLECTION,
                    ),
                ),
            ),
        )
        coEvery {
            vaultRepository.getCipher(any())
        } returns GetCipherResult.Success(createMockCipherView(number = 1))
        coEvery {
            vaultDiskSource.getSelectedCiphers(userId, any())
        } returns listOf(
            createMockCipher(number = 1, organizationId = null),
        )
        coEvery {
            vaultRepository.migrateAttachments(any(), any())
        } returns Result.success(
            value = createMockCipherView(number = 1),
        )

        val encryptionError = IllegalStateException("Encryption failed")
        coEvery {
            vaultSdkSource.bulkMoveToOrganization(
                userId = any(),
                organizationId = any(),
                cipherViews = any(),
                collectionIds = any(),
            )
        } returns Result.failure(encryptionError)

        val vaultMigrationManager = createVaultMigrationManager()
        val result = vaultMigrationManager.migratePersonalVault(userId, organizationId)

        assertTrue(result is MigratePersonalVaultResult.Failure)
        assertEquals(encryptionError, (result as MigratePersonalVaultResult.Failure).error)
    }

    @Test
    fun `migratePersonalVault should fail when bulkShareCiphers fails`() = runTest {
        val userId = "mockId-1"
        val organizationId = "mockOrganizationId-1"
        mutableVaultDataFlow.value = DataState.Loaded(
            data = createVaultData(
                cipherListView = createMockCipherListView(number = 1, organizationId = null),
                collectionViewList = listOf(
                    createMockCollectionView(
                        number = 1,
                        type = com.bitwarden.collections.CollectionType.DEFAULT_USER_COLLECTION,
                    ),
                ),
            ),
        )
        coEvery {
            vaultRepository.getCipher(any())
        } returns GetCipherResult.Success(createMockCipherView(number = 1))
        coEvery {
            vaultDiskSource.getSelectedCiphers(userId, any())
        } returns listOf(
            createMockCipher(number = 1, organizationId = null),
        )
        coEvery {
            vaultRepository.migrateAttachments(
                userId = any(),
                cipherView = any(),
            )
        } returns Result.success(
            createMockCipherView(number = 1),
        )
        coEvery {
            vaultSdkSource.bulkMoveToOrganization(
                userId = any(),
                organizationId = any(),
                cipherViews = any(),
                collectionIds = any(),
            )
        } returns Result.success(
            listOf(
                createMockEncryptionContext(
                    number = 1,
                    cipher = createMockSdkCipher(number = 1),
                ),
            ),
        )

        val shareError = IllegalStateException("Share failed")
        coEvery {
            ciphersService.bulkShareCiphers(any())
        } returns Result.failure(shareError)

        val vaultMigrationManager = createVaultMigrationManager()
        val result = vaultMigrationManager.migratePersonalVault(userId, organizationId)

        assertTrue(result is MigratePersonalVaultResult.Failure)
        assertEquals(shareError, (result as MigratePersonalVaultResult.Failure).error)
    }

    @Test
    fun `migratePersonalVault should skip cipher when not found in encrypted ciphers map`() =
        runTest {
            val userId = "mockId-1"
            val organizationId = "mockOrganizationId-1"

            mutableVaultDataFlow.value = DataState.Loaded(
                data = createVaultData(
                    cipherListView = createMockCipherListView(number = 1, organizationId = null),
                    collectionViewList = listOf(
                        createMockCollectionView(
                            number = 1,
                            type = com.bitwarden.collections.CollectionType.DEFAULT_USER_COLLECTION,
                        ),
                    ),
                ),
            )

            coEvery {
                vaultRepository.getCipher(any())
            } returns GetCipherResult.Success(createMockCipherView(number = 1))
            // Return encrypted cipher with different ID than what's in mini response
            coEvery {
                vaultDiskSource.getSelectedCiphers(userId, any())
            } returns listOf(
                createMockCipher(number = 1, id = "different-id", organizationId = null),
            )
            coEvery {
                vaultRepository.migrateAttachments(any(), any())
            } returns Result.success(
                createMockCipherView(number = 1),
            )
            coEvery {
                vaultSdkSource.bulkMoveToOrganization(
                    userId = any(),
                    organizationId = any(),
                    cipherViews = any(),
                    collectionIds = any(),
                )
            } returns Result.success(
                listOf(
                    createMockEncryptionContext(
                        number = 1,
                        cipher = createMockSdkCipher(number = 1),
                    ),
                ),
            )
            // Return mini response with ID that doesn't match encrypted cipher
            coEvery {
                ciphersService.bulkShareCiphers(any())
            } returns Result.success(
                createMockCipherMiniResponseJson(1),
            )

            val vaultMigrationManager = createVaultMigrationManager()
            val result = vaultMigrationManager.migratePersonalVault(userId, organizationId)

            assertTrue(result is MigratePersonalVaultResult.Success)
            // Verify saveCipher was not called since cipher wasn't found in map
            coVerify(exactly = 0) {
                vaultDiskSource.saveCipher(any(), any())
            }
        }

    @Test
    fun `migratePersonalVault should skip ciphers with null IDs in cipher list view`() = runTest {
        val userId = "mockId-1"
        val organizationId = "mockOrganizationId-1"

        val mockCipherListViewWithNullId = createMockCipherListView(
            number = 1,
            organizationId = null,
        ).copy(id = null)

        mutableVaultDataFlow.value = DataState.Loaded(
            data = createVaultData(
                cipherListView = mockCipherListViewWithNullId,
                collectionViewList = listOf(
                    createMockCollectionView(
                        number = 1,
                        type = com.bitwarden.collections.CollectionType.DEFAULT_USER_COLLECTION,
                    ),
                ),
            ),
        )

        val vaultMigrationManager = createVaultMigrationManager()
        val result = vaultMigrationManager.migratePersonalVault(userId, organizationId)

        // Should succeed with empty list (no ciphers to migrate)
        assertTrue(result is MigratePersonalVaultResult.Success)
        // Verify getCipher was never called since cipher had null ID
        coVerify(exactly = 0) {
            vaultRepository.getCipher(any())
        }
    }

    @Test
    fun `migratePersonalVault should skip cipher not found but continue with others`() = runTest {
        val userId = "mockId-1"
        val organizationId = "mockOrganizationId-1"

        // Create vault data with 2 ciphers
        val mockDecryptResult = createMockDecryptCipherListResult(
            number = 1,
            successes = listOf(
                createMockCipherListView(number = 1, organizationId = null),
                createMockCipherListView(number = 2, organizationId = null),
            ),
        )

        mutableVaultDataFlow.value = DataState.Loaded(
            data = VaultData(
                decryptCipherListResult = mockDecryptResult,
                collectionViewList = listOf(
                    createMockCollectionView(
                        number = 1,
                        type = com.bitwarden.collections.CollectionType.DEFAULT_USER_COLLECTION,
                    ),
                ),
                folderViewList = emptyList(),
                sendViewList = emptyList(),
            ),
        )

        // First cipher not found, second succeeds
        coEvery {
            vaultRepository.getCipher(cipherId = "mockId-1")
        } returns GetCipherResult.CipherNotFound
        coEvery {
            vaultRepository.getCipher(cipherId = "mockId-2")
        } returns GetCipherResult.Success(createMockCipherView(number = 2))

        coEvery {
            vaultDiskSource.getSelectedCiphers(userId, any())
        } returns listOf(
            createMockCipher(number = 2, organizationId = null),
        )
        coEvery {
            vaultRepository.migrateAttachments(any(), any())
        } returns Result.success(
            createMockCipherView(number = 2),
        )
        coEvery {
            vaultSdkSource.bulkMoveToOrganization(
                userId = any(),
                organizationId = any(),
                cipherViews = any(),
                collectionIds = any(),
            )
        } returns Result.success(
            listOf(
                createMockEncryptionContext(
                    number = 2,
                    cipher = createMockSdkCipher(number = 2),
                ),
            ),
        )
        coEvery {
            ciphersService.bulkShareCiphers(any())
        } returns Result.success(createMockCipherMiniResponseJson(2))

        val vaultMigrationManager = createVaultMigrationManager()
        val result = vaultMigrationManager.migratePersonalVault(userId, organizationId)

        // Should succeed, only migrating the second cipher
        assertTrue(result is MigratePersonalVaultResult.Success)
        coVerify(exactly = 1) {
            vaultRepository.migrateAttachments(userId, match { it.id == "mockId-2" })
        }
    }

    @Test
    fun `clearMigrationState should set migration state to NoMigrationRequired`() = runTest {
        val vaultMigrationManager = createVaultMigrationManager()

        // Initially state should be NoMigrationRequired
        assertEquals(
            VaultMigrationData.NoMigrationRequired,
            vaultMigrationManager.vaultMigrationDataStateFlow.value,
        )

        // Call clearMigrationState (should remain NoMigrationRequired)
        vaultMigrationManager.clearMigrationState()

        // Verify state is still NoMigrationRequired
        assertEquals(
            VaultMigrationData.NoMigrationRequired,
            vaultMigrationManager.vaultMigrationDataStateFlow.value,
        )
    }
}

private fun createVaultData(
    cipherListView: CipherListView? = null,
    collectionViewList: List<CollectionView> = emptyList(),
    folderViewList: List<FolderView> = emptyList(),
    sendViewList: List<SendView> = emptyList(),
): VaultData =
    VaultData(
        decryptCipherListResult = createMockDecryptCipherListResult(
            number = 1,
            successes = cipherListView?.let { listOf(it) } ?: emptyList(),
        ),
        collectionViewList = collectionViewList,
        folderViewList = folderViewList,
        sendViewList = sendViewList,
    )

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
