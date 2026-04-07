package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of leaving an organization.
 */
sealed class RevokeFromOrganizationResult {
    /**
     * Revoke from organization succeeded.
     */
    data object Success : RevokeFromOrganizationResult()

    /**
     * There was an error revoking from the organization.
     */
    data class Error(
        val error: Throwable?,
    ) : RevokeFromOrganizationResult()
}
