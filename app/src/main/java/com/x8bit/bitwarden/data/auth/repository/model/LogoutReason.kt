package com.x8bit.bitwarden.data.auth.repository.model

/**
 * Indicates the reason that the user is being logged out.
 */
sealed class LogoutReason {
    /**
     * An optional additional tag for an event.
     */
    open val source: String? = null

    /**
     * Indicates that the logout is happening because the account was deleted.
     */
    data object AccountDelete : LogoutReason()

    /**
     * Indicates that the logout is related to biometrics.
     */
    sealed class Biometrics : LogoutReason() {
        /**
         * Indicates that the logout is caused by a biometrics lockout.
         */
        data object Lockout : Biometrics()

        /**
         * Indicates that the logout is happening because biometrics is no longer supported.
         */
        data object NoLongerSupported : Biometrics()
    }

    /**
     * Indicates that the logout is happening because of an invalid state.
     */
    data class InvalidState(
        override val source: String,
    ) : LogoutReason()

    /**
     * Indicates that the logout is happening because the user opted to logout via a button.
     */
    data class Click(
        override val source: String,
    ) : LogoutReason()

    /**
     * Indicates that the logout is happening because the a logout notification was received.
     */
    data object Notification : LogoutReason()

    /**
     * Indicates that the logout is happening because the sync security stamp was invalidated.
     */
    data object SecurityStamp : LogoutReason()

    /**
     * Indicates that the logout is happening because of a timeout action.
     */
    data object Timeout : LogoutReason()

    /**
     * Indicates that the logout is happening because the access token could not be refreshed.
     */
    data object TokenRefreshFail : LogoutReason()

    /**
     * Indicates that the logout is happening because the user tried to unlock the vault
     * unsuccessfully too many times.
     */
    data object TooManyUnlockAttempts : LogoutReason()

    /**
     * Indicates that the logout is happening because the left the organization.
     */
    data object LeftOrganization : LogoutReason()
}
