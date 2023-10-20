package com.x8bit.bitwarden.data.auth.datasource.disk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Describes the reason for a forced password reset.
 */
@Serializable
enum class ForcePasswordResetReason {
    /**
     * An organization admin forced a user to reset their password.
     */
    @SerialName("adminForcePasswordReset")
    ADMIN_FORCE_PASSWORD_RESET,

    /**
     * A user logged in with a master password that does not meet an organization's master password
     * policy that is enforced on login.
     */
    @SerialName("weakMasterPasswordOnLogin")
    WEAK_MASTER_PASSWORD_ON_LOGIN,
}
