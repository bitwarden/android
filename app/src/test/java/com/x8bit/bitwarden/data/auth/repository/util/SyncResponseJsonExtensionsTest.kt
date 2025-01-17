package com.x8bit.bitwarden.data.auth.repository.util

import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.vault.datasource.network.model.OrganizationType
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockOrganization
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockPolicy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
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
                shouldManageResetPassword = false,
                shouldUseKeyConnector = false,
                role = OrganizationType.ADMIN,
                shouldUsersGetPremium = false,
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
                    shouldManageResetPassword = false,
                    shouldUseKeyConnector = true,
                    role = OrganizationType.ADMIN,
                    shouldUsersGetPremium = false,
                ),
                Organization(
                    id = "mockId-2",
                    name = "mockName-2",
                    shouldManageResetPassword = true,
                    shouldUseKeyConnector = false,
                    role = OrganizationType.USER,
                    shouldUsersGetPremium = false,
                ),
            ),
            listOf(
                createMockOrganization(number = 1).copy(shouldUseKeyConnector = true),
                createMockOrganization(number = 2, shouldManageResetPassword = true)
                    .copy(type = OrganizationType.USER),
            )
                .toOrganizations(),
        )
    }

    @Test
    fun `policyInformation converts the MasterPassword Json data to policy information`() {
        val policyInformation = PolicyInformation.MasterPassword(
            minLength = 10,
            minComplexity = 3,
            requireUpper = null,
            requireLower = null,
            requireNumbers = true,
            requireSpecial = null,
            enforceOnLogin = true,
        )
        val policy = createMockPolicy(
            type = PolicyTypeJson.MASTER_PASSWORD,
            data = Json.encodeToJsonElement(policyInformation).jsonObject,
        )

        assertEquals(
            policyInformation,
            policy.policyInformation,
        )
    }

    @Test
    fun `policyInformation converts the PasswordGenerator Json data to policy information`() {
        val policyInformation = PolicyInformation.PasswordGenerator(
            overridePasswordType = "password",
            minLength = null,
            useUpper = true,
            useLower = true,
            useNumbers = null,
            useSpecial = null,
            minNumbers = null,
            minSpecial = null,
            minNumberWords = 4,
            capitalize = true,
            includeNumber = null,
        )
        val policy = createMockPolicy(
            type = PolicyTypeJson.PASSWORD_GENERATOR,
            data = Json.encodeToJsonElement(policyInformation).jsonObject,
        )

        assertEquals(
            policyInformation,
            policy.policyInformation,
        )
    }

    @Test
    fun `policyInformation converts the VaultTimeout Json data to policy information`() {
        val policyInformation = PolicyInformation.VaultTimeout(
            minutes = 10,
            action = "lock",
        )
        val policy = createMockPolicy(
            type = PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT,
            data = Json.encodeToJsonElement(policyInformation).jsonObject,
        )

        assertEquals(
            policyInformation,
            policy.policyInformation,
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
