package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Represents an organization a user may be a member of.
 *
 * @property id The ID of the organization.
 * @property name The name of the organization (if applicable).
 */
data class Organization(
    val id: String,
    val name: String?,
)
