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
 * @property userIsClaimedByOrganization Indicates that the user is claimed by the organization.
 * @property limitItemDeletion Indicates that the organization limits item deletion.
 * @property shouldUseEvents Indicates if the organization uses tracking events.
 * @property maxCollections The maximum number of collections allowed (nullable).
 * @property canCreateNewCollections Indicates if the user can create new collections.
 * @property canEditAnyCollection Indicates if the user can edit any collection.
 * @property canDeleteAnyCollection Indicates if the user can delete any collection.
 */
data class Organization(
    val id: String,
    val name: String,
    val shouldManageResetPassword: Boolean,
    val shouldUseKeyConnector: Boolean,
    val role: OrganizationType,
    val keyConnectorUrl: String?,
    val userIsClaimedByOrganization: Boolean,
    val limitItemDeletion: Boolean,
    val shouldUseEvents: Boolean,
    val maxCollections: Int?,
    val limitCollectionCreation: Boolean,
    val limitCollectionDeletion: Boolean,
    val organizationUserId: String?,
    val canCreateNewCollections: Boolean,
    val canEditAnyCollection: Boolean,
    val canDeleteAnyCollection: Boolean,
) {
    /**
     * Whether the user can create new collections in this organization, accounting for
     * the organization's role and limitCollectionCreation setting.
     * Matches web client logic: `!limitCollectionCreation || isAdmin || permissions.createNewCollections`
     */
    val canManageCollections: Boolean
        get() = !limitCollectionCreation ||
            role == OrganizationType.ADMIN ||
            role == OrganizationType.OWNER ||
            canCreateNewCollections
}
