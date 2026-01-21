package com.x8bit.bitwarden.data.auth.repository.model

import com.bitwarden.network.model.OrganizationType

/**
 * Creates a mock [Organization] with a given parameters.
 */
fun createMockOrganization(
    number: Int,
    id: String = "mockId-$number",
    name: String = "mockName-$number",
    shouldManageResetPassword: Boolean = false,
    shouldUseKeyConnector: Boolean = false,
    role: OrganizationType = OrganizationType.ADMIN,
    keyConnectorUrl: String? = "mockKeyConnectorUrl-$number",
    userIsClaimedByOrganization: Boolean = false,
    limitItemDeletion: Boolean = false,
): Organization =
    Organization(
        id = id,
        name = name,
        shouldManageResetPassword = shouldManageResetPassword,
        shouldUseKeyConnector = shouldUseKeyConnector,
        role = role,
        keyConnectorUrl = keyConnectorUrl,
        userIsClaimedByOrganization = userIsClaimedByOrganization,
        limitItemDeletion = limitItemDeletion,
    )
