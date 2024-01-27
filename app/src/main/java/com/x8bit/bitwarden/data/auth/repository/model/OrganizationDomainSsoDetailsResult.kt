package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Response types when checking for an email's claimed domain organization.
 */
sealed class OrganizationDomainSsoDetailsResult {
    /**
     * The request was successful.
     *
     * @property isSsoAvailable Indicates if SSO is available for the email address.
     * @property organizationIdentifier The claimed organization identifier for the email address.
     */
    data class Success(
        val isSsoAvailable: Boolean,
        val organizationIdentifier: String,
    ) : OrganizationDomainSsoDetailsResult()

    /**
     * The request failed.
     */
    data object Failure : OrganizationDomainSsoDetailsResult()
}
