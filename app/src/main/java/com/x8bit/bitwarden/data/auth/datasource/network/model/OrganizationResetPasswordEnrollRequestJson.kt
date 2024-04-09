package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body object when enrolling a user in reset password functionality for this organization.
 *
 * @param passwordHash The hash of this user's password. This is not required if the user does not
 * have a password.
 * @param resetPasswordKey The key used for password reset.
 */
@Serializable
data class OrganizationResetPasswordEnrollRequestJson(
    @SerialName("masterPasswordHash") val passwordHash: String?,
    @SerialName("resetPasswordKey") val resetPasswordKey: String,
)
