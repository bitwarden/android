package com.x8bit.bitwarden.data.auth.repository

import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TwoFactorDataModel
import com.x8bit.bitwarden.data.auth.manager.AuthRequestManager
import com.x8bit.bitwarden.data.auth.repository.model.AuthState
import com.x8bit.bitwarden.data.auth.repository.model.BreachCountResult
import com.x8bit.bitwarden.data.auth.repository.model.DeleteAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.KnownDeviceResult
import com.x8bit.bitwarden.data.auth.repository.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.model.NewSsoUserResult
import com.x8bit.bitwarden.data.auth.repository.model.OrganizationDomainSsoDetailsResult
import com.x8bit.bitwarden.data.auth.repository.model.PasswordHintResult
import com.x8bit.bitwarden.data.auth.repository.model.PasswordStrengthResult
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.auth.repository.model.PrevalidateSsoResult
import com.x8bit.bitwarden.data.auth.repository.model.RegisterResult
import com.x8bit.bitwarden.data.auth.repository.model.ResendEmailResult
import com.x8bit.bitwarden.data.auth.repository.model.ResetPasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.SetPasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.SwitchAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.DuoCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.SsoCallbackResult
import com.x8bit.bitwarden.data.auth.util.YubiKeyResult
import com.x8bit.bitwarden.data.platform.datasource.network.authenticator.AuthenticatorProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides an API for observing an modifying authentication state.
 */
@Suppress("TooManyFunctions")
interface AuthRepository : AuthenticatorProvider, AuthRequestManager {
    /**
     * Models the current auth state.
     */
    val authStateFlow: StateFlow<AuthState>

    /**
     * Emits updates for changes to the [UserState].
     */
    val userStateFlow: StateFlow<UserState?>

    /**
     * Flow of the current [CaptchaCallbackTokenResult]. Subscribers should listen to the flow
     * in order to receive updates whenever [setCaptchaCallbackTokenResult] is called.
     */
    val captchaTokenResultFlow: Flow<CaptchaCallbackTokenResult>

    /**
     * Flow of the current [DuoCallbackTokenResult]. Subscribers should listen to the flow
     * in order to receive updates whenever [setDuoCallbackTokenResult] is called.
     */
    val duoTokenResultFlow: Flow<DuoCallbackTokenResult>

    /**
     * Flow of the current [SsoCallbackResult]. Subscribers should listen to the flow in order to
     * receive updates whenever [setSsoCallbackResult] is called.
     */
    val ssoCallbackResultFlow: Flow<SsoCallbackResult>

    /**
     * Flow of the current [YubiKeyResult]. Subscribers should listen to the flow in order to
     * receive updates whenever [setYubiKeyResult] is called.
     */
    val yubiKeyResultFlow: Flow<YubiKeyResult>

    /**
     * The organization identifier currently associated with this user's SSO flow.
     */
    val ssoOrganizationIdentifier: String?

    /**
     * The two-factor response data necessary for login and also to populate the
     * Two-Factor Login screen.
     */
    var twoFactorResponse: GetTokenResponseJson.TwoFactorRequired?

    /**
     * The currently persisted saved email address (or `null` if not set).
     */
    var rememberedEmailAddress: String?

    /**
     * The currently persisted organization identifier (or `null` if not set).
     */
    var rememberedOrgIdentifier: String?

    /**
     * The currently persisted state indicating whether the user has trusted this device.
     */
    var shouldTrustDevice: Boolean

    /**
     * Tracks whether there is an additional account that is pending login/registration in order to
     * have multiple accounts available.
     *
     * This allows a direct view into and modification of [UserState.hasPendingAccountAddition].
     * Note that this call has no effect when there is no [UserState] information available.
     */
    var hasPendingAccountAddition: Boolean

    /**
     * Return the cached password policies for the current user.
     */
    val passwordPolicies: List<PolicyInformation.MasterPassword>

    /**
     * The reason for resetting the password.
     */
    val passwordResetReason: ForcePasswordResetReason?

    /**
     * Clears the pending deletion state that occurs when the an account is successfully deleted.
     */
    fun clearPendingAccountDeletion()

    /**
     * Attempt to delete the current account and logout them out upon success.
     */
    suspend fun deleteAccount(password: String): DeleteAccountResult

    /**
     * Attempt to create a new user via SSO and log them into their account. Upon success the new
     * user will also have the vault automatically unlocked for them.
     */
    suspend fun createNewSsoUser(): NewSsoUserResult

    /**
     * Attempt to complete the trusted device login with the given [requestPrivateKey] and
     * [asymmetricalKey]. This will unlock the vault and finish trusting the device.
     */
    suspend fun completeTdeLogin(
        requestPrivateKey: String,
        asymmetricalKey: String,
    ): LoginResult

    /**
     * Attempt to login with the given email and password. Updated access token will be reflected
     * in [authStateFlow].
     */
    suspend fun login(
        email: String,
        password: String,
        captchaToken: String?,
    ): LoginResult

    /**
     * Attempt to login with the given email and auth request ID and access code. The updated
     * access token will be reflected in [authStateFlow].
     */
    @Suppress("LongParameterList")
    suspend fun login(
        email: String,
        requestId: String,
        accessCode: String,
        asymmetricalKey: String,
        requestPrivateKey: String,
        masterPasswordHash: String?,
        captchaToken: String?,
    ): LoginResult

    /**
     * Repeat the previous login attempt but this time with Two-Factor authentication
     * information. Password is included if available to unlock the vault after
     * authentication. Updated access token will be reflected in [authStateFlow].
     */
    suspend fun login(
        email: String,
        password: String?,
        twoFactorData: TwoFactorDataModel,
        captchaToken: String?,
    ): LoginResult

    /**
     * Attempt to login using a SSO flow. Updated access token will be reflected in [authStateFlow].
     */
    @Suppress("LongParameterList")
    suspend fun login(
        email: String,
        ssoCode: String,
        ssoCodeVerifier: String,
        ssoRedirectUri: String,
        captchaToken: String?,
        organizationIdentifier: String,
    ): LoginResult

    /**
     * Log out the current user.
     */
    fun logout()

    /**
     * Resend the email with the two-factor verification code.
     */
    suspend fun resendVerificationCodeEmail(): ResendEmailResult

    /**
     * Switches to the account corresponding to the given [userId] if possible.
     */
    fun switchAccount(userId: String): SwitchAccountResult

    /**
     * Updates the "last active time" for the current user.
     */
    fun updateLastActiveTime()

    /**
     * Attempt to register a new account with the given parameters.
     */
    @Suppress("LongParameterList")
    suspend fun register(
        email: String,
        masterPassword: String,
        masterPasswordHint: String?,
        captchaToken: String?,
        shouldCheckDataBreaches: Boolean,
        isMasterPasswordStrong: Boolean,
    ): RegisterResult

    /**
     * Attempt to request a password hint.
     */
    suspend fun passwordHintRequest(
        email: String,
    ): PasswordHintResult

    /**
     * Resets the users password from the [currentPassword] (or null for account recovery resets),
     * to the [newPassword] and optional [passwordHint].
     */
    suspend fun resetPassword(
        currentPassword: String?,
        newPassword: String,
        passwordHint: String?,
    ): ResetPasswordResult

    /**
     * Sets the user's password to [password] for the user within the given [organizationIdentifier]
     * with an optional [passwordHint].
     */
    suspend fun setPassword(
        organizationIdentifier: String,
        password: String,
        passwordHint: String?,
    ): SetPasswordResult

    /**
     * Set the value of [captchaTokenResultFlow].
     */
    fun setCaptchaCallbackTokenResult(tokenResult: CaptchaCallbackTokenResult)

    /**
     * Set the value of [duoTokenResultFlow].
     */
    fun setDuoCallbackTokenResult(tokenResult: DuoCallbackTokenResult)

    /**
     * Set the value of [yubiKeyResultFlow].
     */
    fun setYubiKeyResult(yubiKeyResult: YubiKeyResult)

    /**
     * Checks for a claimed domain organization for the [email] that can be used for an SSO request.
     */
    suspend fun getOrganizationDomainSsoDetails(
        email: String,
    ): OrganizationDomainSsoDetailsResult

    /**
     * Prevalidates the organization identifier used in an SSO request.
     */
    suspend fun prevalidateSso(
        organizationIdentifier: String,
    ): PrevalidateSsoResult

    /**
     * Set the value of [ssoCallbackResultFlow].
     */
    fun setSsoCallbackResult(result: SsoCallbackResult)

    /**
     * Get a [Boolean] indicating whether this is a known device.
     */
    suspend fun getIsKnownDevice(emailAddress: String): KnownDeviceResult

    /**
     * Attempts to get the number of times the given [password] has been breached.
     */
    suspend fun getPasswordBreachCount(password: String): BreachCountResult

    /**
     * Get the password strength for the given [email] and [password] combo.
     */
    suspend fun getPasswordStrength(email: String, password: String): PasswordStrengthResult

    /**
     * Validates the master password for the current logged in user.
     */
    suspend fun validatePassword(password: String): ValidatePasswordResult

    /**
     * Validates the given [password] against the master password
     * policies for the current user.
     */
    suspend fun validatePasswordAgainstPolicies(password: String): Boolean
}
