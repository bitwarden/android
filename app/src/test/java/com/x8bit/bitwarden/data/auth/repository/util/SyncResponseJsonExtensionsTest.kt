package com.x8bit.bitwarden.data.auth.repository.util

import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockOrganization
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockPolicy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SyncResponseJsonExtensionsTest {
    @Test
    fun `toOrganization should output the correct organization`() {
        assertEquals(
            Organization(
                id = "mockId-1",
                name = "mockName-1",
            ),
            createMockOrganization(number = 1).toOrganization(),
        )
    }

    @Test
    fun `toOrganizations should output the correct list of organizations`() {
        assertEquals(
            listOf(
                Organization(
                    id = "mockId-1",
                    name = "mockName-1",
                ),
                Organization(
                    id = "mockId-2",
                    name = "mockName-2",
                ),
            ),
            listOf(
                createMockOrganization(number = 1),
                createMockOrganization(number = 2),
            )
                .toOrganizations(),
        )
    }

    @Test
    @OptIn(ExperimentalSerializationApi::class)
    fun `policyInformation converts the Json data to policy information`() {
        val masterPasswordData = buildJsonObject {
            put(key = "minLength", value = 10)
            put(key = "minComplexity", value = 3)
            put(key = "requireUpper", value = null)
            put(key = "requireLower", value = null)
            put(key = "requireNumbers", value = true)
            put(key = "requireSpecial", value = null)
            put(key = "enforceOnLogin", value = true)
        }
        val masterPasswordPolicy = createMockPolicy(
            type = PolicyTypeJson.MASTER_PASSWORD,
            data = masterPasswordData,
        )
        val policyInformation = PolicyInformation.MasterPassword(
            minLength = 10,
            minComplexity = 3,
            requireUpper = null,
            requireLower = null,
            requireNumbers = true,
            requireSpecial = null,
            enforceOnLogin = true,
        )

        assertEquals(
            policyInformation,
            masterPasswordPolicy.policyInformation,
        )
    }

    @Test
    fun `policyInformation returns null policy information for null data`() {
        val masterPasswordPolicy = createMockPolicy(
            type = PolicyTypeJson.MASTER_PASSWORD,
            data = null,
        )

        assertNull(masterPasswordPolicy.policyInformation)
    }
}
