package com.x8bit.bitwarden.ui.auth.feature.auth

import androidx.navigation3.runtime.NavKey
import com.x8bit.bitwarden.ui.auth.feature.loginwithdevice.model.LoginWithDeviceType
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.model.UnlockType
import com.x8bit.bitwarden.ui.platform.feature.settings.PreAuthSettingsGraph
import kotlinx.serialization.Serializable

/**
 * Represents the hierarchy of the Auth UI navigation graph and provides type-safe routes to each
 * destination available in the graph.
 */
@Serializable
sealed class AuthGraph : NavKey {
    /**
     * The type-safe route for the landing screen.
     */
    @Serializable
    data object LandingRoute : AuthGraph()

    /**
     * The type-safe route for the welcome screen.
     */
    @Serializable
    data object WelcomeRoute : AuthGraph()

    /**
     * The type-safe route for the start registration screen.
     */
    @Serializable
    data object StartRegistrationRoute : AuthGraph()

    /**
     * The type-safe route for the check email screen.
     */
    @Serializable
    data class CheckEmailRoute(
        val emailAddress: String,
    ) : AuthGraph()

    /**
     * The type-safe route for the complete registration screen.
     */
    @Serializable
    data class CompleteRegistrationRoute(
        val emailAddress: String,
        val verificationToken: String,
        val fromEmail: Boolean,
    ) : AuthGraph()

    /**
     * The type-safe route for the expired registration link screen.
     */
    @Serializable
    data object ExpiredRegistrationLinkRoute : AuthGraph()

    /**
     * The type-safe route for the login screen.
     */
    @Serializable
    data class LoginRoute(
        val emailAddress: String,
    ) : AuthGraph()

    /**
     * The type-safe route for the login with device screen.
     */
    @Serializable
    data class LoginWithDeviceRoute(
        val emailAddress: String,
        val loginType: LoginWithDeviceType,
    ) : AuthGraph()

    /**
     * The type-safe route for the two-factor login screen.
     */
    @Serializable
    data class TwoFactorLoginRoute(
        val emailAddress: String,
        val password: String?,
        val orgIdentifier: String?,
        val isNewDeviceVerification: Boolean,
    ) : AuthGraph()

    /**
     * The type-safe route for the enterprise sign-on screen.
     */
    @Serializable
    data class EnterpriseSignOnRoute(
        val emailAddress: String,
    ) : AuthGraph()

    /**
     * The type-safe route for the set password screen.
     */
    @Serializable
    data object SetPasswordRoute : AuthGraph()

    /**
     * The type-safe route for the environment screen.
     */
    @Serializable
    data object EnvironmentRoute : AuthGraph()

    /**
     * The type-safe route for the master password hint screen.
     */
    @Serializable
    data class MasterPasswordHintRoute(
        val emailAddress: String,
    ) : AuthGraph()

    /**
     * The type-safe route for the master password guidance screen.
     */
    @Serializable
    data object MasterPasswordGuidanceRoute : AuthGraph()

    /**
     * The type-safe route for the Prevent Account Lockout screen.
     */
    @Serializable
    data object PreventAccountLockoutRoute : AuthGraph()

    /**
     * The type-safe route for the master password generator screen.
     */
    @Serializable
    data object MasterPasswordGeneratorRoute : AuthGraph()

    /**
     * The type-safe route for the trusted device screen.
     */
    @Serializable
    data object TrustedDeviceRoute : AuthGraph()

    /**
     * The type-safe route for the vault unlock screen.
     */
    @Serializable
    sealed class VaultUnlockRoute : AuthGraph() {
        /**
         * The underlying [UnlockType] used in the vault unlock screen.
         */
        abstract val unlockType: UnlockType

        /**
         * The type-safe route for the standard vault unlock screen.
         */
        @Serializable
        data object Standard : VaultUnlockRoute() {
            override val unlockType: UnlockType get() = UnlockType.STANDARD
        }

        /**
         * The type-safe route for the TDE vault unlock screen.
         */
        @Serializable
        data object Tde : VaultUnlockRoute() {
            override val unlockType: UnlockType get() = UnlockType.TDE
        }
    }

    /**
     * The type-safe route for the pre-authentication settings, which hosts the
     * [PreAuthSettingsGraph].
     */
    @Serializable
    data object PreAuthSettingsRoute : AuthGraph()
}
