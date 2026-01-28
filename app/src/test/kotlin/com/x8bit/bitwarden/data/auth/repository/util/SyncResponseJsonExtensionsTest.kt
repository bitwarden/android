package com.x8bit.bitwarden.data.auth.repository.util

import com.bitwarden.network.model.OrganizationType
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.createMockOrganizationNetwork
import com.bitwarden.network.model.createMockPermissions
import com.bitwarden.network.model.createMockPolicy
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.auth.repository.model.createMockOrganization
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
            createMockOrganization(number = 1),
            createMockOrganizationNetwork(number = 1).toOrganization(),
        )
    }

    @Test
    fun `toOrganizations should output the correct list of organizations`() {
        assertEquals(
            listOf(
                createMockOrganization(
                    number = 1,
                    shouldUseKeyConnector = true,
                ),
                createMockOrganization(
                    number = 2,
                    shouldManageResetPassword = true,
                    role = OrganizationType.USER,
                ),
            ),
            listOf(
                createMockOrganizationNetwork(number = 1, shouldUseKeyConnector = true),
                createMockOrganizationNetwork(
                    number = 2,
                    type = OrganizationType.USER,
                    permissions = createMockPermissions(shouldManageResetPassword = true),
                ),
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
            action = PolicyInformation.VaultTimeout.Action.LOCK,
            type = PolicyInformation.VaultTimeout.Type.CUSTOM,
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
