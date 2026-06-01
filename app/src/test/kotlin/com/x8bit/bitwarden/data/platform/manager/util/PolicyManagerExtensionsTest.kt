package com.x8bit.bitwarden.data.platform.manager.util

import app.cash.turbine.test
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.policies.PolicyType
import com.bitwarden.policies.PolicyView
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockPolicyView
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PolicyManagerExtensionsTest {
    private val mutablePolicyFlow = bufferedMutableSharedFlow<List<PolicyView>>()
    private val policyManager: PolicyManager = mockk {
        every { getActivePoliciesFlow(any()) } returns mutablePolicyFlow
    }

    @Test
    fun `getActivePolicies should filter out undesired policies`() {
        every {
            policyManager.getActivePolicies(any())
        } returns listOf(MASTER_PASSWORD_POLICY, PASSWORD_GENERATOR_POLICY)

        val result = policyManager.getActivePolicies<PolicyInformation.MasterPassword>()

        assertEquals(listOf(MASTER_PASSWORD_POLICY_INFO), result)
    }

    @Test
    fun `getActivePoliciesFlow should filter out undesired policies when policyManager emits`() =
        runTest {
            policyManager.getActivePoliciesFlow<PolicyInformation.MasterPassword>().test {
                mutablePolicyFlow.tryEmit(listOf(PASSWORD_GENERATOR_POLICY))
                assertEquals(emptyList<PolicyInformation.MasterPassword>(), awaitItem())
                mutablePolicyFlow.tryEmit(
                    listOf(
                        MASTER_PASSWORD_POLICY,
                        PASSWORD_GENERATOR_POLICY,
                    ),
                )
                assertEquals(listOf(MASTER_PASSWORD_POLICY_INFO), awaitItem())
            }
        }

    @Test
    fun `getPolicyType with MasterPassword should map to appropriate PolicyTypeJson`() {
        assertEquals(
            PolicyType.MASTER_PASSWORD,
            getPolicyType<PolicyInformation.MasterPassword>(),
        )
    }

    @Test
    fun `getPolicyType with PasswordGenerator should map to appropriate PolicyTypeJson`() {
        assertEquals(
            PolicyType.PASSWORD_GENERATOR,
            getPolicyType<PolicyInformation.PasswordGenerator>(),
        )
    }

    @Test
    fun `getPolicyType with SendOptions should map to appropriate PolicyTypeJson`() {
        assertEquals(
            PolicyType.SEND_OPTIONS,
            getPolicyType<PolicyInformation.SendOptions>(),
        )
    }

    @Test
    fun `getPolicyType with VaultTimeout should map to appropriate PolicyTypeJson`() {
        assertEquals(
            PolicyType.MAXIMUM_VAULT_TIMEOUT,
            getPolicyType<PolicyInformation.VaultTimeout>(),
        )
    }
}

private val MASTER_PASSWORD_POLICY = createMockPolicyView(
    organizationId = "organizationId",
    id = "master_password_id",
    type = PolicyType.MASTER_PASSWORD,
    enabled = true,
    data = """
      {
        "minLength":10,
        "minComplexity":10,
        "requireUpper":false,
        "requireLower":true,
        "requireNumbers":null,
        "requireSpecial":false,
        "enforceOnLogin":true
      }
    """,
)

private val MASTER_PASSWORD_POLICY_INFO = PolicyInformation.MasterPassword(
    minLength = 10,
    minComplexity = 10,
    requireUpper = false,
    requireLower = true,
    requireNumbers = null,
    requireSpecial = false,
    enforceOnLogin = true,
)

private val PASSWORD_GENERATOR_POLICY = createMockPolicyView(
    organizationId = "organizationId",
    id = "password_generator_id",
    type = PolicyType.PASSWORD_GENERATOR,
    enabled = true,
    data = """
      {
        "defaultType":null,
        "minLength":10,
        "useUpper":true,
        "useNumbers":true,
        "useSpecial":true,
        "minNumbers":3,
        "minSpecial":3,
        "minNumberWords":5,
        "capitalize":true,
        "includeNumber":true,
        "useLower":true
      }
    """,
)
