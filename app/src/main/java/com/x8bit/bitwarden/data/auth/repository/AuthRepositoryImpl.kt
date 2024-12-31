package com.x8bit.bitwarden.data.auth.repository

import com.bitwarden.core.AuthRequestMethod
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.crypto.HashPurpose
import com.bitwarden.crypto.Kdf
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import com.x8bit.bitwarden.data.auth.datasource.disk.model.NewDeviceNoticeDisplayStatus
import com.x8bit.bitwarden.data.auth.datasource.disk.model.NewDeviceNoticeState
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.DeleteAccountResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.DeviceDataModel
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.IdentityTokenAuthModel
import com.x8bit.bitwarden.data.auth.datasource.network.model.PasswordHintResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RefreshTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterFinishRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.ResendEmailRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.ResetPasswordRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.SendVerificationEmailRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.SendVerificationEmailResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.SetPasswordRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TrustedDeviceUserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TwoFactorAuthMethod
import com.x8bit.bitwarden.data.auth.datasource.network.model.TwoFactorDataModel
import com.x8bit.bitwarden.data.auth.datasource.network.model.VerifyEmailTokenRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.VerifyEmailTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.service.AccountsService
import com.x8bit.bitwarden.data.auth.datasource.network.service.DevicesService
import com.x8bit.bitwarden.data.auth.datasource.network.service.HaveIBeenPwnedService
import com.x8bit.bitwarden.data.auth.datasource.network.service.IdentityService
import com.x8bit.bitwarden.data.auth.datasource.network.service.OrganizationService
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toInt
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toKdfTypeJson
import com.x8bit.bitwarden.data.auth.manager.AuthRequestManager
import com.x8bit.bitwarden.data.auth.manager.KeyConnectorManager
import com.x8bit.bitwarden.data.auth.manager.TrustedDeviceManager
import com.x8bit.bitwarden.data.auth.manager.UserLogoutManager
import com.x8bit.bitwarden.data.auth.repository.model.AuthState
import com.x8bit.bitwarden.data.auth.repository.model.BreachCountResult
import com.x8bit.bitwarden.data.auth.repository.model.DeleteAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.EmailTokenResult
import com.x8bit.bitwarden.data.auth.repository.model.KnownDeviceResult
import com.x8bit.bitwarden.data.auth.repository.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.model.NewSsoUserResult
import com.x8bit.bitwarden.data.auth.repository.model.OrganizationDomainSsoDetailsResult
import com.x8bit.bitwarden.data.auth.repository.model.PasswordHintResult
import com.x8bit.bitwarden.data.auth.repository.model.PasswordStrengthResult
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.auth.repository.model.PrevalidateSsoResult
import com.x8bit.bitwarden.data.auth.repository.model.RegisterResult
import com.x8bit.bitwarden.data.auth.repository.model.RemovePasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.RequestOtpResult
import com.x8bit.bitwarden.data.auth.repository.model.ResendEmailResult
import com.x8bit.bitwarden.data.auth.repository.model.ResetPasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.SendVerificationEmailResult
import com.x8bit.bitwarden.data.auth.repository.model.SetPasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.SwitchAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.UserAccountTokens
import com.x8bit.bitwarden.data.auth.repository.model.UserKeyConnectorState
import com.x8bit.bitwarden.data.auth.repository.model.UserOrganizations
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePinResult
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.auth.repository.model.VerifiedOrganizationDomainSsoDetailsResult
import com.x8bit.bitwarden.data.auth.repository.model.VerifyOtpResult
import com.x8bit.bitwarden.data.auth.repository.model.toLoginErrorResult
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.DuoCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.SsoCallbackResult
import com.x8bit.bitwarden.data.auth.repository.util.WebAuthResult
import com.x8bit.bitwarden.data.auth.repository.util.activeUserIdChangesFlow
import com.x8bit.bitwarden.data.auth.repository.util.currentOnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.util.onboardingStatusChangesFlow
import com.x8bit.bitwarden.data.auth.repository.util.policyInformation
import com.x8bit.bitwarden.data.auth.repository.util.toRemovedPasswordUserStateJson
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
import com.x8bit.bitwarden.data.auth.repository.util.toUserState
import com.x8bit.bitwarden.data.auth.repository.util.toUserStateJsonWithPassword
import com.x8bit.bitwarden.data.auth.repository.util.userAccountTokens
import com.x8bit.bitwarden.data.auth.repository.util.userAccountTokensFlow
import com.x8bit.bitwarden.data.auth.repository.util.userKeyConnectorStateFlow
import com.x8bit.bitwarden.data.auth.repository.util.userKeyConnectorStateList
import com.x8bit.bitwarden.data.auth.repository.util.userOrganizationsList
import com.x8bit.bitwarden.data.auth.repository.util.userOrganizationsListFlow
import com.x8bit.bitwarden.data.auth.repository.util.userSwitchingChangesFlow
import com.x8bit.bitwarden.data.auth.util.KdfParamsConstants.DEFAULT_PBKDF2_ITERATIONS
import com.x8bit.bitwarden.data.auth.util.YubiKeyResult
import com.x8bit.bitwarden.data.auth.util.toSdkParams
import com.x8bit.bitwarden.data.platform.datasource.disk.ConfigDiskSource
import com.x8bit.bitwarden.data.platform.datasource.network.util.isSslHandShakeError
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.manager.LogsManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.manager.util.getActivePolicies
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.platform.repository.util.toEnvironmentUrls
import com.x8bit.bitwarden.data.platform.util.asFailure
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.platform.util.flatMap
import com.x8bit.bitwarden.data.vault.datasource.network.model.OrganizationType
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockError
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.ZonedDateTime
import javax.inject.Singleton

/**
 * Default implementation of [AuthRepository].
 */
@Suppress("LargeClass", "LongParameterList", "TooManyFunctions")
@Singleton
class AuthRepositoryImpl(
    private val accountsService: AccountsService,
    private val devicesService: DevicesService,
    private val haveIBeenPwnedService: HaveIBeenPwnedService,
    private val identityService: IdentityService,
    private val organizationService: OrganizationService,
    private val authSdkSource: AuthSdkSource,
    private val vaultSdkSource: VaultSdkSource,
    private val authDiskSource: AuthDiskSource,
    private val configDiskSource: ConfigDiskSource,
    private val environmentRepository: EnvironmentRepository,
    private val settingsRepository: SettingsRepository,
    private val vaultRepository: VaultRepository,
    private val authRequestManager: AuthRequestManager,
    private val keyConnectorManager: KeyConnectorManager,
    private val trustedDeviceManager: TrustedDeviceManager,
    private val userLogoutManager: UserLogoutManager,
    private val policyManager: PolicyManager,
    private val featureFlagManager: FeatureFlagManager,
    firstTimeActionManager: FirstTimeActionManager,
    logsManager: LogsManager,
    pushManager: PushManager,
    dispatcherManager: DispatcherManager,
) : AuthRepository,
    AuthRequestManager by authRequestManager {
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

    private val mutableHasPendingAccountAdditionStateFlow = MutableStateFlow(false)

    /**
     * If there is a pending account deletion, continue showing the original UserState until it
     * is confirmed. This is accomplished by blocking the emissions of the [userStateFlow]
     * whenever set to `true`.
     */
    private val mutableHasPendingAccountDeletionStateFlow = MutableStateFlow(false)

    /**
     * Whenever a function needs to update multiple underlying data-points that contribute to the
     * [UserState], we update this [MutableStateFlow] and continue to show the original `UserState`
     * until the transaction is complete. This is accomplished by blocking the emissions of the
     * [userStateFlow] whenever this is set to a value above 0 (a count is used if more than one
     * process is updating data simultaneously).
     */
    private val mutableUserStateTransactionCountStateFlow = MutableStateFlow(0)

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

    private var organizationIdentifier: String? = null

    /**
     * The password that needs to be checked against any organization policies before
     * the user can complete the login flow. This value is stored using the user ID.
     */
    private var passwordsToCheckMap = mutableMapOf<String, String>()

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

    @Suppress("UNCHECKED_CAST", "MagicNumber")
    override val userStateFlow: StateFlow<UserState?> = combine(
        authDiskSource.userStateFlow,
        authDiskSource.userAccountTokensFlow,
        authDiskSource.userOrganizationsListFlow,
        authDiskSource.userKeyConnectorStateFlow,
        authDiskSource.onboardingStatusChangesFlow,
        firstTimeActionManager.firstTimeStateFlow,
        vaultRepository.vaultUnlockDataStateFlow,
        mutableHasPendingAccountAdditionStateFlow,
        // Ignore the data in the merge, but trigger an update when they emit.
        merge(
            mutableHasPendingAccountDeletionStateFlow,
            mutableUserStateTransactionCountStateFlow,
        ),
    ) { array ->
        val userStateJson = array[0] as UserStateJson?
        val userAccountTokens = array[1] as List<UserAccountTokens>
        val userOrganizationsList = array[2] as List<UserOrganizations>
        val userIsUsingKeyConnectorList = array[3] as List<UserKeyConnectorState>
        val onboardingStatus = array[4] as OnboardingStatus?
        val firstTimeState = array[5] as FirstTimeState
        val vaultState = array[6] as List<VaultUnlockData>
        val hasPendingAccountAddition = array[7] as Boolean
        userStateJson?.toUserState(
            vaultState = vaultState,
            userAccountTokens = userAccountTokens,
            userOrganizationsList = userOrganizationsList,
            userIsUsingKeyConnectorList = userIsUsingKeyConnectorList,
            hasPendingAccountAddition = hasPendingAccountAddition,
            onboardingStatus = onboardingStatus,
            isBiometricsEnabledProvider = ::isBiometricsEnabled,
            vaultUnlockTypeProvider = ::getVaultUnlockType,
            isDeviceTrustedProvider = ::isDeviceTrusted,
            firstTimeState = firstTimeState,
        )
    }
        .filterNot { mutableHasPendingAccountDeletionStateFlow.value }
        .filterNot { mutableUserStateTransactionCountStateFlow.value > 0 }
        .stateIn(
            scope = unconfinedScope,
            started = SharingStarted.Eagerly,
            initialValue = authDiskSource
                .userState
                ?.toUserState(
                    vaultState = vaultRepository.vaultUnlockDataStateFlow.value,
                    userAccountTokens = authDiskSource.userAccountTokens,
                    userOrganizationsList = authDiskSource.userOrganizationsList,
                    userIsUsingKeyConnectorList = authDiskSource.userKeyConnectorStateList,
                    hasPendingAccountAddition = mutableHasPendingAccountAdditionStateFlow.value,
                    onboardingStatus = authDiskSource.currentOnboardingStatus,
                    isBiometricsEnabledProvider = ::isBiometricsEnabled,
                    vaultUnlockTypeProvider = ::getVaultUnlockType,
                    isDeviceTrustedProvider = ::isDeviceTrusted,
                    firstTimeState = firstTimeActionManager.currentOrDefaultUserFirstTimeState,
                ),
        )

    private val captchaTokenChannel = Channel<CaptchaCallbackTokenResult>(capacity = Int.MAX_VALUE)
    override val captchaTokenResultFlow: Flow<CaptchaCallbackTokenResult> =
        captchaTokenChannel.receiveAsFlow()

    private val duoTokenChannel = Channel<DuoCallbackTokenResult>(capacity = Int.MAX_VALUE)
    override val duoTokenResultFlow: Flow<DuoCallbackTokenResult> = duoTokenChannel.receiveAsFlow()

    private val yubiKeyResultChannel = Channel<YubiKeyResult>(capacity = Int.MAX_VALUE)
    override val yubiKeyResultFlow: Flow<YubiKeyResult> = yubiKeyResultChannel.receiveAsFlow()

    private val webAuthResultChannel = Channel<WebAuthResult>(capacity = Int.MAX_VALUE)
    override val webAuthResultFlow: Flow<WebAuthResult> = webAuthResultChannel.receiveAsFlow()

    private val mutableSsoCallbackResultFlow = bufferedMutableSharedFlow<SsoCallbackResult>()
    override val ssoCallbackResultFlow: Flow<SsoCallbackResult> =
        mutableSsoCallbackResultFlow.asSharedFlow()

    override var rememberedEmailAddress: String? by authDiskSource::rememberedEmailAddress

    override var rememberedOrgIdentifier: String? by authDiskSource::rememberedOrgIdentifier

    override val tdeLoginComplete: Boolean?
        get() = activeUserId?.let { authDiskSource.getIsTdeLoginComplete(userId = it) }

    override var shouldTrustDevice: Boolean
        get() = activeUserId?.let { authDiskSource.getShouldTrustDevice(userId = it) } ?: false
        set(value) {
            activeUserId?.let {
                authDiskSource.storeShouldTrustDevice(userId = it, shouldTrustDevice = value)
            }
        }

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

    override val organizations: List<SyncResponseJson.Profile.Organization>
        get() = activeUserId?.let { authDiskSource.getOrganizations(it) }.orEmpty()

    override val showWelcomeCarousel: Boolean
        get() = !settingsRepository.hasUserLoggedInOrCreatedAccount &&
            featureFlagManager.getFeatureFlag(FlagKey.OnboardingCarousel)

    init {
        combine(
            mutableHasPendingAccountAdditionStateFlow,
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
            .onEach {
                val userId = activeUserId ?: return@onEach
                refreshAccessTokenSynchronously(userId)
                vaultRepository.sync()
            }
            // This requires the ioScope to ensure that refreshAccessTokenSynchronously
            // happens on a background thread
            .launchIn(ioScope)

        pushManager
            .logoutFlow
            .onEach { logout(userId = it.userId) }
            .launchIn(unconfinedScope)

        // When the policies for the user have been set, complete the login process.
        policyManager
            .getActivePoliciesFlow(type = PolicyTypeJson.MASTER_PASSWORD)
            .onEach { policies ->
                val userId = activeUserId ?: return@onEach

                // If the user is logging on without a password, the check should complete.
                val passwordToCheck = passwordsToCheckMap.remove(key = userId) ?: return@onEach

                // If the password already has to be reset for some other reason, there's no
                // need to check the password policies.
                if (passwordResetReason != null) return@onEach

                // Otherwise check the user's password against the policies and set or
                // clear the force reset reason accordingly.
                storeUserResetPasswordReason(
                    userId = userId,
                    reason = ForcePasswordResetReason
                        .WEAK_MASTER_PASSWORD_ON_LOGIN
                        .takeIf {
                            !passwordPassesPolicies(
                                password = passwordToCheck,
                                policies = policies,
                            )
                        },
                )
            }
            .launchIn(unconfinedScope)

        // Clear the cached password whenever the user is no longer active
        // or the vault is locked for that user.
        merge(
            authDiskSource
                .userSwitchingChangesFlow
                .mapNotNull { it.previousActiveUserId },
            vaultRepository
                .vaultUnlockDataStateFlow
                .filter { vaultUnlockDataList ->
                    // Clear if the active user is not currently unlocking or unlocked
                    vaultUnlockDataList.none { it.userId == activeUserId }
                }
                .mapNotNull { activeUserId },
        )
            .onEach { userId -> passwordsToCheckMap.remove(key = userId) }
            .launchIn(unconfinedScope)
    }

    override fun clearPendingAccountDeletion() {
        mutableHasPendingAccountDeletionStateFlow.value = false
    }

    override suspend fun deleteAccountWithMasterPassword(
        masterPassword: String,
    ): DeleteAccountResult {
        val profile = authDiskSource.userState?.activeAccount?.profile
            ?: return DeleteAccountResult.Error(message = null)
        mutableHasPendingAccountDeletionStateFlow.value = true
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
        mutableHasPendingAccountDeletionStateFlow.value = true
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
                clearPendingAccountDeletion()
                DeleteAccountResult.Error(message = null)
            },
            onSuccess = { response ->
                when (response) {
                    is DeleteAccountResponseJson.Invalid -> {
                        clearPendingAccountDeletion()
                        DeleteAccountResult.Error(
                            message = response.validationErrors
                                ?.values
                                ?.firstOrNull()
                                ?.firstOrNull(),
                        )
                    }

                    DeleteAccountResponseJson.Success -> {
                        logout()
                        DeleteAccountResult.Success
                    }
                }
            },
        )

    override suspend fun createNewSsoUser(): NewSsoUserResult {
        val account = authDiskSource.userState?.activeAccount ?: return NewSsoUserResult.Failure
        val orgIdentifier = rememberedOrgIdentifier ?: return NewSsoUserResult.Failure
        val userId = account.profile.userId
        return organizationService
            .getOrganizationAutoEnrollStatus(orgIdentifier)
            .flatMap { orgAutoEnrollStatus ->
                organizationService
                    .getOrganizationKeys(orgAutoEnrollStatus.organizationId)
                    .flatMap { organizationKeys ->
                        authSdkSource.makeRegisterTdeKeysAndUnlockVault(
                            userId = userId,
                            email = account.profile.email,
                            orgPublicKey = organizationKeys.publicKey,
                            rememberDevice = authDiskSource
                                .getShouldTrustDevice(userId = userId) == true,
                        )
                    }
                    .flatMap { keys ->
                        accountsService
                            .createAccountKeys(
                                publicKey = keys.publicKey,
                                encryptedPrivateKey = keys.privateKey,
                            )
                            .map { keys }
                    }
                    .flatMap { keys ->
                        organizationService
                            .organizationResetPasswordEnroll(
                                organizationId = orgAutoEnrollStatus.organizationId,
                                userId = userId,
                                passwordHash = null,
                                resetPasswordKey = keys.adminReset,
                            )
                            .map { keys }
                    }
                    .onSuccess { keys ->
                        authDiskSource.storePrivateKey(
                            userId = userId,
                            privateKey = keys.privateKey,
                        )
                        // Order matters here, we need to make sure that the vault is unlocked
                        // before we trust the device, to avoid state-base navigation issues.
                        vaultRepository.syncVaultState(userId = userId)
                        keys.deviceKey?.let { trustDeviceResponse ->
                            trustedDeviceManager.trustThisDevice(
                                userId = userId,
                                trustDeviceResponse = trustDeviceResponse,
                            )
                        }
                    }
            }
            .fold(
                onSuccess = { NewSsoUserResult.Success },
                onFailure = { NewSsoUserResult.Failure },
            )
    }

    override suspend fun completeTdeLogin(
        requestPrivateKey: String,
        asymmetricalKey: String,
    ): LoginResult {
        val profile = authDiskSource.userState?.activeAccount?.profile
            ?: return LoginResult.Error(errorMessage = null)
        val userId = profile.userId
        val privateKey = authDiskSource.getPrivateKey(userId = userId)
            ?: return LoginResult.Error(errorMessage = null)

        checkForVaultUnlockError(
            onVaultUnlockError = { error ->
                return error.toLoginErrorResult()
            },
        ) {
            unlockVault(
                accountProfile = profile,
                privateKey = privateKey,
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
        captchaToken: String?,
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
                captchaToken = captchaToken,
            )
        }
        .fold(
            onFailure = { throwable ->
                when {
                    throwable.isSslHandShakeError() -> LoginResult.CertificateError
                    else -> LoginResult.Error(errorMessage = null)
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
        orgIdentifier: String?,
    ): LoginResult = identityTokenAuthModel
        ?.let {
            loginCommon(
                email = email,
                password = password,
                authModel = it,
                twoFactorData = twoFactorData,
                captchaToken = captchaToken ?: twoFactorResponse?.captchaToken,
                deviceData = twoFactorDeviceData,
                orgIdentifier = orgIdentifier,
            )
        }
        ?: LoginResult.Error(errorMessage = null)

    override suspend fun login(
        email: String,
        ssoCode: String,
        ssoCodeVerifier: String,
        ssoRedirectUri: String,
        captchaToken: String?,
        organizationIdentifier: String,
    ): LoginResult = loginCommon(
        email = email,
        authModel = IdentityTokenAuthModel.SingleSignOn(
            ssoCode = ssoCode,
            ssoCodeVerifier = ssoCodeVerifier,
            ssoRedirectUri = ssoRedirectUri,
        ),
        captchaToken = captchaToken,
        orgIdentifier = organizationIdentifier,
    )

    override fun refreshAccessTokenSynchronously(userId: String): Result<RefreshTokenResponseJson> {
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
            .onSuccess { refreshTokenResponse ->
                // Update the existing UserState with updated token information
                authDiskSource.storeAccountTokens(
                    userId = userId,
                    accountTokens = AccountTokensJson(
                        accessToken = refreshTokenResponse.accessToken,
                        refreshToken = refreshTokenResponse.refreshToken,
                    ),
                )
            }
    }

    override fun logout() {
        activeUserId?.let { userId -> logout(userId) }
    }

    override fun logout(userId: String) {
        userLogoutManager.logout(userId = userId)
    }

    override suspend fun requestOneTimePasscode(): RequestOtpResult =
        accountsService.requestOneTimePasscode()
            .fold(
                onFailure = { RequestOtpResult.Error(it.message) },
                onSuccess = { RequestOtpResult.Success },
            )

    override suspend fun verifyOneTimePasscode(oneTimePasscode: String): VerifyOtpResult =
        accountsService
            .verifyOneTimePasscode(
                passcode = oneTimePasscode,
            )
            .fold(
                onFailure = { VerifyOtpResult.NotVerified(it.message) },
                onSuccess = { VerifyOtpResult.Verified },
            )

    override suspend fun resendVerificationCodeEmail(): ResendEmailResult =
        resendEmailRequestJson
            ?.let { jsonRequest ->
                accountsService.resendVerificationCodeEmail(body = jsonRequest).fold(
                    onFailure = { ResendEmailResult.Error(message = it.message) },
                    onSuccess = { ResendEmailResult.Success },
                )
            }
            ?: ResendEmailResult.Error(message = null)

    override fun switchAccount(userId: String): SwitchAccountResult {
        val currentUserState = authDiskSource.userState ?: return SwitchAccountResult.NoChange
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

        // Clear any pending account additions
        hasPendingAccountAddition = false

        return SwitchAccountResult.AccountSwitched
    }

    @Suppress("LongMethod")
    override suspend fun register(
        email: String,
        masterPassword: String,
        masterPasswordHint: String?,
        emailVerificationToken: String?,
        captchaToken: String?,
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
        val kdf = Kdf.Pbkdf2(iterations = DEFAULT_PBKDF2_ITERATIONS.toUInt())
        return authSdkSource
            .makeRegisterKeys(
                email = email,
                password = masterPassword,
                kdf = kdf,
            )
            .flatMap { registerKeyResponse ->
                if (emailVerificationToken == null) {
                    // TODO PM-6675: Remove register call and service implementation
                    identityService.register(
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
                } else {
                    identityService.registerFinish(
                        body = RegisterFinishRequestJson(
                            email = email,
                            masterPasswordHash = registerKeyResponse.masterPasswordHash,
                            masterPasswordHint = masterPasswordHint,
                            emailVerificationToken = emailVerificationToken,
                            captchaResponse = captchaToken,
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
            }
            .fold(
                onSuccess = {
                    when (it) {
                        is RegisterResponseJson.CaptchaRequired -> {
                            it.validationErrors.captchaKeys.firstOrNull()
                                ?.let { key -> RegisterResult.CaptchaRequired(captchaId = key) }
                                ?: RegisterResult.Error(errorMessage = null)
                        }

                        is RegisterResponseJson.Success -> {
                            settingsRepository.hasUserLoggedInOrCreatedAccount = true
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
                    }
                },
                onFailure = { RegisterResult.Error(errorMessage = null) },
            )
    }

    override suspend fun passwordHintRequest(email: String): PasswordHintResult {
        return accountsService.requestPasswordHint(email).fold(
            onSuccess = {
                when (it) {
                    is PasswordHintResponseJson.Error -> PasswordHintResult.Error(it.errorMessage)
                    PasswordHintResponseJson.Success -> PasswordHintResult.Success
                }
            },
            onFailure = { PasswordHintResult.Error(null) },
        )
    }

    override suspend fun removePassword(masterPassword: String): RemovePasswordResult {
        val activeAccount = authDiskSource
            .userState
            ?.activeAccount
            ?: return RemovePasswordResult.Error
        val profile = activeAccount.profile
        val userId = profile.userId
        val userKey = authDiskSource
            .getUserKey(userId = userId)
            ?: return RemovePasswordResult.Error
        val keyConnectorUrl = organizations
            .find {
                it.shouldUseKeyConnector &&
                    it.type != OrganizationType.OWNER &&
                    it.type != OrganizationType.ADMIN
            }
            ?.keyConnectorUrl
            ?: return RemovePasswordResult.Error
        return keyConnectorManager
            .migrateExistingUserToKeyConnector(
                userId = userId,
                url = keyConnectorUrl,
                userKeyEncrypted = userKey,
                email = profile.email,
                masterPassword = masterPassword,
                kdf = profile.toSdkParams(),
            )
            .onSuccess {
                authDiskSource.userState = authDiskSource
                    .userState
                    ?.toRemovedPasswordUserStateJson(userId = userId)
                vaultRepository.sync()
                settingsRepository.setDefaultsIfNecessary(userId = userId)
            }
            .fold(
                onFailure = { RemovePasswordResult.Error },
                onSuccess = { RemovePasswordResult.Success },
            )
    }

    override suspend fun resetPassword(
        currentPassword: String?,
        newPassword: String,
        passwordHint: String?,
    ): ResetPasswordResult {
        val activeAccount = authDiskSource
            .userState
            ?.activeAccount
            ?: return ResetPasswordResult.Error
        val currentPasswordHash = currentPassword?.let { password ->
            authSdkSource
                .hashPassword(
                    email = activeAccount.profile.email,
                    password = password,
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

    @Suppress("LongMethod")
    override suspend fun setPassword(
        organizationIdentifier: String,
        password: String,
        passwordHint: String?,
    ): SetPasswordResult {
        val activeAccount = authDiskSource
            .userState
            ?.activeAccount
            ?: return SetPasswordResult.Error
        val userId = activeAccount.profile.userId

        // Update the saved master password hash.
        val passwordHash = authSdkSource
            .hashPassword(
                email = activeAccount.profile.email,
                password = password,
                kdf = activeAccount.profile.toSdkParams(),
                purpose = HashPurpose.SERVER_AUTHORIZATION,
            )
            .getOrElse { return@setPassword SetPasswordResult.Error }

        return when (activeAccount.profile.forcePasswordResetReason) {
            ForcePasswordResetReason.TDE_USER_WITHOUT_PASSWORD_HAS_PASSWORD_RESET_PERMISSION -> {
                vaultSdkSource
                    .updatePassword(userId = userId, newPassword = password)
                    .map { it.newKey to null }
            }

            ForcePasswordResetReason.ADMIN_FORCE_PASSWORD_RESET,
            ForcePasswordResetReason.WEAK_MASTER_PASSWORD_ON_LOGIN,
            null,
                -> {
                authSdkSource
                    .makeRegisterKeys(
                        email = activeAccount.profile.email,
                        password = password,
                        kdf = activeAccount.profile.toSdkParams(),
                    )
                    .map { it.encryptedUserKey to it.keys }
            }
        }
            .flatMap { (encryptedUserKey, rsaKeys) ->
                accountsService
                    .setPassword(
                        body = SetPasswordRequestJson(
                            passwordHash = passwordHash,
                            passwordHint = passwordHint,
                            organizationIdentifier = organizationIdentifier,
                            kdfIterations = activeAccount.profile.kdfIterations,
                            kdfMemory = activeAccount.profile.kdfMemory,
                            kdfParallelism = activeAccount.profile.kdfParallelism,
                            kdfType = activeAccount.profile.kdfType,
                            key = encryptedUserKey,
                            keys = rsaKeys?.let {
                                RegisterRequestJson.Keys(
                                    publicKey = it.public,
                                    encryptedPrivateKey = it.private,
                                )
                            },
                        ),
                    )
                    .onSuccess {
                        rsaKeys?.private?.let {
                            authDiskSource.storePrivateKey(userId = userId, privateKey = it)
                        }
                        authDiskSource.storeUserKey(userId = userId, userKey = encryptedUserKey)
                    }
            }
            .flatMap {
                when (vaultRepository.unlockVaultWithMasterPassword(password)) {
                    is VaultUnlockResult.Success -> {
                        enrollUserInPasswordReset(
                            userId = userId,
                            organizationIdentifier = organizationIdentifier,
                            passwordHash = passwordHash,
                        )
                    }

                    is VaultUnlockResult.AuthenticationError,
                    VaultUnlockResult.InvalidStateError,
                    VaultUnlockResult.GenericError,
                        -> {
                        IllegalStateException("Failed to unlock vault").asFailure()
                    }
                }
            }
            .onSuccess {
                authDiskSource.storeMasterPasswordHash(userId = userId, passwordHash = passwordHash)
                authDiskSource.userState = authDiskSource.userState?.toUserStateJsonWithPassword()
                this.organizationIdentifier = null
            }
            .fold(
                onFailure = { SetPasswordResult.Error },
                onSuccess = { SetPasswordResult.Success },
            )
    }

    override fun setCaptchaCallbackTokenResult(tokenResult: CaptchaCallbackTokenResult) {
        captchaTokenChannel.trySend(tokenResult)
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
                    verifiedDate = it.verifiedDate,
                )
            },
            onFailure = { OrganizationDomainSsoDetailsResult.Failure },
        )

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
            onFailure = { VerifiedOrganizationDomainSsoDetailsResult.Failure },
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
                onSuccess = { PasswordStrengthResult.Success(passwordStrength = it) },
                onFailure = { PasswordStrengthResult.Error },
            )

    override suspend fun validatePassword(password: String): ValidatePasswordResult {
        val userId = activeUserId ?: return ValidatePasswordResult.Error
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
                        onFailure = { ValidatePasswordResult.Error },
                    )
            }
            ?: run {
                val encryptedKey = authDiskSource
                    .getUserKey(userId)
                    ?: return ValidatePasswordResult.Error
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
                            // an invalid password, this is not necessarily the case but we have no
                            // way to differentiate between the different errors.
                            ValidatePasswordResult.Success(isValid = false)
                        },
                    )
            }
    }

    override suspend fun validatePin(pin: String): ValidatePinResult {
        val activeAccount = authDiskSource
            .userState
            ?.activeAccount
            ?.profile
            ?: return ValidatePinResult.Error
        val pinProtectedUserKey = authDiskSource
            .getPinProtectedUserKey(userId = activeAccount.userId)
            ?: return ValidatePinResult.Error
        return vaultSdkSource
            .validatePin(
                userId = activeAccount.userId,
                pin = pin,
                pinProtectedUserKey = pinProtectedUserKey,
            )
            .fold(
                onSuccess = { ValidatePinResult.Success(isValid = it) },
                onFailure = { ValidatePinResult.Error },
            )
    }

    override suspend fun validatePasswordAgainstPolicies(
        password: String,
    ): Boolean = passwordPolicies
        .all { validatePasswordAgainstPolicy(password, it) }

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
                            SendVerificationEmailResult.Error(it.message)
                        }

                        is SendVerificationEmailResponseJson.Success -> {
                            SendVerificationEmailResult.Success(it.emailVerificationToken)
                        }
                    }
                },
                onFailure = {
                    SendVerificationEmailResult.Error(null)
                },
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
                    when (val json = it) {
                        VerifyEmailTokenResponseJson.Valid -> EmailTokenResult.Success
                        is VerifyEmailTokenResponseJson.Invalid -> {
                            EmailTokenResult.Error(json.message)
                        }

                        VerifyEmailTokenResponseJson.TokenExpired -> EmailTokenResult.Expired
                    }
                },
                onFailure = {
                    EmailTokenResult.Error(message = null)
                },
            )
    }

    override fun setOnboardingStatus(userId: String, status: OnboardingStatus?) {
        authDiskSource.storeOnboardingStatus(userId = userId, onboardingStatus = status)
    }

    override fun getNewDeviceNoticeState(): NewDeviceNoticeState? {
        return activeUserId?.let { userId ->
            authDiskSource.getNewDeviceNoticeState(userId = userId)
        }
    }

    override fun setNewDeviceNoticeState(newState: NewDeviceNoticeState?) {
        activeUserId?.let { userId ->
            authDiskSource.storeNewDeviceNoticeState(userId = userId, newState = newState)
        }
    }

    override fun checkUserNeedsNewDeviceTwoFactorNotice(): Boolean {
        return activeUserId?.let { userId ->
            val temporaryFlag = featureFlagManager.getFeatureFlag(FlagKey.NewDeviceTemporaryDismiss)
            val permanentFlag = featureFlagManager.getFeatureFlag(FlagKey.NewDevicePermanentDismiss)

            // check if feature flags are disabled
            if (!temporaryFlag && !permanentFlag) {
                return false
            }

            if (!newDeviceNoticePreConditionsValid()) {
                return false
            }

            val newDeviceNoticeState = authDiskSource.getNewDeviceNoticeState(userId = userId)
            return when (newDeviceNoticeState.displayStatus) {
                // if the user has already attested email access but permanent flag is enabled,
                // the notice needs to appear again
                NewDeviceNoticeDisplayStatus.CAN_ACCESS_EMAIL -> permanentFlag
                // if the user has already seen but 7 days have already passed,
                // the notice needs to appear again
                NewDeviceNoticeDisplayStatus.HAS_SEEN ->
                    newDeviceNoticeState.shouldDisplayNoticeIfSeen
                NewDeviceNoticeDisplayStatus.HAS_NOT_SEEN -> true
                // the user never needs to see the notice again
                NewDeviceNoticeDisplayStatus.CAN_ACCESS_EMAIL_PERMANENT -> false
            }
        }
            ?: false
    }

    /**
     * Checks if the preconditions are met for a user to see a new device notice:
     * - Must be a Bitwarden cloud user.
     * - The account must be at least one week old.
     * - Cannot have an active policy requiring SSO to be enabled.
     * - Cannot have two-factor authentication enabled.
     */
    private fun newDeviceNoticePreConditionsValid(): Boolean {
        if (environmentRepository.environment.type == Environment.Type.SELF_HOSTED) {
            return false
        }

        val userProfile = authDiskSource.userState?.activeAccount?.profile
        val isProfileAtLeastWeekOld = userProfile
            ?.let {
                it.creationDate
                    ?.plusWeeks(1)
                    ?.isBefore(
                        ZonedDateTime.now(),
                    )
            }
            ?: false
        if (!isProfileAtLeastWeekOld) {
            return false
        }

        val hasTwoFactorEnabled = userProfile
            ?.isTwoFactorEnabled
            ?: false
        if (hasTwoFactorEnabled) {
            return false
        }

        val hasSSOPolicy =
            policyManager.getActivePolicies(type = PolicyTypeJson.REQUIRE_SSO)
                .any { p -> p.isEnabled }

        return !hasSSOPolicy
    }

    @Suppress("CyclomaticComplexMethod")
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
    private suspend fun passwordPassesPolicies(
        password: String,
        policies: List<SyncResponseJson.Policy>,
    ): Boolean {
        // If there are no master password policies that are enabled and should be
        // enforced on login, the check should complete.
        val passwordPolicies = policies
            .mapNotNull { it.policyInformation as? PolicyInformation.MasterPassword }
            .filter { it.enforceOnLogin == true }

        // Check the password against all the policies.
        return passwordPolicies.all { policy ->
            validatePasswordAgainstPolicy(password, policy)
        }
    }

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

    private fun isBiometricsEnabled(
        userId: String,
    ): Boolean = authDiskSource.getUserBiometricUnlockKey(userId = userId) != null

    private fun isDeviceTrusted(
        userId: String,
    ): Boolean = authDiskSource.getDeviceKey(userId = userId) != null

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

    //region LoginCommon

    /**
     * A helper function to extract the common logic of logging in through
     * any of the available methods.
     */
    private suspend fun loginCommon(
        email: String,
        password: String? = null,
        authModel: IdentityTokenAuthModel,
        twoFactorData: TwoFactorDataModel? = null,
        deviceData: DeviceDataModel? = null,
        orgIdentifier: String? = null,
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
            onFailure = { throwable ->
                when {
                    throwable.isSslHandShakeError() -> LoginResult.CertificateError
                    configDiskSource.serverConfig?.isOfficialBitwardenServer == false -> {
                        LoginResult.UnofficialServerError
                    }
                    else -> LoginResult.Error(errorMessage = null)
                }
            },
            onSuccess = { loginResponse ->
                when (loginResponse) {
                    is GetTokenResponseJson.CaptchaRequired -> LoginResult.CaptchaRequired(
                        captchaId = loginResponse.captchaKey,
                    )

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
                    )

                    is GetTokenResponseJson.Invalid -> LoginResult.Error(
                        errorMessage = loginResponse.errorMessage,
                    )
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
    ): LoginResult = userStateTransaction {
        val userStateJson = loginResponse.toUserState(
            previousUserState = authDiskSource.userState,
            environmentUrlData = environmentRepository.environment.environmentUrlData,
        )
        val profile = userStateJson.activeAccount.profile
        val userId = profile.userId

        checkForVaultUnlockError(
            onVaultUnlockError = { vaultUnlockError ->
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
            passwordsToCheckMap.put(userId, it)
        }

        authDiskSource.storeAccountTokens(
            userId = userId,
            accountTokens = AccountTokensJson(
                accessToken = loginResponse.accessToken,
                refreshToken = loginResponse.refreshToken,
            ),
        )
        settingsRepository.hasUserLoggedInOrCreatedAccount = true

        val shouldSetOnboardingStatus = featureFlagManager.getFeatureFlag(FlagKey.OnboardingFlow) &&
            !settingsRepository.getUserHasLoggedInValue(userId = userId)
        if (shouldSetOnboardingStatus) {
            setOnboardingStatus(
                userId = userId,
                status = OnboardingStatus.NOT_STARTED,
            )
        }

        authDiskSource.userState = userStateJson
        loginResponse.key?.let {
            // Only set the value if it's present, since we may have set it already
            // when we completed the pending admin auth request.
            authDiskSource.storeUserKey(userId = userId, userKey = it)
        }
        loginResponse.privateKey?.let {
            // Only set the value if it's present, since we may have set it already
            // when we completed the key connector conversion.
            authDiskSource.storePrivateKey(userId = userId, privateKey = it)
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
     * Attempt to unlock the current user's vault with key connector data.
     */
    private suspend fun unlockVaultWithKeyConnectorOnLoginSuccess(
        profile: AccountJson.Profile,
        keyConnectorUrl: String,
        orgIdentifier: String,
        loginResponse: GetTokenResponseJson.Success,
    ): VaultUnlockResult? =
        if (loginResponse.userDecryptionOptions?.hasMasterPassword != false) {
            // This user has a master password, so we skip the key-connector logic as it is not
            // setup yet. The user can still unlock the vault with their master password.
            null
        } else if (loginResponse.key != null && loginResponse.privateKey != null) {
            // This is a returning user who should already have the key connector setup
            keyConnectorManager
                .getMasterKeyFromKeyConnector(
                    url = keyConnectorUrl,
                    accessToken = loginResponse.accessToken,
                )
                .map {
                    unlockVault(
                        accountProfile = profile,
                        privateKey = loginResponse.privateKey,
                        initUserCryptoMethod = InitUserCryptoMethod.KeyConnector(
                            masterKey = it.masterKey,
                            userKey = loginResponse.key,
                        ),
                    )
                }
                .fold(
                    // If the request failed, we want to abort the login process
                    onFailure = { VaultUnlockResult.GenericError },
                    onSuccess = { it },
                )
        } else {
            // This is a new user who needs to setup the key connector
            keyConnectorManager
                .migrateNewUserToKeyConnector(
                    url = keyConnectorUrl,
                    accessToken = loginResponse.accessToken,
                    kdfType = loginResponse.kdfType,
                    kdfIterations = loginResponse.kdfIterations,
                    kdfMemory = loginResponse.kdfMemory,
                    kdfParallelism = loginResponse.kdfParallelism,
                    organizationIdentifier = orgIdentifier,
                )
                .map { keyConnectorResponse ->
                    val result = unlockVault(
                        accountProfile = profile,
                        privateKey = keyConnectorResponse.keys.private,
                        initUserCryptoMethod = InitUserCryptoMethod.KeyConnector(
                            masterKey = keyConnectorResponse.masterKey,
                            userKey = keyConnectorResponse.encryptedUserKey,
                        ),
                    )
                    if (result is VaultUnlockResult.Success) {
                        // We now know that login/unlock was successful, so we store the userKey
                        // and privateKey we now have since it didn't exist on the loginResponse
                        authDiskSource.storeUserKey(
                            userId = profile.userId,
                            userKey = keyConnectorResponse.encryptedUserKey,
                        )
                        authDiskSource.storePrivateKey(
                            userId = profile.userId,
                            privateKey = keyConnectorResponse.keys.private,
                        )
                    }
                    result
                }
                .fold(
                    // If the request failed, we want to abort the login process
                    onFailure = { VaultUnlockResult.GenericError },
                    onSuccess = { it },
                )
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
        val privateKey = loginResponse.privateKey ?: return null
        val key = loginResponse.key ?: return null
        return unlockVault(
            accountProfile = profile,
            privateKey = privateKey,
            initUserCryptoMethod = InitUserCryptoMethod.Password(
                password = masterPassword,
                userKey = key,
            ),
        )
    }

    /**
     * Attempt to unlock the current user's vault with trusted device specific data.
     */
    private suspend fun unlockVaultWithTdeOnLoginSuccess(
        loginResponse: GetTokenResponseJson.Success,
        profile: AccountJson.Profile,
        deviceData: DeviceDataModel?,
    ): VaultUnlockResult? {
        // Attempt to unlock the vault with auth request if possible.
        // These values will only be null during the Just-in-Time provisioning flow.
        if (loginResponse.privateKey != null && loginResponse.key != null) {
            deviceData?.let { model ->
                return unlockVault(
                    accountProfile = profile,
                    privateKey = loginResponse.privateKey,
                    initUserCryptoMethod = InitUserCryptoMethod.AuthRequest(
                        requestPrivateKey = model.privateKey,
                        method = model
                            .masterPasswordHash
                            ?.let {
                                AuthRequestMethod.MasterKey(
                                    protectedMasterKey = model.asymmetricalKey,
                                    authRequestKey = loginResponse.key,
                                )
                            }
                            ?: AuthRequestMethod.UserKey(protectedUserKey = model.asymmetricalKey),
                    ),
                )
                // We are purposely not storing the master password hash here since it is not
                // formatted in in a manner that we can use. We will store it properly the next
                // time the user enters their master password and it is validated.
            }
        }

        // Handle the Trusted Device Encryption flow
        return loginResponse
            .userDecryptionOptions
            ?.trustedDeviceUserDecryptionOptions
            ?.let { options ->
                loginResponse.privateKey?.let { privateKey ->
                    unlockVaultWithTrustedDeviceUserDecryptionOptionsAndStoreKeys(
                        options = options,
                        profile = profile,
                        privateKey = privateKey,
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
                        accountProfile = profile,
                        privateKey = privateKey,
                        initUserCryptoMethod = InitUserCryptoMethod.AuthRequest(
                            requestPrivateKey = pendingRequest.requestPrivateKey,
                            method = AuthRequestMethod.UserKey(protectedUserKey = userKey),
                        ),
                    )
                    authDiskSource.storeUserKey(userId = userId, userKey = userKey)
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
            accountProfile = profile,
            privateKey = privateKey,
            initUserCryptoMethod = InitUserCryptoMethod.DeviceKey(
                deviceKey = deviceKey,
                protectedDevicePrivateKey = encryptedPrivateKey,
                deviceProtectedUserKey = encryptedUserKey,
            ),
        )

        if (vaultUnlockResult is VaultUnlockResult.Success) {
            authDiskSource.storeUserKey(userId = userId, userKey = encryptedUserKey)
        }
        return vaultUnlockResult
    }

    /**
     * A helper function to unlock the vault for the user associated with the [accountProfile].
     */
    private suspend fun unlockVault(
        accountProfile: AccountJson.Profile,
        privateKey: String,
        initUserCryptoMethod: InitUserCryptoMethod,
    ): VaultUnlockResult {
        val userId = accountProfile.userId
        return vaultRepository.unlockVault(
            userId = userId,
            email = accountProfile.email,
            kdf = accountProfile.toSdkParams(),
            privateKey = privateKey,
            initUserCryptoMethod = initUserCryptoMethod,
            // The value for the organization keys here will typically be null. We can separately
            // unlock the vault for organization data after receiving the sync response if this
            // data is currently absent. These keys may be present during certain multi-phase login
            // processes or if we needed to delete the user's token due to an encrypted data
            // corruption issue and they are forced to log back in.
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

    /**
     * Run the given [block] while preventing any updates to [UserState]. This is useful in cases
     * where many individual changes might occur that would normally affect the [UserState] but we
     * only want a single final emission. In the rare case that multiple threads are running
     * transactions simultaneously, there will be no [UserState] updates until the last
     * transaction completes.
     */
    private inline fun <T> userStateTransaction(block: () -> T): T {
        mutableUserStateTransactionCountStateFlow.update { it.inc() }
        return try {
            block()
        } finally {
            mutableUserStateTransactionCountStateFlow.update { it.dec() }
        }
    }
}
