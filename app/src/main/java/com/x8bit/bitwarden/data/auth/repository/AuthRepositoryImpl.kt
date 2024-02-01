package com.x8bit.bitwarden.data.auth.repository

import android.os.SystemClock
import com.bitwarden.core.AuthRequestResponse
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.crypto.HashPurpose
import com.bitwarden.crypto.Kdf
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import com.x8bit.bitwarden.data.auth.datasource.network.model.DeviceDataModel
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson.CaptchaRequired
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson.Success
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson.TwoFactorRequired
import com.x8bit.bitwarden.data.auth.datasource.network.model.IdentityTokenAuthModel
import com.x8bit.bitwarden.data.auth.datasource.network.model.PasswordHintResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RefreshTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.ResendEmailRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.ResetPasswordRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TwoFactorAuthMethod
import com.x8bit.bitwarden.data.auth.datasource.network.model.TwoFactorDataModel
import com.x8bit.bitwarden.data.auth.datasource.network.service.AccountsService
import com.x8bit.bitwarden.data.auth.datasource.network.service.AuthRequestsService
import com.x8bit.bitwarden.data.auth.datasource.network.service.DevicesService
import com.x8bit.bitwarden.data.auth.datasource.network.service.HaveIBeenPwnedService
import com.x8bit.bitwarden.data.auth.datasource.network.service.IdentityService
import com.x8bit.bitwarden.data.auth.datasource.network.service.NewAuthRequestService
import com.x8bit.bitwarden.data.auth.datasource.network.service.OrganizationService
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toInt
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toKdfTypeJson
import com.x8bit.bitwarden.data.auth.manager.UserLogoutManager
import com.x8bit.bitwarden.data.auth.repository.model.AuthRequest
import com.x8bit.bitwarden.data.auth.repository.model.AuthRequestResult
import com.x8bit.bitwarden.data.auth.repository.model.AuthRequestsResult
import com.x8bit.bitwarden.data.auth.repository.model.AuthState
import com.x8bit.bitwarden.data.auth.repository.model.BreachCountResult
import com.x8bit.bitwarden.data.auth.repository.model.CreateAuthRequestResult
import com.x8bit.bitwarden.data.auth.repository.model.DeleteAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.KnownDeviceResult
import com.x8bit.bitwarden.data.auth.repository.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.model.OrganizationDomainSsoDetailsResult
import com.x8bit.bitwarden.data.auth.repository.model.PasswordHintResult
import com.x8bit.bitwarden.data.auth.repository.model.PasswordStrengthResult
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.auth.repository.model.PrevalidateSsoResult
import com.x8bit.bitwarden.data.auth.repository.model.RegisterResult
import com.x8bit.bitwarden.data.auth.repository.model.ResendEmailResult
import com.x8bit.bitwarden.data.auth.repository.model.ResetPasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.SwitchAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.UserFingerprintResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.SsoCallbackResult
import com.x8bit.bitwarden.data.auth.repository.util.policyInformation
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
import com.x8bit.bitwarden.data.auth.repository.util.toUserState
import com.x8bit.bitwarden.data.auth.repository.util.toUserStateJson
import com.x8bit.bitwarden.data.auth.repository.util.userOrganizationsList
import com.x8bit.bitwarden.data.auth.repository.util.userOrganizationsListFlow
import com.x8bit.bitwarden.data.auth.util.KdfParamsConstants.DEFAULT_PBKDF2_ITERATIONS
import com.x8bit.bitwarden.data.auth.util.toSdkParams
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.manager.util.getActivePolicies
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.platform.util.asFailure
import com.x8bit.bitwarden.data.platform.util.flatMap
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import java.time.Clock
import javax.inject.Singleton

private const val PASSWORDLESS_NOTIFICATION_TIMEOUT_MILLIS: Long = 15L * 60L * 1_000L
private const val PASSWORDLESS_NOTIFICATION_RETRY_INTERVAL_MILLIS: Long = 4L * 1_000L

/**
 * Default implementation of [AuthRepository].
 */
@Suppress("LargeClass", "LongParameterList", "TooManyFunctions")
@Singleton
class AuthRepositoryImpl(
    private val clock: Clock,
    private val accountsService: AccountsService,
    private val authRequestsService: AuthRequestsService,
    private val devicesService: DevicesService,
    private val haveIBeenPwnedService: HaveIBeenPwnedService,
    private val identityService: IdentityService,
    private val newAuthRequestService: NewAuthRequestService,
    private val organizationService: OrganizationService,
    private val authSdkSource: AuthSdkSource,
    private val vaultSdkSource: VaultSdkSource,
    private val authDiskSource: AuthDiskSource,
    private val environmentRepository: EnvironmentRepository,
    private val settingsRepository: SettingsRepository,
    private val vaultRepository: VaultRepository,
    private val userLogoutManager: UserLogoutManager,
    private val pushManager: PushManager,
    private val policyManager: PolicyManager,
    dispatcherManager: DispatcherManager,
    private val elapsedRealtimeMillisProvider: () -> Long = { SystemClock.elapsedRealtime() },
) : AuthRepository {
    private val mutableHasPendingAccountAdditionStateFlow = MutableStateFlow(false)
    private val mutableHasPendingAccountDeletionStateFlow = MutableStateFlow(false)

    /**
     * The auth information to make the identity token request will need to be
     * cached to make the request again in the case of two-factor authentication.
     */
    private var identityTokenAuthModel: IdentityTokenAuthModel? = null

    /**
     * The information necessary to resend the verification code email for two-factor login.
     */
    private var resendEmailRequestJson: ResendEmailRequestJson? = null

    /**
     * The password that needs to be checked against any organization policies before
     * the user can complete the login flow.
     */
    private var passwordToCheck: String? = null

    /**
     * A scope intended for use when simply collecting multiple flows in order to combine them. The
     * use of [Dispatchers.Unconfined] allows for this to happen synchronously whenever any of
     * these flows changes.
     */
    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    private val ioScope = CoroutineScope(dispatcherManager.io)

    override var twoFactorResponse: TwoFactorRequired? = null

    override val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    override val authStateFlow: StateFlow<AuthState> = authDiskSource
        .userStateFlow
        .map { userState ->
            userState
                ?.activeAccount
                ?.tokens
                ?.accessToken
                ?.let {
                    AuthState.Authenticated(accessToken = it)
                }
                ?: AuthState.Unauthenticated
        }
        .stateIn(
            scope = unconfinedScope,
            started = SharingStarted.Eagerly,
            initialValue = AuthState.Uninitialized,
        )

    override val userStateFlow: StateFlow<UserState?> = combine(
        authDiskSource.userStateFlow,
        authDiskSource.userOrganizationsListFlow,
        vaultRepository.vaultUnlockDataStateFlow,
        mutableHasPendingAccountAdditionStateFlow,
        mutableHasPendingAccountDeletionStateFlow,
    ) {
            userStateJson,
            userOrganizationsList,
            vaultState,
            hasPendingAccountAddition,
            _,
        ->
        userStateJson
            ?.toUserState(
                vaultState = vaultState,
                userOrganizationsList = userOrganizationsList,
                hasPendingAccountAddition = hasPendingAccountAddition,
                isBiometricsEnabledProvider = ::isBiometricsEnabled,
                vaultUnlockTypeProvider = ::getVaultUnlockType,
            )
    }
        .filter {
            // If there is a pending account deletion, continue showing
            // the original UserState until it is confirmed.
            !mutableHasPendingAccountDeletionStateFlow.value
        }
        .stateIn(
            scope = unconfinedScope,
            started = SharingStarted.Eagerly,
            initialValue = authDiskSource
                .userState
                ?.toUserState(
                    vaultState = vaultRepository.vaultUnlockDataStateFlow.value,
                    userOrganizationsList = authDiskSource.userOrganizationsList,
                    hasPendingAccountAddition = mutableHasPendingAccountAdditionStateFlow.value,
                    isBiometricsEnabledProvider = ::isBiometricsEnabled,
                    vaultUnlockTypeProvider = ::getVaultUnlockType,
                ),
        )

    private val captchaTokenChannel = Channel<CaptchaCallbackTokenResult>(capacity = Int.MAX_VALUE)
    override val captchaTokenResultFlow: Flow<CaptchaCallbackTokenResult> =
        captchaTokenChannel.receiveAsFlow()

    private val mutableSsoCallbackResultFlow =
        bufferedMutableSharedFlow<SsoCallbackResult>()
    override val ssoCallbackResultFlow: Flow<SsoCallbackResult> =
        mutableSsoCallbackResultFlow.asSharedFlow()

    override var rememberedEmailAddress: String? by authDiskSource::rememberedEmailAddress

    override var rememberedOrgIdentifier: String? by authDiskSource::rememberedOrgIdentifier

    override var hasPendingAccountAddition: Boolean
        by mutableHasPendingAccountAdditionStateFlow::value

    override val passwordPolicies: List<PolicyInformation.MasterPassword>
        get() = policyManager.getActivePolicies()

    override val passwordResetReason: ForcePasswordResetReason?
        get() = authDiskSource
            .userState
            ?.activeAccount
            ?.profile
            ?.forcePasswordResetReason

    init {
        pushManager
            .syncOrgKeysFlow
            .onEach {
                val userId = activeUserId ?: return@onEach
                refreshAccessTokenSynchronously(userId)
                vaultRepository.sync()
            }
            .launchIn(ioScope)

        pushManager
            .logoutFlow
            .onEach { logout() }
            .launchIn(unconfinedScope)

        // When the policies for the user have been set, complete the login process.
        policyManager
            .getActivePoliciesFlow(type = PolicyTypeJson.MASTER_PASSWORD)
            .onEach { policies ->
                val userId = activeUserId ?: return@onEach

                // If the password already has to be reset for some other reason, there's no
                // need to check the password policies.
                if (passwordResetReason != null) return@onEach

                // Otherwise check the user's password against the policies and set or
                // clear the force reset reason accordingly.
                storeUserResetPasswordReason(
                    userId = userId,
                    reason = ForcePasswordResetReason.WEAK_MASTER_PASSWORD_ON_LOGIN
                        .takeIf {
                            !passwordPassesPolicies(policies)
                        },
                )
            }
            .launchIn(unconfinedScope)
    }

    override fun clearPendingAccountDeletion() {
        mutableHasPendingAccountDeletionStateFlow.value = false
    }

    override suspend fun deleteAccount(password: String): DeleteAccountResult {
        val profile = authDiskSource.userState?.activeAccount?.profile
            ?: return DeleteAccountResult.Error
        mutableHasPendingAccountDeletionStateFlow.value = true
        return authSdkSource
            .hashPassword(
                email = profile.email,
                password = password,
                kdf = profile.toSdkParams(),
                purpose = HashPurpose.SERVER_AUTHORIZATION,
            )
            .flatMap { hashedPassword -> accountsService.deleteAccount(hashedPassword) }
            .onSuccess { logout() }
            .onFailure { clearPendingAccountDeletion() }
            .fold(
                onFailure = { DeleteAccountResult.Error },
                onSuccess = { DeleteAccountResult.Success },
            )
    }

    override suspend fun login(
        email: String,
        password: String,
        captchaToken: String?,
    ): LoginResult = accountsService
        .preLogin(email = email)
        .flatMap {
            authSdkSource.hashPassword(
                email = email,
                password = password,
                kdf = it.kdfParams.toSdkParams(),
                purpose = HashPurpose.SERVER_AUTHORIZATION,
            )
        }
        .map { passwordHash ->
            loginCommon(
                email = email,
                password = password,
                authModel = IdentityTokenAuthModel.MasterPassword(
                    username = email,
                    password = passwordHash,
                ),
                captchaToken = captchaToken,
            )
        }
        .fold(
            onFailure = { LoginResult.Error(errorMessage = null) },
            onSuccess = { it },
        )

    override suspend fun login(
        email: String,
        requestId: String,
        accessCode: String,
        asymmetricalKey: String,
        requestPrivateKey: String,
        masterPasswordHash: String?,
        captchaToken: String?,
    ): LoginResult =
        loginCommon(
            email = email,
            authModel = IdentityTokenAuthModel.AuthRequest(
                username = email,
                authRequestId = requestId,
                accessCode = accessCode,
            ),
            deviceData = DeviceDataModel(
                accessCode = accessCode,
                masterPasswordHash = masterPasswordHash,
                asymmetricalKey = asymmetricalKey,
                privateKey = requestPrivateKey,
            ),
            captchaToken = captchaToken,
        )

    override suspend fun login(
        email: String,
        password: String?,
        twoFactorData: TwoFactorDataModel,
        captchaToken: String?,
    ): LoginResult = identityTokenAuthModel?.let {
        loginCommon(
            email = email,
            password = password,
            authModel = it,
            twoFactorData = twoFactorData,
            captchaToken = captchaToken ?: twoFactorResponse?.captchaToken,
        )
    } ?: LoginResult.Error(errorMessage = null)

    override suspend fun login(
        email: String,
        ssoCode: String,
        ssoCodeVerifier: String,
        ssoRedirectUri: String,
        captchaToken: String?,
    ): LoginResult = loginCommon(
        email = email,
        authModel = IdentityTokenAuthModel.SingleSignOn(
            ssoCode = ssoCode,
            ssoCodeVerifier = ssoCodeVerifier,
            ssoRedirectUri = ssoRedirectUri,
        ),
        captchaToken = captchaToken,
    )

    /**
     * A helper function to extract the common logic of logging in through
     * any of the available methods.
     */
    @Suppress("LongMethod")
    private suspend fun loginCommon(
        email: String,
        password: String? = null,
        authModel: IdentityTokenAuthModel,
        twoFactorData: TwoFactorDataModel? = null,
        deviceData: DeviceDataModel? = null,
        captchaToken: String?,
    ): LoginResult = identityService
        .getToken(
            uniqueAppId = authDiskSource.uniqueAppId,
            email = email,
            authModel = authModel,
            twoFactorData = twoFactorData ?: getRememberedTwoFactorData(email),
            captchaToken = captchaToken,
        )
        .fold(
            onFailure = { LoginResult.Error(errorMessage = null) },
            onSuccess = { loginResponse ->
                when (loginResponse) {
                    is CaptchaRequired -> LoginResult.CaptchaRequired(loginResponse.captchaKey)

                    is TwoFactorRequired -> {
                        // Cache the data necessary for the remaining two-factor auth flow.
                        identityTokenAuthModel = authModel
                        twoFactorResponse = loginResponse
                        resendEmailRequestJson = ResendEmailRequestJson(
                            deviceIdentifier = authDiskSource.uniqueAppId,
                            email = email,
                            passwordHash = authModel.password,
                            ssoToken = loginResponse.ssoToken,
                        )

                        // If this error was received, it also means any cached two-factor
                        // token is invalid.
                        authDiskSource.storeTwoFactorToken(email, null)

                        LoginResult.TwoFactorRequired
                    }

                    is Success -> {
                        val userStateJson = loginResponse.toUserState(
                            previousUserState = authDiskSource.userState,
                            environmentUrlData = environmentRepository
                                .environment
                                .environmentUrlData,
                        )

                        // If the user just authenticated with a two-factor code and selected
                        // the option to remember it, then the API response will return a token
                        // that will be used in place of the two-factor code on the next login
                        // attempt.
                        loginResponse.twoFactorToken?.let {
                            authDiskSource.storeTwoFactorToken(
                                email = email,
                                twoFactorToken = it,
                            )
                        }

                        // Remove any cached data after successfully logging in.
                        identityTokenAuthModel = null
                        twoFactorResponse = null
                        resendEmailRequestJson = null

                        // Attempt to unlock the vault with password if possible.
                        password?.let {
                            vaultRepository.clearUnlockedData()
                            vaultRepository.unlockVault(
                                userId = userStateJson.activeUserId,
                                email = userStateJson.activeAccount.profile.email,
                                kdf = userStateJson.activeAccount.profile.toSdkParams(),
                                userKey = loginResponse.key,
                                privateKey = loginResponse.privateKey,
                                masterPassword = it,
                                // We can separately unlock the vault for organization data after
                                // receiving the sync response if this data is currently absent.
                                organizationKeys = null,
                            )

                            // Save the master password hash.
                            authSdkSource
                                .hashPassword(
                                    email = email,
                                    password = it,
                                    kdf = userStateJson.activeAccount.profile.toSdkParams(),
                                    purpose = HashPurpose.LOCAL_AUTHORIZATION,
                                )
                                .onSuccess { passwordHash ->
                                    authDiskSource.storeMasterPasswordHash(
                                        userId = userStateJson.activeUserId,
                                        passwordHash = passwordHash,
                                    )
                                }

                            // Cache the password to verify against any password policies
                            // after the sync completes.
                            passwordToCheck = it
                        }

                        // Attempt to unlock the vault with auth request if possible.
                        deviceData?.let {
                            vaultRepository.clearUnlockedData()
                            vaultRepository.unlockVault(
                                userId = userStateJson.activeUserId,
                                email = userStateJson.activeAccount.profile.email,
                                kdf = userStateJson.activeAccount.profile.toSdkParams(),
                                privateKey = loginResponse.privateKey,
                                initUserCryptoMethod = InitUserCryptoMethod.AuthRequest(
                                    requestPrivateKey = it.privateKey,
                                    protectedUserKey = it.asymmetricalKey,
                                ),
                                // We can separately unlock the vault for organization data after
                                // receiving the sync response if this data is currently absent.
                                organizationKeys = null,
                            )
                            authDiskSource.storeMasterPasswordHash(
                                userId = userStateJson.activeUserId,
                                passwordHash = it.masterPasswordHash,
                            )
                        }

                        authDiskSource.userState = userStateJson
                        authDiskSource.storeUserKey(
                            userId = userStateJson.activeUserId,
                            userKey = loginResponse.key,
                        )
                        authDiskSource.storePrivateKey(
                            userId = userStateJson.activeUserId,
                            privateKey = loginResponse.privateKey,
                        )
                        settingsRepository.setDefaultsIfNecessary(
                            userId = userStateJson.activeUserId,
                        )
                        vaultRepository.syncIfNecessary()
                        hasPendingAccountAddition = false
                        LoginResult.Success
                    }

                    is GetTokenResponseJson.Invalid -> {
                        LoginResult.Error(errorMessage = loginResponse.errorModel.errorMessage)
                    }
                }
            },
        )

    override fun refreshAccessTokenSynchronously(userId: String): Result<RefreshTokenResponseJson> {
        val refreshToken = authDiskSource
            .userState
            ?.accounts
            ?.get(userId)
            ?.tokens
            ?.refreshToken
            ?: return IllegalStateException("Must be logged in.").asFailure()
        return identityService
            .refreshTokenSynchronously(refreshToken)
            .onSuccess {
                // Update the existing UserState with updated token information
                authDiskSource.userState = it.toUserStateJson(
                    userId = userId,
                    previousUserState = requireNotNull(authDiskSource.userState),
                )
            }
    }

    override fun logout() {
        activeUserId?.let { userId -> logout(userId) }
    }

    override fun logout(userId: String) {
        val wasActiveUser = userId == activeUserId

        userLogoutManager.logout(userId = userId)

        // Clear the current vault data if the logged out user was the active one.
        if (wasActiveUser) vaultRepository.clearUnlockedData()
    }

    override suspend fun resendVerificationCodeEmail(): ResendEmailResult =
        resendEmailRequestJson?.let { jsonRequest ->
            accountsService.resendVerificationCodeEmail(body = jsonRequest).fold(
                onFailure = { ResendEmailResult.Error(message = it.message) },
                onSuccess = { ResendEmailResult.Success },
            )
        } ?: ResendEmailResult.Error(message = null)

    @Suppress("ReturnCount")
    override fun switchAccount(userId: String): SwitchAccountResult {
        val currentUserState = authDiskSource.userState
            ?: return SwitchAccountResult.NoChange
        val previousActiveUserId = currentUserState.activeUserId

        if (userId == previousActiveUserId) {
            // No switching to do but clear any pending account additions
            hasPendingAccountAddition = false
            return SwitchAccountResult.NoChange
        }

        if (userId !in currentUserState.accounts.keys) {
            // The requested user is not currently stored
            return SwitchAccountResult.NoChange
        }

        // Switch to the new user
        authDiskSource.userState = currentUserState.copy(activeUserId = userId)

        // Clear data for the previous user
        vaultRepository.clearUnlockedData()

        // Clear any pending account additions
        hasPendingAccountAddition = false

        return SwitchAccountResult.AccountSwitched
    }

    override fun updateLastActiveTime() {
        val userId = activeUserId ?: return
        authDiskSource.storeLastActiveTimeMillis(
            userId = userId,
            lastActiveTimeMillis = elapsedRealtimeMillisProvider(),
        )
    }

    @Suppress("ReturnCount", "LongMethod")
    override suspend fun register(
        email: String,
        masterPassword: String,
        masterPasswordHint: String?,
        captchaToken: String?,
        shouldCheckDataBreaches: Boolean,
    ): RegisterResult {
        if (shouldCheckDataBreaches) {
            haveIBeenPwnedService
                .hasPasswordBeenBreached(password = masterPassword)
                .onSuccess { foundDataBreaches ->
                    if (foundDataBreaches) {
                        return RegisterResult.DataBreachFound
                    }
                }
        }
        val kdf = Kdf.Pbkdf2(iterations = DEFAULT_PBKDF2_ITERATIONS.toUInt())
        return authSdkSource
            .makeRegisterKeys(
                email = email,
                password = masterPassword,
                kdf = kdf,
            )
            .flatMap { registerKeyResponse ->
                accountsService.register(
                    body = RegisterRequestJson(
                        email = email,
                        masterPasswordHash = registerKeyResponse.masterPasswordHash,
                        masterPasswordHint = masterPasswordHint,
                        captchaResponse = captchaToken,
                        key = registerKeyResponse.encryptedUserKey,
                        keys = RegisterRequestJson.Keys(
                            publicKey = registerKeyResponse.keys.public,
                            encryptedPrivateKey = registerKeyResponse.keys.private,
                        ),
                        kdfType = kdf.toKdfTypeJson(),
                        kdfIterations = kdf.iterations,
                    ),
                )
            }
            .fold(
                onSuccess = {
                    when (it) {
                        is RegisterResponseJson.CaptchaRequired -> {
                            it.validationErrors.captchaKeys.firstOrNull()?.let { key ->
                                RegisterResult.CaptchaRequired(captchaId = key)
                            } ?: RegisterResult.Error(errorMessage = null)
                        }

                        is RegisterResponseJson.Success -> {
                            RegisterResult.Success(captchaToken = it.captchaBypassToken)
                        }

                        is RegisterResponseJson.Invalid -> {
                            RegisterResult.Error(
                                errorMessage = it
                                    .validationErrors
                                    ?.values
                                    ?.firstOrNull()
                                    ?.firstOrNull()
                                    ?: it.message,
                            )
                        }

                        is RegisterResponseJson.Error -> {
                            RegisterResult.Error(it.message)
                        }
                    }
                },
                onFailure = {
                    RegisterResult.Error(errorMessage = null)
                },
            )
    }

    override suspend fun passwordHintRequest(email: String): PasswordHintResult {
        return accountsService.requestPasswordHint(email).fold(
            onSuccess = {
                when (it) {
                    is PasswordHintResponseJson.Error -> {
                        PasswordHintResult.Error(it.errorMessage)
                    }

                    PasswordHintResponseJson.Success -> PasswordHintResult.Success
                }
            },
            onFailure = { PasswordHintResult.Error(null) },
        )
    }

    @Suppress("ReturnCount")
    override suspend fun resetPassword(
        currentPassword: String?,
        newPassword: String,
        passwordHint: String?,
    ): ResetPasswordResult {
        val activeAccount = authDiskSource
            .userState
            ?.activeAccount
            ?: return ResetPasswordResult.Error
        val currentPasswordHash = currentPassword?.let {
            authSdkSource
                .hashPassword(
                    email = activeAccount.profile.email,
                    password = it,
                    kdf = activeAccount.profile.toSdkParams(),
                    purpose = HashPurpose.SERVER_AUTHORIZATION,
                )
                .fold(
                    onFailure = { return ResetPasswordResult.Error },
                    onSuccess = { it },
                )
        }
        return vaultSdkSource
            .updatePassword(
                userId = activeAccount.profile.userId,
                newPassword = newPassword,
            )
            .flatMap { updatePasswordResponse ->
                accountsService
                    .resetPassword(
                        body = ResetPasswordRequestJson(
                            currentPasswordHash = currentPasswordHash,
                            newPasswordHash = updatePasswordResponse.passwordHash,
                            passwordHint = passwordHint,
                            key = updatePasswordResponse.newKey,
                        ),
                    )
            }
            .fold(
                onSuccess = {
                    // Clear the password reset reason, since it's no longer relevant.
                    storeUserResetPasswordReason(
                        userId = activeAccount.profile.userId,
                        reason = null,
                    )

                    // Update the saved master password hash.
                    authSdkSource
                        .hashPassword(
                            email = activeAccount.profile.email,
                            password = newPassword,
                            kdf = activeAccount.profile.toSdkParams(),
                            purpose = HashPurpose.LOCAL_AUTHORIZATION,
                        )
                        .onSuccess { passwordHash ->
                            authDiskSource.storeMasterPasswordHash(
                                userId = activeAccount.profile.userId,
                                passwordHash = passwordHash,
                            )
                        }

                    // Return the success.
                    ResetPasswordResult.Success
                },
                onFailure = { ResetPasswordResult.Error },
            )
    }

    override fun setCaptchaCallbackTokenResult(tokenResult: CaptchaCallbackTokenResult) {
        captchaTokenChannel.trySend(tokenResult)
    }

    override suspend fun getOrganizationDomainSsoDetails(
        email: String,
    ): OrganizationDomainSsoDetailsResult = organizationService
        .getOrganizationDomainSsoDetails(
            email = email,
        )
        .fold(
            onSuccess = {
                OrganizationDomainSsoDetailsResult.Success(
                    isSsoAvailable = it.isSsoAvailable,
                    organizationIdentifier = it.organizationIdentifier,
                )
            },
            onFailure = { OrganizationDomainSsoDetailsResult.Failure },
        )

    override suspend fun prevalidateSso(
        organizationIdentifier: String,
    ): PrevalidateSsoResult = identityService
        .prevalidateSso(
            organizationIdentifier = organizationIdentifier,
        )
        .fold(
            onSuccess = {
                if (it.token.isNullOrBlank()) {
                    PrevalidateSsoResult.Failure
                } else {
                    PrevalidateSsoResult.Success(it.token)
                }
            },
            onFailure = { PrevalidateSsoResult.Failure },
        )

    override fun setSsoCallbackResult(result: SsoCallbackResult) {
        mutableSsoCallbackResultFlow.tryEmit(result)
    }

    @Suppress("LongMethod")
    override fun createAuthRequestWithUpdates(
        email: String,
    ): Flow<CreateAuthRequestResult> = flow {
        val initialResult = createNewAuthRequest(email)
            .getOrNull()
            ?: run {
                emit(CreateAuthRequestResult.Error)
                return@flow
            }
        val authRequestResponse = initialResult.authRequestResponse
        var authRequest = initialResult.authRequest
        emit(CreateAuthRequestResult.Update(authRequest))

        var isComplete = false
        while (currentCoroutineContext().isActive && !isComplete) {
            delay(timeMillis = PASSWORDLESS_NOTIFICATION_RETRY_INTERVAL_MILLIS)
            newAuthRequestService
                .getAuthRequestUpdate(
                    requestId = authRequest.id,
                    accessCode = authRequestResponse.accessCode,
                )
                .map { request ->
                    AuthRequest(
                        id = request.id,
                        publicKey = request.publicKey,
                        platform = request.platform,
                        ipAddress = request.ipAddress,
                        key = request.key,
                        masterPasswordHash = request.masterPasswordHash,
                        creationDate = request.creationDate,
                        responseDate = request.responseDate,
                        requestApproved = request.requestApproved ?: false,
                        originUrl = request.originUrl,
                        fingerprint = authRequest.fingerprint,
                    )
                }
                .fold(
                    onFailure = { emit(CreateAuthRequestResult.Error) },
                    onSuccess = { updateAuthRequest ->
                        when {
                            updateAuthRequest.requestApproved -> {
                                isComplete = true
                                emit(
                                    CreateAuthRequestResult.Success(
                                        authRequest = updateAuthRequest,
                                        authRequestResponse = authRequestResponse,
                                    ),
                                )
                            }

                            !updateAuthRequest.requestApproved &&
                                updateAuthRequest.responseDate != null -> {
                                isComplete = true
                                emit(CreateAuthRequestResult.Declined)
                            }

                            updateAuthRequest
                                .creationDate
                                .toInstant()
                                .plusMillis(PASSWORDLESS_NOTIFICATION_TIMEOUT_MILLIS)
                                .isBefore(clock.instant()) -> {
                                isComplete = true
                                emit(CreateAuthRequestResult.Expired)
                            }

                            else -> {
                                authRequest = updateAuthRequest
                                emit(CreateAuthRequestResult.Update(authRequest))
                            }
                        }
                    },
                )
        }
    }

    override suspend fun getAuthRequest(
        fingerprint: String,
    ): AuthRequestResult =
        when (val authRequestsResult = getAuthRequests()) {
            AuthRequestsResult.Error -> AuthRequestResult.Error
            is AuthRequestsResult.Success -> {
                val request = authRequestsResult.authRequests
                    .firstOrNull { it.fingerprint == fingerprint }

                request
                    ?.let { AuthRequestResult.Success(it) }
                    ?: AuthRequestResult.Error
            }
        }

    override suspend fun getAuthRequests(): AuthRequestsResult =
        authRequestsService
            .getAuthRequests()
            .fold(
                onFailure = { AuthRequestsResult.Error },
                onSuccess = { response ->
                    AuthRequestsResult.Success(
                        authRequests = response.authRequests.mapNotNull { request ->
                            when (val result = getFingerprintPhrase(request.publicKey)) {
                                is UserFingerprintResult.Error -> null
                                is UserFingerprintResult.Success -> AuthRequest(
                                    id = request.id,
                                    publicKey = request.publicKey,
                                    platform = request.platform,
                                    ipAddress = request.ipAddress,
                                    key = request.key,
                                    masterPasswordHash = request.masterPasswordHash,
                                    creationDate = request.creationDate,
                                    responseDate = request.responseDate,
                                    requestApproved = request.requestApproved ?: false,
                                    originUrl = request.originUrl,
                                    fingerprint = result.fingerprint,
                                )
                            }
                        },
                    )
                },
            )

    override suspend fun updateAuthRequest(
        requestId: String,
        masterPasswordHash: String?,
        publicKey: String,
        isApproved: Boolean,
    ): AuthRequestResult {
        val userId = activeUserId ?: return AuthRequestResult.Error
        return vaultSdkSource
            .getAuthRequestKey(
                publicKey = publicKey,
                userId = userId,
            )
            .flatMap {
                authRequestsService
                    .updateAuthRequest(
                        requestId = requestId,
                        key = it,
                        deviceId = authDiskSource.uniqueAppId,
                        masterPasswordHash = masterPasswordHash,
                        isApproved = isApproved,
                    )
            }
            .map { request ->
                AuthRequestResult.Success(
                    authRequest = AuthRequest(
                        id = request.id,
                        publicKey = request.publicKey,
                        platform = request.platform,
                        ipAddress = request.ipAddress,
                        key = request.key,
                        masterPasswordHash = request.masterPasswordHash,
                        creationDate = request.creationDate,
                        responseDate = request.responseDate,
                        requestApproved = request.requestApproved ?: false,
                        originUrl = request.originUrl,
                        fingerprint = "",
                    ),
                )
            }
            .fold(
                onFailure = { AuthRequestResult.Error },
                onSuccess = { it },
            )
    }

    override suspend fun getIsKnownDevice(emailAddress: String): KnownDeviceResult =
        devicesService
            .getIsKnownDevice(
                emailAddress = emailAddress,
                deviceId = authDiskSource.uniqueAppId,
            )
            .fold(
                onFailure = { KnownDeviceResult.Error },
                onSuccess = { KnownDeviceResult.Success(it) },
            )

    override suspend fun getPasswordBreachCount(password: String): BreachCountResult =
        haveIBeenPwnedService
            .getPasswordBreachCount(password)
            .fold(
                onFailure = { BreachCountResult.Error },
                onSuccess = { BreachCountResult.Success(it) },
            )

    override suspend fun getPasswordStrength(
        email: String,
        password: String,
    ): PasswordStrengthResult =
        authSdkSource
            .passwordStrength(
                email = email,
                password = password,
            )
            .fold(
                onSuccess = {
                    PasswordStrengthResult.Success(
                        passwordStrength = it,
                    )
                },
                onFailure = {
                    PasswordStrengthResult.Error
                },
            )

    @Suppress("ReturnCount")
    override suspend fun validatePassword(password: String): ValidatePasswordResult {
        val userId = activeUserId ?: return ValidatePasswordResult.Error
        val masterPasswordHash = authDiskSource.getMasterPasswordHash(userId = userId)
            ?: return ValidatePasswordResult.Error
        return vaultSdkSource
            .validatePassword(
                userId = userId,
                password = password,
                passwordHash = masterPasswordHash,
            )
            .fold(
                onSuccess = {
                    ValidatePasswordResult.Success(isValid = it)
                },
                onFailure = {
                    ValidatePasswordResult.Error
                },
            )
    }

    @Suppress("CyclomaticComplexMethod", "ReturnCount")
    override suspend fun validatePasswordAgainstPolicies(
        password: String,
    ): Boolean = passwordPolicies
        .all { validatePasswordAgainstPolicy(password, it) }

    @Suppress("CyclomaticComplexMethod", "ReturnCount")
    private suspend fun validatePasswordAgainstPolicy(
        password: String,
        policy: PolicyInformation.MasterPassword,
    ): Boolean {
        // Check the password against all the enforced rules in the policy.
        policy.minLength?.let { minLength ->
            if (minLength > 0 && password.length < minLength) return false
        }
        policy.minComplexity?.let { minComplexity ->
            // If there was a problem checking the complexity of the password, ignore
            // the complexity checks and continue checking the other aspects of the policy.
            val profile = authDiskSource.userState?.activeAccount?.profile ?: return@let
            val passwordStrengthResult = getPasswordStrength(profile.email, password)
            val passwordStrength = (passwordStrengthResult as? PasswordStrengthResult.Success)
                ?.passwordStrength
                ?.toInt()
                ?: return@let
            if (minComplexity > 0 && passwordStrength < minComplexity) return false
        }
        policy.requireUpper?.let { requiresUpper ->
            if (requiresUpper && !password.any { it.isUpperCase() }) return false
        }
        policy.requireLower?.let { requiresLower ->
            if (requiresLower && !password.any { it.isLowerCase() }) return false
        }
        policy.requireNumbers?.let { requiresNumbers ->
            if (requiresNumbers && !password.any { it.isDigit() }) return false
        }
        policy.requireSpecial?.let { requiresSpecial ->
            if (requiresSpecial && !password.contains("^.*[!@#$%\\^&*].*$".toRegex())) return false
        }

        return true
    }

    /**
     * Return true if there are any [PolicyInformation.MasterPassword] policies that the user's
     * master password has failed to pass.
     */
    @Suppress("ReturnCount")
    private suspend fun passwordPassesPolicies(policies: List<SyncResponseJson.Policy>?): Boolean {
        // If the user is logging on without a password or if there are no policies,
        // the check should complete.
        val password = passwordToCheck ?: return true
        val policyList = policies ?: return true

        // If there are no master password policies that are enabled and should be
        // enforced on login, the check should complete.
        val passwordPolicies = policyList
            .mapNotNull { it.policyInformation as? PolicyInformation.MasterPassword }
            .filter { it.enforceOnLogin == true }

        // Check the password against all the policies.
        return passwordPolicies.all { policy ->
            validatePasswordAgainstPolicy(password, policy)
        }
    }

    private suspend fun getFingerprintPhrase(
        publicKey: String,
    ): UserFingerprintResult {
        val profile = authDiskSource.userState?.activeAccount?.profile
            ?: return UserFingerprintResult.Error

        return authSdkSource
            .getUserFingerprint(
                email = profile.email,
                publicKey = publicKey,
            )
            .fold(
                onFailure = { UserFingerprintResult.Error },
                onSuccess = { UserFingerprintResult.Success(it) },
            )
    }

    /**
     * Attempts to create a new auth request for the given email and returns a [NewAuthRequestData]
     * with the [AuthRequest] and [AuthRequestResponse].
     */
    private suspend fun createNewAuthRequest(
        email: String,
    ): Result<NewAuthRequestData> =
        authSdkSource
            .getNewAuthRequest(email)
            .flatMap { authRequestResponse ->
                newAuthRequestService
                    .createAuthRequest(
                        email = email,
                        publicKey = authRequestResponse.publicKey,
                        deviceId = authDiskSource.uniqueAppId,
                        accessCode = authRequestResponse.accessCode,
                        fingerprint = authRequestResponse.fingerprint,
                    )
                    .map { request ->
                        AuthRequest(
                            id = request.id,
                            publicKey = request.publicKey,
                            platform = request.platform,
                            ipAddress = request.ipAddress,
                            key = request.key,
                            masterPasswordHash = request.masterPasswordHash,
                            creationDate = request.creationDate,
                            responseDate = request.responseDate,
                            requestApproved = request.requestApproved ?: false,
                            originUrl = request.originUrl,
                            fingerprint = authRequestResponse.fingerprint,
                        )
                    }
                    .map { NewAuthRequestData(it, authRequestResponse) }
            }

    /**
     * Get the remembered two-factor token associated with the user's email, if applicable.
     */
    private fun getRememberedTwoFactorData(email: String): TwoFactorDataModel? =
        authDiskSource.getTwoFactorToken(email = email)?.let { twoFactorToken ->
            TwoFactorDataModel(
                code = twoFactorToken,
                method = TwoFactorAuthMethod.REMEMBER.value.toString(),
                remember = false,
            )
        }

    private fun isBiometricsEnabled(
        userId: String,
    ): Boolean = authDiskSource.getUserBiometricUnlockKey(userId = userId) != null

    private fun getVaultUnlockType(
        userId: String,
    ): VaultUnlockType =
        when {
            authDiskSource.getPinProtectedUserKey(userId = userId) != null -> {
                VaultUnlockType.PIN
            }

            else -> {
                VaultUnlockType.MASTER_PASSWORD
            }
        }

    /**
     * Update the saved state with the force password reset reason.
     */
    private fun storeUserResetPasswordReason(userId: String, reason: ForcePasswordResetReason?) {
        val accounts = authDiskSource
            .userState
            ?.accounts
            ?.toMutableMap()
            ?: return
        val account = accounts[userId] ?: return
        val updatedProfile = account
            .profile
            .copy(forcePasswordResetReason = reason)
        accounts[userId] = account.copy(profile = updatedProfile)
        authDiskSource.userState = authDiskSource
            .userState
            ?.copy(accounts = accounts)
    }
}

/**
 * Wrapper class for the [AuthRequest] and [AuthRequestResponse] data.
 */
private data class NewAuthRequestData(
    val authRequest: AuthRequest,
    val authRequestResponse: AuthRequestResponse,
)
