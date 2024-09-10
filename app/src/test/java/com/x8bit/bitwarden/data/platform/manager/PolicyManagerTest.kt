package com.x8bit.bitwarden.data.platform.manager

import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockOrganization
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockPolicy
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PolicyManagerTest {
    private val mutableUserStateFlow = MutableStateFlow<UserStateJson?>(null)
    private val mutablePolicyFlow = MutableStateFlow<List<SyncResponseJson.Policy>?>(null)
    private val authDiskSource: AuthDiskSource = mockk {
        every { userStateFlow } returns mutableUserStateFlow
        every { getPoliciesFlow(USER_ID) } returns mutablePolicyFlow
    }

    private lateinit var policyManager: PolicyManager

    @BeforeEach
    fun setUp() {
        policyManager = PolicyManagerImpl(
            authDiskSource = authDiskSource,
        )
    }

    @Test
    fun `currentUserPoliciesListFlow should emit changes to current user's policy data`() =
        runTest {
            val userStateJson = mockk<UserStateJson> {
                every { activeUserId } returns USER_ID
            }
            val organizationsOne = createMockOrganization(
                number = 1,
                isEnabled = true,
                shouldUsePolicies = true,
            )
            val organizationsTwo = createMockOrganization(
                number = 2,
                isEnabled = true,
                shouldUsePolicies = true,
            )
            val expectedPolicyOne = createMockPolicy(
                isEnabled = true,
                number = 1,
                organizationId = organizationsOne.id,
                type = PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT,
            )
            val expectedPolicyTwo = createMockPolicy(
                isEnabled = true,
                number = 2,
                organizationId = organizationsTwo.id,
                type = PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT,
            )
            every {
                authDiskSource.getOrganizations(USER_ID)
            } returns listOf(organizationsOne) andThen listOf(organizationsTwo)

            mutableUserStateFlow.value = userStateJson
            mutablePolicyFlow.value = listOf(expectedPolicyOne)

            policyManager
                .getActivePoliciesFlow(type = PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT)
                .test {
                    assertEquals(listOf(expectedPolicyOne), awaitItem())

                    mutablePolicyFlow.value = listOf(expectedPolicyTwo)

                    assertEquals(listOf(expectedPolicyTwo), awaitItem())
                }
        }

    @Test
    fun `getActivePolicies returns empty list if user id is null`() {
        every {
            authDiskSource.userState
        } returns null

        assertTrue(policyManager.getActivePolicies(type = PolicyTypeJson.MASTER_PASSWORD).isEmpty())
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
            createMockOrganization(
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

        assertTrue(policyManager.getActivePolicies(type = PolicyTypeJson.MASTER_PASSWORD).isEmpty())
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
            createMockOrganization(
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

        assertTrue(policyManager.getActivePolicies(type = PolicyTypeJson.MASTER_PASSWORD).isEmpty())
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
            createMockOrganization(
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

        assertTrue(policyManager.getActivePolicies(type = PolicyTypeJson.PASSWORD_GENERATOR).any())
    }
}

private const val USER_ID = "userId"
