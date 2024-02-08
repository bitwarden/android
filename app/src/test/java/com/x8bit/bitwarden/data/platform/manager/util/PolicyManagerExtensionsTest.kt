package com.x8bit.bitwarden.data.platform.manager.util

import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PolicyManagerExtensionsTest {
    private val mutablePolicyFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Policy>>()
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
    fun `getPolicyTypeJson with MasterPassword should map to appropriate PolicyTypeJson`() {
        assertEquals(
            PolicyTypeJson.MASTER_PASSWORD,
            getPolicyTypeJson<PolicyInformation.MasterPassword>(),
        )
    }

    @Test
    fun `getPolicyTypeJson with PasswordGenerator should map to appropriate PolicyTypeJson`() {
        assertEquals(
            PolicyTypeJson.PASSWORD_GENERATOR,
            getPolicyTypeJson<PolicyInformation.PasswordGenerator>(),
        )
    }

    @Test
    fun `getPolicyTypeJson with SendOptions should map to appropriate PolicyTypeJson`() {
        assertEquals(
            PolicyTypeJson.SEND_OPTIONS,
            getPolicyTypeJson<PolicyInformation.SendOptions>(),
        )
    }

    @Test
    fun `getPolicyTypeJson with VaultTimeout should map to appropriate PolicyTypeJson`() {
        assertEquals(
            PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT,
            getPolicyTypeJson<PolicyInformation.VaultTimeout>(),
        )
    }
}

private val MASTER_PASSWORD_POLICY = SyncResponseJson.Policy(
    organizationId = "organizationId",
    id = "master_password_id",
    type = PolicyTypeJson.MASTER_PASSWORD,
    isEnabled = true,
    data = JsonObject(
        mapOf(
            "minLength" to JsonPrimitive(10),
            "minComplexity" to JsonPrimitive(10),
            "requireUpper" to JsonPrimitive(false),
            "requireLower" to JsonPrimitive(true),
            "requireNumbers" to JsonNull,
            "requireSpecial" to JsonPrimitive(false),
            "enforceOnLogin" to JsonPrimitive(true),
        ),
    ),
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

private val PASSWORD_GENERATOR_POLICY = SyncResponseJson.Policy(
    organizationId = "organizationId",
    id = "password_generator_id",
    type = PolicyTypeJson.PASSWORD_GENERATOR,
    isEnabled = true,
    data = JsonObject(
        mapOf(
            "defaultType" to JsonNull,
            "minLength" to JsonPrimitive(10),
            "useUpper" to JsonPrimitive(true),
            "useNumbers" to JsonPrimitive(true),
            "useSpecial" to JsonPrimitive(true),
            "minNumbers" to JsonPrimitive(3),
            "minSpecial" to JsonPrimitive(3),
            "minNumberWords" to JsonPrimitive(5),
            "capitalize" to JsonPrimitive(true),
            "includeNumber" to JsonPrimitive(true),
            "useLower" to JsonPrimitive(true),
        ),
    ),
)
