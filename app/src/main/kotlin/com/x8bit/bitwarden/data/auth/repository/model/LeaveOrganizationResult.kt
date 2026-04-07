package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Models result of leaving an organization.
 */
sealed class LeaveOrganizationResult {
    /**
     * Leave organization succeeded.
     */
    data object Success : LeaveOrganizationResult()

    /**
     * There was an error leaving the organization.
     */
    data class Error(
        val error: Throwable?,
    ) : LeaveOrganizationResult()
}
