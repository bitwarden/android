package com.x8bit.bitwarden.data.auth.repository

import com.bitwarden.core.AuthRequestMethod
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.core.MasterPasswordUnlockData
import com.bitwarden.core.RegisterTdeKeyResponse
import com.bitwarden.core.WrappedAccountCryptographicState
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.data.manager.toast.ToastManager
import com.bitwarden.core.data.repository.error.MissingPropertyException
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.core.data.util.flatMap
import com.bitwarden.crypto.HashPurpose
import com.bitwarden.crypto.Kdf
import com.bitwarden.data.datasource.disk.ConfigDiskSource
import com.bitwarden.data.repository.util.toEnvironmentUrls
import com.bitwarden.data.repository.util.toEnvironmentUrlsOrDefault
import com.bitwarden.network.model.AccountKeysJson
import com.bitwarden.network.model.CreateAccountKeysResponseJson
import com.bitwarden.network.model.DeleteAccountResponseJson
import com.bitwarden.network.model.GetTokenResponseJson
import com.bitwarden.network.model.IdentityTokenAuthModel
import com.bitwarden.network.model.OrganizationAutoEnrollStatusResponseJson
import com.bitwarden.network.model.OrganizationKeysResponseJson
import com.bitwarden.network.model.OrganizationStatusType
import com.bitwarden.network.model.OrganizationType
import com.bitwarden.network.model.PasswordHintResponseJson
import com.bitwarden.network.model.PrevalidateSsoResponseJson
import com.bitwarden.network.model.RefreshTokenResponseJson
import com.bitwarden.network.model.RegisterFinishRequestJson
import com.bitwarden.network.model.RegisterResponseJson
import com.bitwarden.network.model.ResendEmailRequestJson
import com.bitwarden.network.model.ResendNewDeviceOtpRequestJson
import com.bitwarden.network.model.ResetPasswordRequestJson
import com.bitwarden.network.model.SendVerificationEmailRequestJson
import com.bitwarden.network.model.SendVerificationEmailResponseJson
import com.bitwarden.network.model.SetPasswordRequestJson
import com.bitwarden.network.model.TrustedDeviceUserDecryptionOptionsJson
import com.bitwarden.network.model.TwoFactorAuthMethod
import com.bitwarden.network.model.TwoFactorDataModel
import com.bitwarden.network.model.VerificationCodeResponseJson
import com.bitwarden.network.model.VerificationOtpResponseJson
import com.bitwarden.network.model.VerifyEmailTokenRequestJson
import com.bitwarden.network.model.VerifyEmailTokenResponseJson
import com.bitwarden.network.service.AccountsService
import com.bitwarden.network.service.DevicesService
import com.bitwarden.network.service.HaveIBeenPwnedService
import com.bitwarden.network.service.IdentityService
import com.bitwarden.network.service.OrganizationService
import com.bitwarden.network.util.isSslHandShakeError
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.datasource.network.model.DeviceDataModel
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toKdfTypeJson
import com.x8bit.bitwarden.data.auth.manager.AuthRequestManager
import com.x8bit.bitwarden.data.auth.manager.KdfManager
import com.x8bit.bitwarden.data.auth.manager.KeyConnectorManager
import com.x8bit.bitwarden.data.auth.manager.TrustedDeviceManager
import com.x8bit.bitwarden.data.auth.manager.UserLogoutManager
import com.x8bit.bitwarden.data.auth.manager.UserStateManager
import com.x8bit.bitwarden.data.auth.manager.model.MigrateExistingUserToKeyConnectorResult
import com.x8bit.bitwarden.data.auth.repository.model.AuthState
import com.x8bit.bitwarden.data.auth.repository.model.BreachCountResult
import com.x8bit.bitwarden.data.auth.repository.model.DeleteAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.EmailTokenResult
import com.x8bit.bitwarden.data.auth.repository.model.GetDevicesResult
import com.x8bit.bitwarden.data.auth.repository.model.KnownDeviceResult
import com.x8bit.bitwarden.data.auth.repository.model.LeaveOrganizationResult
import com.x8bit.bitwarden.data.auth.repository.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.model.LogoutReason
import com.x8bit.bitwarden.data.auth.repository.model.NewSsoUserResult
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.PasswordHintResult
import com.x8bit.bitwarden.data.auth.repository.model.PasswordStrengthResult
import com.x8bit.bitwarden.data.auth.repository.model.PrevalidateSsoResult
import com.x8bit.bitwarden.data.auth.repository.model.RegisterResult
import com.x8bit.bitwarden.data.auth.repository.model.RemovePasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.RequestOtpResult
import com.x8bit.bitwarden.data.auth.repository.model.ResendEmailResult
import com.x8bit.bitwarden.data.auth.repository.model.ResetPasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.RevokeFromOrganizationResult
import com.x8bit.bitwarden.data.auth.repository.model.SendVerificationEmailResult
import com.x8bit.bitwarden.data.auth.repository.model.SetPasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.SwitchAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.UpdateKdfMinimumsResult
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePinResult
import com.x8bit.bitwarden.data.auth.repository.model.VerifiedOrganizationDomainSsoDetailsResult
import com.x8bit.bitwarden.data.auth.repository.model.VerifyOtpResult
import com.x8bit.bitwarden.data.auth.repository.model.toLoginErrorResult
import com.x8bit.bitwarden.data.auth.repository.util.CookieCallbackResult
import com.x8bit.bitwarden.data.auth.repository.util.DuoCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.SsoCallbackResult
import com.x8bit.bitwarden.data.auth.repository.util.WebAuthResult
import com.x8bit.bitwarden.data.auth.repository.util.activeUserIdChangesFlow
import com.x8bit.bitwarden.data.auth.repository.util.toAccountCryptographicState
import com.x8bit.bitwarden.data.auth.repository.util.toDeviceInfo
import com.x8bit.bitwarden.data.auth.repository.util.toOrganizations
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
import com.x8bit.bitwarden.data.auth.repository.util.toUserState
import com.x8bit.bitwarden.data.auth.repository.util.updateMasterPasswordUnlock
import com.x8bit.bitwarden.data.auth.util.KdfParamsConstants.DEFAULT_PBKDF2_ITERATIONS
import com.x8bit.bitwarden.data.auth.util.YubiKeyResult
import com.x8bit.bitwarden.data.auth.util.toSdkParams
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.LogsManager
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.manager.policy.PasswordPolicyManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.util.appLinksScheme
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockError
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.data.vault.repository.model.onVaultUnlockSuccess
import com.x8bit.bitwarden.data.vault.repository.util.toSdkMasterPasswordUnlock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import java.time.Clock
import javax.inject.Singleton

/**
 * Default implementation of [AuthRepository].
 */
@Suppress("LargeClass", "LongParameterList", "TooManyFunctions")
@Singleton
class AuthRepositoryImpl(
    private val clock: Clock,
    private val accountsService: AccountsService,
    private val devicesService: DevicesService,
    private val haveIBeenPwnedService: HaveIBeenPwnedService,
    private val identityService: IdentityService,
    private val organizationService: OrganizationService,
    private val authSdkSource: AuthSdkSource,
    private val vaultSdkSource: VaultSdkSource,
    private val authDiskSource: AuthDiskSource,
    private val settingsDiskSource: SettingsDiskSource,
    private val configDiskSource: ConfigDiskSource,
    private val environmentRepository: EnvironmentRepository,
    private val settingsRepository: SettingsRepository,
    private val vaultRepository: VaultRepository,
    private val authRequestManager: AuthRequestManager,
    private val biometricsEncryptionManager: BiometricsEncryptionManager,
    private val keyConnectorManager: KeyConnectorManager,
    private val trustedDeviceManager: TrustedDeviceManager,
    private val userLogoutManager: UserLogoutManager,
    private val userStateManager: UserStateManager,
    private val kdfManager: KdfManager,
    private val toastManager: ToastManager,
    private val featureFlagManager: FeatureFlagManager,
    logsManager: LogsManager,
    pushManager: PushManager,
    passwordPolicyManager: PasswordPolicyManager,
    dispatcherManager: DispatcherManager,
) : AuthRepository,
    AuthRequestManager by authRequestManager,
    BiometricsEncryptionManager by biometricsEncryptionManager,
    KdfManager by kdfManager,
    PasswordPolicyManager by passwordPolicyManager,
    UserStateManager by userStateManager {
    /**
     * A scope intended for use when simply collecting multiple flows in order to combine them. The
     * use of [Dispatchers.Unconfined] allows for this to happen synchronously whenever any of
     * these flows changes.
     */
    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    /**
     * A scope intended for use when operating asynchronously.
     */
    private val ioScope = CoroutineScope(dispatcherManager.io)

    /**
     * The auth information to make the identity token request will need to be
     * cached to make the request again in the case of two-factor authentication.
     */
    private var identityTokenAuthModel: IdentityTokenAuthModel? = null

    /**
     * The device auth information to unlock the vault when logging in with device in the case
     * of two-factor authentication.
     */
    private var twoFactorDeviceData: DeviceDataModel? = null

    /**
     * The information necessary to resend the verification code email for two-factor login.
     */
    private var resendEmailRequestJson: ResendEmailRequestJson? = null

    /**
     * The information necessary to resend the verification code email for new devices.
     */
    private var resendNewDeviceOtpRequestJson: ResendNewDeviceOtpRequestJson? = null

    private var organizationIdentifier: String? = null

    private var keyConnectorResponse: GetTokenResponseJson.Success? = null

    override var twoFactorResponse: GetTokenResponseJson.TwoFactorRequired? = null

    override val ssoOrganizationIdentifier: String? get() = organizationIdentifier
    override val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    @OptIn(ExperimentalCoroutinesApi::class)
    override val authStateFlow: StateFlow<AuthState> = authDiskSource
        .activeUserIdChangesFlow
        .flatMapLatest { activeUserId ->
            activeUserId
                ?.let { userId ->
                    authDiskSource
                        .getAccountTokensFlow(userId)
                        .map { accountTokens ->
                            accountTokens
                                ?.accessToken
                                ?.let { AuthState.Authenticated(it) }
                                ?: AuthState.Unauthenticated
                        }
                }
                ?: flowOf(AuthState.Unauthenticated)
        }
        .stateIn(
            scope = unconfinedScope,
            started = SharingStarted.Eagerly,
            initialValue = AuthState.Uninitialized,
        )

    private val duoTokenChannel = Channel<DuoCallbackTokenResult>(capacity = Int.MAX_VALUE)
    override val duoTokenResultFlow: Flow<DuoCallbackTokenResult> = duoTokenChannel.receiveAsFlow()

    private val yubiKeyResultChannel = Channel<YubiKeyResult>(capacity = Int.MAX_VALUE)
    override val yubiKeyResultFlow: Flow<YubiKeyResult> = yubiKeyResultChannel.receiveAsFlow()

    private val webAuthResultChannel = Channel<WebAuthResult>(capacity = Int.MAX_VALUE)
    override val webAuthResultFlow: Flow<WebAuthResult> = webAuthResultChannel.receiveAsFlow()

    private val mutableSsoCallbackResultFlow = bufferedMutableSharedFlow<SsoCallbackResult>()
    override val ssoCallbackResultFlow: Flow<SsoCallbackResult> =
        mutableSsoCallbackResultFlow.asSharedFlow()

    private val mutableCookieCallbackResultFlow = bufferedMutableSharedFlow<CookieCallbackResult>()
    override val cookieCallbackResultFlow: Flow<CookieCallbackResult> =
        mutableCookieCallbackResultFlow.asSharedFlow()

    override var rememberedEmailAddress: String? by authDiskSource::rememberedEmailAddress

    override var rememberedOrgIdentifier: String? by authDiskSource::rememberedOrgIdentifier

    override val tdeLoginComplete: Boolean?
        get() = activeUserId?.let { authDiskSource.getIsTdeLoginComplete(userId = it) }

    override var shouldTrustDevice: Boolean
        get() = activeUserId?.let { authDiskSource.getShouldTrustDevice(userId = it) } == true
        set(value) {
            activeUserId?.let {
                authDiskSource.storeShouldTrustDevice(userId = it, shouldTrustDevice = value)
            }
        }

    override val organizations: List<Organization>
        get() = activeUserId
            ?.let { authDiskSource.getOrganizations(it) }
            ?.filter { it.status == OrganizationStatusType.CONFIRMED }
            .orEmpty()
            .toOrganizations()

    override val showWelcomeCarousel: Boolean
        get() = !settingsRepository.hasUserLoggedInOrCreatedAccount

    init {
        combine(
            userStateManager.hasPendingAccountAdditionStateFlow,
            authDiskSource.userStateFlow,
            environmentRepository.environmentStateFlow,
        ) { hasPendingAddition, userState, environment ->
            logsManager.setUserData(
                userId = userState?.activeUserId.takeUnless { hasPendingAddition },
                environmentType = userState
                    ?.activeAccount
                    ?.settings
                    ?.environmentUrlData
                    ?.toEnvironmentUrls()
                    ?.type
                    .takeUnless { hasPendingAddition }
                    ?: environment.type,
            )
        }
            .launchIn(unconfinedScope)
        pushManager
            .syncOrgKeysFlow
            .onEach { userId ->
                // This will force the next authenticated request to refresh the auth token.
                authDiskSource.storeAccountTokens(
                    userId = userId,
                    accountTokens = authDiskSource
                        .getAccountTokens(userId = userId)
                        ?.copy(expiresAtSec = 0L),
                )
                if (userId == activeUserId) {
                    // We just sync now to get the latest data
                    vaultRepository.sync(forced = true)
                } else {
                    // We clear the last sync time to ensure we sync when we become the active user
                    settingsDiskSource.storeLastSyncTime(userId = userId, lastSyncTime = null)
                }
            }
            // This requires the ioScope to ensure that refreshAccessTokenSynchronously
            // happens on a background thread
            .launchIn(ioScope)

        pushManager
            .logoutFlow
            .onEach { logout(userId = it.userId, reason = LogoutReason.Notification) }
            .launchIn(unconfinedScope)
    }

    override suspend fun deleteAccountWithMasterPassword(
        masterPassword: String,
    ): DeleteAccountResult {
        val profile = authDiskSource.userState?.activeAccount?.profile
            ?: return DeleteAccountResult.Error(message = null, error = NoActiveUserException())
        userStateManager.hasPendingAccountDeletion = true
        return authSdkSource
            .hashPassword(
                email = profile.email,
                password = masterPassword,
                kdf = profile.toSdkParams(),
                purpose = HashPurpose.SERVER_AUTHORIZATION,
            )
            .flatMap { hashedPassword ->
                accountsService.deleteAccount(
                    masterPasswordHash = hashedPassword,
                    oneTimePassword = null,
                )
            }
            .finalizeDeleteAccount()
    }

    override suspend fun deleteAccountWithOneTimePassword(
        oneTimePassword: String,
    ): DeleteAccountResult {
        userStateManager.hasPendingAccountDeletion = true
        return accountsService
            .deleteAccount(
                masterPasswordHash = null,
                oneTimePassword = oneTimePassword,
            )
            .finalizeDeleteAccount()
    }

    private fun Result<DeleteAccountResponseJson>.finalizeDeleteAccount(): DeleteAccountResult =
        fold(
            onFailure = {
                userStateManager.hasPendingAccountDeletion = false
                DeleteAccountResult.Error(error = it, message = null)
            },
            onSuccess = { response ->
                when (response) {
                    is DeleteAccountResponseJson.Invalid -> {
                        userStateManager.hasPendingAccountDeletion = false
                        DeleteAccountResult.Error(message = response.message, error = null)
                    }

                    DeleteAccountResponseJson.Success -> {
                        logout(reason = LogoutReason.AccountDelete)
                        DeleteAccountResult.Success
                    }
                }
            },
        )

    override suspend fun createNewSsoUser(): NewSsoUserResult {
        val account = authDiskSource.userState?.activeAccount
            ?: return NewSsoUserResult.Failure(error = NoActiveUserException())
        val orgIdentifier = rememberedOrgIdentifier
            ?: return NewSsoUserResult.Failure(error = MissingPropertyException("OrgIdentifier"))
        return userStateManager.userStateTransaction {
            organizationService
                .getOrganizationAutoEnrollStatus(organizationIdentifier = orgIdentifier)
                .flatMap { orgAutoEnrollStatus ->
                    organizationService
                        .getOrganizationKeys(organizationId = orgAutoEnrollStatus.organizationId)
                        .flatMap { organizationKeys ->
                            if (featureFlagManager.getFeatureFlag(FlagKey.V2EncryptionTde)) {
                                registerUserForTdeV2(
                                    profile = account.profile,
                                    orgAutoEnrollStatus = orgAutoEnrollStatus,
                                    orgKeys = organizationKeys,
                                )
                            } else {
                                registerUserForTdeV1(
                                    profile = account.profile,
                                    orgAutoEnrollStatus = orgAutoEnrollStatus,
                                    orgKeys = organizationKeys,
                                )
                            }
                        }
                }
                .fold(
                    onSuccess = { NewSsoUserResult.Success },
                    onFailure = { NewSsoUserResult.Failure(error = it) },
                )
        }
    }

    private suspend fun registerUserForTdeV1(
        profile: AccountJson.Profile,
        orgAutoEnrollStatus: OrganizationAutoEnrollStatusResponseJson,
        orgKeys: OrganizationKeysResponseJson,
    ): Result<Pair<RegisterTdeKeyResponse, CreateAccountKeysResponseJson>> {
        val userId = profile.userId
        return authSdkSource
            .makeRegisterTdeKeysAndUnlockVault(
                userId = userId,
                email = profile.email,
                orgPublicKey = orgKeys.publicKey,
                rememberDevice = authDiskSource.getShouldTrustDevice(userId = userId) == true,
            )
            .flatMap { registerTdeKeyResponse ->
                accountsService
                    .createAccountKeys(
                        publicKey = registerTdeKeyResponse.publicKey,
                        encryptedPrivateKey = registerTdeKeyResponse.privateKey,
                    )
                    .map { createAccountKeysResponse ->
                        registerTdeKeyResponse to createAccountKeysResponse
                    }
            }
            .flatMap { (registerTdeKeyResponse, createAccountKeysResponse) ->
                organizationService
                    .organizationResetPasswordEnroll(
                        organizationId = orgAutoEnrollStatus.organizationId,
                        userId = userId,
                        passwordHash = null,
                        resetPasswordKey = registerTdeKeyResponse.adminReset,
                    )
                    .map { registerTdeKeyResponse to createAccountKeysResponse }
            }
            .onSuccess { (registerTdeKeyResponse, createAccountKeysResponse) ->
                authDiskSource.storeAccountCryptographicState(
                    userId = userId,
                    accountCryptographicState = createAccountKeysResponse
                        .accountKeys
                        .toAccountCryptographicState(
                            privateKey = registerTdeKeyResponse.privateKey,
                        ),
                )
                vaultRepository.syncVaultState(userId = userId)
                registerTdeKeyResponse.deviceKey?.let { response ->
                    trustedDeviceManager.trustThisDevice(
                        userId = userId,
                        trustDeviceResponse = response,
                    )
                }
            }
    }

    private suspend fun registerUserForTdeV2(
        profile: AccountJson.Profile,
        orgAutoEnrollStatus: OrganizationAutoEnrollStatusResponseJson,
        orgKeys: OrganizationKeysResponseJson,
    ): Result<VaultUnlockResult> {
        val userId = profile.userId
        val shouldTrustDevice = authDiskSource.getShouldTrustDevice(userId = userId) == true
        return authSdkSource
            .postKeysForTdeRegistration(
                userId = userId,
                organizationId = orgAutoEnrollStatus.organizationId,
                organizationPublicKey = orgKeys.publicKey,
                deviceIdentifier = authDiskSource.uniqueAppId,
                shouldTrustDevice = shouldTrustDevice,
            )
            .map { response ->
                // Clear the 'should trust device' flag, since the SDK trusted the device above.
                authDiskSource.storeShouldTrustDevice(userId = userId, shouldTrustDevice = null)
                this
                    .unlockVault(
                        accountCryptographicState = response.accountCryptographicState,
                        accountProfile = profile,
                        initUserCryptoMethod = InitUserCryptoMethod.DecryptedKey(
                            decryptedUserKey = response.userKey,
                        ),
                    )
                    .onVaultUnlockSuccess {
                        authDiskSource.storeAccountCryptographicState(
                            userId = userId,
                            accountCryptographicState = response.accountCryptographicState,
                        )
                        if (shouldTrustDevice) {
                            authDiskSource.storeDeviceKey(
                                userId = userId,
                                deviceKey = response.deviceKey,
                            )
                        }
                    }
            }
    }

    override suspend fun completeTdeLogin(
        requestPrivateKey: String,
        asymmetricalKey: String,
    ): LoginResult {
        val profile = authDiskSource.userState?.activeAccount?.profile
            ?: return LoginResult.Error(error = NoActiveUserException())
        val userId = profile.userId
        val accountCryptographicState = authDiskSource
            .getAccountCryptographicState(userId = userId)
            ?: return LoginResult.Error(MissingPropertyException("Account Cryptographic State"))
        checkForVaultUnlockError(
            onVaultUnlockError = { error ->
                return error.toLoginErrorResult()
            },
        ) {
            unlockVault(
                accountCryptographicState = accountCryptographicState,
                accountProfile = profile,
                initUserCryptoMethod = InitUserCryptoMethod.AuthRequest(
                    requestPrivateKey = requestPrivateKey,
                    method = AuthRequestMethod.UserKey(protectedUserKey = asymmetricalKey),
                ),
            )
        }
        settingsRepository.storeUserHasLoggedInValue(userId)
        vaultRepository.syncIfNecessary()
        return LoginResult.Success
    }

    override suspend fun login(
        email: String,
        password: String,
    ): LoginResult = identityService
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
            )
        }
        .fold(
            onFailure = { throwable ->
                when {
                    throwable.isSslHandShakeError() -> LoginResult.CertificateError
                    else -> LoginResult.Error(error = throwable)
                }
            },
            onSuccess = { it },
        )

    override suspend fun login(
        email: String,
        requestId: String,
        accessCode: String,
        asymmetricalKey: String,
        requestPrivateKey: String,
        masterPasswordHash: String?,
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
        )

    override suspend fun login(
        email: String,
        password: String?,
        twoFactorData: TwoFactorDataModel,
        orgIdentifier: String?,
    ): LoginResult = identityTokenAuthModel
        ?.let {
            loginCommon(
                email = email,
                password = password,
                authModel = it,
                twoFactorData = twoFactorData,
                deviceData = twoFactorDeviceData,
                orgIdentifier = orgIdentifier,
            )
        }
        ?: LoginResult.Error(error = MissingPropertyException("Identity Token Auth Model"))

    override suspend fun login(
        email: String,
        password: String?,
        newDeviceOtp: String,
        orgIdentifier: String?,
    ): LoginResult = identityTokenAuthModel
        ?.let {
            loginCommon(
                email = email,
                password = password,
                authModel = it,
                newDeviceOtp = newDeviceOtp,
                deviceData = twoFactorDeviceData,
                orgIdentifier = orgIdentifier,
            )
        }
        ?: LoginResult.Error(error = MissingPropertyException("Identity Token Auth Model"))

    override suspend fun continueKeyConnectorLogin(
        orgIdentifier: String,
        email: String,
    ): LoginResult {
        val response = keyConnectorResponse ?: return LoginResult.Error(
            error = MissingPropertyException("Key Connector Response"),
        )
        return handleLoginCommonSuccess(
            loginResponse = response,
            email = email,
            orgIdentifier = orgIdentifier,
            password = null,
            deviceData = null,
            userConfirmedKeyConnector = true,
        )
    }

    override fun cancelKeyConnectorLogin() {
        keyConnectorResponse = null
    }

    override suspend fun login(
        email: String,
        ssoCode: String,
        ssoCodeVerifier: String,
        ssoRedirectUri: String,
        organizationIdentifier: String,
    ): LoginResult = loginCommon(
        email = email,
        authModel = IdentityTokenAuthModel.SingleSignOn(
            ssoCode = ssoCode,
            ssoCodeVerifier = ssoCodeVerifier,
            ssoRedirectUri = ssoRedirectUri,
        ),
        orgIdentifier = organizationIdentifier,
    )

    override fun refreshAccessTokenSynchronously(
        userId: String,
    ): Result<String> {
        val refreshToken = authDiskSource
            .getAccountTokens(userId = userId)
            ?.refreshToken
            ?: return IllegalStateException("Must be logged in.").asFailure()
        return identityService
            .refreshTokenSynchronously(refreshToken)
            .flatMap { refreshTokenResponse ->
                // Check to make sure the user is still logged in after making the request
                authDiskSource
                    .userState
                    ?.accounts
                    ?.get(userId)
                    ?.let { refreshTokenResponse.asSuccess() }
                    ?: IllegalStateException("Must be logged in.").asFailure()
            }
            .flatMap { refreshTokenResponse ->
                when (refreshTokenResponse) {
                    is RefreshTokenResponseJson.Error -> {
                        if (refreshTokenResponse.isInvalidGrant) {
                            userLogoutManager.softLogout(
                                userId = userId,
                                reason = LogoutReason.InvalidGrant,
                            )
                        }
                        IllegalStateException(refreshTokenResponse.error).asFailure()
                    }

                    is RefreshTokenResponseJson.Forbidden -> {
                        userLogoutManager.softLogout(
                            userId = userId,
                            reason = LogoutReason.RefreshForbidden,
                        )
                        refreshTokenResponse.error.asFailure()
                    }

                    is RefreshTokenResponseJson.Unauthorized -> {
                        userLogoutManager.softLogout(
                            userId = userId,
                            reason = LogoutReason.RefreshUnauthorized,
                        )
                        refreshTokenResponse.error.asFailure()
                    }

                    is RefreshTokenResponseJson.Success -> {
                        // Store the new token information
                        authDiskSource.storeAccountTokens(
                            userId = userId,
                            accountTokens = AccountTokensJson(
                                accessToken = refreshTokenResponse.accessToken,
                                refreshToken = refreshTokenResponse.refreshToken,
                                expiresAtSec = clock.instant().epochSecond +
                                    refreshTokenResponse.expiresIn,
                            ),
                        )
                        refreshTokenResponse.accessToken.asSuccess()
                    }
                }
            }
    }

    override fun logout(reason: LogoutReason) {
        activeUserId?.let { userId -> logout(userId = userId, reason = reason) }
    }

    override fun logout(userId: String, reason: LogoutReason) {
        userLogoutManager.logout(userId = userId, reason = reason)
    }

    override suspend fun requestOneTimePasscode(): RequestOtpResult =
        accountsService.requestOneTimePasscode()
            .fold(
                onFailure = { RequestOtpResult.Error(message = it.message, error = it) },
                onSuccess = { RequestOtpResult.Success },
            )

    override suspend fun verifyOneTimePasscode(oneTimePasscode: String): VerifyOtpResult =
        accountsService
            .verifyOneTimePasscode(
                passcode = oneTimePasscode,
            )
            .fold(
                onFailure = { VerifyOtpResult.NotVerified(errorMessage = it.message, error = it) },
                onSuccess = { VerifyOtpResult.Verified },
            )

    override suspend fun resendVerificationCodeEmail(): ResendEmailResult =
        resendEmailRequestJson
            ?.let { jsonRequest ->
                accountsService.resendVerificationCodeEmail(body = jsonRequest).fold(
                    onFailure = { ResendEmailResult.Error(message = it.message, error = it) },
                    onSuccess = {
                        when (it) {
                            VerificationCodeResponseJson.Success -> ResendEmailResult.Success
                            is VerificationCodeResponseJson.Invalid -> {
                                ResendEmailResult.Error(
                                    message = it.firstValidationErrorMessage,
                                    error = null,
                                )
                            }
                        }
                    },
                )
            }
            ?: ResendEmailResult.Error(
                message = null,
                error = MissingPropertyException("Resend Email Request"),
            )

    override suspend fun resendNewDeviceOtp(): ResendEmailResult =
        resendNewDeviceOtpRequestJson
            ?.let { jsonRequest ->
                accountsService.resendNewDeviceOtp(body = jsonRequest).fold(
                    onFailure = { ResendEmailResult.Error(message = null, error = it) },
                    onSuccess = {
                        when (it) {
                            VerificationOtpResponseJson.Success -> ResendEmailResult.Success
                            is VerificationOtpResponseJson.Invalid -> {
                                ResendEmailResult.Error(
                                    message = it.firstValidationErrorMessage,
                                    error = null,
                                )
                            }
                        }
                    },
                )
            }
            ?: ResendEmailResult.Error(
                message = null,
                error = MissingPropertyException("Resend New Device OTP Request"),
            )

    override fun switchAccount(userId: String): SwitchAccountResult {
        val currentUserState = authDiskSource.userState ?: return SwitchAccountResult.NoChange
        val previousActiveUserId = currentUserState.activeUserId
        val updateEnvironment: () -> Unit = {
            environmentRepository.environment = currentUserState
                .activeAccount
                .settings
                .environmentUrlData
                .toEnvironmentUrlsOrDefault()
        }

        if (userId == previousActiveUserId) {
            // We need to make sure that the environment is set back to the correct spot.
            updateEnvironment()
            // No switching to do but clear any pending account additions
            userStateManager.hasPendingAccountAddition = false
            return SwitchAccountResult.NoChange
        }

        if (userId !in currentUserState.accounts.keys) {
            // We need to make sure that the environment is set back to the correct spot.
            updateEnvironment()
            // The requested user is not currently stored
            return SwitchAccountResult.NoChange
        }

        // Switch to the new user
        authDiskSource.userState = currentUserState.copy(activeUserId = userId)

        // Clear any pending account additions
        userStateManager.hasPendingAccountAddition = false

        return SwitchAccountResult.AccountSwitched
    }

    @Suppress("LongMethod")
    override suspend fun register(
        email: String,
        masterPassword: String,
        masterPasswordHint: String?,
        emailVerificationToken: String,
        shouldCheckDataBreaches: Boolean,
        isMasterPasswordStrong: Boolean,
    ): RegisterResult {
        if (shouldCheckDataBreaches) {
            haveIBeenPwnedService
                .hasPasswordBeenBreached(password = masterPassword)
                .onSuccess { foundDataBreaches ->
                    if (foundDataBreaches) {
                        return if (isMasterPasswordStrong) {
                            RegisterResult.DataBreachFound
                        } else {
                            RegisterResult.DataBreachAndWeakPassword
                        }
                    }
                }
        }
        if (!isMasterPasswordStrong) {
            return RegisterResult.WeakPassword
        }
        if (featureFlagManager.getFeatureFlag(key = FlagKey.V2EncryptionPassword)) {
            return authSdkSource
                .postKeysForUserPasswordRegistration(
                    email = email,
                    salt = email,
                    masterPassword = masterPassword,
                    masterPasswordHint = masterPasswordHint,
                    emailVerificationToken = emailVerificationToken,
                )
                .fold(
                    onSuccess = { RegisterResult.Success },
                    onFailure = { RegisterResult.Error(errorMessage = null, error = it) },
                )
        }

        val kdf = Kdf.Pbkdf2(iterations = DEFAULT_PBKDF2_ITERATIONS.toUInt())
        return authSdkSource
            .makeRegisterKeys(
                email = email,
                password = masterPassword,
                kdf = kdf,
            )
            .flatMap { registerKeyResponse ->
                identityService.registerFinish(
                    body = RegisterFinishRequestJson(
                        email = email,
                        masterPasswordHash = registerKeyResponse.masterPasswordHash,
                        masterPasswordHint = masterPasswordHint,
                        emailVerificationToken = emailVerificationToken,
                        userSymmetricKey = registerKeyResponse.encryptedUserKey,
                        userAsymmetricKeys = RegisterFinishRequestJson.Keys(
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
                        is RegisterResponseJson.Success -> {
                            settingsRepository.hasUserLoggedInOrCreatedAccount = true
                            RegisterResult.Success
                        }

                        is RegisterResponseJson.Invalid -> {
                            RegisterResult.Error(errorMessage = it.message, error = null)
                        }
                    }
                },
                onFailure = { RegisterResult.Error(errorMessage = null, error = it) },
            )
    }

    override suspend fun passwordHintRequest(email: String): PasswordHintResult {
        return accountsService.requestPasswordHint(email).fold(
            onSuccess = {
                when (it) {
                    is PasswordHintResponseJson.Error -> PasswordHintResult.Error(
                        message = it.errorMessage,
                        error = null,
                    )

                    PasswordHintResponseJson.Success -> PasswordHintResult.Success
                }
            },
            onFailure = { PasswordHintResult.Error(message = null, error = it) },
        )
    }

    override suspend fun removePassword(masterPassword: String): RemovePasswordResult {
        val profile = authDiskSource
            .userState
            ?.activeAccount
            ?.profile
            ?: return RemovePasswordResult.Error(error = NoActiveUserException())
        val userId = profile.userId
        val userKey = profile
            .userDecryptionOptions
            ?.masterPasswordUnlock
            ?.masterKeyWrappedUserKey
            ?: return RemovePasswordResult.Error(error = MissingPropertyException("User Key"))
        val keyConnectorUrl = organizations
            .find {
                it.isKeyConnectorEnabled &&
                    it.role != OrganizationType.OWNER &&
                    it.role != OrganizationType.ADMIN
            }
            ?.keyConnectorUrl
            ?: return RemovePasswordResult.Error(
                error = MissingPropertyException("Key Connector URL"),
            )
        return keyConnectorManager
            .migrateExistingUserToKeyConnector(
                userId = userId,
                url = keyConnectorUrl,
                userKeyEncrypted = userKey,
                email = profile.email,
                masterPassword = masterPassword,
                kdf = profile.toSdkParams(),
            )
            .map { migrateResult: MigrateExistingUserToKeyConnectorResult ->
                when (migrateResult) {
                    is MigrateExistingUserToKeyConnectorResult.Error -> {
                        RemovePasswordResult.Error(error = migrateResult.error)
                    }

                    MigrateExistingUserToKeyConnectorResult.Success -> {
                        authDiskSource.userState = authDiskSource
                            .userState
                            ?.updateMasterPasswordUnlock(
                                userId = userId,
                                masterPasswordUnlock = null,
                            )
                        vaultRepository.sync()
                        settingsRepository.setDefaultsIfNecessary(userId = userId)
                        RemovePasswordResult.Success
                    }

                    MigrateExistingUserToKeyConnectorResult.WrongPasswordError -> {
                        RemovePasswordResult.WrongPasswordError
                    }
                }
            }
            .getOrElse {
                RemovePasswordResult.Error(error = it)
            }
    }

    override suspend fun resetPassword(
        currentPassword: String?,
        newPassword: String,
        passwordHint: String?,
    ): ResetPasswordResult {
        val profile = authDiskSource.userState?.activeAccount?.profile
            ?: return ResetPasswordResult.Error(error = NoActiveUserException())
        val currentPasswordHash = currentPassword?.let { password ->
            authSdkSource
                .hashPassword(
                    email = profile.email,
                    password = password,
                    kdf = profile.toSdkParams(),
                    purpose = HashPurpose.SERVER_AUTHORIZATION,
                )
                .fold(
                    onFailure = { return ResetPasswordResult.Error(error = it) },
                    onSuccess = { it },
                )
        }
        val userId = profile.userId
        return vaultSdkSource
            .updatePassword(
                userId = userId,
                newPassword = newPassword,
            )
            .flatMap { response ->
                accountsService.resetPassword(
                    body = ResetPasswordRequestJson.V1(
                        currentPasswordHash = currentPasswordHash,
                        newPasswordHash = response.passwordHash,
                        passwordHint = passwordHint,
                        key = response.newKey,
                    ),
                )
            }
            .onSuccess {
                toastManager.show(BitwardenString.updated_master_password)
                // Log out the user after successful password reset. This clears all
                // user data, so there is no need to store any of the updated info.
                logout(reason = LogoutReason.PasswordReset, userId = userId)
            }
            .fold(
                onSuccess = { ResetPasswordResult.Success },
                onFailure = { ResetPasswordResult.Error(error = it) },
            )
    }

    override suspend fun setPassword(
        organizationIdentifier: String,
        password: String,
        passwordHint: String?,
    ): SetPasswordResult = userStateManager.userStateTransaction {
        val profile = authDiskSource.userState?.activeAccount?.profile
            ?: return@userStateTransaction SetPasswordResult.Error(error = NoActiveUserException())
        return@userStateTransaction when (profile.forcePasswordResetReason) {
            ForcePasswordResetReason.TDE_USER_WITHOUT_PASSWORD_HAS_PASSWORD_RESET_PERMISSION -> {
                setUpdatedPassword(
                    profile = profile,
                    organizationIdentifier = organizationIdentifier,
                    password = password,
                    passwordHint = passwordHint,
                )
            }

            ForcePasswordResetReason.ADMIN_FORCE_PASSWORD_RESET,
            ForcePasswordResetReason.WEAK_MASTER_PASSWORD_ON_LOGIN,
            null,
                -> {
                setPasswordForJit(
                    profile = profile,
                    organizationIdentifier = organizationIdentifier,
                    password = password,
                    passwordHint = passwordHint,
                )
            }
        }
    }

    private suspend fun setUpdatedPassword(
        profile: AccountJson.Profile,
        organizationIdentifier: String,
        password: String,
        passwordHint: String?,
    ): SetPasswordResult {
        val userId = profile.userId
        return vaultSdkSource
            .updatePassword(userId = userId, newPassword = password)
            .flatMap { response ->
                accountsService
                    .setPassword(
                        body = SetPasswordRequestJson.V1(
                            passwordHint = passwordHint,
                            organizationIdentifier = organizationIdentifier,
                            kdfIterations = profile.kdfIterations,
                            kdfMemory = profile.kdfMemory,
                            kdfParallelism = profile.kdfParallelism,
                            kdfType = profile.kdfType,
                            key = response.newKey,
                            passwordHash = response.passwordHash,
                            keys = null,
                        ),
                    )
                    .map { response }
            }
            .onSuccess { response ->
                authDiskSource.userState = authDiskSource.userState?.updateMasterPasswordUnlock(
                    userId = userId,
                    masterPasswordUnlock = MasterPasswordUnlockData(
                        kdf = profile.toSdkParams(),
                        masterKeyWrappedUserKey = response.newKey,
                        salt = profile.email,
                    ),
                )
                this.organizationIdentifier = null
            }
            .flatMap { response ->
                enrollUserInPasswordReset(
                    userId = userId,
                    organizationIdentifier = organizationIdentifier,
                    passwordHash = response.passwordHash,
                )
            }
            .fold(
                onFailure = { SetPasswordResult.Error(error = it) },
                onSuccess = { SetPasswordResult.Success },
            )
    }

    private suspend fun setPasswordForJit(
        profile: AccountJson.Profile,
        organizationIdentifier: String,
        password: String,
        passwordHint: String?,
    ): SetPasswordResult {
        if (!featureFlagManager.getFeatureFlag(FlagKey.V2EncryptionJitPassword)) {
            return setPasswordForJitV1(
                profile = profile,
                organizationIdentifier = organizationIdentifier,
                password = password,
                passwordHint = passwordHint,
            )
        }
        val userId = profile.userId
        return organizationService
            .getOrganizationAutoEnrollStatus(organizationIdentifier = organizationIdentifier)
            .flatMap { enrollStatus ->
                organizationService
                    .getOrganizationKeys(organizationId = enrollStatus.organizationId)
                    .map { orgKeys -> enrollStatus to orgKeys }
            }
            .flatMap { (enrollStatus, orgKeys) ->
                authSdkSource.postKeysForJitPasswordRegistration(
                    userId = userId,
                    organizationId = enrollStatus.organizationId,
                    organizationPublicKey = orgKeys.publicKey,
                    organizationSsoIdentifier = organizationIdentifier,
                    salt = profile.email,
                    masterPassword = password,
                    masterPasswordHint = passwordHint,
                    shouldResetPasswordEnroll = enrollStatus.isResetPasswordEnabled,
                )
            }
            .onSuccess { response ->
                authDiskSource.storeAccountCryptographicState(
                    userId = userId,
                    accountCryptographicState = response.accountCryptographicState,
                )
                authDiskSource.userState = authDiskSource.userState?.updateMasterPasswordUnlock(
                    userId = userId,
                    masterPasswordUnlock = response.masterPasswordUnlock,
                )
                this.organizationIdentifier = null
            }
            .flatMap { response ->
                // Logging in with the password instead of the decrypted userKey will store
                // the master password hash automatically.
                when (val result = vaultRepository.unlockVaultWithMasterPassword(password)) {
                    VaultUnlockResult.Success -> response.asSuccess()
                    is VaultUnlockError -> {
                        (result.error ?: IllegalStateException("Failed to unlock vault"))
                            .asFailure()
                    }
                }
            }
            .fold(
                onFailure = { SetPasswordResult.Error(error = it) },
                onSuccess = { SetPasswordResult.Success },
            )
    }

    @Suppress("LongMethod")
    private suspend fun setPasswordForJitV1(
        profile: AccountJson.Profile,
        organizationIdentifier: String,
        password: String,
        passwordHint: String?,
    ): SetPasswordResult {
        val userId = profile.userId
        return authSdkSource
            .makeRegisterKeys(
                email = profile.email,
                password = password,
                kdf = profile.toSdkParams(),
            )
            .flatMap { response ->
                accountsService
                    .setPassword(
                        body = SetPasswordRequestJson.V1(
                            passwordHash = response.masterPasswordHash,
                            passwordHint = passwordHint,
                            organizationIdentifier = organizationIdentifier,
                            kdfIterations = profile.kdfIterations,
                            kdfMemory = profile.kdfMemory,
                            kdfParallelism = profile.kdfParallelism,
                            kdfType = profile.kdfType,
                            key = response.encryptedUserKey,
                            keys = SetPasswordRequestJson.V1.Keys(
                                publicKey = response.keys.public,
                                encryptedPrivateKey = response.keys.private,
                            ),
                        ),
                    )
                    .onSuccess {
                        authDiskSource.storeAccountCryptographicState(
                            userId = userId,
                            accountCryptographicState = WrappedAccountCryptographicState.V1(
                                privateKey = response.keys.private,
                            ),
                        )
                        authDiskSource.userState = authDiskSource
                            .userState
                            ?.updateMasterPasswordUnlock(
                                userId = userId,
                                masterPasswordUnlock = MasterPasswordUnlockData(
                                    kdf = profile.toSdkParams(),
                                    masterKeyWrappedUserKey = response.encryptedUserKey,
                                    salt = profile.email,
                                ),
                            )
                        this.organizationIdentifier = null
                    }
                    .map { response }
            }
            .flatMap { response ->
                when (val result = vaultRepository.unlockVaultWithMasterPassword(password)) {
                    is VaultUnlockResult.Success -> {
                        enrollUserInPasswordReset(
                            userId = userId,
                            organizationIdentifier = organizationIdentifier,
                            passwordHash = response.masterPasswordHash,
                        )
                    }

                    is VaultUnlockError -> {
                        (result.error ?: IllegalStateException("Failed to unlock vault"))
                            .asFailure()
                    }
                }
            }
            .fold(
                onFailure = { SetPasswordResult.Error(error = it) },
                onSuccess = { SetPasswordResult.Success },
            )
    }

    override fun setDuoCallbackTokenResult(tokenResult: DuoCallbackTokenResult) {
        duoTokenChannel.trySend(tokenResult)
    }

    override fun setYubiKeyResult(yubiKeyResult: YubiKeyResult) {
        yubiKeyResultChannel.trySend(yubiKeyResult)
    }

    override fun setWebAuthResult(webAuthResult: WebAuthResult) {
        webAuthResultChannel.trySend(webAuthResult)
    }

    override suspend fun getVerifiedOrganizationDomainSsoDetails(
        email: String,
    ): VerifiedOrganizationDomainSsoDetailsResult = organizationService
        .getVerifiedOrganizationDomainSsoDetails(
            email = email,
        )
        .fold(
            onSuccess = {
                VerifiedOrganizationDomainSsoDetailsResult.Success(
                    verifiedOrganizationDomainSsoDetails = it.verifiedOrganizationDomainSsoDetails,
                )
            },
            onFailure = { VerifiedOrganizationDomainSsoDetailsResult.Failure(error = it) },
        )

    override suspend fun prevalidateSso(
        organizationIdentifier: String,
    ): PrevalidateSsoResult = identityService
        .prevalidateSso(
            organizationIdentifier = organizationIdentifier,
        )
        .fold(
            onSuccess = { response ->
                when (response) {
                    is PrevalidateSsoResponseJson.Error -> {
                        PrevalidateSsoResult.Failure(message = response.message, error = null)
                    }

                    is PrevalidateSsoResponseJson.Success -> {
                        response.token
                            ?.takeUnless { it.isBlank() }
                            ?.let { PrevalidateSsoResult.Success(token = it) }
                            ?: PrevalidateSsoResult.Failure(
                                error = MissingPropertyException("Token"),
                            )
                    }
                }
            },
            onFailure = { PrevalidateSsoResult.Failure(error = it) },
        )

    override fun setSsoCallbackResult(result: SsoCallbackResult) {
        mutableSsoCallbackResultFlow.tryEmit(result)
    }

    override fun setCookieCallbackResult(result: CookieCallbackResult) {
        mutableCookieCallbackResultFlow.tryEmit(result)
    }

    override suspend fun getDevices(): GetDevicesResult =
        devicesService
            .getDevices()
            .fold(
                onFailure = { GetDevicesResult.Error },
                onSuccess = { response ->
                    GetDevicesResult.Success(
                        devices = response.devices.map { json ->
                            json.toDeviceInfo(currentDeviceIdentifier = authDiskSource.uniqueAppId)
                        },
                    )
                },
            )

    override suspend fun getIsKnownDevice(emailAddress: String): KnownDeviceResult =
        devicesService
            .getIsKnownDevice(
                emailAddress = emailAddress,
                deviceId = authDiskSource.uniqueAppId,
            )
            .fold(
                onFailure = { KnownDeviceResult.Error(error = it) },
                onSuccess = { KnownDeviceResult.Success(isKnownDevice = it) },
            )

    override suspend fun getPasswordBreachCount(password: String): BreachCountResult =
        haveIBeenPwnedService
            .getPasswordBreachCount(password)
            .fold(
                onFailure = { BreachCountResult.Error(error = it) },
                onSuccess = { BreachCountResult.Success(it) },
            )

    override suspend fun getPasswordStrength(
        email: String?,
        password: String,
    ): PasswordStrengthResult =
        authSdkSource
            .passwordStrength(
                email = email
                    ?: userStateFlow
                        .value
                        ?.activeAccount
                        ?.email
                        .orEmpty(),
                password = password,
            )
            .fold(
                onSuccess = { PasswordStrengthResult.Success(passwordStrength = it) },
                onFailure = { PasswordStrengthResult.Error(error = it) },
            )

    override suspend fun validatePassword(password: String): ValidatePasswordResult {
        val profile = authDiskSource
            .userState
            ?.activeAccount
            ?.profile
            ?: return ValidatePasswordResult.Error(error = NoActiveUserException())
        val userId = profile.userId
        return authDiskSource
            .getMasterPasswordHash(userId = userId)
            ?.let { masterPasswordHash ->
                vaultSdkSource
                    .validatePassword(
                        userId = userId,
                        password = password,
                        passwordHash = masterPasswordHash,
                    )
                    .fold(
                        onSuccess = { ValidatePasswordResult.Success(isValid = it) },
                        onFailure = { ValidatePasswordResult.Error(error = it) },
                    )
            }
            ?: run {
                val encryptedKey = profile
                    .userDecryptionOptions
                    ?.masterPasswordUnlock
                    ?.masterKeyWrappedUserKey
                    ?: return ValidatePasswordResult.Error(MissingPropertyException("UserKey"))
                vaultSdkSource
                    .validatePasswordUserKey(
                        userId = userId,
                        password = password,
                        encryptedUserKey = encryptedKey,
                    )
                    .onSuccess { masterPasswordHash ->
                        authDiskSource.storeMasterPasswordHash(
                            userId = userId,
                            passwordHash = masterPasswordHash,
                        )
                    }
                    .fold(
                        onSuccess = { ValidatePasswordResult.Success(isValid = true) },
                        onFailure = {
                            // We currently assume that all errors are caused by the user entering
                            // an invalid password, this is not necessarily the case, but we have
                            // no way to differentiate between the different errors.
                            ValidatePasswordResult.Success(isValid = false)
                        },
                    )
            }
    }

    override suspend fun validatePinUserKey(pin: String): ValidatePinResult {
        val activeAccount = authDiskSource
            .userState
            ?.activeAccount
            ?.profile
            ?: return ValidatePinResult.Error(error = NoActiveUserException())
        val pinProtectedUserKeyEnvelope = authDiskSource
            .getPinProtectedUserKeyEnvelope(userId = activeAccount.userId)
            ?: return ValidatePinResult.Error(
                error = MissingPropertyException("Pin Protected User Key Envelope"),
            )
        return vaultSdkSource
            .validatePinUserKey(
                userId = activeAccount.userId,
                pin = pin,
                pinProtectedUserKeyEnvelope = pinProtectedUserKeyEnvelope,
            )
            .fold(
                onSuccess = { ValidatePinResult.Success(isValid = it) },
                onFailure = { ValidatePinResult.Error(error = it) },
            )
    }

    override suspend fun sendVerificationEmail(
        email: String,
        name: String,
        receiveMarketingEmails: Boolean,
    ): SendVerificationEmailResult =
        identityService
            .sendVerificationEmail(
                SendVerificationEmailRequestJson(
                    email = email,
                    name = name.takeUnless { it.isBlank() },
                    receiveMarketingEmails = receiveMarketingEmails,
                ),
            )
            .fold(
                onSuccess = {
                    when (it) {
                        is SendVerificationEmailResponseJson.Invalid -> {
                            SendVerificationEmailResult.Error(
                                errorMessage = it.message,
                                error = null,
                            )
                        }

                        is SendVerificationEmailResponseJson.Success -> {
                            SendVerificationEmailResult.Success(it.emailVerificationToken)
                        }
                    }
                },
                onFailure = { SendVerificationEmailResult.Error(errorMessage = null, error = it) },
            )

    override suspend fun validateEmailToken(email: String, token: String): EmailTokenResult {
        return identityService
            .verifyEmailRegistrationToken(
                body = VerifyEmailTokenRequestJson(
                    email = email,
                    token = token,
                ),
            )
            .fold(
                onSuccess = {
                    when (it) {
                        VerifyEmailTokenResponseJson.Valid -> EmailTokenResult.Success
                        is VerifyEmailTokenResponseJson.Invalid -> {
                            EmailTokenResult.Error(message = it.message, error = null)
                        }

                        VerifyEmailTokenResponseJson.TokenExpired -> EmailTokenResult.Expired
                    }
                },
                onFailure = { EmailTokenResult.Error(message = null, error = it) },
            )
    }

    override fun setOnboardingStatus(status: OnboardingStatus) {
        activeUserId?.let { userId ->
            authDiskSource.storeOnboardingStatus(
                userId = userId,
                onboardingStatus = status,
            )
        }
    }

    override suspend fun leaveOrganization(organizationId: String): LeaveOrganizationResult =
        organizationService.leaveOrganization(organizationId).fold(
            onSuccess = { LeaveOrganizationResult.Success },
            onFailure = { LeaveOrganizationResult.Error(error = it) },
        )

    override suspend fun revokeFromOrganization(
        organizationId: String,
    ): RevokeFromOrganizationResult =
        organizationService.revokeFromOrganization(organizationId).fold(
            onSuccess = { RevokeFromOrganizationResult.Success },
            onFailure = { RevokeFromOrganizationResult.Error(error = it) },
        )

    /**
     * Enrolls the active user in password reset if their organization requires it.
     */
    private suspend fun enrollUserInPasswordReset(
        userId: String,
        organizationIdentifier: String,
        passwordHash: String,
    ): Result<Unit> =
        organizationService
            .getOrganizationAutoEnrollStatus(organizationIdentifier = organizationIdentifier)
            .flatMap { statusResponse ->
                if (statusResponse.isResetPasswordEnabled) {
                    organizationService
                        .getOrganizationKeys(statusResponse.organizationId)
                        .flatMap { keys ->
                            vaultSdkSource.getResetPasswordKey(
                                orgPublicKey = keys.publicKey,
                                userId = userId,
                            )
                        }
                        .flatMap { key ->
                            organizationService.organizationResetPasswordEnroll(
                                organizationId = statusResponse.organizationId,
                                passwordHash = passwordHash,
                                resetPasswordKey = key,
                                userId = userId,
                            )
                        }
                } else {
                    Unit.asSuccess()
                }
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
    //region LoginCommon

    /**
     * A helper function to extract the common logic of logging in through
     * any of the available methods.
     */
    @Suppress("LongMethod", "MaxLineLength")
    private suspend fun loginCommon(
        email: String,
        password: String? = null,
        authModel: IdentityTokenAuthModel,
        twoFactorData: TwoFactorDataModel? = null,
        deviceData: DeviceDataModel? = null,
        orgIdentifier: String? = null,
        newDeviceOtp: String? = null,
    ): LoginResult = identityService
        .getToken(
            uniqueAppId = authDiskSource.uniqueAppId,
            deeplinkScheme = environmentRepository.environment.appLinksScheme,
            email = email,
            authModel = authModel,
            twoFactorData = twoFactorData ?: getRememberedTwoFactorData(email),
            newDeviceOtp = newDeviceOtp,
        )
        .fold(
            onFailure = { throwable ->
                when {
                    throwable.isSslHandShakeError() -> LoginResult.CertificateError
                    configDiskSource.serverConfig?.isOfficialBitwardenServer == false -> {
                        LoginResult.UnofficialServerError
                    }

                    else -> LoginResult.Error(error = throwable)
                }
            },
            onSuccess = { loginResponse ->
                when (loginResponse) {
                    is GetTokenResponseJson.TwoFactorRequired -> handleLoginCommonTwoFactorRequired(
                        loginResponse = loginResponse,
                        email = email,
                        authModel = authModel,
                        deviceData = deviceData,
                    )

                    is GetTokenResponseJson.Success -> handleLoginCommonSuccess(
                        loginResponse = loginResponse,
                        email = email,
                        password = password,
                        deviceData = deviceData,
                        orgIdentifier = orgIdentifier,
                        userConfirmedKeyConnector = false,
                    )

                    is GetTokenResponseJson.Invalid -> {
                        when (loginResponse.invalidType) {
                            is GetTokenResponseJson.Invalid.InvalidType.NewDeviceVerification ->
                                handleLoginCommonNewDeviceVerification(
                                    email = email,
                                    authModel = authModel,
                                    error = loginResponse.errorMessage,
                                )

                            is GetTokenResponseJson.Invalid.InvalidType.EncryptionKeyMigrationRequired -> {
                                LoginResult.EncryptionKeyMigrationRequired
                            }

                            is GetTokenResponseJson.Invalid.InvalidType.GenericInvalid -> {
                                LoginResult.Error(
                                    errorMessage = loginResponse.errorMessage,
                                    error = null,
                                )
                            }
                        }
                    }
                }
            },
        )

    /**
     * A helper method that processes the [GetTokenResponseJson.Success] when logging in.
     */
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private suspend fun handleLoginCommonSuccess(
        loginResponse: GetTokenResponseJson.Success,
        email: String,
        password: String?,
        deviceData: DeviceDataModel?,
        orgIdentifier: String?,
        userConfirmedKeyConnector: Boolean,
    ): LoginResult = userStateManager.userStateTransaction {
        val userStateJson = loginResponse.toUserState(
            previousUserState = authDiskSource.userState,
            environmentUrlData = environmentRepository.environment.environmentUrlData,
        )
        val profile = userStateJson.activeAccount.profile
        val userId = profile.userId
        authDiskSource.storeAccountTokens(
            userId = userId,
            accountTokens = AccountTokensJson(
                accessToken = loginResponse.accessToken,
                refreshToken = loginResponse.refreshToken,
                expiresAtSec = clock.instant().epochSecond + loginResponse.expiresInSeconds,
            ),
        )

        checkForVaultUnlockError(
            onVaultUnlockError = { vaultUnlockError ->
                authDiskSource.storeAccountTokens(userId = profile.userId, accountTokens = null)
                return@userStateTransaction vaultUnlockError.toLoginErrorResult()
            },
        ) {
            val keyConnectorUrl = loginResponse
                .keyConnectorUrl
                ?: loginResponse
                    .userDecryptionOptions
                    ?.keyConnectorUserDecryptionOptions
                    ?.keyConnectorUrl
            val isDeviceUnlockAvailable = deviceData != null ||
                loginResponse.userDecryptionOptions?.trustedDeviceUserDecryptionOptions != null
            // if possible attempt to unlock the vault with trusted device data
            if (isDeviceUnlockAvailable) {
                unlockVaultWithTdeOnLoginSuccess(
                    loginResponse = loginResponse,
                    profile = profile,
                    deviceData = deviceData,
                )
            } else if (keyConnectorUrl != null && orgIdentifier != null) {
                val isNewKeyConnectorUser =
                    loginResponse.userDecryptionOptions?.hasMasterPassword == false &&
                        loginResponse.key == null &&
                        loginResponse.privateKeyOrNull() == null
                val isNotConfirmed = !userConfirmedKeyConnector

                // If a new KeyConnector user is logging in for the first time,
                // we should ask him to confirm the domain
                if (isNewKeyConnectorUser && isNotConfirmed) {
                    authDiskSource.storeAccountTokens(userId = profile.userId, accountTokens = null)
                    keyConnectorResponse = loginResponse
                    return@userStateTransaction LoginResult.ConfirmKeyConnectorDomain(
                        domain = keyConnectorUrl,
                    )
                }

                unlockVaultWithKeyConnectorOnLoginSuccess(
                    profile = profile,
                    keyConnectorUrl = keyConnectorUrl,
                    orgIdentifier = orgIdentifier,
                    loginResponse = loginResponse,
                )
            } else {
                unlockVaultWithPasswordOnLoginSuccess(
                    loginResponse = loginResponse,
                    profile = profile,
                    password = password,
                )
            }
        }

        password?.let {
            // Save the master password hash.
            authSdkSource
                .hashPassword(
                    email = email,
                    password = it,
                    kdf = profile.toSdkParams(),
                    purpose = HashPurpose.LOCAL_AUTHORIZATION,
                )
                .onSuccess { passwordHash ->
                    authDiskSource.storeMasterPasswordHash(
                        userId = userId,
                        passwordHash = passwordHash,
                    )
                }

            // Cache the password to verify against any password policies after the sync completes.
            storePasswordToCheck(userId = userId, password = it)
        }

        settingsRepository.hasUserLoggedInOrCreatedAccount = true
        authDiskSource.userState = userStateJson
        password?.let {
            // Automatically update kdf to minimums after password unlock and userState update
            kdfManager
                .updateKdfToMinimumsIfNeeded(password = password)
                .also { result ->
                    if (result is UpdateKdfMinimumsResult.Error) {
                        Timber.e(result.error, message = "Failed to silent update KDF settings.")
                    }
                }
        }

        loginResponse.privateKeyOrNull()?.let { privateKey ->
            // Only set the value if the private key is present, since we may have set
            // the value already when we completed the key connector conversion.
            authDiskSource.storeAccountCryptographicState(
                userId = userId,
                accountCryptographicState = loginResponse.accountKeys.toAccountCryptographicState(
                    privateKey = privateKey,
                ),
            )
        }
        // If the user just authenticated with a two-factor code and selected the option to
        // remember it, then the API response will return a token that will be used in place
        // of the two-factor code on the next login attempt.
        loginResponse.twoFactorToken?.let {
            authDiskSource.storeTwoFactorToken(email = email, twoFactorToken = it)
        }

        // Set the current organization identifier for use in JIT provisioning.
        if (loginResponse.userDecryptionOptions?.hasMasterPassword == false) {
            organizationIdentifier = orgIdentifier
        }

        // Remove any cached data after successfully logging in.
        identityTokenAuthModel = null
        twoFactorResponse = null
        resendEmailRequestJson = null
        twoFactorDeviceData = null
        resendNewDeviceOtpRequestJson = null
        keyConnectorResponse = null
        settingsRepository.setDefaultsIfNecessary(userId = userId)
        settingsRepository.storeUserHasLoggedInValue(userId)
        vaultRepository.syncIfNecessary()
        hasPendingAccountAddition = false
        LoginResult.Success
    }

    /**
     * A helper method that processes the [GetTokenResponseJson.TwoFactorRequired] when logging in.
     */
    private fun handleLoginCommonTwoFactorRequired(
        loginResponse: GetTokenResponseJson.TwoFactorRequired,
        email: String,
        authModel: IdentityTokenAuthModel,
        deviceData: DeviceDataModel?,
    ): LoginResult {
        // Cache the data necessary for the remaining two-factor auth flow.
        identityTokenAuthModel = authModel
        twoFactorResponse = loginResponse
        twoFactorDeviceData = deviceData
        resendEmailRequestJson = ResendEmailRequestJson(
            deviceIdentifier = authDiskSource.uniqueAppId,
            email = email,
            passwordHash = authModel.password,
            ssoToken = loginResponse.ssoToken,
        )

        // If this error was received, it also means any cached two-factor token is invalid.
        authDiskSource.storeTwoFactorToken(email = email, twoFactorToken = null)
        return LoginResult.TwoFactorRequired
    }

    /**
     * A helper method that processes the
     * [GetTokenResponseJson.Invalid.InvalidType.NewDeviceVerification] when logging in.
     */
    private fun handleLoginCommonNewDeviceVerification(
        email: String,
        authModel: IdentityTokenAuthModel,
        error: String?,
    ): LoginResult {
        identityTokenAuthModel = authModel
        resendNewDeviceOtpRequestJson = ResendNewDeviceOtpRequestJson(
            email = email,
            passwordHash = authModel.password,
        )

        return LoginResult.NewDeviceVerification(error)
    }

    /**
     * Attempt to unlock the current user's vault with key connector data.
     */
    @Suppress("LongMethod")
    private suspend fun unlockVaultWithKeyConnectorOnLoginSuccess(
        profile: AccountJson.Profile,
        keyConnectorUrl: String,
        orgIdentifier: String,
        loginResponse: GetTokenResponseJson.Success,
    ): VaultUnlockResult? {
        val key = loginResponse.key
        val privateKey = loginResponse.privateKeyOrNull()
        return if (loginResponse.userDecryptionOptions?.hasMasterPassword != false) {
            // This user has a master password, so we skip the key-connector logic as it is not
            // setup yet. The user can still unlock the vault with their master password.
            null
        } else if (key != null && privateKey != null) {
            // This is a returning user who should already have the key connector setup
            unlockVault(
                accountCryptographicState = loginResponse
                    .accountKeys
                    .toAccountCryptographicState(privateKey = privateKey),
                accountProfile = profile,
                initUserCryptoMethod = InitUserCryptoMethod.KeyConnectorUrl(
                    url = keyConnectorUrl,
                    keyConnectorKeyWrappedUserKey = key,
                ),
            )
        } else {
            // This is a new user who needs to set up the key connector
            val userId = profile.userId
            keyConnectorManager
                .migrateNewUserToKeyConnector(
                    userId = userId,
                    accountKeys = loginResponse.accountKeys,
                    url = keyConnectorUrl,
                    accessToken = loginResponse.accessToken,
                    kdfType = loginResponse.kdfType,
                    kdfIterations = loginResponse.kdfIterations,
                    kdfMemory = loginResponse.kdfMemory,
                    kdfParallelism = loginResponse.kdfParallelism,
                    organizationIdentifier = orgIdentifier,
                )
                .map { keyConnector ->
                    val accountCryptographicState = keyConnector.accountCryptographicState
                    this
                        .unlockVault(
                            accountCryptographicState = accountCryptographicState,
                            accountProfile = profile,
                            initUserCryptoMethod = InitUserCryptoMethod.KeyConnector(
                                masterKey = keyConnector.masterKey,
                                userKey = keyConnector.encryptedUserKey,
                            ),
                        )
                        .onVaultUnlockSuccess {
                            authDiskSource.storeAccountCryptographicState(
                                userId = userId,
                                accountCryptographicState = accountCryptographicState,
                            )
                        }
                }
                .fold(
                    // If the request failed, we want to abort the login process
                    onFailure = { VaultUnlockResult.GenericError(error = it) },
                    onSuccess = { it },
                )
        }
    }

    /**
     * Attempt to unlock the current user's vault with password data.
     */
    private suspend fun unlockVaultWithPasswordOnLoginSuccess(
        loginResponse: GetTokenResponseJson.Success,
        profile: AccountJson.Profile,
        password: String?,
    ): VaultUnlockResult? {
        // Attempt to unlock the vault with password if possible.
        val masterPassword = password ?: return null
        val privateKey = loginResponse.privateKeyOrNull() ?: return null

        val masterPasswordUnlock = loginResponse
            .userDecryptionOptions
            ?.masterPasswordUnlock
            ?: return null
        val initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
            password = masterPassword,
            masterPasswordUnlock = masterPasswordUnlock.toSdkMasterPasswordUnlock(),
        )

        return unlockVault(
            accountCryptographicState = loginResponse.accountKeys.toAccountCryptographicState(
                privateKey = privateKey,
            ),
            accountProfile = profile,
            initUserCryptoMethod = initUserCryptoMethod,
        )
    }

    /**
     * Attempt to unlock the current user's vault with trusted device specific data.
     */
    @Suppress("LongMethod")
    private suspend fun unlockVaultWithTdeOnLoginSuccess(
        loginResponse: GetTokenResponseJson.Success,
        profile: AccountJson.Profile,
        deviceData: DeviceDataModel?,
    ): VaultUnlockResult? {
        // Attempt to unlock the vault with auth request if possible.
        // These values will only be null during the Just-in-Time provisioning flow.
        val privateKey = loginResponse.privateKeyOrNull()
        val key = loginResponse.key
        if (privateKey != null && key != null) {
            deviceData?.let { model ->
                return unlockVault(
                    accountCryptographicState = loginResponse
                        .accountKeys
                        .toAccountCryptographicState(privateKey = privateKey),
                    accountProfile = profile,
                    initUserCryptoMethod = InitUserCryptoMethod.AuthRequest(
                        requestPrivateKey = model.privateKey,
                        method = model
                            .masterPasswordHash
                            ?.let {
                                AuthRequestMethod.MasterKey(
                                    protectedMasterKey = model.asymmetricalKey,
                                    authRequestKey = key,
                                )
                            }
                            ?: AuthRequestMethod.UserKey(protectedUserKey = model.asymmetricalKey),
                    ),
                )
                // We are purposely not storing the master password hash here since it is not
                // formatted in a manner that we can use. We will store it properly the next
                // time the user enters their master password, and it is validated.
            }
        }

        // Handle the Trusted Device Encryption flow
        return loginResponse
            .userDecryptionOptions
            ?.trustedDeviceUserDecryptionOptions
            ?.let { options ->
                loginResponse.privateKeyOrNull()?.let { privateKey ->
                    unlockVaultWithTrustedDeviceUserDecryptionOptionsAndStoreKeys(
                        options = options,
                        profile = profile,
                        privateKey = privateKey,
                        accountKeys = loginResponse.accountKeys,
                    )
                }
            }
    }

    /**
     * A helper method to handle the [TrustedDeviceUserDecryptionOptionsJson] specific to TDE
     * and store the necessary keys when appropriate.
     */
    private suspend fun unlockVaultWithTrustedDeviceUserDecryptionOptionsAndStoreKeys(
        options: TrustedDeviceUserDecryptionOptionsJson,
        profile: AccountJson.Profile,
        privateKey: String,
        accountKeys: AccountKeysJson?,
    ): VaultUnlockResult? {
        var vaultUnlockResult: VaultUnlockResult? = null
        val userId = profile.userId
        val deviceKey = authDiskSource.getDeviceKey(userId = userId)
        if (deviceKey == null) {
            // A null device key means this device is not trusted.
            val pendingRequest = authDiskSource
                .getPendingAuthRequest(userId = userId)
                ?: return null
            authRequestManager
                .getAuthRequestIfApproved(pendingRequest.requestId)
                .getOrNull()
                ?.let { request ->
                    // For approved requests the key will always be present.
                    val userKey = requireNotNull(request.key)
                    vaultUnlockResult = unlockVault(
                        accountCryptographicState = accountKeys.toAccountCryptographicState(
                            privateKey = privateKey,
                        ),
                        accountProfile = profile,
                        initUserCryptoMethod = InitUserCryptoMethod.AuthRequest(
                            requestPrivateKey = pendingRequest.requestPrivateKey,
                            method = AuthRequestMethod.UserKey(protectedUserKey = userKey),
                        ),
                    )
                }
            authDiskSource.storePendingAuthRequest(
                userId = userId,
                pendingAuthRequest = null,
            )
            return vaultUnlockResult
        }

        val encryptedPrivateKey = options.encryptedPrivateKey
        val encryptedUserKey = options.encryptedUserKey

        if (encryptedPrivateKey == null || encryptedUserKey == null) {
            // If we have a device key but server is missing private key and user key, we
            // need to clear the device key and let the user go through the TDE flow again.
            authDiskSource.storeDeviceKey(userId = userId, deviceKey = null)
            return null
        }

        vaultUnlockResult = unlockVault(
            accountCryptographicState = accountKeys.toAccountCryptographicState(
                privateKey = privateKey,
            ),
            accountProfile = profile,
            initUserCryptoMethod = InitUserCryptoMethod.DeviceKey(
                deviceKey = deviceKey,
                protectedDevicePrivateKey = encryptedPrivateKey,
                deviceProtectedUserKey = encryptedUserKey,
            ),
        )
        return vaultUnlockResult
    }

    /**
     * A helper function to unlock the vault for the user associated with the [accountProfile].
     */
    private suspend fun unlockVault(
        accountCryptographicState: WrappedAccountCryptographicState,
        accountProfile: AccountJson.Profile,
        initUserCryptoMethod: InitUserCryptoMethod,
    ): VaultUnlockResult {
        val userId = accountProfile.userId
        return vaultRepository.unlockVault(
            accountCryptographicState = accountCryptographicState,
            userId = userId,
            email = accountProfile.email,
            kdf = accountProfile.toSdkParams(),
            initUserCryptoMethod = initUserCryptoMethod,
            // The value for the organization keys here will typically be null. We can separately
            // unlock the vault for organization data after receiving the sync response if this
            // data is currently absent. These keys may be present during certain multi-phase login
            // processes or if we needed to delete the user's token due to an encrypted data
            // corruption issue, and they are forced to log back in.
            organizationKeys = authDiskSource.getOrganizationKeys(userId = userId),
        )
    }

    /**
     * A helper function to check for a vault unlock related error when logging in.
     *
     * @param onVaultUnlockError a lambda function to be invoked in the event a [VaultUnlockError]
     * is produced via the passed in [block]
     * @param block a lambda representing logic which produces either a [VaultUnlockResult] which
     * is castable to [VaultUnlockError] or `null`
     */
    private inline fun checkForVaultUnlockError(
        onVaultUnlockError: (VaultUnlockError) -> Unit,
        block: () -> VaultUnlockResult?,
    ) {
        (block() as? VaultUnlockError)?.also(onVaultUnlockError)
    }

    //endregion LoginCommon
}

/**
 * Convenience function to extract the private key from the
 * [GetTokenResponseJson.Success] response.
 */
private fun GetTokenResponseJson.Success.privateKeyOrNull(): String? =
    this.accountKeys?.publicKeyEncryptionKeyPair?.wrappedPrivateKey
        ?: this.privateKey
