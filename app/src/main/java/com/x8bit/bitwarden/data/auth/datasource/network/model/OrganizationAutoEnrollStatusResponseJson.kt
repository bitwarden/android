package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response object returned when requesting organization domain SSO details.
 *
 * @property organizationId The ID of this organization.
 * @property isResetPasswordEnabled Indicates whether the auto-enroll reset password functionality
 * is enabled.
 */
@Serializable
data class OrganizationAutoEnrollStatusResponseJson(
    @SerialName("id") val organizationId: String,
    @SerialName("resetPasswordEnabled") val isResetPasswordEnabled: Boolean,
)
