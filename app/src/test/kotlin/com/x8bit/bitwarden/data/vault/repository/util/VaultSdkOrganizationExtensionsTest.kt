package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.network.model.OrganizationStatusType
import com.bitwarden.network.model.OrganizationType
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.createMockOrganizationNetwork
import com.bitwarden.organizations.OrganizationUserStatusType
import com.bitwarden.organizations.OrganizationUserType
import com.bitwarden.policies.OrganizationUserPolicyContext
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockOrganizationUserPolicyContext
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

class VaultSdkOrganizationExtensionsTest {

    @Test
    fun `toSdkOrganizationPolicyContexts should return empty list when given empty list`() {
        assertEquals(
            emptyList<OrganizationUserPolicyContext>(),
            emptyList<SyncResponseJson.Profile.Organization>().toSdkOrganizationPolicyContexts(),
        )
    }

    @Test
    fun `toSdkOrganizationPolicyContexts should convert all organizations in a list`() {
        assertEquals(
            listOf(
                createMockOrganizationUserPolicyContext(number = 1),
                createMockOrganizationUserPolicyContext(number = 2),
            ),
            listOf(
                createMockOrganizationNetwork(number = 1),
                createMockOrganizationNetwork(number = 2),
            )
                .toSdkOrganizationPolicyContexts(),
        )
    }

    @Test
    fun `toSdkOrganizationPolicyContexts should map all OrganizationStatusType values`() {
        STATUS_TYPE_MAP.forEach { (inputStatus, expectedStatus) ->
            assertEquals(
                listOf(createMockOrganizationUserPolicyContext(status = expectedStatus)),
                listOf(createMockOrganizationNetwork(number = 1, status = inputStatus))
                    .toSdkOrganizationPolicyContexts(),
            )
        }
    }

    @Test
    fun `toSdkOrganizationPolicyContexts should map all OrganizationType values`() {
        ORGANIZATION_TYPE_MAP.forEach { (inputType, expectedRole) ->
            assertEquals(
                listOf(createMockOrganizationUserPolicyContext(role = expectedRole)),
                listOf(createMockOrganizationNetwork(number = 1, type = inputType))
                    .toSdkOrganizationPolicyContexts(),
            )
        }
    }
}

private val STATUS_TYPE_MAP: Map<OrganizationStatusType, OrganizationUserStatusType> = mapOf(
    OrganizationStatusType.REVOKED to OrganizationUserStatusType.REVOKED,
    OrganizationStatusType.INVITED to OrganizationUserStatusType.INVITED,
    OrganizationStatusType.ACCEPTED to OrganizationUserStatusType.ACCEPTED,
    OrganizationStatusType.CONFIRMED to OrganizationUserStatusType.CONFIRMED,
)

private val ORGANIZATION_TYPE_MAP: Map<OrganizationType, OrganizationUserType> = mapOf(
    OrganizationType.OWNER to OrganizationUserType.OWNER,
    OrganizationType.ADMIN to OrganizationUserType.ADMIN,
    OrganizationType.USER to OrganizationUserType.USER,
    OrganizationType.CUSTOM to OrganizationUserType.CUSTOM,
)
