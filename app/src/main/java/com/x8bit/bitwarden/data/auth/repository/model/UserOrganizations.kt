package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Associates a list of [organizations] with the given [userId].
 */
data class UserOrganizations(
    val userId: String,
    val organizations: List<Organization>,
)
