package com.x8bit.bitwarden.data.auth.repository.model

import java.time.ZonedDateTime

/**
 * Response types when checking for an email's claimed domain organization.
 */
sealed class OrganizationDomainSsoDetailsResult {
    /**
     * The request was successful.
     *
     * @property isSsoAvailable Indicates if SSO is available for the email address.
     * @property organizationIdentifier The claimed organization identifier for the email address.
     * @property verifiedDate The date and time when the domain was verified.
     */
    data class Success(
        val isSsoAvailable: Boolean,
        val organizationIdentifier: String,
        val verifiedDate: ZonedDateTime?,
    ) : OrganizationDomainSsoDetailsResult()

    /**
     * The request failed.
     */
    data object Failure : OrganizationDomainSsoDetailsResult()
}
