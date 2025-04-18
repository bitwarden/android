package com.x8bit.bitwarden.data.auth.repository.model

import com.bitwarden.network.model.OrganizationType

/**
 * Represents an organization a user may be a member of.
 *
 * @property id The ID of the organization.
 * @property name The name of the organization (if applicable).
 * @property shouldManageResetPassword Indicates that this user has the permission to manage their
 * own password.
 * @property shouldUseKeyConnector Indicates that the organization uses a key connector.
 * @property role The user's role in the organization.
 * @property keyConnectorUrl The key connector domain (if applicable).
 */
data class Organization(
    val id: String,
    val name: String?,
    val shouldManageResetPassword: Boolean,
    val shouldUseKeyConnector: Boolean,
    val role: OrganizationType,
    val keyConnectorUrl: String?,
)
