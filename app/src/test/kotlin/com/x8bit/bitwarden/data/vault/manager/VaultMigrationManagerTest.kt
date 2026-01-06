package com.x8bit.bitwarden.data.vault.manager

import app.cash.turbine.test
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.createMockCipher
import com.bitwarden.network.model.createMockPolicy
import com.bitwarden.network.model.createMockProfile
import com.bitwarden.network.model.createMockSyncResponse
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.network.NetworkConnectionManager
import com.x8bit.bitwarden.data.vault.manager.model.VaultMigrationData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class VaultMigrationManagerTest {

    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val policyManager: PolicyManager = mockk {
        every { getActivePolicies(any()) } returns emptyList()
    }
    private val mockFeatureFlagManager = mockk<FeatureFlagManager>(relaxed = true) {
        every { getFeatureFlag(FlagKey.MigrateMyVaultToMyItems) } returns true
    }
    private val connectionManager: NetworkConnectionManager = mockk {
        every { isNetworkConnected } returns true
    }

    private val vaultMigrationManager: VaultMigrationManager = VaultMigrationManagerImpl(
        authDiskSource = fakeAuthDiskSource,
        policyManager = policyManager,
        featureFlagManager = mockFeatureFlagManager,
        connectionManager = connectionManager,
    )

    @Test
    fun `vaultMigrationDataStateFlow should initially emit NoMigrationRequired`() = runTest {
        assertEquals(
            VaultMigrationData.NoMigrationRequired,
            vaultMigrationManager.vaultMigrationDataStateFlow.value,
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `verifyAndUpdateMigrationState should emit MigrationRequired when all conditions are met`() =
        runTest {
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

            // Setup sync with personal ciphers (organizationId = null)
            val personalCipher = createMockCipher(number = 1).copy(organizationId = null)
            val cipherList = listOf(personalCipher)

            vaultMigrationManager.vaultMigrationDataStateFlow.test {
                assertEquals(VaultMigrationData.NoMigrationRequired, awaitItem())

                vaultMigrationManager.verifyAndUpdateMigrationState(cipherList)

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
    @Suppress("MaxLineLength")
    fun `verifyAndUpdateMigrationState should emit NoMigrationRequired when no personal ownership policy`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            every {
                policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
            } returns emptyList()

            val personalCipher = createMockCipher(number = 1).copy(organizationId = null)
            val cipherList = listOf(personalCipher)

            vaultMigrationManager.vaultMigrationDataStateFlow.test {
                assertEquals(VaultMigrationData.NoMigrationRequired, awaitItem())

                vaultMigrationManager.verifyAndUpdateMigrationState(cipherList)

                // Should still be NoMigrationRequired since no policy
                expectNoEvents()
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `verifyAndUpdateMigrationState should emit NoMigrationRequired when feature flag is disabled`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            val mockPolicy = createMockPolicy(number = 1, type = PolicyTypeJson.PERSONAL_OWNERSHIP)
            every {
                policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
            } returns listOf(mockPolicy)

            every {
                mockFeatureFlagManager.getFeatureFlag(FlagKey.MigrateMyVaultToMyItems)
            } returns false

            val personalCipher = createMockCipher(number = 1).copy(organizationId = null)
            val cipherList = listOf(personalCipher)

            vaultMigrationManager.vaultMigrationDataStateFlow.test {
                assertEquals(VaultMigrationData.NoMigrationRequired, awaitItem())

                vaultMigrationManager.verifyAndUpdateMigrationState(cipherList)

                // Should still be NoMigrationRequired since feature flag is disabled
                expectNoEvents()
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `verifyAndUpdateMigrationState should emit NoMigrationRequired when no network connection`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            val mockPolicy = createMockPolicy(number = 1, type = PolicyTypeJson.PERSONAL_OWNERSHIP)
            every {
                policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
            } returns listOf(mockPolicy)

            every { connectionManager.isNetworkConnected } returns false

            val personalCipher = createMockCipher(number = 1).copy(organizationId = null)
            val cipherList = listOf(personalCipher)

            vaultMigrationManager.vaultMigrationDataStateFlow.test {
                assertEquals(VaultMigrationData.NoMigrationRequired, awaitItem())

                vaultMigrationManager.verifyAndUpdateMigrationState(cipherList)

                // Should still be NoMigrationRequired since no network
                expectNoEvents()
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `verifyAndUpdateMigrationState should emit NoMigrationRequired when no personal ciphers exist`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            val mockPolicy = createMockPolicy(number = 1, type = PolicyTypeJson.PERSONAL_OWNERSHIP)
            every {
                policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
            } returns listOf(mockPolicy)

            // All ciphers belong to organizations
            val orgCipher = createMockCipher(number = 1).copy(organizationId = "org-id")
            val cipherList = listOf(orgCipher)

            vaultMigrationManager.vaultMigrationDataStateFlow.test {
                assertEquals(VaultMigrationData.NoMigrationRequired, awaitItem())

                vaultMigrationManager.verifyAndUpdateMigrationState(cipherList)

                // Should still be NoMigrationRequired since no personal ciphers
                expectNoEvents()
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `verifyAndUpdateMigrationState should emit NoMigrationRequired when cipher list is empty`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            val mockPolicy = createMockPolicy(number = 1, type = PolicyTypeJson.PERSONAL_OWNERSHIP)
            every {
                policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
            } returns listOf(mockPolicy)

            val cipherList = emptyList<SyncResponseJson.Cipher>()

            vaultMigrationManager.vaultMigrationDataStateFlow.test {
                assertEquals(VaultMigrationData.NoMigrationRequired, awaitItem())

                vaultMigrationManager.verifyAndUpdateMigrationState(cipherList)

                // Should still be NoMigrationRequired since cipher list is empty
                expectNoEvents()
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `verifyAndUpdateMigrationState should emit NoMigrationRequired when organization ID is null`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            val mockPolicy = createMockPolicy(number = 1, type = PolicyTypeJson.PERSONAL_OWNERSHIP)
            every {
                policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
            } returns listOf(mockPolicy)
            every {
                policyManager.getPersonalOwnershipPolicyOrganizationId()
            } returns null

            val personalCipher = createMockCipher(number = 1).copy(organizationId = null)
            val cipherList = listOf(personalCipher)

            vaultMigrationManager.vaultMigrationDataStateFlow.test {
                assertEquals(VaultMigrationData.NoMigrationRequired, awaitItem())

                vaultMigrationManager.verifyAndUpdateMigrationState(cipherList)

                // Should still be NoMigrationRequired since organization ID is null
                expectNoEvents()
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `verifyAndUpdateMigrationState should emit NoMigrationRequired when organizations list is null`() =
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

            // Store organizations as null in authDiskSource
            val syncResponse = createMockSyncResponse(
                number = 1,
                profile = createMockProfile(number = 1, organizations = null),
            )
            fakeAuthDiskSource.storeOrganizations(userId = userId, organizations = null)

            val personalCipher = createMockCipher(number = 1).copy(organizationId = null)
            val cipherList = listOf(personalCipher)

            vaultMigrationManager.vaultMigrationDataStateFlow.test {
                assertEquals(VaultMigrationData.NoMigrationRequired, awaitItem())

                vaultMigrationManager.verifyAndUpdateMigrationState(cipherList)

                // Should still be NoMigrationRequired since organizations list is null
                expectNoEvents()
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `verifyAndUpdateMigrationState should emit NoMigrationRequired when organization not found`() =
        runTest {
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

            val personalCipher = createMockCipher(number = 1).copy(organizationId = null)
            val cipherList = listOf(personalCipher)

            vaultMigrationManager.vaultMigrationDataStateFlow.test {
                assertEquals(VaultMigrationData.NoMigrationRequired, awaitItem())

                vaultMigrationManager.verifyAndUpdateMigrationState(cipherList)

                // Should still be NoMigrationRequired since organization is not found
                expectNoEvents()
            }
        }

    @Test
    fun `shouldMigrateVault returns true when all conditions are met`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        val mockPolicy = createMockPolicy(number = 1, type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        every {
            policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns listOf(mockPolicy)

        val result = vaultMigrationManager.shouldMigrateVault { true }

        assertEquals(true, result)
    }

    @Test
    fun `shouldMigrateVault returns false when no policies`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        every {
            policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns emptyList()

        val result = vaultMigrationManager.shouldMigrateVault { true }

        assertEquals(false, result)
    }

    @Test
    fun `shouldMigrateVault returns false when feature flag is disabled`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        val mockPolicy = createMockPolicy(number = 1, type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        every {
            policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns listOf(mockPolicy)
        every {
            mockFeatureFlagManager.getFeatureFlag(FlagKey.MigrateMyVaultToMyItems)
        } returns false

        val result = vaultMigrationManager.shouldMigrateVault { true }

        assertEquals(false, result)
    }

    @Test
    fun `shouldMigrateVault returns false when no network`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        val mockPolicy = createMockPolicy(number = 1, type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        every {
            policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns listOf(mockPolicy)
        every { connectionManager.isNetworkConnected } returns false

        val result = vaultMigrationManager.shouldMigrateVault { true }

        assertEquals(false, result)
    }

    @Test
    fun `shouldMigrateVault returns false when no personal items`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        val mockPolicy = createMockPolicy(number = 1, type = PolicyTypeJson.PERSONAL_OWNERSHIP)
        every {
            policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns listOf(mockPolicy)

        val result = vaultMigrationManager.shouldMigrateVault { false }

        assertEquals(false, result)
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
