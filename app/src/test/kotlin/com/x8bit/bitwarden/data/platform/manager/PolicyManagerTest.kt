package com.x8bit.bitwarden.data.platform.manager

import app.cash.turbine.test
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.network.model.OrganizationStatusType
import com.bitwarden.network.model.OrganizationType
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.createMockOrganizationNetwork
import com.bitwarden.network.model.createMockPolicy
import com.bitwarden.policies.PolicyType
import com.bitwarden.policies.PolicyView
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockPolicyView
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

@Suppress("LargeClass")
class PolicyManagerTest {
    private val mutableUserStateFlow = MutableStateFlow<UserStateJson?>(null)
    private val mutablePolicyFlow = MutableStateFlow<List<SyncResponseJson.Policy>?>(null)
    private val mutableOrganizationsFlow =
        MutableStateFlow<List<SyncResponseJson.Profile.Organization>?>(null)
    private val authDiskSource: AuthDiskSource = mockk {
        every { userStateFlow } returns mutableUserStateFlow
        every { getPoliciesFlow(USER_ID) } returns mutablePolicyFlow
        every { getOrganizationsFlow(USER_ID) } returns mutableOrganizationsFlow
    }
    private val authSdkSource: AuthSdkSource = mockk()
    private val mutablePoliciesInAcceptedStateFlagFlow = MutableStateFlow(true)
    private val featureFlagManager: FeatureFlagManager = mockk {
        every {
            getFeatureFlagFlow(key = FlagKey.PoliciesInAcceptedState)
        } returns mutablePoliciesInAcceptedStateFlagFlow
        every { getFeatureFlag(key = FlagKey.PoliciesInAcceptedState) } answers {
            mutablePoliciesInAcceptedStateFlagFlow.value
        }
    }

    private val policyManager: PolicyManager = PolicyManagerImpl(
        authDiskSource = authDiskSource,
        authSdkSource = authSdkSource,
        featureFlagManager = featureFlagManager,
    )

    @Test
    fun `getActivePoliciesFlow should emit changes to current user's policy data`() =
        runTest {
            val userStateJson = mockk<UserStateJson> {
                every { activeUserId } returns USER_ID
            }
            val organizations = createMockOrganizationNetwork(
                number = 1,
                isEnabled = true,
                shouldUsePolicies = true,
            )
            val storedPolicyOne = createMockPolicy(
                isEnabled = true,
                number = 1,
                organizationId = organizations.id,
                type = PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT,
            )
            val storedPolicyTwo = createMockPolicy(
                isEnabled = true,
                number = 2,
                organizationId = organizations.id,
                type = PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT,
            )
            val expectedPolicyOne = createMockPolicyView(
                enabled = true,
                number = 1,
                organizationId = organizations.id,
                type = PolicyType.MAXIMUM_VAULT_TIMEOUT,
            )
            val expectedPolicyTwo = createMockPolicyView(
                enabled = true,
                number = 2,
                organizationId = organizations.id,
                type = PolicyType.MAXIMUM_VAULT_TIMEOUT,
            )
            every {
                authSdkSource.filterPolicies(
                    policies = any(),
                    organizations = any(),
                    policyType = any(),
                )
            } returnsMany listOf(
                listOf(expectedPolicyOne).asSuccess(),
                listOf(expectedPolicyTwo).asSuccess(),
            )

            mutableUserStateFlow.value = userStateJson
            mutableOrganizationsFlow.value = listOf(organizations)
            mutablePolicyFlow.value = listOf(storedPolicyOne)

            policyManager
                .getActivePoliciesFlow(type = PolicyType.MAXIMUM_VAULT_TIMEOUT)
                .test {
                    assertEquals(listOf(expectedPolicyOne), awaitItem())

                    mutablePolicyFlow.value = listOf(storedPolicyTwo)

                    assertEquals(listOf(expectedPolicyTwo), awaitItem())
                }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getActivePoliciesFlow when feature flag is false should emit policies using legacy filtering`() =
        runTest {
            mutablePoliciesInAcceptedStateFlagFlow.value = false

            val userStateJson = mockk<UserStateJson> {
                every { activeUserId } returns USER_ID
            }
            val organizations = createMockOrganizationNetwork(
                number = 1,
                isEnabled = true,
                shouldUsePolicies = true,
            )
            val storedPolicyOne = createMockPolicy(
                isEnabled = true,
                number = 1,
                organizationId = organizations.id,
                type = PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT,
            )
            val expectedPolicyOne = createMockPolicyView(
                enabled = true,
                number = 1,
                organizationId = organizations.id,
                type = PolicyType.MAXIMUM_VAULT_TIMEOUT,
            )
            val storedPolicyTwo = createMockPolicy(
                isEnabled = true,
                number = 2,
                organizationId = organizations.id,
                type = PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT,
            )
            val expectedPolicyTwo = createMockPolicyView(
                enabled = true,
                number = 2,
                organizationId = organizations.id,
                type = PolicyType.MAXIMUM_VAULT_TIMEOUT,
            )

            mutableUserStateFlow.value = userStateJson
            mutableOrganizationsFlow.value = listOf(organizations)
            mutablePolicyFlow.value = listOf(storedPolicyOne)

            policyManager
                .getActivePoliciesFlow(type = PolicyType.MAXIMUM_VAULT_TIMEOUT)
                .test {
                    assertEquals(listOf(expectedPolicyOne), awaitItem())

                    mutablePolicyFlow.value = listOf(storedPolicyOne, storedPolicyTwo)
                    assertEquals(listOf(expectedPolicyOne, expectedPolicyTwo), awaitItem())
                }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getActivePoliciesFlow when feature flag is false should emit empty list when organization status is below ACCEPTED`() =
        runTest {
            mutablePoliciesInAcceptedStateFlagFlow.value = false

            val userStateJson = mockk<UserStateJson> {
                every { activeUserId } returns USER_ID
            }
            val organizations = createMockOrganizationNetwork(
                number = 1,
                isEnabled = true,
                shouldUsePolicies = true,
                status = OrganizationStatusType.INVITED,
            )
            val storedPolicy = createMockPolicy(
                isEnabled = true,
                number = 1,
                organizationId = organizations.id,
                type = PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT,
            )

            mutableUserStateFlow.value = userStateJson
            mutableOrganizationsFlow.value = listOf(organizations)
            mutablePolicyFlow.value = listOf(storedPolicy)

            policyManager
                .getActivePoliciesFlow(type = PolicyType.MAXIMUM_VAULT_TIMEOUT)
                .test {
                    assertEquals(emptyList<PolicyView>(), awaitItem())
                }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getActivePoliciesFlow when feature flag is false should emit empty list when organization does not use policies`() =
        runTest {
            mutablePoliciesInAcceptedStateFlagFlow.value = false

            val userStateJson = mockk<UserStateJson> {
                every { activeUserId } returns USER_ID
            }
            val organizations = createMockOrganizationNetwork(
                number = 1,
                isEnabled = true,
                shouldUsePolicies = false,
            )
            val storedPolicy = createMockPolicy(
                isEnabled = true,
                number = 1,
                organizationId = organizations.id,
                type = PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT,
            )

            mutableUserStateFlow.value = userStateJson
            mutableOrganizationsFlow.value = listOf(organizations)
            mutablePolicyFlow.value = listOf(storedPolicy)

            policyManager
                .getActivePoliciesFlow(type = PolicyType.MAXIMUM_VAULT_TIMEOUT)
                .test {
                    assertEquals(emptyList<PolicyView>(), awaitItem())
                }
        }

    @Test
    fun `getActivePolicies returns empty list if user id is null`() {
        every { authDiskSource.userState } returns null

        assertTrue(policyManager.getActivePolicies(type = PolicyType.MASTER_PASSWORD).isEmpty())
    }

    @Test
    fun `getActivePolicies returns empty list if the policies are not active`() {
        val userState: UserStateJson = mockk {
            every { activeUserId } returns USER_ID
        }
        every { authDiskSource.userState } returns userState
        every {
            authDiskSource.getOrganizations(USER_ID)
        } returns listOf(
            createMockOrganizationNetwork(
                number = 3,
                isEnabled = true,
            ),
        )
        every {
            authDiskSource.getPolicies(USER_ID)
        } returns listOf(
            createMockPolicy(
                organizationId = "mockId-3",
                isEnabled = true,
            ),
        )
        every {
            authSdkSource.filterPolicies(
                policies = any(),
                organizations = any(),
                policyType = any(),
            )
        } returns emptyList<PolicyView>().asSuccess()

        assertTrue(policyManager.getActivePolicies(type = PolicyType.MASTER_PASSWORD).isEmpty())
    }

    @Test
    fun `getActivePolicies returns active and applied policies`() {
        val userState: UserStateJson = mockk {
            every { activeUserId } returns USER_ID
        }
        every { authDiskSource.userState } returns userState
        every {
            authDiskSource.getOrganizations(USER_ID)
        } returns listOf(
            createMockOrganizationNetwork(
                number = 3,
                isEnabled = false,
            ),
        )
        every {
            authDiskSource.getPolicies(USER_ID)
        } returns listOf(
            createMockPolicy(
                organizationId = "mockId-3",
                isEnabled = true,
            ),
        )
        every {
            authSdkSource.filterPolicies(
                policies = any(),
                organizations = any(),
                policyType = any(),
            )
        } returns emptyList<PolicyView>().asSuccess()

        assertTrue(policyManager.getActivePolicies(type = PolicyType.MASTER_PASSWORD).isEmpty())
    }

    @Test
    fun `getActivePolicies returns active and applied policies for disabled organizations`() {
        val userState: UserStateJson = mockk {
            every { activeUserId } returns USER_ID
        }
        val storedPolicy = createMockPolicy(organizationId = "mockId-3", isEnabled = true)
        val expectedPolicy = createMockPolicyView(organizationId = "mockId-3", enabled = true)
        every { authDiskSource.userState } returns userState
        every {
            authDiskSource.getOrganizations(USER_ID)
        } returns listOf(
            createMockOrganizationNetwork(
                number = 3,
                isEnabled = false,
                shouldUsePolicies = true,
                type = OrganizationType.USER,
            ),
        )
        every { authDiskSource.getPolicies(USER_ID) } returns listOf(storedPolicy)
        every {
            authSdkSource.filterPolicies(any(), any(), any())
        } returns listOf(expectedPolicy).asSuccess()

        assertEquals(
            listOf(expectedPolicy),
            policyManager.getActivePolicies(type = PolicyType.MASTER_PASSWORD),
        )
    }

    @Test
    fun `getActivePolicies returns active and applied PasswordGenerator policies`() {
        val userState: UserStateJson = mockk {
            every { activeUserId } returns USER_ID
        }
        every { authDiskSource.userState } returns userState
        every {
            authDiskSource.getOrganizations(USER_ID)
        } returns listOf(
            createMockOrganizationNetwork(
                number = 3,
                isEnabled = true,
                shouldUsePolicies = true,
            ),
        )
        every {
            authDiskSource.getPolicies(USER_ID)
        } returns listOf(
            createMockPolicy(
                organizationId = "mockId-3",
                isEnabled = true,
                type = PolicyTypeJson.PASSWORD_GENERATOR,
            ),
        )
        every {
            authSdkSource.filterPolicies(any(), any(), any())
        } returns listOf(
            createMockPolicyView(
                organizationId = "mockId-3",
                enabled = true,
                type = PolicyType.PASSWORD_GENERATOR,
            ),
        )
            .asSuccess()

        assertTrue(policyManager.getActivePolicies(type = PolicyType.PASSWORD_GENERATOR).any())
    }

    @Test
    fun `getActivePolicies returns active and applied restrict item types policies`() {
        val userState: UserStateJson = mockk {
            every { activeUserId } returns USER_ID
        }
        every { authDiskSource.userState } returns userState
        every {
            authDiskSource.getOrganizations(USER_ID)
        } returns listOf(
            createMockOrganizationNetwork(
                number = 3,
                isEnabled = true,
                shouldUsePolicies = true,
            ),
        )
        every {
            authDiskSource.getPolicies(USER_ID)
        } returns listOf(
            createMockPolicy(
                organizationId = "mockId-3",
                isEnabled = true,
                type = PolicyTypeJson.RESTRICT_ITEM_TYPES,
            ),
        )
        every {
            authSdkSource.filterPolicies(any(), any(), any())
        } returns listOf(
            createMockPolicyView(
                organizationId = "mockId-3",
                enabled = true,
                type = PolicyType.RESTRICTED_ITEM_TYPES,
            ),
        )
            .asSuccess()

        assertTrue(
            policyManager.getActivePolicies(type = PolicyType.RESTRICTED_ITEM_TYPES).any(),
        )
    }

    @Test
    fun `getUserPolicies returns empty list if policies is null`() {
        every { authDiskSource.userState } returns null
        every { authDiskSource.getPolicies(USER_ID) } returns null
        every { authDiskSource.getOrganizations(USER_ID) } returns null

        assertEquals(
            emptyList<PolicyView>(),
            policyManager.getUserPolicies(
                userId = USER_ID,
                type = PolicyType.ORGANIZATION_DATA_OWNERSHIP,
            ),
        )
    }

    @Test
    fun `getUserPolicies returns active and applied Disabled personal vault export policies`() {
        val userState: UserStateJson = mockk {
            every { activeUserId } returns USER_ID
        }
        every { authDiskSource.userState } returns userState
        every {
            authDiskSource.getOrganizations(USER_ID)
        } returns listOf(
            createMockOrganizationNetwork(
                number = 3,
                isEnabled = true,
                shouldUsePolicies = true,
                type = OrganizationType.USER,
            ),
        )

        val storedListOfPolicies = listOf(
            createMockPolicy(
                organizationId = "mockId-3",
                isEnabled = true,
                type = PolicyTypeJson.DISABLE_PERSONAL_VAULT_EXPORT,
            ),
        )
        val expectedListOfPolicies = listOf(
            createMockPolicyView(
                organizationId = "mockId-3",
                enabled = true,
                type = PolicyType.DISABLE_PERSONAL_VAULT_EXPORT,
            ),
        )
        every {
            authDiskSource.getPolicies(USER_ID)
        } returns storedListOfPolicies
        every {
            authSdkSource.filterPolicies(any(), any(), any())
        } returns expectedListOfPolicies.asSuccess()

        assertEquals(
            expectedListOfPolicies,
            policyManager.getUserPolicies(
                userId = USER_ID,
                type = PolicyType.DISABLE_PERSONAL_VAULT_EXPORT,
            ),
        )
    }

    @Test
    fun `getPersonalOwnershipPolicyOrganizationId returns null when no active user`() {
        every { authDiskSource.userState } returns null

        assertNull(policyManager.getPersonalOwnershipPolicyOrganizationId())
    }

    @Test
    fun `getPersonalOwnershipPolicyOrganizationId returns null when no policies exist`() {
        val userState: UserStateJson = mockk {
            every { activeUserId } returns USER_ID
        }
        every { authDiskSource.userState } returns userState
        every { authDiskSource.getOrganizations(USER_ID) } returns emptyList()
        every { authDiskSource.getPolicies(USER_ID) } returns emptyList()

        assertNull(policyManager.getPersonalOwnershipPolicyOrganizationId())
    }

    @Test
    fun `getPersonalOwnershipPolicyOrganizationId returns null when policy is disabled`() {
        val userState: UserStateJson = mockk {
            every { activeUserId } returns USER_ID
        }
        every { authDiskSource.userState } returns userState
        every {
            authDiskSource.getOrganizations(USER_ID)
        } returns listOf(
            createMockOrganizationNetwork(
                number = 1,
                isEnabled = true,
                shouldUsePolicies = true,
                type = OrganizationType.USER,
            ),
        )
        every {
            authDiskSource.getPolicies(USER_ID)
        } returns listOf(
            createMockPolicy(
                organizationId = "mockId-1",
                isEnabled = false,
                type = PolicyTypeJson.PERSONAL_OWNERSHIP,
            ),
        )
        every {
            authSdkSource.filterPolicies(
                policies = any(),
                organizations = any(),
                policyType = any(),
            )
        } returns emptyList<PolicyView>().asSuccess()

        assertNull(policyManager.getPersonalOwnershipPolicyOrganizationId())
    }

    @Test
    fun `getPersonalOwnershipPolicyOrganizationId returns organization id for single policy`() {
        val userState: UserStateJson = mockk {
            every { activeUserId } returns USER_ID
        }
        val expectedOrganizationId = "mockId-1"
        every { authDiskSource.userState } returns userState
        every {
            authDiskSource.getOrganizations(USER_ID)
        } returns listOf(
            createMockOrganizationNetwork(
                number = 1,
                isEnabled = true,
                shouldUsePolicies = true,
                type = OrganizationType.USER,
            ),
        )
        every {
            authDiskSource.getPolicies(USER_ID)
        } returns listOf(
            createMockPolicy(
                organizationId = expectedOrganizationId,
                isEnabled = true,
                type = PolicyTypeJson.PERSONAL_OWNERSHIP,
            ),
        )
        every {
            authSdkSource.filterPolicies(any(), any(), any())
        } returns listOf(
            createMockPolicyView(
                organizationId = expectedOrganizationId,
                enabled = true,
            ),
        )
            .asSuccess()

        assertEquals(
            expectedOrganizationId,
            policyManager.getPersonalOwnershipPolicyOrganizationId(),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getPersonalOwnershipPolicyOrganizationId returns earliest policy when multiple orgs have policy`() {
        val userState: UserStateJson = mockk {
            every { activeUserId } returns USER_ID
        }
        val earliestRevisionDate = Instant.parse("2024-01-01T00:00:00Z")
        val middleRevisionDate = Instant.parse("2024-06-01T00:00:00Z")
        val latestRevisionDate = Instant.parse("2024-12-01T00:00:00Z")

        val expectedOrganizationId = "mockId-1"

        every { authDiskSource.userState } returns userState
        every {
            authDiskSource.getOrganizations(USER_ID)
        } returns listOf(
            createMockOrganizationNetwork(
                number = 1,
                isEnabled = true,
                shouldUsePolicies = true,
                type = OrganizationType.USER,
            ),
            createMockOrganizationNetwork(
                number = 2,
                isEnabled = true,
                shouldUsePolicies = true,
                type = OrganizationType.USER,
            ),
            createMockOrganizationNetwork(
                number = 3,
                isEnabled = true,
                shouldUsePolicies = true,
                type = OrganizationType.USER,
            ),
        )
        every {
            authDiskSource.getPolicies(USER_ID)
        } returns listOf(
            createMockPolicy(
                number = 3,
                organizationId = "mockId-3",
                isEnabled = true,
                type = PolicyTypeJson.PERSONAL_OWNERSHIP,
                revisionDate = latestRevisionDate,
            ),
            createMockPolicy(
                number = 1,
                organizationId = expectedOrganizationId,
                isEnabled = true,
                type = PolicyTypeJson.PERSONAL_OWNERSHIP,
                revisionDate = earliestRevisionDate,
            ),
            createMockPolicy(
                number = 2,
                organizationId = "mockId-2",
                isEnabled = true,
                type = PolicyTypeJson.PERSONAL_OWNERSHIP,
                revisionDate = middleRevisionDate,
            ),
        )
        every {
            authSdkSource.filterPolicies(any(), any(), any())
        } returns listOf(
            createMockPolicyView(
                number = 3,
                organizationId = "mockId-3",
                enabled = true,
                revisionDate = latestRevisionDate,
            ),
            createMockPolicyView(
                number = 1,
                organizationId = expectedOrganizationId,
                enabled = true,
                revisionDate = earliestRevisionDate,
            ),
            createMockPolicyView(
                number = 2,
                organizationId = "mockId-2",
                enabled = true,
                revisionDate = middleRevisionDate,
            ),
        )
            .asSuccess()

        assertEquals(
            expectedOrganizationId,
            policyManager.getPersonalOwnershipPolicyOrganizationId(),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getPersonalOwnershipPolicyOrganizationId filters out policies from organizations not using policies`() {
        val userState: UserStateJson = mockk {
            every { activeUserId } returns USER_ID
        }
        val earlierRevisionDate = Instant.parse("2024-01-01T00:00:00Z")
        val laterRevisionDate = Instant.parse("2024-06-01T00:00:00Z")
        val expectedOrganizationId = "mockId-2"

        every { authDiskSource.userState } returns userState
        every {
            authDiskSource.getOrganizations(USER_ID)
        } returns listOf(
            createMockOrganizationNetwork(
                number = 1,
                isEnabled = true,
                shouldUsePolicies = false, // This org does NOT use policies
                type = OrganizationType.USER,
            ),
            createMockOrganizationNetwork(
                number = 2,
                isEnabled = true,
                shouldUsePolicies = true, // This org uses policies
                type = OrganizationType.USER,
            ),
        )
        every {
            authDiskSource.getPolicies(USER_ID)
        } returns listOf(
            createMockPolicy(
                number = 1,
                organizationId = "mockId-1",
                isEnabled = true,
                type = PolicyTypeJson.PERSONAL_OWNERSHIP,
                revisionDate = earlierRevisionDate, // Earlier but org doesn't enforce
            ),
            createMockPolicy(
                number = 2,
                organizationId = expectedOrganizationId,
                isEnabled = true,
                type = PolicyTypeJson.PERSONAL_OWNERSHIP,
                revisionDate = laterRevisionDate,
            ),
        )
        every {
            authSdkSource.filterPolicies(any(), any(), any())
        } returns listOf(
            createMockPolicyView(
                number = 2,
                organizationId = expectedOrganizationId,
                enabled = true,
                revisionDate = laterRevisionDate,
            ),
        )
            .asSuccess()

        // Should return mockId-2 because mockId-1's organization doesn't enforce policies
        assertEquals(
            expectedOrganizationId,
            policyManager.getPersonalOwnershipPolicyOrganizationId(),
        )
    }
}

private const val USER_ID = "userId"
