package com.x8bit.bitwarden.data.auth.repository

import app.cash.turbine.test
import com.bitwarden.core.AuthRequestMethod
import com.bitwarden.core.AuthRequestResponse
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.core.KeyConnectorResponse
import com.bitwarden.core.MasterPasswordAuthenticationData
import com.bitwarden.core.MasterPasswordUnlockData
import com.bitwarden.core.RegisterKeyResponse
import com.bitwarden.core.RegisterTdeKeyResponse
import com.bitwarden.core.UpdateKdfResponse
import com.bitwarden.core.UpdatePasswordResponse
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.repository.error.MissingPropertyException
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.crypto.HashPurpose
import com.bitwarden.crypto.Kdf
import com.bitwarden.crypto.RsaKeyPair
import com.bitwarden.crypto.TrustDeviceResponse
import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.data.datasource.disk.model.ServerConfig
import com.bitwarden.data.datasource.disk.util.FakeConfigDiskSource
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.network.model.ConfigResponseJson
import com.bitwarden.network.model.DeleteAccountResponseJson
import com.bitwarden.network.model.GetTokenResponseJson
import com.bitwarden.network.model.IdentityTokenAuthModel
import com.bitwarden.network.model.KdfJson
import com.bitwarden.network.model.KdfTypeJson
import com.bitwarden.network.model.KeyConnectorMasterKeyResponseJson
import com.bitwarden.network.model.MasterPasswordUnlockDataJson
import com.bitwarden.network.model.OrganizationAutoEnrollStatusResponseJson
import com.bitwarden.network.model.OrganizationKeysResponseJson
import com.bitwarden.network.model.OrganizationType
import com.bitwarden.network.model.PasswordHintResponseJson
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.PreLoginResponseJson
import com.bitwarden.network.model.PrevalidateSsoResponseJson
import com.bitwarden.network.model.RefreshTokenResponseJson
import com.bitwarden.network.model.RegisterFinishRequestJson
import com.bitwarden.network.model.RegisterRequestJson
import com.bitwarden.network.model.RegisterResponseJson
import com.bitwarden.network.model.ResendEmailRequestJson
import com.bitwarden.network.model.ResetPasswordRequestJson
import com.bitwarden.network.model.SendVerificationEmailRequestJson
import com.bitwarden.network.model.SendVerificationEmailResponseJson
import com.bitwarden.network.model.SetPasswordRequestJson
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.TrustedDeviceUserDecryptionOptionsJson
import com.bitwarden.network.model.TwoFactorAuthMethod
import com.bitwarden.network.model.TwoFactorDataModel
import com.bitwarden.network.model.UserDecryptionOptionsJson
import com.bitwarden.network.model.VerificationCodeResponseJson
import com.bitwarden.network.model.VerifiedOrganizationDomainSsoDetailsResponse
import com.bitwarden.network.model.VerifyEmailTokenRequestJson
import com.bitwarden.network.model.VerifyEmailTokenResponseJson
import com.bitwarden.network.model.createMockAccountKeysJson
import com.bitwarden.network.model.createMockAccountKeysJsonWithNullFields
import com.bitwarden.network.model.createMockOrganization
import com.bitwarden.network.model.createMockPolicy
import com.bitwarden.network.service.AccountsService
import com.bitwarden.network.service.DevicesService
import com.bitwarden.network.service.HaveIBeenPwnedService
import com.bitwarden.network.service.IdentityService
import com.bitwarden.network.service.OrganizationService
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.datasource.disk.model.PendingAuthRequestJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength.LEVEL_0
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength.LEVEL_1
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength.LEVEL_2
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength.LEVEL_3
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength.LEVEL_4
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toKdfRequestModel
import com.x8bit.bitwarden.data.auth.manager.AuthRequestManager
import com.x8bit.bitwarden.data.auth.manager.KdfManager
import com.x8bit.bitwarden.data.auth.manager.KeyConnectorManager
import com.x8bit.bitwarden.data.auth.manager.TrustedDeviceManager
import com.x8bit.bitwarden.data.auth.manager.UserLogoutManager
import com.x8bit.bitwarden.data.auth.manager.UserStateManager
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequest
import com.x8bit.bitwarden.data.auth.manager.model.MigrateExistingUserToKeyConnectorResult
import com.x8bit.bitwarden.data.auth.repository.model.AuthState
import com.x8bit.bitwarden.data.auth.repository.model.BreachCountResult
import com.x8bit.bitwarden.data.auth.repository.model.DeleteAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.EmailTokenResult
import com.x8bit.bitwarden.data.auth.repository.model.KnownDeviceResult
import com.x8bit.bitwarden.data.auth.repository.model.LeaveOrganizationResult
import com.x8bit.bitwarden.data.auth.repository.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.model.LogoutReason
import com.x8bit.bitwarden.data.auth.repository.model.NewSsoUserResult
import com.x8bit.bitwarden.data.auth.repository.model.PasswordHintResult
import com.x8bit.bitwarden.data.auth.repository.model.PasswordStrengthResult
import com.x8bit.bitwarden.data.auth.repository.model.PrevalidateSsoResult
import com.x8bit.bitwarden.data.auth.repository.model.RegisterResult
import com.x8bit.bitwarden.data.auth.repository.model.RemovePasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.RequestOtpResult
import com.x8bit.bitwarden.data.auth.repository.model.ResendEmailResult
import com.x8bit.bitwarden.data.auth.repository.model.ResetPasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.SendVerificationEmailResult
import com.x8bit.bitwarden.data.auth.repository.model.SetPasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.SwitchAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.UpdateKdfMinimumsResult
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePinResult
import com.x8bit.bitwarden.data.auth.repository.model.VerifiedOrganizationDomainSsoDetailsResult
import com.x8bit.bitwarden.data.auth.repository.model.VerifyOtpResult
import com.x8bit.bitwarden.data.auth.repository.util.DuoCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.SsoCallbackResult
import com.x8bit.bitwarden.data.auth.repository.util.WebAuthResult
import com.x8bit.bitwarden.data.auth.repository.util.toRemovedPasswordUserStateJson
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
import com.x8bit.bitwarden.data.auth.repository.util.toUserState
import com.x8bit.bitwarden.data.auth.util.YubiKeyResult
import com.x8bit.bitwarden.data.auth.util.toSdkParams
import com.x8bit.bitwarden.data.platform.datasource.disk.util.FakeSettingsDiskSource
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
import com.x8bit.bitwarden.data.platform.manager.LogsManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.manager.model.NotificationLogoutData
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.data.vault.repository.util.createWrappedAccountCryptographicState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import javax.net.ssl.SSLHandshakeException

@Suppress("LargeClass")
class AuthRepositoryTest {

    private val dispatcherManager: DispatcherManager = FakeDispatcherManager()
    private val accountsService: AccountsService = mockk()
    private val devicesService: DevicesService = mockk()
    private val identityService: IdentityService = mockk()
    private val haveIBeenPwnedService: HaveIBeenPwnedService = mockk()
    private val organizationService: OrganizationService = mockk()
    private val mutableVaultUnlockDataStateFlow = MutableStateFlow(VAULT_UNLOCK_DATA)
    private val mutableIsActiveUserUnlockingFlow = MutableStateFlow(false)
    private val vaultRepository: VaultRepository = mockk {
        every { vaultUnlockDataStateFlow } returns mutableVaultUnlockDataStateFlow
        every { deleteVaultData(any()) } just runs
        every { isActiveUserUnlockingFlow } returns mutableIsActiveUserUnlockingFlow
    }
    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val fakeSettingsDiskSource = FakeSettingsDiskSource()
    private val fakeEnvironmentRepository =
        FakeEnvironmentRepository()
            .apply {
                environment = Environment.Us
            }
    private val settingsRepository: SettingsRepository = mockk {
        every { setDefaultsIfNecessary(any()) } just runs
        every { hasUserLoggedInOrCreatedAccount = true } just runs
        every { storeUserHasLoggedInValue(any()) } just runs
    }
    private val authSdkSource = mockk<AuthSdkSource> {
        coEvery {
            getNewAuthRequest(
                email = EMAIL,
            )
        } returns AUTH_REQUEST_RESPONSE.asSuccess()
        coEvery {
            hashPassword(
                email = EMAIL,
                password = PASSWORD,
                kdf = PRE_LOGIN_SUCCESS.kdfParams.toSdkParams(),
                purpose = HashPurpose.SERVER_AUTHORIZATION,
            )
        } returns PASSWORD_HASH.asSuccess()
        coEvery {
            hashPassword(
                email = EMAIL,
                password = PASSWORD,
                kdf = ACCOUNT_1.profile.toSdkParams(),
                purpose = HashPurpose.LOCAL_AUTHORIZATION,
            )
        } returns PASSWORD_HASH.asSuccess()
        coEvery {
            makeRegisterKeys(
                email = EMAIL,
                password = PASSWORD,
                kdf = Kdf.Pbkdf2(DEFAULT_KDF_ITERATIONS.toUInt()),
            )
        } returns RegisterKeyResponse(
            masterPasswordHash = PASSWORD_HASH,
            encryptedUserKey = ENCRYPTED_USER_KEY,
            keys = RsaKeyPair(
                public = PUBLIC_KEY,
                private = PRIVATE_KEY,
            ),
        )
            .asSuccess()
    }
    private val configDiskSource = FakeConfigDiskSource()
    private val vaultSdkSource = mockk<VaultSdkSource> {
        coEvery {
            getAuthRequestKey(
                publicKey = PUBLIC_KEY,
                userId = USER_ID_1,
            )
        } returns "AsymmetricEncString".asSuccess()
    }
    private val authRequestManager: AuthRequestManager = mockk()
    private val keyConnectorManager: KeyConnectorManager = mockk()
    private val trustedDeviceManager: TrustedDeviceManager = mockk()
    private val userLogoutManager: UserLogoutManager = mockk {
        every { logout(any(), any()) } just runs
    }

    private val mutableLogoutFlow = bufferedMutableSharedFlow<NotificationLogoutData>()
    private val mutableSyncOrgKeysFlow = bufferedMutableSharedFlow<String>()
    private val mutableActivePolicyFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Policy>>()
    private val pushManager: PushManager = mockk {
        every { logoutFlow } returns mutableLogoutFlow
        every { syncOrgKeysFlow } returns mutableSyncOrgKeysFlow
    }
    private val policyManager: PolicyManager = mockk {
        every {
            getActivePoliciesFlow(type = PolicyTypeJson.MASTER_PASSWORD)
        } returns mutableActivePolicyFlow
    }
    private val logsManager: LogsManager = mockk {
        every { setUserData(userId = any(), environmentType = any()) } just runs
    }
    private val mutableHasPendingAccountAdditionStateFlow = MutableStateFlow(false)
    private val userStateManager: UserStateManager = mockk {
        every {
            hasPendingAccountAdditionStateFlow
        } returns mutableHasPendingAccountAdditionStateFlow
        every { hasPendingAccountAddition = any() } just runs
        every { hasPendingAccountAddition } returns mutableHasPendingAccountAdditionStateFlow.value
        every { hasPendingAccountDeletion = any() } just runs
        val blockSlot = slot<suspend () -> LoginResult>()
        coEvery { userStateTransaction(capture(blockSlot)) } coAnswers { blockSlot.captured() }
    }
    private val kdfManager: KdfManager = mockk {
        every { needsKdfUpdateToMinimums() } returns false
        coEvery {
            updateKdfToMinimumsIfNeeded(password = any())
        } returns UpdateKdfMinimumsResult.Success
    }

    private val repository: AuthRepository = AuthRepositoryImpl(
        clock = FIXED_CLOCK,
        accountsService = accountsService,
        devicesService = devicesService,
        identityService = identityService,
        haveIBeenPwnedService = haveIBeenPwnedService,
        organizationService = organizationService,
        authSdkSource = authSdkSource,
        vaultSdkSource = vaultSdkSource,
        authDiskSource = fakeAuthDiskSource,
        settingsDiskSource = fakeSettingsDiskSource,
        configDiskSource = configDiskSource,
        environmentRepository = fakeEnvironmentRepository,
        settingsRepository = settingsRepository,
        vaultRepository = vaultRepository,
        authRequestManager = authRequestManager,
        biometricsEncryptionManager = mockk(),
        keyConnectorManager = keyConnectorManager,
        trustedDeviceManager = trustedDeviceManager,
        userLogoutManager = userLogoutManager,
        dispatcherManager = dispatcherManager,
        pushManager = pushManager,
        policyManager = policyManager,
        logsManager = logsManager,
        userStateManager = userStateManager,
        kdfManager = kdfManager,
    )

    @BeforeEach
    fun beforeEach() {
        mockkStatic(
            GetTokenResponseJson.Success::toUserState,
            UserStateJson::toRemovedPasswordUserStateJson,
        )
        mockkConstructor(
            NoActiveUserException::class,
            MissingPropertyException::class,
        )
        every {
            anyConstructed<NoActiveUserException>() == any<NoActiveUserException>()
        } returns true
        every {
            anyConstructed<MissingPropertyException>() == any<MissingPropertyException>()
        } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            GetTokenResponseJson.Success::toUserState,
            UserStateJson::toRemovedPasswordUserStateJson,
        )
        mockkConstructor(
            NoActiveUserException::class,
            MissingPropertyException::class,
        )
    }

    @Test
    fun `authStateFlow should react to user state changes and account token changes`() = runTest {
        repository.authStateFlow.test {
            assertEquals(AuthState.Unauthenticated, awaitItem())

            // Store the tokens, nothing happens yet since there is technically no active user yet
            fakeAuthDiskSource.storeAccountTokens(
                userId = USER_ID_1,
                accountTokens = ACCOUNT_TOKENS_1,
            )
            expectNoEvents()
            // Update the active user, we are now authenticated
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            assertEquals(AuthState.Authenticated(ACCESS_TOKEN), awaitItem())

            // Adding a tokens for the non-active user does not update the state
            fakeAuthDiskSource.storeAccountTokens(
                userId = USER_ID_2,
                accountTokens = ACCOUNT_TOKENS_2,
            )
            expectNoEvents()
            // Adding a non-active user does not update the state
            fakeAuthDiskSource.userState = MULTI_USER_STATE
            expectNoEvents()

            // Changing the active users tokens causes an update
            val newAccessToken = "new_access_token"
            fakeAuthDiskSource.storeAccountTokens(
                userId = USER_ID_1,
                accountTokens = ACCOUNT_TOKENS_1.copy(accessToken = newAccessToken),
            )
            assertEquals(AuthState.Authenticated(newAccessToken), awaitItem())

            // Change the active user causes an update
            fakeAuthDiskSource.userState = MULTI_USER_STATE.copy(activeUserId = USER_ID_2)
            assertEquals(AuthState.Authenticated(ACCESS_TOKEN_2), awaitItem())

            // Clearing the tokens of the active state results in the Unauthenticated state
            fakeAuthDiskSource.storeAccountTokens(
                userId = USER_ID_2,
                accountTokens = null,
            )
            assertEquals(AuthState.Unauthenticated, awaitItem())
        }
    }

    @Test
    @OptIn(ExperimentalSerializationApi::class)
    @Suppress("MaxLineLength")
    fun `loading the policies should emit masterPasswordPolicyFlow if the password fails any checks`() =
        runTest {
            val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS
            coEvery {
                identityService.preLogin(email = EMAIL)
            } returns PRE_LOGIN_SUCCESS.asSuccess()
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns successResponse.asSuccess()
            coEvery {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .wrappedPrivateKey,
                        securityState = successResponse.accountKeys
                            ?.securityState
                            ?.securityState,
                        signedPublicKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .signedPublicKey,
                        signingKey = successResponse.accountKeys
                            ?.signatureKeyPair
                            ?.wrappedSigningKey,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                        password = PASSWORD,
                        masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK,
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.Success
            coEvery { vaultRepository.syncIfNecessary() } just runs
            every {
                GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.toUserState(
                    previousUserState = null,
                    environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
                )
            } returns SINGLE_USER_STATE_1

            // Start the login flow so that all the necessary data is cached.
            val result = repository.login(email = EMAIL, password = PASSWORD)

            // Set policies that will fail the password.
            mutableActivePolicyFlow.emit(
                listOf(
                    createMockPolicy(
                        type = PolicyTypeJson.MASTER_PASSWORD,
                        isEnabled = true,
                        data = buildJsonObject {
                            put(key = "minLength", value = 100)
                            put(key = "minComplexity", value = null)
                            put(key = "requireUpper", value = null)
                            put(key = "requireLower", value = null)
                            put(key = "requireNumbers", value = null)
                            put(key = "requireSpecial", value = null)
                            put(key = "enforceOnLogin", value = true)
                        },
                    ),
                ),
            )

            // Verify the results.
            assertEquals(LoginResult.Success, result)
            assertEquals(AuthState.Authenticated(ACCESS_TOKEN), repository.authStateFlow.value)
            coVerify { identityService.preLogin(email = EMAIL) }
            fakeAuthDiskSource.assertAccountKeys(
                userId = USER_ID_1,
                accountKeys = ACCOUNT_KEYS,
            )
            fakeAuthDiskSource.assertUserKey(
                userId = USER_ID_1,
                userKey = "key",
            )
            fakeAuthDiskSource.assertMasterPasswordHash(
                userId = USER_ID_1,
                passwordHash = PASSWORD_HASH,
            )
            coVerify {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .wrappedPrivateKey,
                        securityState = successResponse.accountKeys
                            ?.securityState
                            ?.securityState,
                        signedPublicKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .signedPublicKey,
                        signingKey = successResponse.accountKeys
                            ?.signatureKeyPair
                            ?.wrappedSigningKey,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                        password = PASSWORD,
                        masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK,
                    ),
                    organizationKeys = null,
                )
                vaultRepository.syncIfNecessary()
            }
            assertEquals(
                UserStateJson(
                    activeUserId = USER_ID_1,
                    accounts = mapOf(
                        USER_ID_1 to ACCOUNT_1.copy(
                            profile = ACCOUNT_1.profile.copy(
                                forcePasswordResetReason = ForcePasswordResetReason.WEAK_MASTER_PASSWORD_ON_LOGIN,
                            ),
                        ),
                    ),
                ),
                fakeAuthDiskSource.userState,
            )
            verify(exactly = 1) {
                userStateManager.hasPendingAccountAddition = false
                settingsRepository.setDefaultsIfNecessary(userId = USER_ID_1)
            }
        }

    @Test
    fun `rememberedEmailAddress should pull from and update AuthDiskSource`() {
        // AuthDiskSource and the repository start with the same value.
        assertNull(repository.rememberedEmailAddress)
        assertNull(fakeAuthDiskSource.rememberedEmailAddress)

        // Updating the repository updates AuthDiskSource
        repository.rememberedEmailAddress = "remembered@gmail.com"
        assertEquals("remembered@gmail.com", fakeAuthDiskSource.rememberedEmailAddress)

        // Updating AuthDiskSource updates the repository
        fakeAuthDiskSource.rememberedEmailAddress = null
        assertNull(repository.rememberedEmailAddress)
    }

    @Test
    fun `rememberedOrgIdentifier should pull from and update AuthDiskSource`() {
        // AuthDiskSource and the repository start with the same value.
        assertNull(repository.rememberedOrgIdentifier)
        assertNull(fakeAuthDiskSource.rememberedOrgIdentifier)

        // Updating the repository updates AuthDiskSource
        repository.rememberedOrgIdentifier = "Bitwarden"
        assertEquals("Bitwarden", fakeAuthDiskSource.rememberedOrgIdentifier)

        // Updating AuthDiskSource updates the repository
        fakeAuthDiskSource.rememberedOrgIdentifier = null
        assertNull(repository.rememberedOrgIdentifier)
    }

    @Test
    fun `tdeLoginComplete should directly access the authDiskSource`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        // AuthDiskSource and the repository start with the same default value.
        assertNull(repository.tdeLoginComplete)
        assertNull(fakeAuthDiskSource.getIsTdeLoginComplete(userId = USER_ID_1))

        // Updating AuthDiskSource updates the repository
        fakeAuthDiskSource.storeIsTdeLoginComplete(userId = USER_ID_1, isTdeLoginComplete = true)
        assertEquals(true, repository.tdeLoginComplete)
    }

    @Test
    fun `shouldTrustDevice should directly access the authDiskSource`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        // AuthDiskSource and the repository start with the same default value.
        assertFalse(repository.shouldTrustDevice)
        assertNull(fakeAuthDiskSource.getShouldTrustDevice(userId = USER_ID_1))

        // Updating the repository updates AuthDiskSource
        repository.shouldTrustDevice = true
        assertEquals(true, fakeAuthDiskSource.getShouldTrustDevice(userId = USER_ID_1))

        // Updating AuthDiskSource updates the repository
        fakeAuthDiskSource.storeShouldTrustDevice(userId = USER_ID_1, shouldTrustDevice = false)
        assertEquals(false, repository.shouldTrustDevice)
    }

    @Test
    fun `passwordResetReason should pull from the user's profile in AuthDiskSource`() = runTest {
        val updatedProfile = ACCOUNT_1.profile.copy(
            forcePasswordResetReason = ForcePasswordResetReason.WEAK_MASTER_PASSWORD_ON_LOGIN,
        )
        fakeAuthDiskSource.userState = UserStateJson(
            activeUserId = USER_ID_1,
            accounts = mapOf(
                USER_ID_1 to ACCOUNT_1.copy(
                    profile = updatedProfile,
                ),
            ),
        )
        assertEquals(
            ForcePasswordResetReason.WEAK_MASTER_PASSWORD_ON_LOGIN,
            repository.passwordResetReason,
        )
    }

    @Test
    fun `organizations should return an empty list when there is no active user`() = runTest {
        assertEquals(emptyList<SyncResponseJson.Profile.Organization>(), repository.organizations)
    }

    @Test
    fun `organizations should pull from the organizations in the AuthDiskSource`() = runTest {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        fakeAuthDiskSource.storeOrganizations(
            userId = USER_ID_1,
            organizations = ORGANIZATIONS,
        )
        assertEquals(ORGANIZATIONS, repository.organizations)
    }

    @Test
    fun `delete account fails if not logged in`() = runTest {
        val masterPassword = "hello world"
        val result = repository.deleteAccountWithMasterPassword(masterPassword = masterPassword)
        assertEquals(
            DeleteAccountResult.Error(message = null, error = NoActiveUserException()),
            result,
        )
    }

    @Test
    fun `delete account fails if hashPassword fails`() = runTest {
        val masterPassword = "hello world"
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        val kdf = SINGLE_USER_STATE_1.activeAccount.profile.toSdkParams()
        val error = Throwable("Fail")
        coEvery {
            authSdkSource.hashPassword(EMAIL, masterPassword, kdf, HashPurpose.SERVER_AUTHORIZATION)
        } returns error.asFailure()

        val result = repository.deleteAccountWithMasterPassword(masterPassword = masterPassword)

        assertEquals(DeleteAccountResult.Error(message = null, error = error), result)
        verify(exactly = 1) {
            userStateManager.hasPendingAccountDeletion = true
        }
        coVerify {
            authSdkSource.hashPassword(EMAIL, masterPassword, kdf, HashPurpose.SERVER_AUTHORIZATION)
        }
    }

    @Test
    fun `delete account fails if deleteAccount fails`() = runTest {
        val masterPassword = "hello world"
        val hashedMasterPassword = "hashed password"
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        val kdf = SINGLE_USER_STATE_1.activeAccount.profile.toSdkParams()
        val error = Throwable("Fail")
        coEvery {
            authSdkSource.hashPassword(EMAIL, masterPassword, kdf, HashPurpose.SERVER_AUTHORIZATION)
        } returns hashedMasterPassword.asSuccess()
        coEvery {
            accountsService.deleteAccount(
                masterPasswordHash = hashedMasterPassword,
                oneTimePassword = null,
            )
        } returns error.asFailure()

        val result = repository.deleteAccountWithMasterPassword(masterPassword = masterPassword)

        assertEquals(DeleteAccountResult.Error(message = null, error = error), result)
        verify(exactly = 1) {
            userStateManager.hasPendingAccountDeletion = true
        }
        coVerify {
            authSdkSource.hashPassword(EMAIL, masterPassword, kdf, HashPurpose.SERVER_AUTHORIZATION)
            accountsService.deleteAccount(
                masterPasswordHash = hashedMasterPassword,
                oneTimePassword = null,
            )
        }
    }

    @Test
    fun `delete account fails if deleteAccount fails with message`() = runTest {
        val masterPassword = "hello world"
        val hashedMasterPassword = "hashed password"
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        val kdf = SINGLE_USER_STATE_1.activeAccount.profile.toSdkParams()
        coEvery {
            authSdkSource.hashPassword(EMAIL, masterPassword, kdf, HashPurpose.SERVER_AUTHORIZATION)
        } returns hashedMasterPassword.asSuccess()
        coEvery {
            accountsService.deleteAccount(
                masterPasswordHash = hashedMasterPassword,
                oneTimePassword = null,
            )
        } returns DeleteAccountResponseJson.Invalid(
            errorMessage = "Fail",
            validationErrors = null,
        ).asSuccess()

        val result = repository.deleteAccountWithMasterPassword(masterPassword = masterPassword)

        assertEquals(DeleteAccountResult.Error(message = "Fail", error = null), result)
        verify(exactly = 1) {
            userStateManager.hasPendingAccountDeletion = true
        }
        coVerify {
            authSdkSource.hashPassword(EMAIL, masterPassword, kdf, HashPurpose.SERVER_AUTHORIZATION)
            accountsService.deleteAccount(
                masterPasswordHash = hashedMasterPassword,
                oneTimePassword = null,
            )
        }
    }

    @Test
    fun `deleteAccountWithMasterPassword succeeds`() = runTest {
        val masterPassword = "hello world"
        val hashedMasterPassword = "hashed password"
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        val kdf = SINGLE_USER_STATE_1.activeAccount.profile.toSdkParams()
        coEvery {
            authSdkSource.hashPassword(EMAIL, masterPassword, kdf, HashPurpose.SERVER_AUTHORIZATION)
        } returns hashedMasterPassword.asSuccess()
        coEvery {
            accountsService.deleteAccount(
                masterPasswordHash = hashedMasterPassword,
                oneTimePassword = null,
            )
        } returns DeleteAccountResponseJson.Success.asSuccess()

        val result = repository.deleteAccountWithMasterPassword(masterPassword = masterPassword)

        assertEquals(DeleteAccountResult.Success, result)
        verify(exactly = 1) {
            userStateManager.hasPendingAccountDeletion = true
        }
        coVerify {
            authSdkSource.hashPassword(EMAIL, masterPassword, kdf, HashPurpose.SERVER_AUTHORIZATION)
            accountsService.deleteAccount(
                masterPasswordHash = hashedMasterPassword,
                oneTimePassword = null,
            )
        }
    }

    @Test
    fun `deleteAccountWithOneTimePassword succeeds`() = runTest {
        val oneTimePassword = "123456"
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        coEvery {
            accountsService.deleteAccount(
                masterPasswordHash = null,
                oneTimePassword = oneTimePassword,
            )
        } returns DeleteAccountResponseJson.Success.asSuccess()

        val result = repository.deleteAccountWithOneTimePassword(
            oneTimePassword = oneTimePassword,
        )

        assertEquals(DeleteAccountResult.Success, result)
        verify(exactly = 1) {
            userStateManager.hasPendingAccountDeletion = true
        }
        coVerify {
            accountsService.deleteAccount(
                masterPasswordHash = null,
                oneTimePassword = oneTimePassword,
            )
        }
    }

    @Test
    fun `refreshAccessTokenSynchronously returns failure if not logged in`() = runTest {
        fakeAuthDiskSource.userState = null

        val result = repository.refreshAccessTokenSynchronously(USER_ID_1)

        assertTrue(result.isFailure)
    }

    @Test
    fun `refreshAccessTokenSynchronously returns failure if refreshTokenSynchronously fails`() =
        runTest {
            fakeAuthDiskSource.storeAccountTokens(
                userId = USER_ID_1,
                accountTokens = ACCOUNT_TOKENS_1,
            )
            coEvery {
                identityService.refreshTokenSynchronously(REFRESH_TOKEN)
            } returns Throwable("Fail").asFailure()

            assertTrue(repository.refreshAccessTokenSynchronously(USER_ID_1).isFailure)

            coVerify(exactly = 1) {
                identityService.refreshTokenSynchronously(REFRESH_TOKEN)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `refreshAccessTokenSynchronously returns logs out and returns failure if refreshTokenSynchronously returns invalid_grant`() =
        runTest {
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            fakeAuthDiskSource.storeAccountTokens(
                userId = USER_ID_1,
                accountTokens = ACCOUNT_TOKENS_1,
            )
            coEvery {
                identityService.refreshTokenSynchronously(REFRESH_TOKEN)
            } returns RefreshTokenResponseJson.Error(error = "invalid_grant").asSuccess()

            assertTrue(repository.refreshAccessTokenSynchronously(USER_ID_1).isFailure)

            coVerify(exactly = 1) {
                identityService.refreshTokenSynchronously(REFRESH_TOKEN)
                userLogoutManager.logout(userId = USER_ID_1, reason = LogoutReason.InvalidGrant)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `refreshAccessTokenSynchronously returns logs out and returns failure if refreshTokenSynchronously returns Forbidden`() =
        runTest {
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            fakeAuthDiskSource.storeAccountTokens(
                userId = USER_ID_1,
                accountTokens = ACCOUNT_TOKENS_1,
            )
            coEvery {
                identityService.refreshTokenSynchronously(REFRESH_TOKEN)
            } returns RefreshTokenResponseJson.Forbidden(error = Throwable("Fail!")).asSuccess()

            assertTrue(repository.refreshAccessTokenSynchronously(USER_ID_1).isFailure)

            coVerify(exactly = 1) {
                identityService.refreshTokenSynchronously(REFRESH_TOKEN)
                userLogoutManager.logout(userId = USER_ID_1, reason = LogoutReason.RefreshForbidden)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `refreshAccessTokenSynchronously returns logs out and returns failure if refreshTokenSynchronously returns Unauthorized`() =
        runTest {
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            fakeAuthDiskSource.storeAccountTokens(
                userId = USER_ID_1,
                accountTokens = ACCOUNT_TOKENS_1,
            )
            coEvery {
                identityService.refreshTokenSynchronously(REFRESH_TOKEN)
            } returns RefreshTokenResponseJson.Unauthorized(error = Throwable("Fail!")).asSuccess()

            assertTrue(repository.refreshAccessTokenSynchronously(USER_ID_1).isFailure)

            coVerify(exactly = 1) {
                identityService.refreshTokenSynchronously(REFRESH_TOKEN)
                userLogoutManager.logout(
                    userId = USER_ID_1,
                    reason = LogoutReason.RefreshUnauthorized,
                )
            }
        }

    @Test
    fun `refreshAccessTokenSynchronously returns success and sets account tokens`() = runTest {
        val updatedAccountTokens = AccountTokensJson(
            accessToken = ACCESS_TOKEN_2,
            refreshToken = REFRESH_TOKEN_2,
            expiresAtSec = FIXED_CLOCK.instant().epochSecond + ACCESS_TOKEN_2_EXPIRES_IN,
        )
        fakeAuthDiskSource.storeAccountTokens(
            userId = USER_ID_1,
            accountTokens = ACCOUNT_TOKENS_1,
        )
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        coEvery {
            identityService.refreshTokenSynchronously(REFRESH_TOKEN)
        } returns REFRESH_TOKEN_RESPONSE_JSON.asSuccess()

        val result = repository.refreshAccessTokenSynchronously(USER_ID_1)

        assertEquals(REFRESH_TOKEN_RESPONSE_JSON.accessToken.asSuccess(), result)
        fakeAuthDiskSource.assertAccountTokens(
            userId = USER_ID_1,
            accountTokens = updatedAccountTokens,
        )
        coVerify(exactly = 1) {
            identityService.refreshTokenSynchronously(REFRESH_TOKEN)
        }
    }

    @Test
    fun `createNewSsoUser when no active user returns failure`() = runTest {
        fakeAuthDiskSource.userState = null

        val result = repository.createNewSsoUser()

        assertEquals(NewSsoUserResult.Failure(error = NoActiveUserException()), result)
    }

    @Test
    fun `createNewSsoUser when remembered org identifier returns failure`() = runTest {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        fakeAuthDiskSource.rememberedOrgIdentifier = null

        val result = repository.createNewSsoUser()

        assertEquals(
            NewSsoUserResult.Failure(error = MissingPropertyException("OrgIdentifier")),
            result,
        )
    }

    @Test
    fun `createNewSsoUser when getOrganizationAutoEnrollStatus fails returns failure`() = runTest {
        val orgIdentifier = "rememberedOrgIdentifier"
        val error = Throwable("Fail!")
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        fakeAuthDiskSource.rememberedOrgIdentifier = orgIdentifier
        coEvery {
            organizationService.getOrganizationAutoEnrollStatus(orgIdentifier)
        } returns error.asFailure()

        val result = repository.createNewSsoUser()

        assertEquals(NewSsoUserResult.Failure(error = error), result)
        coVerify(exactly = 1) {
            organizationService.getOrganizationAutoEnrollStatus(orgIdentifier)
        }
    }

    @Test
    fun `createNewSsoUser when getOrganizationKeys fails returns failure`() = runTest {
        val orgIdentifier = "rememberedOrgIdentifier"
        val orgId = "organizationId"
        val error = Throwable("Fail!")
        val orgAutoEnrollStatusResponse = OrganizationAutoEnrollStatusResponseJson(
            organizationId = orgId,
            isResetPasswordEnabled = false,
        )
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        fakeAuthDiskSource.rememberedOrgIdentifier = orgIdentifier
        coEvery {
            organizationService.getOrganizationAutoEnrollStatus(orgIdentifier)
        } returns orgAutoEnrollStatusResponse.asSuccess()
        coEvery { organizationService.getOrganizationKeys(orgId) } returns error.asFailure()

        val result = repository.createNewSsoUser()

        assertEquals(NewSsoUserResult.Failure(error = error), result)
        coVerify(exactly = 1) {
            organizationService.getOrganizationAutoEnrollStatus(orgIdentifier)
            organizationService.getOrganizationKeys(orgId)
        }
    }

    @Test
    fun `createNewSsoUser when makeRegisterTdeKeysAndUnlockVault fails returns failure`() =
        runTest {
            val shouldTrustDevice = false
            val orgIdentifier = "rememberedOrgIdentifier"
            val orgId = "organizationId"
            val orgPublicKey = "organizationPublicKey"
            val orgAutoEnrollStatusResponse = OrganizationAutoEnrollStatusResponseJson(
                organizationId = orgId,
                isResetPasswordEnabled = false,
            )
            val orgKeysResponse = OrganizationKeysResponseJson(
                privateKey = "privateKey",
                publicKey = orgPublicKey,
            )
            val error = Throwable("Fail!")
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            fakeAuthDiskSource.rememberedOrgIdentifier = orgIdentifier

            fakeAuthDiskSource.storeShouldTrustDevice(
                userId = USER_ID_1,
                shouldTrustDevice = shouldTrustDevice,
            )
            coEvery {
                organizationService.getOrganizationAutoEnrollStatus(orgIdentifier)
            } returns orgAutoEnrollStatusResponse.asSuccess()
            coEvery {
                organizationService.getOrganizationKeys(orgId)
            } returns orgKeysResponse.asSuccess()
            coEvery {
                authSdkSource.makeRegisterTdeKeysAndUnlockVault(
                    userId = USER_ID_1,
                    email = EMAIL,
                    orgPublicKey = orgPublicKey,
                    rememberDevice = shouldTrustDevice,
                )
            } returns error.asFailure()

            val result = repository.createNewSsoUser()

            assertEquals(NewSsoUserResult.Failure(error = error), result)
            coVerify(exactly = 1) {
                organizationService.getOrganizationAutoEnrollStatus(orgIdentifier)
                organizationService.getOrganizationKeys(orgId)
                authSdkSource.makeRegisterTdeKeysAndUnlockVault(
                    userId = USER_ID_1,
                    email = EMAIL,
                    orgPublicKey = orgPublicKey,
                    rememberDevice = shouldTrustDevice,
                )
            }
        }

    @Test
    fun `createNewSsoUser when createAccountKeys fails returns failure`() = runTest {
        val shouldTrustDevice = false
        val orgIdentifier = "rememberedOrgIdentifier"
        val orgId = "organizationId"
        val orgPublicKey = "organizationPublicKey"
        val userPrivateKey = "userPrivateKey"
        val userPublicKey = "userPublicKey"
        val userAdminReset = "userAdminReset"
        val orgAutoEnrollStatusResponse = OrganizationAutoEnrollStatusResponseJson(
            organizationId = orgId,
            isResetPasswordEnabled = false,
        )
        val orgKeysResponse = OrganizationKeysResponseJson(
            privateKey = "privateKey",
            publicKey = orgPublicKey,
        )
        val registerTdeKeyResponse = RegisterTdeKeyResponse(
            privateKey = userPrivateKey,
            publicKey = userPublicKey,
            adminReset = userAdminReset,
            deviceKey = null,
        )
        val error = Throwable("Fail!")
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        fakeAuthDiskSource.rememberedOrgIdentifier = orgIdentifier
        fakeAuthDiskSource.storeShouldTrustDevice(
            userId = USER_ID_1,
            shouldTrustDevice = shouldTrustDevice,
        )
        coEvery {
            organizationService.getOrganizationAutoEnrollStatus(orgIdentifier)
        } returns orgAutoEnrollStatusResponse.asSuccess()
        coEvery {
            organizationService.getOrganizationKeys(orgId)
        } returns orgKeysResponse.asSuccess()
        coEvery {
            authSdkSource.makeRegisterTdeKeysAndUnlockVault(
                userId = USER_ID_1,
                email = EMAIL,
                orgPublicKey = orgPublicKey,
                rememberDevice = shouldTrustDevice,
            )
        } returns registerTdeKeyResponse.asSuccess()
        coEvery {
            accountsService.createAccountKeys(
                publicKey = userPublicKey,
                encryptedPrivateKey = userPrivateKey,
            )
        } returns error.asFailure()

        val result = repository.createNewSsoUser()

        assertEquals(NewSsoUserResult.Failure(error = error), result)
        coVerify(exactly = 1) {
            organizationService.getOrganizationAutoEnrollStatus(orgIdentifier)
            organizationService.getOrganizationKeys(orgId)
            authSdkSource.makeRegisterTdeKeysAndUnlockVault(
                userId = USER_ID_1,
                email = EMAIL,
                orgPublicKey = orgPublicKey,
                rememberDevice = shouldTrustDevice,
            )
            accountsService.createAccountKeys(
                publicKey = userPublicKey,
                encryptedPrivateKey = userPrivateKey,
            )
        }
    }

    @Test
    fun `createNewSsoUser when organizationResetPasswordEnroll fails returns failure`() = runTest {
        val shouldTrustDevice = false
        val orgIdentifier = "rememberedOrgIdentifier"
        val orgId = "organizationId"
        val orgPublicKey = "organizationPublicKey"
        val userPrivateKey = "userPrivateKey"
        val userPublicKey = "userPublicKey"
        val userAdminReset = "userAdminReset"
        val orgAutoEnrollStatusResponse = OrganizationAutoEnrollStatusResponseJson(
            organizationId = orgId,
            isResetPasswordEnabled = false,
        )
        val orgKeysResponse = OrganizationKeysResponseJson(
            privateKey = "privateKey",
            publicKey = orgPublicKey,
        )
        val registerTdeKeyResponse = RegisterTdeKeyResponse(
            privateKey = userPrivateKey,
            publicKey = userPublicKey,
            adminReset = userAdminReset,
            deviceKey = null,
        )
        val error = Throwable("Fail!")
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        fakeAuthDiskSource.rememberedOrgIdentifier = orgIdentifier
        fakeAuthDiskSource.storeShouldTrustDevice(
            userId = USER_ID_1,
            shouldTrustDevice = shouldTrustDevice,
        )
        coEvery {
            organizationService.getOrganizationAutoEnrollStatus(orgIdentifier)
        } returns orgAutoEnrollStatusResponse.asSuccess()
        coEvery {
            organizationService.getOrganizationKeys(orgId)
        } returns orgKeysResponse.asSuccess()
        coEvery {
            authSdkSource.makeRegisterTdeKeysAndUnlockVault(
                userId = USER_ID_1,
                email = EMAIL,
                orgPublicKey = orgPublicKey,
                rememberDevice = shouldTrustDevice,
            )
        } returns registerTdeKeyResponse.asSuccess()
        coEvery {
            accountsService.createAccountKeys(
                publicKey = userPublicKey,
                encryptedPrivateKey = userPrivateKey,
            )
        } returns Unit.asSuccess()
        coEvery {
            organizationService.organizationResetPasswordEnroll(
                organizationId = orgId,
                userId = USER_ID_1,
                passwordHash = null,
                resetPasswordKey = userAdminReset,
            )
        } returns error.asFailure()

        val result = repository.createNewSsoUser()

        assertEquals(NewSsoUserResult.Failure(error = error), result)
        coVerify(exactly = 1) {
            organizationService.getOrganizationAutoEnrollStatus(orgIdentifier)
            organizationService.getOrganizationKeys(orgId)
            authSdkSource.makeRegisterTdeKeysAndUnlockVault(
                userId = USER_ID_1,
                email = EMAIL,
                orgPublicKey = orgPublicKey,
                rememberDevice = shouldTrustDevice,
            )
            accountsService.createAccountKeys(
                publicKey = userPublicKey,
                encryptedPrivateKey = userPrivateKey,
            )
            organizationService.organizationResetPasswordEnroll(
                organizationId = orgId,
                userId = USER_ID_1,
                passwordHash = null,
                resetPasswordKey = userAdminReset,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `createNewSsoUser when shouldTrustDevice false should not trust the device returns Success`() =
        runTest {
            val shouldTrustDevice = false
            val orgIdentifier = "rememberedOrgIdentifier"
            val orgId = "organizationId"
            val orgPublicKey = "organizationPublicKey"
            val userPrivateKey = "userPrivateKey"
            val userPublicKey = "userPublicKey"
            val userAdminReset = "userAdminReset"
            val orgAutoEnrollStatusResponse = OrganizationAutoEnrollStatusResponseJson(
                organizationId = orgId,
                isResetPasswordEnabled = false,
            )
            val orgKeysResponse = OrganizationKeysResponseJson(
                privateKey = "privateKey",
                publicKey = orgPublicKey,
            )
            val registerTdeKeyResponse = RegisterTdeKeyResponse(
                privateKey = userPrivateKey,
                publicKey = userPublicKey,
                adminReset = userAdminReset,
                deviceKey = null,
            )
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            fakeAuthDiskSource.rememberedOrgIdentifier = orgIdentifier
            fakeAuthDiskSource.storeShouldTrustDevice(
                userId = USER_ID_1,
                shouldTrustDevice = shouldTrustDevice,
            )
            coEvery {
                organizationService.getOrganizationAutoEnrollStatus(orgIdentifier)
            } returns orgAutoEnrollStatusResponse.asSuccess()
            coEvery {
                organizationService.getOrganizationKeys(orgId)
            } returns orgKeysResponse.asSuccess()
            coEvery {
                authSdkSource.makeRegisterTdeKeysAndUnlockVault(
                    userId = USER_ID_1,
                    email = EMAIL,
                    orgPublicKey = orgPublicKey,
                    rememberDevice = shouldTrustDevice,
                )
            } returns registerTdeKeyResponse.asSuccess()
            coEvery {
                accountsService.createAccountKeys(
                    publicKey = userPublicKey,
                    encryptedPrivateKey = userPrivateKey,
                )
            } returns Unit.asSuccess()
            coEvery {
                organizationService.organizationResetPasswordEnroll(
                    organizationId = orgId,
                    userId = USER_ID_1,
                    passwordHash = null,
                    resetPasswordKey = userAdminReset,
                )
            } returns Unit.asSuccess()
            coEvery { vaultRepository.syncVaultState(userId = USER_ID_1) } just runs

            val result = repository.createNewSsoUser()

            fakeAuthDiskSource.assertPrivateKey(userId = USER_ID_1, privateKey = userPrivateKey)
            assertEquals(NewSsoUserResult.Success, result)
            coVerify(exactly = 1) {
                organizationService.getOrganizationAutoEnrollStatus(orgIdentifier)
                organizationService.getOrganizationKeys(orgId)
                authSdkSource.makeRegisterTdeKeysAndUnlockVault(
                    userId = USER_ID_1,
                    email = EMAIL,
                    orgPublicKey = orgPublicKey,
                    rememberDevice = shouldTrustDevice,
                )
                accountsService.createAccountKeys(
                    publicKey = userPublicKey,
                    encryptedPrivateKey = userPrivateKey,
                )
                organizationService.organizationResetPasswordEnroll(
                    organizationId = orgId,
                    userId = USER_ID_1,
                    passwordHash = null,
                    resetPasswordKey = userAdminReset,
                )
                vaultRepository.syncVaultState(userId = USER_ID_1)
            }
        }

    @Test
    fun `createNewSsoUser when shouldTrustDevice true should trust the device returns Success`() =
        runTest {
            val shouldTrustDevice = true
            val orgIdentifier = "rememberedOrgIdentifier"
            val orgId = "organizationId"
            val orgPublicKey = "organizationPublicKey"
            val userPrivateKey = "userPrivateKey"
            val userPublicKey = "userPublicKey"
            val userAdminReset = "userAdminReset"
            val orgAutoEnrollStatusResponse = OrganizationAutoEnrollStatusResponseJson(
                organizationId = orgId,
                isResetPasswordEnabled = false,
            )
            val orgKeysResponse = OrganizationKeysResponseJson(
                privateKey = "privateKey",
                publicKey = orgPublicKey,
            )
            val trustDeviceResponse = mockk<TrustDeviceResponse>()
            val registerTdeKeyResponse = RegisterTdeKeyResponse(
                privateKey = userPrivateKey,
                publicKey = userPublicKey,
                adminReset = userAdminReset,
                deviceKey = trustDeviceResponse,
            )
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            fakeAuthDiskSource.rememberedOrgIdentifier = orgIdentifier
            fakeAuthDiskSource.storeShouldTrustDevice(
                userId = USER_ID_1,
                shouldTrustDevice = shouldTrustDevice,
            )
            coEvery {
                organizationService.getOrganizationAutoEnrollStatus(orgIdentifier)
            } returns orgAutoEnrollStatusResponse.asSuccess()
            coEvery {
                organizationService.getOrganizationKeys(orgId)
            } returns orgKeysResponse.asSuccess()
            coEvery {
                authSdkSource.makeRegisterTdeKeysAndUnlockVault(
                    userId = USER_ID_1,
                    email = EMAIL,
                    orgPublicKey = orgPublicKey,
                    rememberDevice = shouldTrustDevice,
                )
            } returns registerTdeKeyResponse.asSuccess()
            coEvery {
                accountsService.createAccountKeys(
                    publicKey = userPublicKey,
                    encryptedPrivateKey = userPrivateKey,
                )
            } returns Unit.asSuccess()
            coEvery {
                organizationService.organizationResetPasswordEnroll(
                    organizationId = orgId,
                    userId = USER_ID_1,
                    passwordHash = null,
                    resetPasswordKey = userAdminReset,
                )
            } returns Unit.asSuccess()
            coEvery {
                trustedDeviceManager.trustThisDevice(
                    userId = USER_ID_1,
                    trustDeviceResponse = trustDeviceResponse,
                )
            } returns Unit.asSuccess()
            coEvery { vaultRepository.syncVaultState(userId = USER_ID_1) } just runs

            val result = repository.createNewSsoUser()

            fakeAuthDiskSource.assertPrivateKey(userId = USER_ID_1, privateKey = userPrivateKey)
            assertEquals(NewSsoUserResult.Success, result)
            coVerify(exactly = 1) {
                organizationService.getOrganizationAutoEnrollStatus(orgIdentifier)
                organizationService.getOrganizationKeys(orgId)
                authSdkSource.makeRegisterTdeKeysAndUnlockVault(
                    userId = USER_ID_1,
                    email = EMAIL,
                    orgPublicKey = orgPublicKey,
                    rememberDevice = shouldTrustDevice,
                )
                accountsService.createAccountKeys(
                    publicKey = userPublicKey,
                    encryptedPrivateKey = userPrivateKey,
                )
                organizationService.organizationResetPasswordEnroll(
                    organizationId = orgId,
                    userId = USER_ID_1,
                    passwordHash = null,
                    resetPasswordKey = userAdminReset,
                )
                trustedDeviceManager.trustThisDevice(
                    userId = USER_ID_1,
                    trustDeviceResponse = trustDeviceResponse,
                )
                vaultRepository.syncVaultState(userId = USER_ID_1)
            }
        }

    @Test
    fun `completeTdeLogin without active user fails`() = runTest {
        val requestPrivateKey = "requestPrivateKey"
        val asymmetricalKey = "asymmetricalKey"
        val result = repository.completeTdeLogin(
            requestPrivateKey = requestPrivateKey,
            asymmetricalKey = asymmetricalKey,
        )
        assertEquals(
            LoginResult.Error(errorMessage = null, error = NoActiveUserException()),
            result,
        )
    }

    @Test
    fun `completeTdeLogin without private key fails when EnrollAeadOnKeyRotation is disabled`() =
        runTest {
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            val requestPrivateKey = "requestPrivateKey"
            val asymmetricalKey = "asymmetricalKey"
            val result = repository.completeTdeLogin(
                requestPrivateKey = requestPrivateKey,
                asymmetricalKey = asymmetricalKey,
            )
            assertEquals(
                LoginResult.Error(
                    errorMessage = null,
                    error = MissingPropertyException("Private Key"),
                ),
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `completeTdeLogin without private key and account keys fails when EnrollAeadOnKeyRotation is enabled`() =
        runTest {
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            val requestPrivateKey = "requestPrivateKey"
            val asymmetricalKey = "asymmetricalKey"
            val result = repository.completeTdeLogin(
                requestPrivateKey = requestPrivateKey,
                asymmetricalKey = asymmetricalKey,
            )
            assertEquals(
                LoginResult.Error(
                    errorMessage = null,
                    error = MissingPropertyException("Private Key"),
                ),
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `completeTdeLogin without account keys unlocks vault with private key when EnrollAeadOnKeyRotation is enabled`() =
        runTest {
            val privateKey = "privateKey"
            val requestPrivateKey = "requestPrivateKey"
            val asymmetricalKey = "asymmetricalKey"
            val orgKeys = mapOf("orgId" to "orgKey")
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            fakeAuthDiskSource.storePrivateKey(userId = USER_ID_1, privateKey = privateKey)
            fakeAuthDiskSource.storeOrganizationKeys(userId = USER_ID_1, organizationKeys = orgKeys)
            fakeAuthDiskSource.storeAccountKeys(userId = USER_ID_1, accountKeys = null)
            coEvery {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = privateKey,
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = USER_ID_1,
                    email = SINGLE_USER_STATE_1.activeAccount.profile.email,
                    kdf = SINGLE_USER_STATE_1.activeAccount.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.AuthRequest(
                        requestPrivateKey = requestPrivateKey,
                        method = AuthRequestMethod.UserKey(protectedUserKey = asymmetricalKey),
                    ),
                    organizationKeys = orgKeys,
                )
            } returns VaultUnlockResult.Success
            coEvery { vaultRepository.syncIfNecessary() } just runs

            val result = repository.completeTdeLogin(
                requestPrivateKey = requestPrivateKey,
                asymmetricalKey = asymmetricalKey,
            )
            coVerify(exactly = 1) {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = privateKey,
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = USER_ID_1,
                    email = SINGLE_USER_STATE_1.activeAccount.profile.email,
                    kdf = SINGLE_USER_STATE_1.activeAccount.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.AuthRequest(
                        requestPrivateKey = requestPrivateKey,
                        method = AuthRequestMethod.UserKey(protectedUserKey = asymmetricalKey),
                    ),
                    organizationKeys = orgKeys,
                )
                vaultRepository.syncIfNecessary()
                settingsRepository.storeUserHasLoggedInValue(userId = USER_ID_1)
            }
            assertEquals(LoginResult.Success, result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `completeTdeLogin unlocks vault with account keys when EnrollAeadOnKeyRotation is enabled`() =
        runTest {
            val requestPrivateKey = "requestPrivateKey"
            val asymmetricKey = "asymmetricalKey"
            val privateKey = "privateKey"
            val accountKeys = createMockAccountKeysJson(number = 1)
            val orgKeys = mapOf("orgId" to "orgKey")
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            fakeAuthDiskSource.storePrivateKey(userId = USER_ID_1, privateKey = privateKey)
            fakeAuthDiskSource.storeAccountKeys(userId = USER_ID_1, accountKeys = accountKeys)
            fakeAuthDiskSource.storeOrganizationKeys(userId = USER_ID_1, organizationKeys = orgKeys)
            coEvery {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = accountKeys.publicKeyEncryptionKeyPair.wrappedPrivateKey,
                        securityState = accountKeys.securityState?.securityState,
                        signedPublicKey = accountKeys.publicKeyEncryptionKeyPair.signedPublicKey,
                        signingKey = accountKeys.signatureKeyPair?.wrappedSigningKey,
                    ),
                    userId = USER_ID_1,
                    email = SINGLE_USER_STATE_1.activeAccount.profile.email,
                    kdf = SINGLE_USER_STATE_1.activeAccount.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.AuthRequest(
                        requestPrivateKey = requestPrivateKey,
                        method = AuthRequestMethod.UserKey(protectedUserKey = asymmetricKey),
                    ),
                    organizationKeys = orgKeys,
                )
            } returns VaultUnlockResult.Success
            coEvery { vaultRepository.syncIfNecessary() } just runs

            val result = repository.completeTdeLogin(
                requestPrivateKey = requestPrivateKey,
                asymmetricalKey = asymmetricKey,
            )

            coVerify(exactly = 1) {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = accountKeys.publicKeyEncryptionKeyPair.wrappedPrivateKey,
                        securityState = accountKeys.securityState?.securityState,
                        signedPublicKey = accountKeys.publicKeyEncryptionKeyPair.signedPublicKey,
                        signingKey = accountKeys.signatureKeyPair?.wrappedSigningKey,
                    ),
                    userId = USER_ID_1,
                    email = SINGLE_USER_STATE_1.activeAccount.profile.email,
                    kdf = SINGLE_USER_STATE_1.activeAccount.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.AuthRequest(
                        requestPrivateKey = requestPrivateKey,
                        method = AuthRequestMethod.UserKey(protectedUserKey = asymmetricKey),
                    ),
                    organizationKeys = orgKeys,
                )
                vaultRepository.syncIfNecessary()
                settingsRepository.storeUserHasLoggedInValue(userId = USER_ID_1)
            }
            assertEquals(LoginResult.Success, result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `completeTdeLogin unlocks vault with private key when EnrollAeadOnKeyRotation is disabled`() =
        runTest {
            val requestPrivateKey = "requestPrivateKey"
            val asymmetricalKey = "asymmetricalKey"
            val privateKey = "privateKey"
            val orgKeys = mapOf("orgId" to "orgKey")
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            fakeAuthDiskSource.storePrivateKey(userId = USER_ID_1, privateKey = privateKey)
            fakeAuthDiskSource.storeOrganizationKeys(userId = USER_ID_1, organizationKeys = orgKeys)
            coEvery {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = privateKey,
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = USER_ID_1,
                    email = SINGLE_USER_STATE_1.activeAccount.profile.email,
                    kdf = SINGLE_USER_STATE_1.activeAccount.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.AuthRequest(
                        requestPrivateKey = requestPrivateKey,
                        method = AuthRequestMethod.UserKey(protectedUserKey = asymmetricalKey),
                    ),
                    organizationKeys = orgKeys,
                )
            } returns VaultUnlockResult.Success
            coEvery { vaultRepository.syncIfNecessary() } just runs

            val result = repository.completeTdeLogin(
                requestPrivateKey = requestPrivateKey,
                asymmetricalKey = asymmetricalKey,
            )

            coVerify(exactly = 1) {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = privateKey,
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = USER_ID_1,
                    email = SINGLE_USER_STATE_1.activeAccount.profile.email,
                    kdf = SINGLE_USER_STATE_1.activeAccount.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.AuthRequest(
                        requestPrivateKey = requestPrivateKey,
                        method = AuthRequestMethod.UserKey(protectedUserKey = asymmetricalKey),
                    ),
                    organizationKeys = orgKeys,
                )
                vaultRepository.syncIfNecessary()
                settingsRepository.storeUserHasLoggedInValue(userId = USER_ID_1)
            }
            assertEquals(LoginResult.Success, result)
        }

    @Test
    fun `completeTdeLogin where vault unlock fails should return LoginResult error`() = runTest {
        val requestPrivateKey = "requestPrivateKey"
        val asymmetricalKey = "asymmetricalKey"
        val privateKey = "privateKey"
        val orgKeys = mapOf("orgId" to "orgKey")
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        fakeAuthDiskSource.storePrivateKey(userId = USER_ID_1, privateKey = privateKey)
        fakeAuthDiskSource.storeOrganizationKeys(userId = USER_ID_1, organizationKeys = orgKeys)
        val error = Throwable("Fail")
        coEvery {
            vaultRepository.unlockVault(
                accountCryptographicState = createWrappedAccountCryptographicState(
                    privateKey = privateKey,
                    securityState = null,
                    signedPublicKey = null,
                    signingKey = null,
                ),
                userId = USER_ID_1,
                email = SINGLE_USER_STATE_1.activeAccount.profile.email,
                kdf = SINGLE_USER_STATE_1.activeAccount.profile.toSdkParams(),
                initUserCryptoMethod = InitUserCryptoMethod.AuthRequest(
                    requestPrivateKey = requestPrivateKey,
                    method = AuthRequestMethod.UserKey(protectedUserKey = asymmetricalKey),
                ),
                organizationKeys = orgKeys,
            )
        } returns VaultUnlockResult.AuthenticationError(message = null, error = error)
        coEvery { vaultRepository.syncIfNecessary() } just runs

        val result = repository.completeTdeLogin(
            requestPrivateKey = requestPrivateKey,
            asymmetricalKey = asymmetricalKey,
        )

        coVerify(exactly = 1) {
            vaultRepository.unlockVault(
                accountCryptographicState = createWrappedAccountCryptographicState(
                    privateKey = privateKey,
                    securityState = null,
                    signedPublicKey = null,
                    signingKey = null,
                ),
                userId = USER_ID_1,
                email = SINGLE_USER_STATE_1.activeAccount.profile.email,
                kdf = SINGLE_USER_STATE_1.activeAccount.profile.toSdkParams(),
                initUserCryptoMethod = InitUserCryptoMethod.AuthRequest(
                    requestPrivateKey = requestPrivateKey,
                    method = AuthRequestMethod.UserKey(protectedUserKey = asymmetricalKey),
                ),
                organizationKeys = orgKeys,
            )
        }
        coVerify(exactly = 0) {
            vaultRepository.syncIfNecessary()
            settingsRepository.storeUserHasLoggedInValue(userId = USER_ID_1)
        }
        assertEquals(LoginResult.Error(errorMessage = null, error = error), result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeTdeLogin with accountKeys with null nested fields should unlock vault with null properties`() =
        runTest {
            val requestPrivateKey = "requestPrivateKey"
            val asymmetricalKey = "asymmetricalKey"
            val accountKeys = ACCOUNT_KEYS_WITH_NULL_FIELDS
            val orgKeys = mapOf("orgId" to "orgKey")
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            fakeAuthDiskSource.storeAccountKeys(userId = USER_ID_1, accountKeys = accountKeys)
            fakeAuthDiskSource.storeOrganizationKeys(userId = USER_ID_1, organizationKeys = orgKeys)
            coEvery {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = accountKeys.publicKeyEncryptionKeyPair.wrappedPrivateKey,
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = USER_ID_1,
                    email = SINGLE_USER_STATE_1.activeAccount.profile.email,
                    kdf = SINGLE_USER_STATE_1.activeAccount.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.AuthRequest(
                        requestPrivateKey = requestPrivateKey,
                        method = AuthRequestMethod.UserKey(protectedUserKey = asymmetricalKey),
                    ),
                    organizationKeys = orgKeys,
                )
            } returns VaultUnlockResult.Success
            coEvery { vaultRepository.syncIfNecessary() } just runs

            val result = repository.completeTdeLogin(
                requestPrivateKey = requestPrivateKey,
                asymmetricalKey = asymmetricalKey,
            )

            coVerify(exactly = 1) {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = accountKeys.publicKeyEncryptionKeyPair.wrappedPrivateKey,
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = USER_ID_1,
                    email = SINGLE_USER_STATE_1.activeAccount.profile.email,
                    kdf = SINGLE_USER_STATE_1.activeAccount.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.AuthRequest(
                        requestPrivateKey = requestPrivateKey,
                        method = AuthRequestMethod.UserKey(protectedUserKey = asymmetricalKey),
                    ),
                    organizationKeys = orgKeys,
                )
                vaultRepository.syncIfNecessary()
                settingsRepository.storeUserHasLoggedInValue(userId = USER_ID_1)
            }
            assertEquals(LoginResult.Success, result)
        }

    @Test
    fun `login when pre login fails should return Error with no message`() = runTest {
        val error = RuntimeException()
        coEvery {
            identityService.preLogin(email = EMAIL)
        } returns error.asFailure()
        val result = repository.login(email = EMAIL, password = PASSWORD)
        assertEquals(LoginResult.Error(errorMessage = null, error = error), result)
        assertEquals(AuthState.Unauthenticated, repository.authStateFlow.value)
        coVerify { identityService.preLogin(email = EMAIL) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `login get token fails should return Error with no message when server is an official Bitwarden server`() =
        runTest {
            val error = RuntimeException()
            coEvery {
                identityService.preLogin(email = EMAIL)
            } returns PRE_LOGIN_SUCCESS.asSuccess()
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns error.asFailure()
            val result = repository.login(email = EMAIL, password = PASSWORD)
            assertEquals(LoginResult.Error(errorMessage = null, error = error), result)
            assertEquals(AuthState.Unauthenticated, repository.authStateFlow.value)
            coVerify { identityService.preLogin(email = EMAIL) }
            coVerify {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `login get token fails should return UnofficialServerError when server is an unofficial Bitwarden server`() =
        runTest {
            configDiskSource.serverConfig = SERVER_CONFIG_UNOFFICIAL
            coEvery {
                identityService.preLogin(email = EMAIL)
            } returns PRE_LOGIN_SUCCESS.asSuccess()
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns RuntimeException().asFailure()
            val result = repository.login(email = EMAIL, password = PASSWORD)
            assertEquals(LoginResult.UnofficialServerError, result)
            assertEquals(AuthState.Unauthenticated, repository.authStateFlow.value)
            coVerify { identityService.preLogin(email = EMAIL) }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `login get token fails should return CertificateError when SSLHandshakeException is thrown`() =
        runTest {
            coEvery {
                identityService.preLogin(email = EMAIL)
            } returns PRE_LOGIN_SUCCESS.asSuccess()
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns SSLHandshakeException("error").asFailure()
            val result = repository.login(email = EMAIL, password = PASSWORD)
            assertEquals(LoginResult.CertificateError, result)
            assertEquals(AuthState.Unauthenticated, repository.authStateFlow.value)
            coVerify { identityService.preLogin(email = EMAIL) }
        }

    @Test
    fun `prelogin fails should return CertificateError when SSLHandshakeException is thrown`() =
        runTest {
            coEvery {
                identityService.preLogin(email = EMAIL)
            } returns SSLHandshakeException("error").asFailure()
            val result = repository.login(email = EMAIL, password = PASSWORD)
            assertEquals(LoginResult.CertificateError, result)
            assertEquals(AuthState.Unauthenticated, repository.authStateFlow.value)
            coVerify { identityService.preLogin(email = EMAIL) }
        }

    @Test
    fun `login get token returns Invalid should return Error with correct message`() = runTest {
        coEvery {
            identityService.preLogin(email = EMAIL)
        } returns PRE_LOGIN_SUCCESS.asSuccess()
        coEvery {
            identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.MasterPassword(
                    username = EMAIL,
                    password = PASSWORD_HASH,
                ),
                uniqueAppId = UNIQUE_APP_ID,
            )
        } returns GetTokenResponseJson
            .Invalid(
                errorModel = GetTokenResponseJson.Invalid.ErrorModel(
                    errorMessage = "mock_error_message",
                ),
            )
            .asSuccess()

        val result = repository.login(email = EMAIL, password = PASSWORD)
        assertEquals(LoginResult.Error(errorMessage = "mock_error_message", error = null), result)
        assertEquals(AuthState.Unauthenticated, repository.authStateFlow.value)
        coVerify { identityService.preLogin(email = EMAIL) }
        coVerify {
            identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.MasterPassword(
                    username = EMAIL,
                    password = PASSWORD_HASH,
                ),
                uniqueAppId = UNIQUE_APP_ID,
            )
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `login get token should return InvalidType NewDeviceVerification when message is new device verification needed`() =
        runTest {
            coEvery {
                identityService.preLogin(email = EMAIL)
            } returns PRE_LOGIN_SUCCESS.asSuccess()
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns GetTokenResponseJson
                .Invalid(
                    errorModel = GetTokenResponseJson.Invalid.ErrorModel(
                        errorMessage = "new device verification required",
                    ),
                )
                .asSuccess()

            val result = repository.login(email = EMAIL, password = PASSWORD)
            assertEquals(
                LoginResult.NewDeviceVerification(errorMessage = "new device verification required"),
                result,
            )
            assertEquals(AuthState.Unauthenticated, repository.authStateFlow.value)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `login get token succeeds should return Success, unlockVault, update AuthState, update stored keys, and sync`() =
        runTest {
            val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS
            coEvery {
                identityService.preLogin(email = EMAIL)
            } returns PRE_LOGIN_SUCCESS.asSuccess()
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns successResponse.asSuccess()
            coEvery {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .wrappedPrivateKey,
                        securityState = successResponse.accountKeys
                            ?.securityState
                            ?.securityState,
                        signedPublicKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .signedPublicKey,
                        signingKey = successResponse.accountKeys
                            ?.signatureKeyPair
                            ?.wrappedSigningKey,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                        password = PASSWORD,
                        masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK,
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.Success
            coEvery { vaultRepository.syncIfNecessary() } just runs
            every {
                GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.toUserState(
                    previousUserState = null,
                    environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
                )
            } returns SINGLE_USER_STATE_1
            val result = repository.login(email = EMAIL, password = PASSWORD)
            assertEquals(LoginResult.Success, result)
            assertEquals(AuthState.Authenticated(ACCESS_TOKEN), repository.authStateFlow.value)
            coVerify { identityService.preLogin(email = EMAIL) }
            fakeAuthDiskSource.assertPrivateKey(
                userId = USER_ID_1,
                privateKey = "privateKey",
            )
            fakeAuthDiskSource.assertAccountKeys(
                userId = USER_ID_1,
                accountKeys = ACCOUNT_KEYS,
            )
            fakeAuthDiskSource.assertUserKey(
                userId = USER_ID_1,
                userKey = "key",
            )
            fakeAuthDiskSource.assertMasterPasswordHash(
                userId = USER_ID_1,
                passwordHash = PASSWORD_HASH,
            )
            coVerify {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .wrappedPrivateKey,
                        securityState = successResponse.accountKeys
                            ?.securityState
                            ?.securityState,
                        signedPublicKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .signedPublicKey,
                        signingKey = successResponse.accountKeys
                            ?.signatureKeyPair
                            ?.wrappedSigningKey,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                        password = PASSWORD,
                        masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK,
                    ),
                    organizationKeys = null,
                )
                vaultRepository.syncIfNecessary()
                settingsRepository.storeUserHasLoggedInValue(userId = USER_ID_1)
            }
            assertEquals(
                SINGLE_USER_STATE_1,
                fakeAuthDiskSource.userState,
            )
            verify(exactly = 1) {
                userStateManager.hasPendingAccountAddition = false
                settingsRepository.setDefaultsIfNecessary(userId = USER_ID_1)
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `login get token succeeds with accountKeys with null nested fields should unlock vault with null properties`() =
        runTest {
            val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.copy(
                accountKeys = ACCOUNT_KEYS_WITH_NULL_FIELDS,
            )
            coEvery {
                identityService.preLogin(email = EMAIL)
            } returns PRE_LOGIN_SUCCESS.asSuccess()
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns successResponse.asSuccess()
            coEvery {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .wrappedPrivateKey,
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                        password = PASSWORD,
                        masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK,
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.Success
            coEvery { vaultRepository.syncIfNecessary() } just runs
            every {
                successResponse.toUserState(
                    previousUserState = null,
                    environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
                )
            } returns SINGLE_USER_STATE_1
            val result = repository.login(email = EMAIL, password = PASSWORD)
            assertEquals(LoginResult.Success, result)
            assertEquals(AuthState.Authenticated(ACCESS_TOKEN), repository.authStateFlow.value)
            coVerify { identityService.preLogin(email = EMAIL) }
            fakeAuthDiskSource.assertPrivateKey(
                userId = USER_ID_1,
                privateKey = "privateKey",
            )
            fakeAuthDiskSource.assertAccountKeys(
                userId = USER_ID_1,
                accountKeys = ACCOUNT_KEYS_WITH_NULL_FIELDS,
            )
            fakeAuthDiskSource.assertUserKey(
                userId = USER_ID_1,
                userKey = "key",
            )
            fakeAuthDiskSource.assertMasterPasswordHash(
                userId = USER_ID_1,
                passwordHash = PASSWORD_HASH,
            )
            coVerify {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .wrappedPrivateKey,
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                        password = PASSWORD,
                        masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK,
                    ),
                    organizationKeys = null,
                )
                vaultRepository.syncIfNecessary()
                settingsRepository.storeUserHasLoggedInValue(userId = USER_ID_1)
            }
            assertEquals(
                SINGLE_USER_STATE_1,
                fakeAuthDiskSource.userState,
            )
            verify(exactly = 1) {
                userStateManager.hasPendingAccountAddition = false
                settingsRepository.setDefaultsIfNecessary(userId = USER_ID_1)
            }
        }

    @Test
    fun `login should return Error result when get token succeeds but unlock vault fails`() =
        runTest {
            val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS
            val expectedErrorMessage = "crypto key failure"

            coEvery {
                identityService.preLogin(email = EMAIL)
            } returns PRE_LOGIN_SUCCESS.asSuccess()
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns successResponse.asSuccess()
            val error = Throwable("Fail")
            coEvery {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .wrappedPrivateKey,
                        securityState = successResponse.accountKeys
                            ?.securityState
                            ?.securityState,
                        signedPublicKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .signedPublicKey,
                        signingKey = successResponse.accountKeys
                            ?.signatureKeyPair
                            ?.wrappedSigningKey,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                        password = PASSWORD,
                        masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK,
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.AuthenticationError(
                message = expectedErrorMessage,
                error = error,
            )
            coEvery { vaultRepository.syncIfNecessary() } just runs
            every {
                GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.toUserState(
                    previousUserState = null,
                    environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
                )
            } returns SINGLE_USER_STATE_1
            val result = repository.login(email = EMAIL, password = PASSWORD)
            assertEquals(
                LoginResult.Error(errorMessage = expectedErrorMessage, error = error),
                result,
            )
            assertEquals(AuthState.Unauthenticated, repository.authStateFlow.value)
            coVerify { identityService.preLogin(email = EMAIL) }
            fakeAuthDiskSource.assertPrivateKey(
                userId = USER_ID_1,
                privateKey = null,
            )
            fakeAuthDiskSource.assertAccountKeys(
                userId = USER_ID_1,
                accountKeys = null,
            )
            fakeAuthDiskSource.assertUserKey(
                userId = USER_ID_1,
                userKey = null,
            )
            fakeAuthDiskSource.assertMasterPasswordHash(
                userId = USER_ID_1,
                passwordHash = null,
            )
            coVerify(exactly = 1) {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )

                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .wrappedPrivateKey,
                        securityState = successResponse.accountKeys
                            ?.securityState
                            ?.securityState,
                        signedPublicKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .signedPublicKey,
                        signingKey = successResponse.accountKeys
                            ?.signatureKeyPair
                            ?.wrappedSigningKey,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                        password = PASSWORD,
                        masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK,
                    ),
                    organizationKeys = null,
                )
            }

            coVerify(exactly = 0) {
                vaultRepository.syncIfNecessary()
                settingsRepository.setDefaultsIfNecessary(userId = USER_ID_1)
                settingsRepository.storeUserHasLoggedInValue(userId = USER_ID_1)
            }

            assertEquals(
                null,
                fakeAuthDiskSource.userState,
            )
        }

    @Test
    @Suppress("MaxLineLength")
    fun `login get token succeeds with null keys and hasMasterPassword false should not call unlockVault`() =
        runTest {
            val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.copy(
                key = null,
                privateKey = null,
                userDecryptionOptions = UserDecryptionOptionsJson(
                    hasMasterPassword = false,
                    keyConnectorUserDecryptionOptions = null,
                    trustedDeviceUserDecryptionOptions = null,
                    masterPasswordUnlock = null,
                ),
            )
            coEvery {
                identityService.preLogin(email = EMAIL)
            } returns PRE_LOGIN_SUCCESS.asSuccess()
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns successResponse.asSuccess()
            coEvery { vaultRepository.syncIfNecessary() } just runs
            every {
                successResponse.toUserState(
                    previousUserState = null,
                    environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
                )
            } returns SINGLE_USER_STATE_1
            val result = repository.login(
                email = EMAIL,
                password = PASSWORD,
            )
            assertEquals(LoginResult.Success, result)
            assertEquals(AuthState.Authenticated(ACCESS_TOKEN), repository.authStateFlow.value)
            coVerify { identityService.preLogin(email = EMAIL) }
            fakeAuthDiskSource.assertMasterPasswordHash(
                userId = USER_ID_1,
                passwordHash = PASSWORD_HASH,
            )
            coVerify {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
                vaultRepository.syncIfNecessary()
                settingsRepository.storeUserHasLoggedInValue(userId = USER_ID_1)
            }
            assertEquals(
                SINGLE_USER_STATE_1,
                fakeAuthDiskSource.userState,
            )
            verify(exactly = 1) {
                userStateManager.hasPendingAccountAddition = false
                settingsRepository.setDefaultsIfNecessary(userId = USER_ID_1)
            }
            coVerify(exactly = 0) {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .wrappedPrivateKey,
                        securityState = successResponse.accountKeys
                            ?.securityState
                            ?.securityState,
                        signedPublicKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .signedPublicKey,
                        signingKey = successResponse.accountKeys
                            ?.signatureKeyPair
                            ?.wrappedSigningKey,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = any(),
                    organizationKeys = null,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `login get token succeeds when there is an existing user should switch to the new logged in user and lock the old user's vault`() =
        runTest {
            // Ensure the initial state for User 2 with a account addition
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_2
            repository.hasPendingAccountAddition = true

            // Set up login for User 1
            val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS
            coEvery {
                identityService.preLogin(email = EMAIL)
            } returns PRE_LOGIN_SUCCESS.asSuccess()
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns successResponse.asSuccess()
            coEvery {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .wrappedPrivateKey,
                        securityState = successResponse.accountKeys
                            ?.securityState
                            ?.securityState,
                        signedPublicKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .signedPublicKey,
                        signingKey = successResponse.accountKeys
                            ?.signatureKeyPair
                            ?.wrappedSigningKey,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                        password = PASSWORD,
                        masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK,
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.Success
            coEvery { vaultRepository.syncIfNecessary() } just runs
            coEvery {
                vaultSdkSource.makeUpdateKdf(
                    userId = any(),
                    password = any(),
                    kdf = any(),
                )
            } returns UPDATE_KDF_RESPONSE.asSuccess()
            coEvery {
                accountsService.updateKdf(
                    body = any(),
                )
            } returns Unit.asSuccess()
            every {
                GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.toUserState(
                    previousUserState = SINGLE_USER_STATE_2,
                    environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
                )
            } returns MULTI_USER_STATE

            val result = repository.login(email = EMAIL, password = PASSWORD)

            assertEquals(LoginResult.Success, result)
            assertEquals(AuthState.Authenticated(ACCESS_TOKEN), repository.authStateFlow.value)
            coVerify { identityService.preLogin(email = EMAIL) }
            fakeAuthDiskSource.assertPrivateKey(
                userId = USER_ID_1,
                privateKey = "privateKey",
            )
            fakeAuthDiskSource.assertAccountKeys(
                userId = USER_ID_1,
                accountKeys = ACCOUNT_KEYS,
            )
            fakeAuthDiskSource.assertUserKey(
                userId = USER_ID_1,
                userKey = "key",
            )
            coVerify {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .wrappedPrivateKey,
                        securityState = successResponse.accountKeys
                            ?.securityState
                            ?.securityState,
                        signedPublicKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .signedPublicKey,
                        signingKey = successResponse.accountKeys
                            ?.signatureKeyPair
                            ?.wrappedSigningKey,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                        password = PASSWORD,
                        masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK,
                    ),
                    organizationKeys = null,
                )
                vaultRepository.syncIfNecessary()
                settingsRepository.storeUserHasLoggedInValue(userId = USER_ID_1)
            }
            assertEquals(
                MULTI_USER_STATE,
                fakeAuthDiskSource.userState,
            )
            assertFalse(repository.hasPendingAccountAddition)
            verify(exactly = 1) {
                userStateManager.hasPendingAccountAddition
                userStateManager.hasPendingAccountAddition = true
                settingsRepository.setDefaultsIfNecessary(userId = USER_ID_1)
            }
        }

    @Test
    fun `login get token returns two factor request should return TwoFactorRequired`() = runTest {
        coEvery { identityService.preLogin(EMAIL) } returns PRE_LOGIN_SUCCESS.asSuccess()
        coEvery {
            identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.MasterPassword(
                    username = EMAIL,
                    password = PASSWORD_HASH,
                ),
                uniqueAppId = UNIQUE_APP_ID,
            )
        } returns GetTokenResponseJson
            .TwoFactorRequired(
                authMethodsData = TWO_FACTOR_AUTH_METHODS_DATA,
                ssoToken = null,
                twoFactorProviders = null,
            )
            .asSuccess()
        val result = repository.login(email = EMAIL, password = PASSWORD)
        assertEquals(LoginResult.TwoFactorRequired, result)
        assertEquals(
            repository.twoFactorResponse,
            GetTokenResponseJson.TwoFactorRequired(
                authMethodsData = TWO_FACTOR_AUTH_METHODS_DATA,
                twoFactorProviders = null,
                ssoToken = null,
            ),
        )
        assertEquals(AuthState.Unauthenticated, repository.authStateFlow.value)
        coVerify { identityService.preLogin(email = EMAIL) }
        coVerify {
            identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.MasterPassword(
                    username = EMAIL,
                    password = PASSWORD_HASH,
                ),
                uniqueAppId = UNIQUE_APP_ID,
            )
        }
    }

    @Test
    fun `login two factor with remember saves two factor auth token`() = runTest {
        // Attempt a normal login with a two factor error first, so that the auth
        // data will be cached.
        coEvery { identityService.preLogin(EMAIL) } returns PRE_LOGIN_SUCCESS.asSuccess()
        coEvery {
            identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.MasterPassword(
                    username = EMAIL,
                    password = PASSWORD_HASH,
                ),
                uniqueAppId = UNIQUE_APP_ID,
            )
        } returns GetTokenResponseJson
            .TwoFactorRequired(
                authMethodsData = TWO_FACTOR_AUTH_METHODS_DATA,
                ssoToken = null,
                twoFactorProviders = null,
            )
            .asSuccess()
        val firstResult = repository.login(email = EMAIL, password = PASSWORD)
        assertEquals(LoginResult.TwoFactorRequired, firstResult)
        coVerify { identityService.preLogin(email = EMAIL) }
        coVerify {
            identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.MasterPassword(
                    username = EMAIL,
                    password = PASSWORD_HASH,
                ),
                uniqueAppId = UNIQUE_APP_ID,
            )
        }

        // Login with two factor data.
        val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.copy(
            twoFactorToken = "twoFactorTokenToStore",
        )
        coEvery {
            identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.MasterPassword(
                    username = EMAIL,
                    password = PASSWORD_HASH,
                ),
                uniqueAppId = UNIQUE_APP_ID,
                twoFactorData = TWO_FACTOR_DATA,
            )
        } returns successResponse.asSuccess()
        coEvery {
            vaultRepository.unlockVault(
                accountCryptographicState = createWrappedAccountCryptographicState(
                    privateKey = successResponse.accountKeys!!
                        .publicKeyEncryptionKeyPair
                        .wrappedPrivateKey,
                    securityState = successResponse.accountKeys
                        ?.securityState
                        ?.securityState,
                    signedPublicKey = successResponse.accountKeys!!
                        .publicKeyEncryptionKeyPair
                        .signedPublicKey,
                    signingKey = successResponse.accountKeys
                        ?.signatureKeyPair
                        ?.wrappedSigningKey,
                ),
                userId = USER_ID_1,
                email = EMAIL,
                kdf = ACCOUNT_1.profile.toSdkParams(),
                initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                    password = PASSWORD,
                    masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK,
                ),
                organizationKeys = null,
            )
        } returns VaultUnlockResult.Success
        coEvery { vaultRepository.syncIfNecessary() } just runs
        every {
            successResponse.toUserState(
                previousUserState = null,
                environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
            )
        } returns SINGLE_USER_STATE_1
        val finalResult = repository.login(
            email = EMAIL,
            password = PASSWORD,
            twoFactorData = TWO_FACTOR_DATA,
            orgIdentifier = null,
        )
        assertEquals(LoginResult.Success, finalResult)
        assertNull(repository.twoFactorResponse)
        fakeAuthDiskSource.assertTwoFactorToken(
            email = EMAIL,
            twoFactorToken = "twoFactorTokenToStore",
        )
        verify(exactly = 1) {
            userStateManager.hasPendingAccountAddition = false
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `login two factor should return Error result when get token succeeds but unlock vault fails`() =
        runTest {
            val twoFactorResponse = GetTokenResponseJson.TwoFactorRequired(
                authMethodsData = TWO_FACTOR_AUTH_METHODS_DATA,
                ssoToken = null,
                twoFactorProviders = null,
            )
            // Attempt a normal login with a two factor error first, so that the auth
            // data will be cached.
            coEvery { identityService.preLogin(EMAIL) } returns PRE_LOGIN_SUCCESS.asSuccess()
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns twoFactorResponse.asSuccess()

            val firstResult = repository.login(
                email = EMAIL,
                password = PASSWORD,
            )

            assertEquals(LoginResult.TwoFactorRequired, firstResult)
            coVerify { identityService.preLogin(email = EMAIL) }
            coVerify {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            }

            // Login with two factor data.
            val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.copy(
                twoFactorToken = "twoFactorTokenToStore",
            )
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                    twoFactorData = TWO_FACTOR_DATA,
                )
            } returns successResponse.asSuccess()
            val error = Throwable("Fail")
            coEvery {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .wrappedPrivateKey,
                        securityState = successResponse.accountKeys
                            ?.securityState
                            ?.securityState,
                        signedPublicKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .signedPublicKey,
                        signingKey = successResponse.accountKeys
                            ?.signatureKeyPair
                            ?.wrappedSigningKey,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                        password = PASSWORD,
                        masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK,
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.InvalidStateError(error = error)
            every {
                successResponse.toUserState(
                    previousUserState = null,
                    environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
                )
            } returns SINGLE_USER_STATE_1
            val finalResult = repository.login(
                email = EMAIL,
                password = PASSWORD,
                twoFactorData = TWO_FACTOR_DATA,
                orgIdentifier = null,
            )
            assertEquals(LoginResult.Error(errorMessage = null, error = error), finalResult)
            assertEquals(twoFactorResponse, repository.twoFactorResponse)
            fakeAuthDiskSource.assertTwoFactorToken(
                email = EMAIL,
                twoFactorToken = null,
            )

            coVerify(exactly = 0) {
                vaultRepository.syncIfNecessary()
                settingsRepository.storeUserHasLoggedInValue(userId = USER_ID_1)
            }
        }

    @Test
    fun `login uses remembered two factor tokens`() = runTest {
        fakeAuthDiskSource.storeTwoFactorToken(EMAIL, "storedTwoFactorToken")
        val rememberedTwoFactorData = TwoFactorDataModel(
            code = "storedTwoFactorToken",
            method = TwoFactorAuthMethod.REMEMBER.value.toString(),
            remember = false,
        )
        val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS
        coEvery {
            identityService.preLogin(email = EMAIL)
        } returns PRE_LOGIN_SUCCESS.asSuccess()
        coEvery {
            identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.MasterPassword(
                    username = EMAIL,
                    password = PASSWORD_HASH,
                ),
                uniqueAppId = UNIQUE_APP_ID,
                twoFactorData = rememberedTwoFactorData,
            )
        } returns successResponse.asSuccess()
        coEvery {
            vaultRepository.unlockVault(
                accountCryptographicState = createWrappedAccountCryptographicState(
                    privateKey = successResponse.accountKeys!!
                        .publicKeyEncryptionKeyPair
                        .wrappedPrivateKey,
                    securityState = successResponse.accountKeys
                        ?.securityState
                        ?.securityState,
                    signedPublicKey = successResponse.accountKeys!!
                        .publicKeyEncryptionKeyPair
                        .signedPublicKey,
                    signingKey = successResponse.accountKeys
                        ?.signatureKeyPair
                        ?.wrappedSigningKey,
                ),
                userId = USER_ID_1,
                email = EMAIL,
                kdf = ACCOUNT_1.profile.toSdkParams(),
                initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                    password = PASSWORD,
                    masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK,
                ),
                organizationKeys = null,
            )
        } returns VaultUnlockResult.Success
        coEvery { vaultRepository.syncIfNecessary() } just runs
        every {
            GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.toUserState(
                previousUserState = null,
                environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
            )
        } returns SINGLE_USER_STATE_1
        val result = repository.login(email = EMAIL, password = PASSWORD)
        assertEquals(LoginResult.Success, result)
        assertEquals(AuthState.Authenticated(ACCESS_TOKEN), repository.authStateFlow.value)
        coVerify { identityService.preLogin(email = EMAIL) }
        fakeAuthDiskSource.assertPrivateKey(
            userId = USER_ID_1,
            privateKey = "privateKey",
        )
        fakeAuthDiskSource.assertAccountKeys(
            userId = USER_ID_1,
            accountKeys = ACCOUNT_KEYS,
        )
        fakeAuthDiskSource.assertUserKey(
            userId = USER_ID_1,
            userKey = "key",
        )
        coVerify {
            identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.MasterPassword(
                    username = EMAIL,
                    password = PASSWORD_HASH,
                ),
                uniqueAppId = UNIQUE_APP_ID,
                twoFactorData = rememberedTwoFactorData,
            )
            vaultRepository.unlockVault(
                accountCryptographicState = createWrappedAccountCryptographicState(
                    privateKey = successResponse.accountKeys!!
                        .publicKeyEncryptionKeyPair
                        .wrappedPrivateKey,
                    securityState = successResponse.accountKeys
                        ?.securityState
                        ?.securityState,
                    signedPublicKey = successResponse.accountKeys!!
                        .publicKeyEncryptionKeyPair
                        .signedPublicKey,
                    signingKey = successResponse.accountKeys
                        ?.signatureKeyPair
                        ?.wrappedSigningKey,
                ),
                userId = USER_ID_1,
                email = EMAIL,
                kdf = ACCOUNT_1.profile.toSdkParams(),
                initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                    password = PASSWORD,
                    masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK,
                ),
                organizationKeys = null,
            )
            vaultRepository.syncIfNecessary()
        }
        assertEquals(
            SINGLE_USER_STATE_1,
            fakeAuthDiskSource.userState,
        )
        verify(exactly = 1) {
            userStateManager.hasPendingAccountAddition = false
            settingsRepository.setDefaultsIfNecessary(userId = USER_ID_1)
            settingsRepository.storeUserHasLoggedInValue(userId = USER_ID_1)
        }
    }

    @Test
    fun `login two factor returns error if no cached auth data`() = runTest {
        val result = repository.login(
            email = EMAIL,
            password = PASSWORD,
            twoFactorData = TWO_FACTOR_DATA,
            orgIdentifier = null,
        )
        assertEquals(
            LoginResult.Error(
                errorMessage = null,
                error = MissingPropertyException("Identity Token Auth Model"),
            ),
            result,
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `login get token returns invalid request should return EncryptionKeyMigrationRequired`() =
        runTest {
            coEvery { identityService.preLogin(EMAIL) } returns PRE_LOGIN_SUCCESS.asSuccess()
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns GetTokenResponseJson
                .Invalid(
                    errorModel = GetTokenResponseJson.Invalid.ErrorModel(
                        errorMessage =
                            "Encryption key migration is required. Please log in to the web vault at",
                    ),
                )
                .asSuccess()

            val result = repository.login(email = EMAIL, password = PASSWORD)
            assertEquals(LoginResult.EncryptionKeyMigrationRequired, result)
            assertEquals(AuthState.Unauthenticated, repository.authStateFlow.value)
            coVerify {
                identityService.preLogin(email = EMAIL)
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            }
        }

    @Test
    fun `login with device get token fails should return Error with no message`() = runTest {
        val error = Throwable("Fail!")
        coEvery {
            identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.AuthRequest(
                    username = EMAIL,
                    authRequestId = DEVICE_REQUEST_ID,
                    accessCode = DEVICE_ACCESS_CODE,
                ),
                uniqueAppId = UNIQUE_APP_ID,
            )
        } returns error.asFailure()
        val result = repository.login(
            email = EMAIL,
            requestId = DEVICE_REQUEST_ID,
            accessCode = DEVICE_ACCESS_CODE,
            asymmetricalKey = DEVICE_ASYMMETRICAL_KEY,
            requestPrivateKey = DEVICE_REQUEST_PRIVATE_KEY,
            masterPasswordHash = PASSWORD_HASH,
        )
        assertEquals(LoginResult.Error(errorMessage = null, error = error), result)
        assertEquals(AuthState.Unauthenticated, repository.authStateFlow.value)
        coVerify {
            identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.AuthRequest(
                    username = EMAIL,
                    authRequestId = DEVICE_REQUEST_ID,
                    accessCode = DEVICE_ACCESS_CODE,
                ),
                uniqueAppId = UNIQUE_APP_ID,
            )
        }
    }

    @Test
    fun `login with device get token returns Invalid should return Error with correct message`() =
        runTest {
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.AuthRequest(
                        username = EMAIL,
                        authRequestId = DEVICE_REQUEST_ID,
                        accessCode = DEVICE_ACCESS_CODE,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns GetTokenResponseJson
                .Invalid(
                    errorModel = GetTokenResponseJson.Invalid.ErrorModel(
                        errorMessage = "mock_error_message",
                    ),
                )
                .asSuccess()

            val result = repository.login(
                email = EMAIL,
                requestId = DEVICE_REQUEST_ID,
                accessCode = DEVICE_ACCESS_CODE,
                asymmetricalKey = DEVICE_ASYMMETRICAL_KEY,
                requestPrivateKey = DEVICE_REQUEST_PRIVATE_KEY,
                masterPasswordHash = PASSWORD_HASH,
            )
            assertEquals(
                LoginResult.Error(errorMessage = "mock_error_message", error = null),
                result,
            )
            assertEquals(AuthState.Unauthenticated, repository.authStateFlow.value)
            coVerify {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.AuthRequest(
                        username = EMAIL,
                        authRequestId = DEVICE_REQUEST_ID,
                        accessCode = DEVICE_ACCESS_CODE,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `login with device get token succeeds should return Success, update AuthState, update stored keys, and sync with MasteryKey`() =
        runTest {
            val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.AuthRequest(
                        username = EMAIL,
                        authRequestId = DEVICE_REQUEST_ID,
                        accessCode = DEVICE_ACCESS_CODE,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns successResponse.asSuccess()
            coEvery { vaultRepository.syncIfNecessary() } just runs
            every {
                GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.toUserState(
                    previousUserState = null,
                    environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
                )
            } returns SINGLE_USER_STATE_1
            coEvery {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .wrappedPrivateKey,
                        securityState = successResponse.accountKeys
                            ?.securityState
                            ?.securityState,
                        signedPublicKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .signedPublicKey,
                        signingKey = successResponse.accountKeys
                            ?.signatureKeyPair
                            ?.wrappedSigningKey,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.AuthRequest(
                        requestPrivateKey = DEVICE_REQUEST_PRIVATE_KEY,
                        method = AuthRequestMethod.MasterKey(
                            authRequestKey = successResponse.key!!,
                            protectedMasterKey = DEVICE_ASYMMETRICAL_KEY,
                        ),
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.Success
            val result = repository.login(
                email = EMAIL,
                requestId = DEVICE_REQUEST_ID,
                accessCode = DEVICE_ACCESS_CODE,
                asymmetricalKey = DEVICE_ASYMMETRICAL_KEY,
                requestPrivateKey = DEVICE_REQUEST_PRIVATE_KEY,
                masterPasswordHash = PASSWORD_HASH,
            )
            assertEquals(LoginResult.Success, result)
            assertEquals(AuthState.Authenticated(ACCESS_TOKEN), repository.authStateFlow.value)
            fakeAuthDiskSource.assertPrivateKey(
                userId = USER_ID_1,
                privateKey = "privateKey",
            )
            fakeAuthDiskSource.assertAccountKeys(
                userId = USER_ID_1,
                accountKeys = ACCOUNT_KEYS,
            )
            fakeAuthDiskSource.assertUserKey(
                userId = USER_ID_1,
                userKey = "key",
            )
            coVerify {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.AuthRequest(
                        username = EMAIL,
                        authRequestId = DEVICE_REQUEST_ID,
                        accessCode = DEVICE_ACCESS_CODE,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
                vaultRepository.syncIfNecessary()
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .wrappedPrivateKey,
                        securityState = successResponse.accountKeys
                            ?.securityState
                            ?.securityState,
                        signedPublicKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .signedPublicKey,
                        signingKey = successResponse.accountKeys
                            ?.signatureKeyPair
                            ?.wrappedSigningKey,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.AuthRequest(
                        requestPrivateKey = DEVICE_REQUEST_PRIVATE_KEY,
                        method = AuthRequestMethod.MasterKey(
                            authRequestKey = successResponse.key!!,
                            protectedMasterKey = DEVICE_ASYMMETRICAL_KEY,
                        ),
                    ),
                    organizationKeys = null,
                )
                settingsRepository.setDefaultsIfNecessary(userId = USER_ID_1)
                settingsRepository.storeUserHasLoggedInValue(userId = USER_ID_1)
            }
            assertEquals(
                SINGLE_USER_STATE_1,
                fakeAuthDiskSource.userState,
            )
            verify(exactly = 1) {
                userStateManager.hasPendingAccountAddition = false
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `login with device should return Error result when get token succeeds but unlock vault fails`() =
        runTest {
            val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.AuthRequest(
                        username = EMAIL,
                        authRequestId = DEVICE_REQUEST_ID,
                        accessCode = DEVICE_ACCESS_CODE,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns successResponse.asSuccess()
            coEvery { vaultRepository.syncIfNecessary() } just runs
            every {
                GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.toUserState(
                    previousUserState = null,
                    environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
                )
            } returns SINGLE_USER_STATE_1
            coEvery {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .wrappedPrivateKey,
                        securityState = successResponse.accountKeys
                            ?.securityState
                            ?.securityState,
                        signedPublicKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .signedPublicKey,
                        signingKey = successResponse.accountKeys
                            ?.signatureKeyPair
                            ?.wrappedSigningKey,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.AuthRequest(
                        requestPrivateKey = DEVICE_REQUEST_PRIVATE_KEY,
                        method = AuthRequestMethod.MasterKey(
                            authRequestKey = successResponse.key!!,
                            protectedMasterKey = DEVICE_ASYMMETRICAL_KEY,
                        ),
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.Success
            val result = repository.login(
                email = EMAIL,
                requestId = DEVICE_REQUEST_ID,
                accessCode = DEVICE_ACCESS_CODE,
                asymmetricalKey = DEVICE_ASYMMETRICAL_KEY,
                requestPrivateKey = DEVICE_REQUEST_PRIVATE_KEY,
                masterPasswordHash = PASSWORD_HASH,
            )
            assertEquals(LoginResult.Success, result)
            assertEquals(AuthState.Authenticated(ACCESS_TOKEN), repository.authStateFlow.value)
            fakeAuthDiskSource.assertPrivateKey(
                userId = USER_ID_1,
                privateKey = "privateKey",
            )
            fakeAuthDiskSource.assertAccountKeys(
                userId = USER_ID_1,
                accountKeys = ACCOUNT_KEYS,
            )
            fakeAuthDiskSource.assertUserKey(
                userId = USER_ID_1,
                userKey = "key",
            )
            coVerify {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.AuthRequest(
                        username = EMAIL,
                        authRequestId = DEVICE_REQUEST_ID,
                        accessCode = DEVICE_ACCESS_CODE,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
                vaultRepository.syncIfNecessary()
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .wrappedPrivateKey,
                        securityState = successResponse.accountKeys
                            ?.securityState
                            ?.securityState,
                        signedPublicKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .signedPublicKey,
                        signingKey = successResponse.accountKeys
                            ?.signatureKeyPair
                            ?.wrappedSigningKey,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.AuthRequest(
                        requestPrivateKey = DEVICE_REQUEST_PRIVATE_KEY,
                        method = AuthRequestMethod.MasterKey(
                            authRequestKey = successResponse.key!!,
                            protectedMasterKey = DEVICE_ASYMMETRICAL_KEY,
                        ),
                    ),
                    organizationKeys = null,
                )
            }
            assertEquals(
                SINGLE_USER_STATE_1,
                fakeAuthDiskSource.userState,
            )
            verify(exactly = 1) {
                userStateManager.hasPendingAccountAddition = false
                settingsRepository.setDefaultsIfNecessary(userId = USER_ID_1)
            }
        }

    @Test
    fun `login with device get token returns two factor request should return TwoFactorRequired`() =
        runTest {
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.AuthRequest(
                        username = EMAIL,
                        authRequestId = DEVICE_REQUEST_ID,
                        accessCode = DEVICE_ACCESS_CODE,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns GetTokenResponseJson
                .TwoFactorRequired(
                    authMethodsData = TWO_FACTOR_AUTH_METHODS_DATA,
                    twoFactorProviders = null,
                    ssoToken = null,
                )
                .asSuccess()
            val result = repository.login(
                email = EMAIL,
                requestId = DEVICE_REQUEST_ID,
                accessCode = DEVICE_ACCESS_CODE,
                asymmetricalKey = DEVICE_ASYMMETRICAL_KEY,
                requestPrivateKey = DEVICE_REQUEST_PRIVATE_KEY,
                masterPasswordHash = PASSWORD_HASH,
            )
            assertEquals(LoginResult.TwoFactorRequired, result)
            assertEquals(
                repository.twoFactorResponse,
                GetTokenResponseJson.TwoFactorRequired(
                    authMethodsData = TWO_FACTOR_AUTH_METHODS_DATA,
                    twoFactorProviders = null,
                    ssoToken = null,
                ),
            )
            assertEquals(AuthState.Unauthenticated, repository.authStateFlow.value)
            coVerify {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.AuthRequest(
                        username = EMAIL,
                        authRequestId = DEVICE_REQUEST_ID,
                        accessCode = DEVICE_ACCESS_CODE,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            }
        }

    @Test
    fun `login with device two factor with remember saves two factor auth token`() = runTest {
        // Attempt a normal login with a two factor error first, so that the auth
        // data will be cached.
        coEvery {
            identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.AuthRequest(
                    username = EMAIL,
                    authRequestId = DEVICE_REQUEST_ID,
                    accessCode = DEVICE_ACCESS_CODE,
                ),
                uniqueAppId = UNIQUE_APP_ID,
            )
        } returns GetTokenResponseJson
            .TwoFactorRequired(
                authMethodsData = TWO_FACTOR_AUTH_METHODS_DATA,
                twoFactorProviders = null,
                ssoToken = null,
            )
            .asSuccess()
        val firstResult = repository.login(
            email = EMAIL,
            requestId = DEVICE_REQUEST_ID,
            accessCode = DEVICE_ACCESS_CODE,
            asymmetricalKey = DEVICE_ASYMMETRICAL_KEY,
            requestPrivateKey = DEVICE_REQUEST_PRIVATE_KEY,
            masterPasswordHash = PASSWORD_HASH,
        )
        assertEquals(LoginResult.TwoFactorRequired, firstResult)
        coVerify {
            identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.AuthRequest(
                    username = EMAIL,
                    authRequestId = DEVICE_REQUEST_ID,
                    accessCode = DEVICE_ACCESS_CODE,
                ),
                uniqueAppId = UNIQUE_APP_ID,
            )
        }

        // Login with two factor data.
        val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.copy(
            twoFactorToken = "twoFactorTokenToStore",
        )
        coEvery {
            identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.AuthRequest(
                    username = EMAIL,
                    authRequestId = DEVICE_REQUEST_ID,
                    accessCode = DEVICE_ACCESS_CODE,
                ),
                uniqueAppId = UNIQUE_APP_ID,
                twoFactorData = TWO_FACTOR_DATA,
            )
        } returns successResponse.asSuccess()
        coEvery { vaultRepository.syncIfNecessary() } just runs
        every {
            successResponse.toUserState(
                previousUserState = null,
                environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
            )
        } returns SINGLE_USER_STATE_1
        coEvery {
            vaultRepository.unlockVault(
                accountCryptographicState = createWrappedAccountCryptographicState(
                    privateKey = successResponse.accountKeys!!
                        .publicKeyEncryptionKeyPair
                        .wrappedPrivateKey,
                    securityState = successResponse.accountKeys
                        ?.securityState
                        ?.securityState,
                    signedPublicKey = successResponse.accountKeys!!
                        .publicKeyEncryptionKeyPair
                        .signedPublicKey,
                    signingKey = successResponse.accountKeys
                        ?.signatureKeyPair
                        ?.wrappedSigningKey,
                ),
                userId = SINGLE_USER_STATE_1.activeUserId,
                email = SINGLE_USER_STATE_1.activeAccount.profile.email,
                kdf = SINGLE_USER_STATE_1.activeAccount.profile.toSdkParams(),
                initUserCryptoMethod = InitUserCryptoMethod.AuthRequest(
                    requestPrivateKey = DEVICE_REQUEST_PRIVATE_KEY,
                    method = AuthRequestMethod.MasterKey(
                        protectedMasterKey = DEVICE_ASYMMETRICAL_KEY,
                        authRequestKey = successResponse.key!!,
                    ),
                ),
                organizationKeys = null,
            )
        } returns VaultUnlockResult.Success
        val finalResult = repository.login(
            email = EMAIL,
            password = null,
            twoFactorData = TWO_FACTOR_DATA,
            orgIdentifier = null,
        )
        assertEquals(LoginResult.Success, finalResult)
        assertNull(repository.twoFactorResponse)
        fakeAuthDiskSource.assertTwoFactorToken(
            email = EMAIL,
            twoFactorToken = "twoFactorTokenToStore",
        )
        verify(exactly = 1) {
            userStateManager.hasPendingAccountAddition = false
        }
    }

    @Test
    fun `SSO login get token fails should return Error with no message`() = runTest {
        val error = RuntimeException()
        coEvery {
            identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.SingleSignOn(
                    ssoCode = SSO_CODE,
                    ssoCodeVerifier = SSO_CODE_VERIFIER,
                    ssoRedirectUri = SSO_REDIRECT_URI,
                ),
                uniqueAppId = UNIQUE_APP_ID,
            )
        } returns error.asFailure()
        val result = repository.login(
            email = EMAIL,
            ssoCode = SSO_CODE,
            ssoCodeVerifier = SSO_CODE_VERIFIER,
            ssoRedirectUri = SSO_REDIRECT_URI,
            organizationIdentifier = ORGANIZATION_IDENTIFIER,
        )
        assertEquals(LoginResult.Error(errorMessage = null, error = error), result)
        assertEquals(AuthState.Unauthenticated, repository.authStateFlow.value)
        coVerify {
            identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.SingleSignOn(
                    ssoCode = SSO_CODE,
                    ssoCodeVerifier = SSO_CODE_VERIFIER,
                    ssoRedirectUri = SSO_REDIRECT_URI,
                ),
                uniqueAppId = UNIQUE_APP_ID,
            )
        }
    }

    @Test
    fun `SSO login get token returns Invalid should return Error with correct message`() = runTest {
        coEvery {
            identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.SingleSignOn(
                    ssoCode = SSO_CODE,
                    ssoCodeVerifier = SSO_CODE_VERIFIER,
                    ssoRedirectUri = SSO_REDIRECT_URI,
                ),
                uniqueAppId = UNIQUE_APP_ID,
            )
        } returns GetTokenResponseJson
            .Invalid(
                errorModel = GetTokenResponseJson.Invalid.ErrorModel(
                    errorMessage = "mock_error_message",
                ),
            )
            .asSuccess()

        val result = repository.login(
            email = EMAIL,
            ssoCode = SSO_CODE,
            ssoCodeVerifier = SSO_CODE_VERIFIER,
            ssoRedirectUri = SSO_REDIRECT_URI,
            organizationIdentifier = ORGANIZATION_IDENTIFIER,
        )
        assertEquals(LoginResult.Error(errorMessage = "mock_error_message", error = null), result)
        assertEquals(AuthState.Unauthenticated, repository.authStateFlow.value)
        coVerify {
            identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.SingleSignOn(
                    ssoCode = SSO_CODE,
                    ssoCodeVerifier = SSO_CODE_VERIFIER,
                    ssoRedirectUri = SSO_REDIRECT_URI,
                ),
                uniqueAppId = UNIQUE_APP_ID,
            )
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `SSO login get token succeeds should return Success, update AuthState, update stored keys, and sync`() =
        runTest {
            val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns successResponse.asSuccess()
            coEvery { vaultRepository.syncIfNecessary() } just runs
            every {
                GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.toUserState(
                    previousUserState = null,
                    environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
                )
            } returns SINGLE_USER_STATE_1
            val result = repository.login(
                email = EMAIL,
                ssoCode = SSO_CODE,
                ssoCodeVerifier = SSO_CODE_VERIFIER,
                ssoRedirectUri = SSO_REDIRECT_URI,
                organizationIdentifier = ORGANIZATION_IDENTIFIER,
            )
            assertEquals(LoginResult.Success, result)
            assertEquals(AuthState.Authenticated(ACCESS_TOKEN), repository.authStateFlow.value)
            fakeAuthDiskSource.assertPrivateKey(
                userId = USER_ID_1,
                privateKey = "privateKey",
            )
            fakeAuthDiskSource.assertAccountKeys(
                userId = USER_ID_1,
                accountKeys = ACCOUNT_KEYS,
            )
            fakeAuthDiskSource.assertUserKey(
                userId = USER_ID_1,
                userKey = "key",
            )
            coVerify {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
                vaultRepository.syncIfNecessary()
                settingsRepository.storeUserHasLoggedInValue(userId = USER_ID_1)
            }
            assertEquals(
                SINGLE_USER_STATE_1,
                fakeAuthDiskSource.userState,
            )
            verify(exactly = 1) {
                userStateManager.hasPendingAccountAddition = false
                settingsRepository.setDefaultsIfNecessary(userId = USER_ID_1)
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `SSO login get token succeeds with key connector and master password should return success and not unlock the vault`() =
        runTest {
            val keyConnectorUrl = "www.example.com"
            val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.copy(
                keyConnectorUrl = keyConnectorUrl,
                userDecryptionOptions = USER_DECRYPTION_OPTIONS.copy(
                    hasMasterPassword = true,
                    trustedDeviceUserDecryptionOptions = null,
                ),
            )
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns successResponse.asSuccess()
            coEvery { vaultRepository.syncIfNecessary() } just runs
            every {
                successResponse.toUserState(
                    previousUserState = null,
                    environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
                )
            } returns SINGLE_USER_STATE_1
            val result = repository.login(
                email = EMAIL,
                ssoCode = SSO_CODE,
                ssoCodeVerifier = SSO_CODE_VERIFIER,
                ssoRedirectUri = SSO_REDIRECT_URI,
                organizationIdentifier = ORGANIZATION_IDENTIFIER,
            )
            assertEquals(LoginResult.Success, result)
            assertEquals(AuthState.Authenticated(ACCESS_TOKEN), repository.authStateFlow.value)
            fakeAuthDiskSource.assertPrivateKey(userId = USER_ID_1, privateKey = "privateKey")
            fakeAuthDiskSource.assertAccountKeys(userId = USER_ID_1, accountKeys = ACCOUNT_KEYS)
            fakeAuthDiskSource.assertUserKey(userId = USER_ID_1, userKey = "key")
            coVerify(exactly = 1) {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
                vaultRepository.syncIfNecessary()
            }
            assertEquals(SINGLE_USER_STATE_1, fakeAuthDiskSource.userState)
            verify(exactly = 1) {
                userStateManager.hasPendingAccountAddition = false
                settingsRepository.setDefaultsIfNecessary(userId = USER_ID_1)
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `SSO login get token succeeds with key connector and no master password should return failure`() =
        runTest {
            val error = Throwable("Fail!")
            val keyConnectorUrl = "www.example.com"
            val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.copy(
                keyConnectorUrl = keyConnectorUrl,
                userDecryptionOptions = USER_DECRYPTION_OPTIONS.copy(
                    hasMasterPassword = false,
                    trustedDeviceUserDecryptionOptions = null,
                ),
            )
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns successResponse.asSuccess()
            coEvery {
                keyConnectorManager.getMasterKeyFromKeyConnector(
                    url = keyConnectorUrl,
                    accessToken = ACCESS_TOKEN,
                )
            } returns error.asFailure()
            every {
                successResponse.toUserState(
                    previousUserState = null,
                    environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
                )
            } returns SINGLE_USER_STATE_1

            val result = repository.login(
                email = EMAIL,
                ssoCode = SSO_CODE,
                ssoCodeVerifier = SSO_CODE_VERIFIER,
                ssoRedirectUri = SSO_REDIRECT_URI,
                organizationIdentifier = ORGANIZATION_IDENTIFIER,
            )

            assertEquals(LoginResult.Error(errorMessage = null, error = error), result)
            fakeAuthDiskSource.assertPrivateKey(userId = USER_ID_1, privateKey = null)
            fakeAuthDiskSource.assertAccountKeys(userId = USER_ID_1, accountKeys = null)
            fakeAuthDiskSource.assertUserKey(userId = USER_ID_1, userKey = null)
            coVerify(exactly = 1) {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
                keyConnectorManager.getMasterKeyFromKeyConnector(
                    url = keyConnectorUrl,
                    accessToken = ACCESS_TOKEN,
                )
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `SSO login get token succeeds with key connector and no master password should return success and unlock the vault`() =
        runTest {
            val keyConnectorUrl = "www.example.com"
            val successResponse = GET_TOKEN_RESPONSE_SUCCESS.copy(
                keyConnectorUrl = keyConnectorUrl,
                userDecryptionOptions = USER_DECRYPTION_OPTIONS.copy(
                    hasMasterPassword = false,
                    trustedDeviceUserDecryptionOptions = null,
                ),
            )
            val masterKey = "masterKey"
            val keyConnectorMasterKeyResponseJson = mockk<KeyConnectorMasterKeyResponseJson> {
                every { this@mockk.masterKey } returns masterKey
            }
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns successResponse.asSuccess()
            coEvery {
                keyConnectorManager.getMasterKeyFromKeyConnector(
                    url = keyConnectorUrl,
                    accessToken = ACCESS_TOKEN,
                )
            } returns keyConnectorMasterKeyResponseJson.asSuccess()
            coEvery {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = "privateKey",
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.KeyConnector(
                        masterKey = masterKey,
                        userKey = "key",
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.Success
            coEvery { vaultRepository.syncIfNecessary() } just runs
            every {
                successResponse.toUserState(
                    previousUserState = null,
                    environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
                )
            } returns SINGLE_USER_STATE_1
            val result = repository.login(
                email = EMAIL,
                ssoCode = SSO_CODE,
                ssoCodeVerifier = SSO_CODE_VERIFIER,
                ssoRedirectUri = SSO_REDIRECT_URI,
                organizationIdentifier = ORGANIZATION_IDENTIFIER,
            )

            assertEquals(LoginResult.Success, result)
            assertEquals(AuthState.Authenticated(ACCESS_TOKEN), repository.authStateFlow.value)
            fakeAuthDiskSource.assertPrivateKey(userId = USER_ID_1, privateKey = "privateKey")
            fakeAuthDiskSource.assertUserKey(userId = USER_ID_1, userKey = "key")
            coVerify(exactly = 1) {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
                keyConnectorManager.getMasterKeyFromKeyConnector(
                    url = keyConnectorUrl,
                    accessToken = ACCESS_TOKEN,
                )
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = "privateKey",
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.KeyConnector(
                        masterKey = masterKey,
                        userKey = "key",
                    ),
                    organizationKeys = null,
                )
                vaultRepository.syncIfNecessary()
            }
            assertEquals(SINGLE_USER_STATE_1, fakeAuthDiskSource.userState)
            verify(exactly = 1) {
                userStateManager.hasPendingAccountAddition = false
                settingsRepository.setDefaultsIfNecessary(userId = USER_ID_1)
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `SSO login get token succeeds with key connector and accountKeys with null nested fields should unlock vault`() =
        runTest {
            val keyConnectorUrl = "www.example.com"
            val successResponse = GET_TOKEN_RESPONSE_SUCCESS.copy(
                keyConnectorUrl = keyConnectorUrl,
                accountKeys = ACCOUNT_KEYS_WITH_NULL_FIELDS,
                userDecryptionOptions = USER_DECRYPTION_OPTIONS.copy(
                    hasMasterPassword = false,
                    trustedDeviceUserDecryptionOptions = null,
                ),
            )
            val masterKey = "masterKey"
            val keyConnectorMasterKeyResponseJson = mockk<KeyConnectorMasterKeyResponseJson> {
                every { this@mockk.masterKey } returns masterKey
            }
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns successResponse.asSuccess()
            coEvery {
                keyConnectorManager.getMasterKeyFromKeyConnector(
                    url = keyConnectorUrl,
                    accessToken = ACCESS_TOKEN,
                )
            } returns keyConnectorMasterKeyResponseJson.asSuccess()
            coEvery {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = "privateKey",
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.KeyConnector(
                        masterKey = masterKey,
                        userKey = "key",
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.Success
            coEvery { vaultRepository.syncIfNecessary() } just runs
            every {
                successResponse.toUserState(
                    previousUserState = null,
                    environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
                )
            } returns SINGLE_USER_STATE_1
            val result = repository.login(
                email = EMAIL,
                ssoCode = SSO_CODE,
                ssoCodeVerifier = SSO_CODE_VERIFIER,
                ssoRedirectUri = SSO_REDIRECT_URI,
                organizationIdentifier = ORGANIZATION_IDENTIFIER,
            )

            assertEquals(LoginResult.Success, result)
            assertEquals(AuthState.Authenticated(ACCESS_TOKEN), repository.authStateFlow.value)
            fakeAuthDiskSource.assertPrivateKey(userId = USER_ID_1, privateKey = "privateKey")
            fakeAuthDiskSource.assertUserKey(userId = USER_ID_1, userKey = "key")
            coVerify(exactly = 1) {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
                keyConnectorManager.getMasterKeyFromKeyConnector(
                    url = keyConnectorUrl,
                    accessToken = ACCESS_TOKEN,
                )
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = "privateKey",
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.KeyConnector(
                        masterKey = masterKey,
                        userKey = "key",
                    ),
                    organizationKeys = null,
                )
                vaultRepository.syncIfNecessary()
            }
            assertEquals(SINGLE_USER_STATE_1, fakeAuthDiskSource.userState)
            verify(exactly = 1) {
                userStateManager.hasPendingAccountAddition = false
                settingsRepository.setDefaultsIfNecessary(userId = USER_ID_1)
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `SSO login get token succeeds with key connector, no master password, no key and no private key should return failure`() =
        runTest {
            val error = Throwable("Fail!")
            val keyConnectorUrl = "www.example.com"
            val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.copy(
                keyConnectorUrl = keyConnectorUrl,
                userDecryptionOptions = USER_DECRYPTION_OPTIONS.copy(
                    hasMasterPassword = false,
                    trustedDeviceUserDecryptionOptions = null,
                ),
                key = null,
                privateKey = null,
                accountKeys = null,
            )
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns successResponse.asSuccess()
            coEvery {
                keyConnectorManager.migrateNewUserToKeyConnector(
                    url = keyConnectorUrl,
                    accessToken = ACCESS_TOKEN,
                    kdfType = PROFILE_1.kdfType!!,
                    kdfIterations = PROFILE_1.kdfIterations,
                    kdfMemory = PROFILE_1.kdfMemory,
                    kdfParallelism = PROFILE_1.kdfParallelism,
                    organizationIdentifier = ORGANIZATION_IDENTIFIER,
                )
            } returns error.asFailure()
            every {
                successResponse.toUserState(
                    previousUserState = null,
                    environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
                )
            } returns SINGLE_USER_STATE_1
            repository.rememberedOrgIdentifier = ORGANIZATION_IDENTIFIER

            val result = repository.login(
                email = EMAIL,
                ssoCode = SSO_CODE,
                ssoCodeVerifier = SSO_CODE_VERIFIER,
                ssoRedirectUri = SSO_REDIRECT_URI,
                organizationIdentifier = ORGANIZATION_IDENTIFIER,
            )

            assertEquals(LoginResult.ConfirmKeyConnectorDomain(keyConnectorUrl), result)

            val continueResult = repository.continueKeyConnectorLogin()
            assertEquals(LoginResult.Error(errorMessage = null, error = error), continueResult)
            fakeAuthDiskSource.assertPrivateKey(userId = USER_ID_1, privateKey = null)
            fakeAuthDiskSource.assertAccountKeys(userId = USER_ID_1, accountKeys = null)
            fakeAuthDiskSource.assertUserKey(userId = USER_ID_1, userKey = null)
            coVerify(exactly = 1) {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
                keyConnectorManager.migrateNewUserToKeyConnector(
                    url = keyConnectorUrl,
                    accessToken = ACCESS_TOKEN,
                    kdfType = PROFILE_1.kdfType!!,
                    kdfIterations = PROFILE_1.kdfIterations,
                    kdfMemory = PROFILE_1.kdfMemory,
                    kdfParallelism = PROFILE_1.kdfParallelism,
                    organizationIdentifier = ORGANIZATION_IDENTIFIER,
                )
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `SSO login get token succeeds with key connector, no master password, no key and no private key should return success and unlock the vault`() =
        runTest {
            val keyConnectorUrl = "www.example.com"
            val successResponse = GET_TOKEN_RESPONSE_SUCCESS.copy(
                keyConnectorUrl = keyConnectorUrl,
                userDecryptionOptions = USER_DECRYPTION_OPTIONS.copy(
                    hasMasterPassword = false,
                    trustedDeviceUserDecryptionOptions = null,
                ),
                key = null,
                privateKey = null,
            )
            val masterKey = "masterKey"
            val keyConnectorResponse = mockk<KeyConnectorResponse> {
                every {
                    this@mockk.keys
                } returns RsaKeyPair(public = PUBLIC_KEY, private = PRIVATE_KEY)
                every { this@mockk.masterKey } returns masterKey
                every { this@mockk.encryptedUserKey } returns ENCRYPTED_USER_KEY
            }
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns successResponse.asSuccess()
            coEvery {
                keyConnectorManager.migrateNewUserToKeyConnector(
                    url = keyConnectorUrl,
                    accessToken = ACCESS_TOKEN,
                    kdfType = PROFILE_1.kdfType!!,
                    kdfIterations = PROFILE_1.kdfIterations,
                    kdfMemory = PROFILE_1.kdfMemory,
                    kdfParallelism = PROFILE_1.kdfParallelism,
                    organizationIdentifier = ORGANIZATION_IDENTIFIER,
                )
            } returns keyConnectorResponse.asSuccess()
            coEvery {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = PRIVATE_KEY,
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.KeyConnector(
                        masterKey = masterKey,
                        userKey = ENCRYPTED_USER_KEY,
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.Success
            coEvery { vaultRepository.syncIfNecessary() } just runs
            every {
                successResponse.toUserState(
                    previousUserState = null,
                    environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
                )
            } returns SINGLE_USER_STATE_1
            repository.rememberedOrgIdentifier = ORGANIZATION_IDENTIFIER
            val result = repository.login(
                email = EMAIL,
                ssoCode = SSO_CODE,
                ssoCodeVerifier = SSO_CODE_VERIFIER,
                ssoRedirectUri = SSO_REDIRECT_URI,
                organizationIdentifier = ORGANIZATION_IDENTIFIER,
            )

            assertEquals(LoginResult.ConfirmKeyConnectorDomain(keyConnectorUrl), result)

            val continueResult = repository.continueKeyConnectorLogin()
            assertEquals(LoginResult.Success, continueResult)
            assertEquals(AuthState.Authenticated(ACCESS_TOKEN), repository.authStateFlow.value)
            fakeAuthDiskSource.assertPrivateKey(userId = USER_ID_1, privateKey = PRIVATE_KEY)
            fakeAuthDiskSource.assertUserKey(userId = USER_ID_1, userKey = ENCRYPTED_USER_KEY)
            coVerify(exactly = 1) {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
                keyConnectorManager.migrateNewUserToKeyConnector(
                    url = keyConnectorUrl,
                    accessToken = ACCESS_TOKEN,
                    kdfType = PROFILE_1.kdfType!!,
                    kdfIterations = PROFILE_1.kdfIterations,
                    kdfMemory = PROFILE_1.kdfMemory,
                    kdfParallelism = PROFILE_1.kdfParallelism,
                    organizationIdentifier = ORGANIZATION_IDENTIFIER,
                )
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = "privateKey",
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.KeyConnector(
                        masterKey = masterKey,
                        userKey = ENCRYPTED_USER_KEY,
                    ),
                    organizationKeys = null,
                )
                vaultRepository.syncIfNecessary()
            }
            assertEquals(SINGLE_USER_STATE_1, fakeAuthDiskSource.userState)
            verify(exactly = 1) {
                userStateManager.hasPendingAccountAddition = false
                settingsRepository.setDefaultsIfNecessary(userId = USER_ID_1)
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `SSO login get token succeeds with key connector when key, privateKey and master password are null should return ConfirmKeyConnectorDomain`() =
        runTest {
            val keyConnectorUrl = "www.example.com"
            val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.copy(
                keyConnectorUrl = keyConnectorUrl,
                userDecryptionOptions = USER_DECRYPTION_OPTIONS.copy(
                    hasMasterPassword = false,
                    trustedDeviceUserDecryptionOptions = null,
                ),
                key = null,
                privateKey = null,
                accountKeys = null,
            )
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns successResponse.asSuccess()
            every {
                successResponse.toUserState(
                    previousUserState = null,
                    environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
                )
            } returns SINGLE_USER_STATE_1
            val result = repository.login(
                email = EMAIL,
                ssoCode = SSO_CODE,
                ssoCodeVerifier = SSO_CODE_VERIFIER,
                ssoRedirectUri = SSO_REDIRECT_URI,
                organizationIdentifier = ORGANIZATION_IDENTIFIER,
            )
            assertEquals(LoginResult.ConfirmKeyConnectorDomain(keyConnectorUrl), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `ContinueKeyConnectorLogin should return Success and unlock the vault after login returns ConfirmKeyConnector`() =
        runTest {
            val keyConnectorUrl = "www.example.com"
            val successResponse = GET_TOKEN_RESPONSE_SUCCESS.copy(
                keyConnectorUrl = keyConnectorUrl,
                userDecryptionOptions = USER_DECRYPTION_OPTIONS.copy(
                    hasMasterPassword = false,
                    trustedDeviceUserDecryptionOptions = null,
                ),
                key = null,
                privateKey = null,
            )

            val masterKey = "masterKey"
            val keyConnectorResponse = mockk<KeyConnectorResponse> {
                every {
                    this@mockk.keys
                } returns RsaKeyPair(public = PUBLIC_KEY, private = PRIVATE_KEY)
                every { this@mockk.masterKey } returns masterKey
                every { this@mockk.encryptedUserKey } returns ENCRYPTED_USER_KEY
            }

            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns successResponse.asSuccess()
            coEvery { vaultRepository.syncIfNecessary() } just runs
            every {
                successResponse.toUserState(
                    previousUserState = null,
                    environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
                )
            } returns SINGLE_USER_STATE_1

            coEvery {
                keyConnectorManager.migrateNewUserToKeyConnector(
                    url = keyConnectorUrl,
                    accessToken = ACCESS_TOKEN,
                    kdfType = PROFILE_1.kdfType!!,
                    kdfIterations = PROFILE_1.kdfIterations,
                    kdfMemory = PROFILE_1.kdfMemory,
                    kdfParallelism = PROFILE_1.kdfParallelism,
                    organizationIdentifier = ORGANIZATION_IDENTIFIER,
                )
            } returns keyConnectorResponse.asSuccess()

            coEvery {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = PRIVATE_KEY,
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.KeyConnector(
                        masterKey = masterKey,
                        userKey = ENCRYPTED_USER_KEY,
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.Success

            repository.rememberedOrgIdentifier = ORGANIZATION_IDENTIFIER

            val loginResult = repository.login(
                email = EMAIL,
                ssoCode = SSO_CODE,
                ssoCodeVerifier = SSO_CODE_VERIFIER,
                ssoRedirectUri = SSO_REDIRECT_URI,
                organizationIdentifier = ORGANIZATION_IDENTIFIER,
            )
            assertEquals(LoginResult.ConfirmKeyConnectorDomain(keyConnectorUrl), loginResult)

            val result = repository.continueKeyConnectorLogin()
            assertEquals(LoginResult.Success, result)
            assertEquals(AuthState.Authenticated(ACCESS_TOKEN), repository.authStateFlow.value)
            fakeAuthDiskSource.assertPrivateKey(userId = USER_ID_1, privateKey = "privateKey")
            fakeAuthDiskSource.assertUserKey(userId = USER_ID_1, userKey = "encryptedUserKey")
            coVerify(exactly = 1) {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
                vaultRepository.syncIfNecessary()
            }
            assertEquals(SINGLE_USER_STATE_1, fakeAuthDiskSource.userState)
            verify(exactly = 1) {
                userStateManager.hasPendingAccountAddition = false
                settingsRepository.setDefaultsIfNecessary(userId = USER_ID_1)
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `login with device get token succeeds should return Success, update AuthState, update stored keys, and sync with UserKey`() =
        runTest {
            val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.AuthRequest(
                        username = EMAIL,
                        authRequestId = DEVICE_REQUEST_ID,
                        accessCode = DEVICE_ACCESS_CODE,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns successResponse.asSuccess()
            coEvery { vaultRepository.syncIfNecessary() } just runs
            every {
                GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.toUserState(
                    previousUserState = null,
                    environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
                )
            } returns SINGLE_USER_STATE_1
            coEvery {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .wrappedPrivateKey,
                        securityState = successResponse.accountKeys
                            ?.securityState
                            ?.securityState,
                        signedPublicKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .signedPublicKey,
                        signingKey = successResponse.accountKeys
                            ?.signatureKeyPair
                            ?.wrappedSigningKey,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.AuthRequest(
                        requestPrivateKey = DEVICE_REQUEST_PRIVATE_KEY,
                        method = AuthRequestMethod.UserKey(
                            protectedUserKey = DEVICE_ASYMMETRICAL_KEY,
                        ),
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.Success
            val result = repository.login(
                email = EMAIL,
                requestId = DEVICE_REQUEST_ID,
                accessCode = DEVICE_ACCESS_CODE,
                asymmetricalKey = DEVICE_ASYMMETRICAL_KEY,
                requestPrivateKey = DEVICE_REQUEST_PRIVATE_KEY,
                masterPasswordHash = null,
            )
            assertEquals(LoginResult.Success, result)
            assertEquals(AuthState.Authenticated(ACCESS_TOKEN), repository.authStateFlow.value)
            fakeAuthDiskSource.assertPrivateKey(
                userId = USER_ID_1,
                privateKey = "privateKey",
            )
            fakeAuthDiskSource.assertAccountKeys(
                userId = USER_ID_1,
                accountKeys = ACCOUNT_KEYS,
            )
            fakeAuthDiskSource.assertUserKey(
                userId = USER_ID_1,
                userKey = "key",
            )
            coVerify {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.AuthRequest(
                        username = EMAIL,
                        authRequestId = DEVICE_REQUEST_ID,
                        accessCode = DEVICE_ACCESS_CODE,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
                vaultRepository.syncIfNecessary()
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .wrappedPrivateKey,
                        securityState = successResponse.accountKeys
                            ?.securityState
                            ?.securityState,
                        signedPublicKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .signedPublicKey,
                        signingKey = successResponse.accountKeys
                            ?.signatureKeyPair
                            ?.wrappedSigningKey,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.AuthRequest(
                        requestPrivateKey = DEVICE_REQUEST_PRIVATE_KEY,
                        method = AuthRequestMethod.UserKey(
                            protectedUserKey = DEVICE_ASYMMETRICAL_KEY,
                        ),
                    ),
                    organizationKeys = null,
                )
            }
            assertEquals(
                SINGLE_USER_STATE_1,
                fakeAuthDiskSource.userState,
            )
            verify(exactly = 1) {
                userStateManager.hasPendingAccountAddition = false
                settingsRepository.setDefaultsIfNecessary(userId = USER_ID_1)
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `SSO login get token succeeds with trusted device key and no keys should return Success, clear device key, update AuthState, update stored keys, and sync`() =
        runTest {
            val deviceKey = "deviceKey"
            fakeAuthDiskSource.storeDeviceKey(USER_ID_1, deviceKey)
            val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.copy(
                key = null,
                userDecryptionOptions = USER_DECRYPTION_OPTIONS,
            )
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns successResponse.asSuccess()
            coEvery { vaultRepository.syncIfNecessary() } just runs
            every {
                successResponse.toUserState(
                    previousUserState = null,
                    environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
                )
            } returns SINGLE_USER_STATE_1

            val result = repository.login(
                email = EMAIL,
                ssoCode = SSO_CODE,
                ssoCodeVerifier = SSO_CODE_VERIFIER,
                ssoRedirectUri = SSO_REDIRECT_URI,
                organizationIdentifier = ORGANIZATION_IDENTIFIER,
            )

            assertEquals(LoginResult.Success, result)
            assertEquals(AuthState.Authenticated(ACCESS_TOKEN), repository.authStateFlow.value)
            fakeAuthDiskSource.assertPrivateKey(userId = USER_ID_1, privateKey = "privateKey")
            fakeAuthDiskSource.assertAccountKeys(userId = USER_ID_1, accountKeys = ACCOUNT_KEYS)
            fakeAuthDiskSource.assertUserKey(userId = USER_ID_1, userKey = null)
            fakeAuthDiskSource.assertDeviceKey(userId = USER_ID_1, deviceKey = null)
            assertEquals(SINGLE_USER_STATE_1, fakeAuthDiskSource.userState)
            coVerify(exactly = 1) {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
                vaultRepository.syncIfNecessary()
            }
            verify(exactly = 1) {
                userStateManager.hasPendingAccountAddition = false
                settingsRepository.setDefaultsIfNecessary(userId = USER_ID_1)
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `SSO login get token succeeds with trusted device key should return Success, clear device key, update AuthState, update stored keys, and sync`() =
        runTest {
            val deviceKey = "deviceKey"
            val encryptedUserKey = "encryptedUserKey"
            val encryptedPrivateKey = "encryptedPrivateKey"
            fakeAuthDiskSource.storeDeviceKey(USER_ID_1, deviceKey)
            val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.copy(
                key = null,
                userDecryptionOptions = USER_DECRYPTION_OPTIONS.copy(
                    trustedDeviceUserDecryptionOptions = TRUSTED_DEVICE_DECRYPTION_OPTIONS.copy(
                        encryptedUserKey = encryptedUserKey,
                        encryptedPrivateKey = encryptedPrivateKey,
                    ),
                ),
            )
            coEvery {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .wrappedPrivateKey,
                        securityState = successResponse.accountKeys
                            ?.securityState
                            ?.securityState,
                        signedPublicKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .signedPublicKey,
                        signingKey = successResponse.accountKeys
                            ?.signatureKeyPair
                            ?.wrappedSigningKey,
                    ),
                    userId = USER_ID_1,
                    email = SINGLE_USER_STATE_1.activeAccount.profile.email,
                    kdf = SINGLE_USER_STATE_1.activeAccount.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.DeviceKey(
                        deviceKey = deviceKey,
                        protectedDevicePrivateKey = encryptedPrivateKey,
                        deviceProtectedUserKey = encryptedUserKey,
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.Success
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns successResponse.asSuccess()
            coEvery { vaultRepository.syncIfNecessary() } just runs
            every {
                successResponse.toUserState(
                    previousUserState = null,
                    environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
                )
            } returns SINGLE_USER_STATE_1

            val result = repository.login(
                email = EMAIL,
                ssoCode = SSO_CODE,
                ssoCodeVerifier = SSO_CODE_VERIFIER,
                ssoRedirectUri = SSO_REDIRECT_URI,
                organizationIdentifier = ORGANIZATION_IDENTIFIER,
            )

            assertEquals(LoginResult.Success, result)
            assertEquals(AuthState.Authenticated(ACCESS_TOKEN), repository.authStateFlow.value)
            fakeAuthDiskSource.assertPrivateKey(userId = USER_ID_1, privateKey = "privateKey")
            fakeAuthDiskSource.assertAccountKeys(userId = USER_ID_1, accountKeys = ACCOUNT_KEYS)
            fakeAuthDiskSource.assertUserKey(userId = USER_ID_1, userKey = encryptedUserKey)
            fakeAuthDiskSource.assertDeviceKey(userId = USER_ID_1, deviceKey = deviceKey)
            assertEquals(SINGLE_USER_STATE_1, fakeAuthDiskSource.userState)
            coVerify {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .wrappedPrivateKey,
                        securityState = successResponse.accountKeys
                            ?.securityState
                            ?.securityState,
                        signedPublicKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .signedPublicKey,
                        signingKey = successResponse.accountKeys
                            ?.signatureKeyPair
                            ?.wrappedSigningKey,
                    ),
                    userId = USER_ID_1,
                    email = SINGLE_USER_STATE_1.activeAccount.profile.email,
                    kdf = SINGLE_USER_STATE_1.activeAccount.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.DeviceKey(
                        deviceKey = deviceKey,
                        protectedDevicePrivateKey = encryptedPrivateKey,
                        deviceProtectedUserKey = encryptedUserKey,
                    ),
                    organizationKeys = null,
                )
                vaultRepository.syncIfNecessary()
            }
            verify(exactly = 1) {
                userStateManager.hasPendingAccountAddition = false
                settingsRepository.setDefaultsIfNecessary(userId = USER_ID_1)
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `SSO login get token succeeds without trusted device key should return Success, unlock the vault with pending request, update AuthState, update stored keys, and sync`() =
        runTest {
            val pendingAuthRequest = PendingAuthRequestJson(
                requestId = "requestId",
                requestPrivateKey = "requestPrivateKey",
                requestFingerprint = "fingerprint",
                requestAccessCode = "accessCode",
            )
            val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.copy(
                key = null,
                userDecryptionOptions = USER_DECRYPTION_OPTIONS,
            )
            val authRequestKey = "key"
            val authRequest = mockk<AuthRequest> {
                every { this@mockk.key } returns authRequestKey
            }
            coEvery {
                authRequestManager.getAuthRequestIfApproved(pendingAuthRequest.requestId)
            } returns authRequest.asSuccess()
            fakeAuthDiskSource.storePendingAuthRequest(USER_ID_1, pendingAuthRequest)
            coEvery {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .wrappedPrivateKey,
                        securityState = successResponse.accountKeys
                            ?.securityState
                            ?.securityState,
                        signedPublicKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .signedPublicKey,
                        signingKey = successResponse.accountKeys
                            ?.signatureKeyPair
                            ?.wrappedSigningKey,
                    ),
                    userId = USER_ID_1,
                    email = SINGLE_USER_STATE_1.activeAccount.profile.email,
                    kdf = SINGLE_USER_STATE_1.activeAccount.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.AuthRequest(
                        requestPrivateKey = pendingAuthRequest.requestPrivateKey,
                        method = AuthRequestMethod.UserKey(protectedUserKey = authRequestKey),
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.Success
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns successResponse.asSuccess()
            coEvery { vaultRepository.syncIfNecessary() } just runs
            every {
                successResponse.toUserState(
                    previousUserState = null,
                    environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
                )
            } returns SINGLE_USER_STATE_1

            val result = repository.login(
                email = EMAIL,
                ssoCode = SSO_CODE,
                ssoCodeVerifier = SSO_CODE_VERIFIER,
                ssoRedirectUri = SSO_REDIRECT_URI,
                organizationIdentifier = ORGANIZATION_IDENTIFIER,
            )

            assertEquals(LoginResult.Success, result)
            assertEquals(AuthState.Authenticated(ACCESS_TOKEN), repository.authStateFlow.value)
            fakeAuthDiskSource.assertPrivateKey(userId = USER_ID_1, privateKey = "privateKey")
            fakeAuthDiskSource.assertAccountKeys(userId = USER_ID_1, accountKeys = ACCOUNT_KEYS)
            fakeAuthDiskSource.assertUserKey(userId = USER_ID_1, userKey = authRequestKey)
            assertEquals(SINGLE_USER_STATE_1, fakeAuthDiskSource.userState)
            coVerify(exactly = 1) {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .wrappedPrivateKey,
                        securityState = successResponse.accountKeys
                            ?.securityState
                            ?.securityState,
                        signedPublicKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .signedPublicKey,
                        signingKey = successResponse.accountKeys
                            ?.signatureKeyPair
                            ?.wrappedSigningKey,
                    ),
                    userId = USER_ID_1,
                    email = SINGLE_USER_STATE_1.activeAccount.profile.email,
                    kdf = SINGLE_USER_STATE_1.activeAccount.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.AuthRequest(
                        requestPrivateKey = pendingAuthRequest.requestPrivateKey,
                        method = AuthRequestMethod.UserKey(protectedUserKey = authRequestKey),
                    ),
                    organizationKeys = null,
                )
                vaultRepository.syncIfNecessary()
            }
            verify(exactly = 1) {
                userStateManager.hasPendingAccountAddition = false
                settingsRepository.setDefaultsIfNecessary(userId = USER_ID_1)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `SSO login get token succeeds when there is an existing user should switch to the new logged in user`() =
        runTest {
            // Ensure the initial state for User 2 with a account addition
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_2
            repository.hasPendingAccountAddition = true

            // Set up login for User 1
            val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns successResponse.asSuccess()
            coEvery { vaultRepository.syncIfNecessary() } just runs
            every {
                GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.toUserState(
                    previousUserState = SINGLE_USER_STATE_2,
                    environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
                )
            } returns MULTI_USER_STATE

            val result = repository.login(
                email = EMAIL,
                ssoCode = SSO_CODE,
                ssoCodeVerifier = SSO_CODE_VERIFIER,
                ssoRedirectUri = SSO_REDIRECT_URI,
                organizationIdentifier = ORGANIZATION_IDENTIFIER,
            )

            assertEquals(LoginResult.Success, result)
            assertEquals(AuthState.Authenticated(ACCESS_TOKEN), repository.authStateFlow.value)
            fakeAuthDiskSource.assertPrivateKey(
                userId = USER_ID_1,
                privateKey = "privateKey",
            )
            fakeAuthDiskSource.assertAccountKeys(
                userId = USER_ID_1,
                accountKeys = ACCOUNT_KEYS,
            )
            fakeAuthDiskSource.assertUserKey(
                userId = USER_ID_1,
                userKey = "key",
            )
            coVerify {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
                vaultRepository.syncIfNecessary()
            }
            assertEquals(
                MULTI_USER_STATE,
                fakeAuthDiskSource.userState,
            )
            assertFalse(repository.hasPendingAccountAddition)
            verify(exactly = 1) {
                userStateManager.hasPendingAccountAddition
                userStateManager.hasPendingAccountAddition = false
                settingsRepository.setDefaultsIfNecessary(userId = USER_ID_1)
            }
        }

    @Test
    fun `SSO login get token returns two factor request should return TwoFactorRequired`() =
        runTest {
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns GetTokenResponseJson
                .TwoFactorRequired(
                    authMethodsData = TWO_FACTOR_AUTH_METHODS_DATA,
                    ssoToken = null,
                    twoFactorProviders = null,
                )
                .asSuccess()
            val result = repository.login(
                email = EMAIL,
                ssoCode = SSO_CODE,
                ssoCodeVerifier = SSO_CODE_VERIFIER,
                ssoRedirectUri = SSO_REDIRECT_URI,
                organizationIdentifier = ORGANIZATION_IDENTIFIER,
            )
            assertEquals(LoginResult.TwoFactorRequired, result)
            assertEquals(
                repository.twoFactorResponse,
                GetTokenResponseJson.TwoFactorRequired(
                    authMethodsData = TWO_FACTOR_AUTH_METHODS_DATA,
                    ssoToken = null,
                    twoFactorProviders = null,
                ),
            )
            assertEquals(AuthState.Unauthenticated, repository.authStateFlow.value)
            coVerify {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.SingleSignOn(
                        ssoCode = SSO_CODE,
                        ssoCodeVerifier = SSO_CODE_VERIFIER,
                        ssoRedirectUri = SSO_REDIRECT_URI,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            }
        }

    @Test
    fun `SSO login two factor with remember saves two factor auth token`() = runTest {
        // Attempt a normal login with a two factor error first, so that the auth
        // data will be cached.
        coEvery {
            identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.SingleSignOn(
                    ssoCode = SSO_CODE,
                    ssoCodeVerifier = SSO_CODE_VERIFIER,
                    ssoRedirectUri = SSO_REDIRECT_URI,
                ),
                uniqueAppId = UNIQUE_APP_ID,
            )
        } returns GetTokenResponseJson
            .TwoFactorRequired(
                authMethodsData = TWO_FACTOR_AUTH_METHODS_DATA,
                ssoToken = null,
                twoFactorProviders = null,
            )
            .asSuccess()

        val firstResult = repository.login(
            email = EMAIL,
            ssoCode = SSO_CODE,
            ssoCodeVerifier = SSO_CODE_VERIFIER,
            ssoRedirectUri = SSO_REDIRECT_URI,
            organizationIdentifier = ORGANIZATION_IDENTIFIER,
        )
        assertEquals(LoginResult.TwoFactorRequired, firstResult)
        coVerify {
            identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.SingleSignOn(
                    ssoCode = SSO_CODE,
                    ssoCodeVerifier = SSO_CODE_VERIFIER,
                    ssoRedirectUri = SSO_REDIRECT_URI,
                ),
                uniqueAppId = UNIQUE_APP_ID,
            )
        }

        // Login with two factor data.
        val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.copy(
            twoFactorToken = "twoFactorTokenToStore",
        )
        coEvery {
            identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.SingleSignOn(
                    ssoCode = SSO_CODE,
                    ssoCodeVerifier = SSO_CODE_VERIFIER,
                    ssoRedirectUri = SSO_REDIRECT_URI,
                ),
                uniqueAppId = UNIQUE_APP_ID,
                twoFactorData = TWO_FACTOR_DATA,
            )
        } returns successResponse.asSuccess()
        coEvery { vaultRepository.syncIfNecessary() } just runs
        every {
            successResponse.toUserState(
                previousUserState = null,
                environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
            )
        } returns SINGLE_USER_STATE_1
        val finalResult = repository.login(
            email = EMAIL,
            password = null,
            twoFactorData = TWO_FACTOR_DATA,
            orgIdentifier = null,
        )
        assertEquals(LoginResult.Success, finalResult)
        assertNull(repository.twoFactorResponse)
        fakeAuthDiskSource.assertTwoFactorToken(
            email = EMAIL,
            twoFactorToken = "twoFactorTokenToStore",
        )
        verify(exactly = 1) {
            userStateManager.hasPendingAccountAddition = false
        }
    }

    @Test
    fun `SSO login uses remembered two factor tokens`() = runTest {
        fakeAuthDiskSource.storeTwoFactorToken(EMAIL, "storedTwoFactorToken")
        val rememberedTwoFactorData = TwoFactorDataModel(
            code = "storedTwoFactorToken",
            method = TwoFactorAuthMethod.REMEMBER.value.toString(),
            remember = false,
        )
        val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS
        coEvery {
            identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.SingleSignOn(
                    ssoCode = SSO_CODE,
                    ssoCodeVerifier = SSO_CODE_VERIFIER,
                    ssoRedirectUri = SSO_REDIRECT_URI,
                ),
                uniqueAppId = UNIQUE_APP_ID,
                twoFactorData = rememberedTwoFactorData,
            )
        } returns successResponse.asSuccess()
        coEvery { vaultRepository.syncIfNecessary() } just runs
        every {
            GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.toUserState(
                previousUserState = null,
                environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
            )
        } returns SINGLE_USER_STATE_1
        val result = repository.login(
            email = EMAIL,
            ssoCode = SSO_CODE,
            ssoCodeVerifier = SSO_CODE_VERIFIER,
            ssoRedirectUri = SSO_REDIRECT_URI,
            organizationIdentifier = ORGANIZATION_IDENTIFIER,
        )
        assertEquals(LoginResult.Success, result)
        assertEquals(AuthState.Authenticated(ACCESS_TOKEN), repository.authStateFlow.value)
        fakeAuthDiskSource.assertPrivateKey(
            userId = USER_ID_1,
            privateKey = "privateKey",
        )
        fakeAuthDiskSource.assertAccountKeys(
            userId = USER_ID_1,
            accountKeys = ACCOUNT_KEYS,
        )
        fakeAuthDiskSource.assertUserKey(
            userId = USER_ID_1,
            userKey = "key",
        )
        coVerify {
            identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.SingleSignOn(
                    ssoCode = SSO_CODE,
                    ssoCodeVerifier = SSO_CODE_VERIFIER,
                    ssoRedirectUri = SSO_REDIRECT_URI,
                ),
                uniqueAppId = UNIQUE_APP_ID,
                twoFactorData = rememberedTwoFactorData,
            )
            vaultRepository.syncIfNecessary()
        }
        assertEquals(
            SINGLE_USER_STATE_1,
            fakeAuthDiskSource.userState,
        )
        verify(exactly = 1) {
            userStateManager.hasPendingAccountAddition = false
            settingsRepository.setDefaultsIfNecessary(userId = USER_ID_1)
        }
    }

    @Test
    fun `register check data breaches error should still return register success`() = runTest {
        coEvery {
            haveIBeenPwnedService.hasPasswordBeenBreached(PASSWORD)
        } returns Throwable().asFailure()
        coEvery {
            identityService.register(
                body = RegisterRequestJson(
                    email = EMAIL,
                    masterPasswordHash = PASSWORD_HASH,
                    masterPasswordHint = null,
                    key = ENCRYPTED_USER_KEY,
                    keys = RegisterRequestJson.Keys(
                        publicKey = PUBLIC_KEY,
                        encryptedPrivateKey = PRIVATE_KEY,
                    ),
                    kdfType = KdfTypeJson.PBKDF2_SHA256,
                    kdfIterations = DEFAULT_KDF_ITERATIONS.toUInt(),
                ),
            )
        } returns RegisterResponseJson.Success.asSuccess()

        val result = repository.register(
            email = EMAIL,
            masterPassword = PASSWORD,
            masterPasswordHint = null,
            shouldCheckDataBreaches = true,
            isMasterPasswordStrong = true,
        )
        assertEquals(RegisterResult.Success, result)
    }

    @Test
    fun `register check data breaches found and strong password should return DataBreachFound`() =
        runTest {
            coEvery {
                haveIBeenPwnedService.hasPasswordBeenBreached(PASSWORD)
            } returns true.asSuccess()

            val result = repository.register(
                email = EMAIL,
                masterPassword = PASSWORD,
                masterPasswordHint = null,
                shouldCheckDataBreaches = true,
                isMasterPasswordStrong = true,
            )
            assertEquals(RegisterResult.DataBreachFound, result)
        }

    @Test
    fun `register check data breaches and weak password should return DataBreachAndWeakPassword`() =
        runTest {
            coEvery {
                haveIBeenPwnedService.hasPasswordBeenBreached(PASSWORD)
            } returns true.asSuccess()

            val result = repository.register(
                email = EMAIL,
                masterPassword = PASSWORD,
                masterPasswordHint = null,
                shouldCheckDataBreaches = true,
                isMasterPasswordStrong = false,
            )
            assertEquals(RegisterResult.DataBreachAndWeakPassword, result)
        }

    @Test
    fun `register check no data breaches found with weak password should return WeakPassword`() =
        runTest {
            coEvery {
                haveIBeenPwnedService.hasPasswordBeenBreached(PASSWORD)
            } returns false.asSuccess()

            val result = repository.register(
                email = EMAIL,
                masterPassword = PASSWORD,
                masterPasswordHint = null,
                shouldCheckDataBreaches = true,
                isMasterPasswordStrong = false,
            )
            assertEquals(RegisterResult.WeakPassword, result)
        }

    @Test
    fun `register check data breaches Success should return Success`() = runTest {
        coEvery {
            haveIBeenPwnedService.hasPasswordBeenBreached(PASSWORD)
        } returns false.asSuccess()
        coEvery {
            identityService.register(
                body = RegisterRequestJson(
                    email = EMAIL,
                    masterPasswordHash = PASSWORD_HASH,
                    masterPasswordHint = null,
                    key = ENCRYPTED_USER_KEY,
                    keys = RegisterRequestJson.Keys(
                        publicKey = PUBLIC_KEY,
                        encryptedPrivateKey = PRIVATE_KEY,
                    ),
                    kdfType = KdfTypeJson.PBKDF2_SHA256,
                    kdfIterations = DEFAULT_KDF_ITERATIONS.toUInt(),
                ),
            )
        } returns RegisterResponseJson.Success.asSuccess()

        val result = repository.register(
            email = EMAIL,
            masterPassword = PASSWORD,
            masterPasswordHint = null,
            shouldCheckDataBreaches = true,
            isMasterPasswordStrong = true,
        )
        assertEquals(RegisterResult.Success, result)
        coVerify { haveIBeenPwnedService.hasPasswordBeenBreached(PASSWORD) }
    }

    @Test
    fun `register Success should return Success`() = runTest {
        coEvery { identityService.preLogin(EMAIL) } returns PRE_LOGIN_SUCCESS.asSuccess()
        coEvery {
            identityService.register(
                body = RegisterRequestJson(
                    email = EMAIL,
                    masterPasswordHash = PASSWORD_HASH,
                    masterPasswordHint = null,
                    key = ENCRYPTED_USER_KEY,
                    keys = RegisterRequestJson.Keys(
                        publicKey = PUBLIC_KEY,
                        encryptedPrivateKey = PRIVATE_KEY,
                    ),
                    kdfType = KdfTypeJson.PBKDF2_SHA256,
                    kdfIterations = DEFAULT_KDF_ITERATIONS.toUInt(),
                ),
            )
        } returns RegisterResponseJson.Success.asSuccess()

        val result = repository.register(
            email = EMAIL,
            masterPassword = PASSWORD,
            masterPasswordHint = null,
            shouldCheckDataBreaches = false,
            isMasterPasswordStrong = true,
        )
        assertEquals(RegisterResult.Success, result)
    }

    @Test
    fun `register Failure should return Error with no message`() = runTest {
        val error = RuntimeException()
        coEvery { identityService.preLogin(EMAIL) } returns PRE_LOGIN_SUCCESS.asSuccess()
        coEvery {
            identityService.register(
                body = RegisterRequestJson(
                    email = EMAIL,
                    masterPasswordHash = PASSWORD_HASH,
                    masterPasswordHint = null,
                    key = ENCRYPTED_USER_KEY,
                    keys = RegisterRequestJson.Keys(
                        publicKey = PUBLIC_KEY,
                        encryptedPrivateKey = PRIVATE_KEY,
                    ),
                    kdfType = KdfTypeJson.PBKDF2_SHA256,
                    kdfIterations = DEFAULT_KDF_ITERATIONS.toUInt(),
                ),
            )
        } returns error.asFailure()

        val result = repository.register(
            email = EMAIL,
            masterPassword = PASSWORD,
            masterPasswordHint = null,
            shouldCheckDataBreaches = false,
            isMasterPasswordStrong = true,
        )
        assertEquals(RegisterResult.Error(errorMessage = null, error = error), result)
    }

    @Test
    fun `register returns Invalid should return Error with invalid message`() = runTest {
        coEvery { identityService.preLogin(EMAIL) } returns PRE_LOGIN_SUCCESS.asSuccess()
        coEvery {
            identityService.register(
                body = RegisterRequestJson(
                    email = EMAIL,
                    masterPasswordHash = PASSWORD_HASH,
                    masterPasswordHint = null,
                    key = ENCRYPTED_USER_KEY,
                    keys = RegisterRequestJson.Keys(
                        publicKey = PUBLIC_KEY,
                        encryptedPrivateKey = PRIVATE_KEY,
                    ),
                    kdfType = KdfTypeJson.PBKDF2_SHA256,
                    kdfIterations = DEFAULT_KDF_ITERATIONS.toUInt(),
                ),
            )
        } returns RegisterResponseJson
            .Invalid(invalidMessage = "message", validationErrors = mapOf())
            .asSuccess()

        val result = repository.register(
            email = EMAIL,
            masterPassword = PASSWORD,
            masterPasswordHint = null,
            shouldCheckDataBreaches = false,
            isMasterPasswordStrong = true,
        )
        assertEquals(RegisterResult.Error(errorMessage = "message", error = null), result)
    }

    @Test
    fun `register returns Invalid should return Error with first message in map`() = runTest {
        coEvery { identityService.preLogin(EMAIL) } returns PRE_LOGIN_SUCCESS.asSuccess()
        coEvery {
            identityService.register(
                body = RegisterRequestJson(
                    email = EMAIL,
                    masterPasswordHash = PASSWORD_HASH,
                    masterPasswordHint = null,
                    key = ENCRYPTED_USER_KEY,
                    keys = RegisterRequestJson.Keys(
                        publicKey = PUBLIC_KEY,
                        encryptedPrivateKey = PRIVATE_KEY,
                    ),
                    kdfType = KdfTypeJson.PBKDF2_SHA256,
                    kdfIterations = DEFAULT_KDF_ITERATIONS.toUInt(),
                ),
            )
        } returns RegisterResponseJson
            .Invalid(
                invalidMessage = "message",
                validationErrors = mapOf("" to listOf("expected")),
            )
            .asSuccess()

        val result = repository.register(
            email = EMAIL,
            masterPassword = PASSWORD,
            masterPasswordHint = null,
            shouldCheckDataBreaches = false,
            isMasterPasswordStrong = true,
        )
        assertEquals(RegisterResult.Error(errorMessage = "expected", error = null), result)
    }

    @Test
    fun `register with email token Success should return Success`() = runTest {
        coEvery { identityService.preLogin(EMAIL) } returns PRE_LOGIN_SUCCESS.asSuccess()
        coEvery {
            identityService.registerFinish(
                body = RegisterFinishRequestJson(
                    email = EMAIL,
                    masterPasswordHash = PASSWORD_HASH,
                    masterPasswordHint = null,
                    emailVerificationToken = EMAIL_VERIFICATION_TOKEN,
                    userSymmetricKey = ENCRYPTED_USER_KEY,
                    userAsymmetricKeys = RegisterFinishRequestJson.Keys(
                        publicKey = PUBLIC_KEY,
                        encryptedPrivateKey = PRIVATE_KEY,
                    ),
                    kdfType = KdfTypeJson.PBKDF2_SHA256,
                    kdfIterations = DEFAULT_KDF_ITERATIONS.toUInt(),
                ),
            )
        } returns RegisterResponseJson.Success.asSuccess()

        val result = repository.register(
            email = EMAIL,
            masterPassword = PASSWORD,
            masterPasswordHint = null,
            emailVerificationToken = EMAIL_VERIFICATION_TOKEN,
            shouldCheckDataBreaches = false,
            isMasterPasswordStrong = true,
        )
        assertEquals(RegisterResult.Success, result)
    }

    @Test
    fun `removePassword with no active account should return error`() = runTest {
        fakeAuthDiskSource.userState = null

        val result = repository.removePassword(masterPassword = PASSWORD)

        assertEquals(RemovePasswordResult.Error(error = NoActiveUserException()), result)
    }

    @Test
    fun `removePassword with no userKey should return error`() = runTest {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        fakeAuthDiskSource.storeUserKey(userId = USER_ID_1, userKey = null)

        val result = repository.removePassword(masterPassword = PASSWORD)

        assertEquals(
            RemovePasswordResult.Error(error = MissingPropertyException("User Key")),
            result,
        )
    }

    @Test
    fun `removePassword with no keyConnectorUrl should return error`() = runTest {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        fakeAuthDiskSource.storeUserKey(userId = USER_ID_1, userKey = ENCRYPTED_USER_KEY)
        val organizations = listOf(
            mockk<SyncResponseJson.Profile.Organization> {
                every { id } returns "orgId"
                every { name } returns "orgName"
                every { permissions } returns mockk {
                    every { shouldManageResetPassword } returns false
                }
                every { shouldUseKeyConnector } returns true
                every { type } returns OrganizationType.USER
                every { keyConnectorUrl } returns null
                every { userIsClaimedByOrganization } returns false
                every { limitItemDeletion } returns false
            },
        )
        fakeAuthDiskSource.storeOrganizations(userId = USER_ID_1, organizations = organizations)

        val result = repository.removePassword(masterPassword = PASSWORD)

        assertEquals(
            RemovePasswordResult.Error(error = MissingPropertyException("Key Connector URL")),
            result,
        )
    }

    @Test
    fun `removePassword with migrateExistingUserToKeyConnector exception should return error`() =
        runTest {
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            fakeAuthDiskSource.storeUserKey(userId = USER_ID_1, userKey = ENCRYPTED_USER_KEY)
            val url = "www.example.com"
            val error = Throwable("Fail!")
            val organizations = listOf(
                mockk<SyncResponseJson.Profile.Organization> {
                    every { id } returns "orgId"
                    every { name } returns "orgName"
                    every { permissions } returns mockk {
                        every { shouldManageResetPassword } returns false
                    }
                    every { shouldUseKeyConnector } returns true
                    every { type } returns OrganizationType.USER
                    every { keyConnectorUrl } returns url
                    every { userIsClaimedByOrganization } returns false
                    every { limitItemDeletion } returns false
                },
            )
            fakeAuthDiskSource.storeOrganizations(userId = USER_ID_1, organizations = organizations)
            coEvery {
                keyConnectorManager.migrateExistingUserToKeyConnector(
                    userId = USER_ID_1,
                    url = url,
                    userKeyEncrypted = ENCRYPTED_USER_KEY,
                    email = PROFILE_1.email,
                    masterPassword = PASSWORD,
                    kdf = PROFILE_1.toSdkParams(),
                )
            } returns error.asFailure()

            val result = repository.removePassword(masterPassword = PASSWORD)

            assertEquals(RemovePasswordResult.Error(error = error), result)
        }

    @Test
    fun `removePassword with migrateExistingUserToKeyConnector error should return error`() =
        runTest {
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            fakeAuthDiskSource.storeUserKey(userId = USER_ID_1, userKey = ENCRYPTED_USER_KEY)
            val url = "www.example.com"
            val error = Throwable("Fail!")
            val expectedResult = MigrateExistingUserToKeyConnectorResult.Error(error)
            val organizations = listOf(
                mockk<SyncResponseJson.Profile.Organization> {
                    every { id } returns "orgId"
                    every { name } returns "orgName"
                    every { permissions } returns mockk {
                        every { shouldManageResetPassword } returns false
                    }
                    every { shouldUseKeyConnector } returns true
                    every { type } returns OrganizationType.USER
                    every { keyConnectorUrl } returns url
                    every { userIsClaimedByOrganization } returns false
                    every { limitItemDeletion } returns false
                },
            )
            fakeAuthDiskSource.storeOrganizations(userId = USER_ID_1, organizations = organizations)
            coEvery {
                keyConnectorManager.migrateExistingUserToKeyConnector(
                    userId = USER_ID_1,
                    url = url,
                    userKeyEncrypted = ENCRYPTED_USER_KEY,
                    email = PROFILE_1.email,
                    masterPassword = PASSWORD,
                    kdf = PROFILE_1.toSdkParams(),
                )
            } returns expectedResult.asSuccess()

            val result = repository.removePassword(masterPassword = PASSWORD)

            assertEquals(
                RemovePasswordResult.Error(error = error),
                result,
            )
        }

    @Test
    @Suppress("MaxLineLength")
    fun `removePassword with migrateExistingUserToKeyConnector wrong password error should return WrongPasswordError error`() =
        runTest {
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            fakeAuthDiskSource.storeUserKey(userId = USER_ID_1, userKey = ENCRYPTED_USER_KEY)
            val url = "www.example.com"
            val expectedResult = MigrateExistingUserToKeyConnectorResult.WrongPasswordError
            val organizations = listOf(
                mockk<SyncResponseJson.Profile.Organization> {
                    every { id } returns "orgId"
                    every { name } returns "orgName"
                    every { permissions } returns mockk {
                        every { shouldManageResetPassword } returns false
                    }
                    every { shouldUseKeyConnector } returns true
                    every { type } returns OrganizationType.USER
                    every { keyConnectorUrl } returns url
                    every { userIsClaimedByOrganization } returns false
                    every { limitItemDeletion } returns false
                },
            )
            fakeAuthDiskSource.storeOrganizations(userId = USER_ID_1, organizations = organizations)
            coEvery {
                keyConnectorManager.migrateExistingUserToKeyConnector(
                    userId = USER_ID_1,
                    url = url,
                    userKeyEncrypted = ENCRYPTED_USER_KEY,
                    email = PROFILE_1.email,
                    masterPassword = PASSWORD,
                    kdf = PROFILE_1.toSdkParams(),
                )
            } returns expectedResult.asSuccess()

            val result = repository.removePassword(masterPassword = PASSWORD)

            assertEquals(
                RemovePasswordResult.WrongPasswordError,
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `removePassword with migrateExistingUserToKeyConnector success should sync and return success`() =
        runTest {
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            fakeAuthDiskSource.storeUserKey(userId = USER_ID_1, userKey = ENCRYPTED_USER_KEY)
            val url = "www.example.com"
            val organizations = listOf(
                mockk<SyncResponseJson.Profile.Organization> {
                    every { id } returns "orgId"
                    every { name } returns "orgName"
                    every { permissions } returns mockk {
                        every { shouldManageResetPassword } returns false
                    }
                    every { shouldUseKeyConnector } returns true
                    every { type } returns OrganizationType.USER
                    every { keyConnectorUrl } returns url
                    every { userIsClaimedByOrganization } returns false
                    every { limitItemDeletion } returns false
                },
            )
            fakeAuthDiskSource.storeOrganizations(userId = USER_ID_1, organizations = organizations)
            coEvery {
                keyConnectorManager.migrateExistingUserToKeyConnector(
                    userId = USER_ID_1,
                    url = url,
                    userKeyEncrypted = ENCRYPTED_USER_KEY,
                    email = PROFILE_1.email,
                    masterPassword = PASSWORD,
                    kdf = PROFILE_1.toSdkParams(),
                )
            } returns MigrateExistingUserToKeyConnectorResult.Success.asSuccess()
            every {
                SINGLE_USER_STATE_1.toRemovedPasswordUserStateJson(userId = USER_ID_1)
            } returns SINGLE_USER_STATE_1
            every { vaultRepository.sync() } just runs
            every { settingsRepository.setDefaultsIfNecessary(userId = USER_ID_1) } just runs

            val result = repository.removePassword(masterPassword = PASSWORD)

            assertEquals(RemovePasswordResult.Success, result)
            verify(exactly = 1) {
                SINGLE_USER_STATE_1.toRemovedPasswordUserStateJson(userId = USER_ID_1)
                vaultRepository.sync()
                settingsRepository.setDefaultsIfNecessary(userId = USER_ID_1)
            }
        }

    @Test
    fun `resetPassword Success should return Success`() = runTest {
        val currentPassword = "currentPassword"
        val currentPasswordHash = "hashedCurrentPassword"
        val newPassword = "newPassword"
        val newPasswordHash = "newPasswordHash"
        val newKey = "newKey"
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        coEvery {
            authSdkSource.hashPassword(
                email = ACCOUNT_1.profile.email,
                password = currentPassword,
                kdf = ACCOUNT_1.profile.toSdkParams(),
                purpose = HashPurpose.SERVER_AUTHORIZATION,
            )
        } returns currentPasswordHash.asSuccess()
        coEvery {
            vaultSdkSource.updatePassword(
                userId = ACCOUNT_1.profile.userId,
                newPassword = newPassword,
            )
        } returns UpdatePasswordResponse(
            passwordHash = newPasswordHash,
            newKey = newKey,
        )
            .asSuccess()
        coEvery {
            accountsService.resetPassword(
                body = ResetPasswordRequestJson(
                    currentPasswordHash = currentPasswordHash,
                    newPasswordHash = newPasswordHash,
                    passwordHint = null,
                    key = newKey,
                ),
            )
        } returns Unit.asSuccess()
        coEvery {
            authSdkSource.hashPassword(
                email = ACCOUNT_1.profile.email,
                password = newPassword,
                kdf = ACCOUNT_1.profile.toSdkParams(),
                purpose = HashPurpose.LOCAL_AUTHORIZATION,
            )
        } returns newPasswordHash.asSuccess()

        val result = repository.resetPassword(
            currentPassword = currentPassword,
            newPassword = newPassword,
            passwordHint = null,
        )

        assertEquals(
            ResetPasswordResult.Success,
            result,
        )
        coVerify {
            authSdkSource.hashPassword(
                email = ACCOUNT_1.profile.email,
                password = currentPassword,
                kdf = ACCOUNT_1.profile.toSdkParams(),
                purpose = HashPurpose.SERVER_AUTHORIZATION,
            )
            vaultSdkSource.updatePassword(
                userId = ACCOUNT_1.profile.userId,
                newPassword = newPassword,
            )
            accountsService.resetPassword(
                body = ResetPasswordRequestJson(
                    currentPasswordHash = currentPasswordHash,
                    newPasswordHash = newPasswordHash,
                    passwordHint = null,
                    key = newKey,
                ),
            )
        }
        fakeAuthDiskSource.assertMasterPasswordHash(
            userId = USER_ID_1,
            passwordHash = newPasswordHash,
        )
        verify {
            userLogoutManager.logout(
                userId = ACCOUNT_1.profile.userId,
                reason = LogoutReason.PasswordReset,
            )
        }
    }

    @Test
    fun `resetPassword Failure should return Error`() = runTest {
        val currentPassword = "currentPassword"
        val currentPasswordHash = "hashedCurrentPassword"
        val newPassword = "newPassword"
        val error = Throwable("Fail")
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        coEvery {
            authSdkSource.hashPassword(
                email = ACCOUNT_1.profile.email,
                password = currentPassword,
                kdf = ACCOUNT_1.profile.toSdkParams(),
                purpose = HashPurpose.SERVER_AUTHORIZATION,
            )
        } returns currentPasswordHash.asSuccess()
        coEvery {
            vaultSdkSource.updatePassword(
                userId = ACCOUNT_1.profile.userId,
                newPassword = newPassword,
            )
        } returns error.asFailure()

        val result = repository.resetPassword(
            currentPassword = currentPassword,
            newPassword = newPassword,
            passwordHint = null,
        )

        assertEquals(
            ResetPasswordResult.Error(error = error),
            result,
        )
        coVerify {
            authSdkSource.hashPassword(
                email = ACCOUNT_1.profile.email,
                password = currentPassword,
                kdf = ACCOUNT_1.profile.toSdkParams(),
                purpose = HashPurpose.SERVER_AUTHORIZATION,
            )
            vaultSdkSource.updatePassword(
                userId = ACCOUNT_1.profile.userId,
                newPassword = newPassword,
            )
        }
    }

    @Test
    fun `setPassword without active account should return Error`() = runTest {
        fakeAuthDiskSource.userState = null

        val result = repository.setPassword(
            organizationIdentifier = "organizationId",
            password = "password",
            passwordHint = "passwordHint",
        )

        assertEquals(SetPasswordResult.Error(error = NoActiveUserException()), result)
        fakeAuthDiskSource.assertMasterPasswordHash(userId = USER_ID_1, passwordHash = null)
        fakeAuthDiskSource.assertPrivateKey(userId = USER_ID_1, privateKey = null)
        fakeAuthDiskSource.assertAccountKeys(userId = USER_ID_1, accountKeys = null)
        fakeAuthDiskSource.assertUserKey(userId = USER_ID_1, userKey = null)
    }

    @Test
    fun `setPassword with authSdkSource hashPassword failure should return Error`() = runTest {
        val password = "password"
        val error = Throwable("Fail")
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        coEvery {
            authSdkSource.hashPassword(
                email = EMAIL,
                password = password,
                kdf = SINGLE_USER_STATE_1.activeAccount.profile.toSdkParams(),
                purpose = HashPurpose.SERVER_AUTHORIZATION,
            )
        } returns error.asFailure()

        val result = repository.setPassword(
            organizationIdentifier = "organizationId",
            password = password,
            passwordHint = "passwordHint",
        )

        assertEquals(SetPasswordResult.Error(error = error), result)
        fakeAuthDiskSource.assertMasterPasswordHash(userId = USER_ID_1, passwordHash = null)
        fakeAuthDiskSource.assertPrivateKey(userId = USER_ID_1, privateKey = null)
        fakeAuthDiskSource.assertUserKey(userId = USER_ID_1, userKey = null)
    }

    @Test
    fun `setPassword with authSdkSource makeRegisterKeys failure should return Error`() = runTest {
        val password = "password"
        val passwordHash = "passwordHash"
        val error = Throwable("Fail")
        val kdf = SINGLE_USER_STATE_1.activeAccount.profile.toSdkParams()
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        coEvery {
            authSdkSource.hashPassword(
                email = EMAIL,
                password = password,
                kdf = kdf,
                purpose = HashPurpose.SERVER_AUTHORIZATION,
            )
        } returns passwordHash.asSuccess()
        coEvery {
            authSdkSource.makeRegisterKeys(
                email = EMAIL,
                password = password,
                kdf = kdf,
            )
        } returns error.asFailure()

        val result = repository.setPassword(
            organizationIdentifier = "organizationId",
            password = password,
            passwordHint = "passwordHint",
        )

        assertEquals(SetPasswordResult.Error(error = error), result)
        fakeAuthDiskSource.assertMasterPasswordHash(userId = USER_ID_1, passwordHash = null)
        fakeAuthDiskSource.assertPrivateKey(userId = USER_ID_1, privateKey = null)
        fakeAuthDiskSource.assertAccountKeys(userId = USER_ID_1, accountKeys = null)
        fakeAuthDiskSource.assertUserKey(userId = USER_ID_1, userKey = null)
    }

    @Test
    fun `setPassword with vaultSdkSource updatePassword failure should return Error`() = runTest {
        val password = "password"
        val passwordHash = "passwordHash"
        val error = Throwable("Fail")
        val kdf = SINGLE_USER_STATE_1.activeAccount.profile.toSdkParams()
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1.copy(
            accounts = mapOf(
                USER_ID_1 to ACCOUNT_1.copy(
                    profile = PROFILE_1.copy(
                        forcePasswordResetReason = ForcePasswordResetReason
                            .TDE_USER_WITHOUT_PASSWORD_HAS_PASSWORD_RESET_PERMISSION,
                    ),
                ),
            ),
        )
        coEvery {
            authSdkSource.hashPassword(
                email = EMAIL,
                password = password,
                kdf = kdf,
                purpose = HashPurpose.SERVER_AUTHORIZATION,
            )
        } returns passwordHash.asSuccess()
        coEvery {
            vaultSdkSource.updatePassword(userId = USER_ID_1, newPassword = password)
        } returns error.asFailure()

        val result = repository.setPassword(
            organizationIdentifier = "organizationId",
            password = password,
            passwordHint = "passwordHint",
        )

        assertEquals(SetPasswordResult.Error(error = error), result)
        fakeAuthDiskSource.assertMasterPasswordHash(userId = USER_ID_1, passwordHash = null)
        fakeAuthDiskSource.assertPrivateKey(userId = USER_ID_1, privateKey = null)
        fakeAuthDiskSource.assertAccountKeys(userId = USER_ID_1, accountKeys = null)
        fakeAuthDiskSource.assertUserKey(userId = USER_ID_1, userKey = null)
    }

    @Test
    fun `setPassword with accountsService setPassword failure should return Error`() = runTest {
        val password = "password"
        val passwordHash = "passwordHash"
        val passwordHint = "passwordHint"
        val organizationId = ORGANIZATION_IDENTIFIER
        val encryptedUserKey = "encryptedUserKey"
        val privateRsaKey = "privateRsaKey"
        val publicRsaKey = "publicRsaKey"
        val profile = SINGLE_USER_STATE_1.activeAccount.profile
        val kdf = profile.toSdkParams()
        val error = Throwable("Fail")
        val registerKeyResponse = RegisterKeyResponse(
            masterPasswordHash = passwordHash,
            encryptedUserKey = encryptedUserKey,
            keys = RsaKeyPair(public = publicRsaKey, private = privateRsaKey),
        )
        val setPasswordRequestJson = SetPasswordRequestJson(
            passwordHash = passwordHash,
            passwordHint = passwordHint,
            organizationIdentifier = organizationId,
            kdfIterations = profile.kdfIterations,
            kdfMemory = profile.kdfMemory,
            kdfParallelism = profile.kdfParallelism,
            kdfType = profile.kdfType,
            key = encryptedUserKey,
            keys = RegisterRequestJson.Keys(
                publicKey = publicRsaKey,
                encryptedPrivateKey = privateRsaKey,
            ),
        )
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        coEvery {
            authSdkSource.hashPassword(
                email = EMAIL,
                password = password,
                kdf = kdf,
                purpose = HashPurpose.SERVER_AUTHORIZATION,
            )
        } returns passwordHash.asSuccess()
        coEvery {
            authSdkSource.makeRegisterKeys(email = EMAIL, password = password, kdf = kdf)
        } returns registerKeyResponse.asSuccess()
        coEvery {
            accountsService.setPassword(body = setPasswordRequestJson)
        } returns error.asFailure()

        val result = repository.setPassword(
            organizationIdentifier = organizationId,
            password = password,
            passwordHint = passwordHint,
        )

        assertEquals(SetPasswordResult.Error(error = error), result)
        fakeAuthDiskSource.assertMasterPasswordHash(userId = USER_ID_1, passwordHash = null)
        fakeAuthDiskSource.assertPrivateKey(userId = USER_ID_1, privateKey = null)
        fakeAuthDiskSource.assertAccountKeys(userId = USER_ID_1, accountKeys = null)
        fakeAuthDiskSource.assertUserKey(userId = USER_ID_1, userKey = null)
    }

    @Test
    fun `setPassword with accountsService setPassword success should return Success`() = runTest {
        val password = "password"
        val passwordHash = "passwordHash"
        val passwordHint = "passwordHint"
        val organizationIdentifier = ORGANIZATION_IDENTIFIER
        val organizationId = "orgId"
        val encryptedUserKey = "encryptedUserKey"
        val privateRsaKey = "privateRsaKey"
        val publicRsaKey = "publicRsaKey"
        val publicOrgKey = "publicOrgKey"
        val resetPasswordKey = "resetPasswordKey"
        val profile = SINGLE_USER_STATE_1.activeAccount.profile
        val kdf = profile.toSdkParams()
        val registerKeyResponse = RegisterKeyResponse(
            masterPasswordHash = passwordHash,
            encryptedUserKey = encryptedUserKey,
            keys = RsaKeyPair(public = publicRsaKey, private = privateRsaKey),
        )
        val setPasswordRequestJson = SetPasswordRequestJson(
            passwordHash = passwordHash,
            passwordHint = passwordHint,
            organizationIdentifier = organizationIdentifier,
            kdfIterations = profile.kdfIterations,
            kdfMemory = profile.kdfMemory,
            kdfParallelism = profile.kdfParallelism,
            kdfType = profile.kdfType,
            key = encryptedUserKey,
            keys = RegisterRequestJson.Keys(
                publicKey = publicRsaKey,
                encryptedPrivateKey = privateRsaKey,
            ),
        )
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        coEvery {
            authSdkSource.hashPassword(
                email = EMAIL,
                password = password,
                kdf = kdf,
                purpose = HashPurpose.SERVER_AUTHORIZATION,
            )
        } returns passwordHash.asSuccess()
        coEvery {
            authSdkSource.makeRegisterKeys(email = EMAIL, password = password, kdf = kdf)
        } returns registerKeyResponse.asSuccess()
        coEvery {
            accountsService.setPassword(body = setPasswordRequestJson)
        } returns Unit.asSuccess()
        coEvery {
            organizationService.getOrganizationAutoEnrollStatus(organizationIdentifier)
        } returns OrganizationAutoEnrollStatusResponseJson(
            organizationId = organizationId,
            isResetPasswordEnabled = true,
        )
            .asSuccess()
        coEvery {
            organizationService.getOrganizationKeys(organizationId)
        } returns OrganizationKeysResponseJson(
            privateKey = "",
            publicKey = publicOrgKey,
        )
            .asSuccess()
        coEvery {
            organizationService.organizationResetPasswordEnroll(
                organizationId = organizationId,
                userId = profile.userId,
                passwordHash = passwordHash,
                resetPasswordKey = resetPasswordKey,
            )
        } returns Unit.asSuccess()
        coEvery {
            vaultSdkSource.getResetPasswordKey(
                orgPublicKey = publicOrgKey,
                userId = profile.userId,
            )
        } returns resetPasswordKey.asSuccess()
        coEvery {
            vaultRepository.unlockVaultWithMasterPassword(password)
        } returns VaultUnlockResult.Success

        val result = repository.setPassword(
            organizationIdentifier = organizationIdentifier,
            password = password,
            passwordHint = passwordHint,
        )

        assertEquals(SetPasswordResult.Success, result)
        fakeAuthDiskSource.assertMasterPasswordHash(userId = USER_ID_1, passwordHash = passwordHash)
        fakeAuthDiskSource.assertPrivateKey(userId = USER_ID_1, privateKey = privateRsaKey)
        fakeAuthDiskSource.assertUserKey(userId = USER_ID_1, userKey = encryptedUserKey)
        fakeAuthDiskSource.assertUserState(SINGLE_USER_STATE_1_WITH_PASS)
        coVerify {
            authSdkSource.hashPassword(
                email = EMAIL,
                password = password,
                kdf = kdf,
                purpose = HashPurpose.SERVER_AUTHORIZATION,
            )
            authSdkSource.makeRegisterKeys(email = EMAIL, password = password, kdf = kdf)
            accountsService.setPassword(body = setPasswordRequestJson)
            organizationService.getOrganizationAutoEnrollStatus(organizationIdentifier)
            organizationService.getOrganizationKeys(organizationId)
            organizationService.organizationResetPasswordEnroll(
                organizationId = organizationId,
                userId = profile.userId,
                passwordHash = passwordHash,
                resetPasswordKey = resetPasswordKey,
            )
            vaultRepository.unlockVaultWithMasterPassword(password)
            vaultSdkSource.getResetPasswordKey(
                orgPublicKey = publicOrgKey,
                userId = profile.userId,
            )
        }
    }

    @Test
    fun `setPassword with updatePassword success should return Success`() = runTest {
        val password = "password"
        val passwordHash = "passwordHash"
        val passwordHint = "passwordHint"
        val organizationIdentifier = ORGANIZATION_IDENTIFIER
        val organizationId = "orgId"
        val encryptedUserKey = "encryptedUserKey"
        val publicOrgKey = "publicOrgKey"
        val resetPasswordKey = "resetPasswordKey"
        val userState = SINGLE_USER_STATE_1.copy(
            accounts = mapOf(
                USER_ID_1 to ACCOUNT_1.copy(
                    profile = PROFILE_1.copy(
                        forcePasswordResetReason = ForcePasswordResetReason
                            .TDE_USER_WITHOUT_PASSWORD_HAS_PASSWORD_RESET_PERMISSION,
                    ),
                ),
            ),
        )
        val profile = userState.activeAccount.profile
        val kdf = profile.toSdkParams()
        val updatePasswordResponse = UpdatePasswordResponse(
            passwordHash = passwordHash,
            newKey = encryptedUserKey,
        )
        val setPasswordRequestJson = SetPasswordRequestJson(
            passwordHash = passwordHash,
            passwordHint = passwordHint,
            organizationIdentifier = organizationIdentifier,
            kdfIterations = profile.kdfIterations,
            kdfMemory = profile.kdfMemory,
            kdfParallelism = profile.kdfParallelism,
            kdfType = profile.kdfType,
            key = encryptedUserKey,
            keys = null,
        )
        fakeAuthDiskSource.userState = userState
        coEvery {
            authSdkSource.hashPassword(
                email = EMAIL,
                password = password,
                kdf = kdf,
                purpose = HashPurpose.SERVER_AUTHORIZATION,
            )
        } returns passwordHash.asSuccess()
        coEvery {
            vaultSdkSource.updatePassword(userId = USER_ID_1, newPassword = password)
        } returns updatePasswordResponse.asSuccess()
        coEvery {
            accountsService.setPassword(body = setPasswordRequestJson)
        } returns Unit.asSuccess()
        coEvery {
            organizationService.getOrganizationAutoEnrollStatus(organizationIdentifier)
        } returns OrganizationAutoEnrollStatusResponseJson(
            organizationId = organizationId,
            isResetPasswordEnabled = true,
        )
            .asSuccess()
        coEvery {
            organizationService.getOrganizationKeys(organizationId)
        } returns OrganizationKeysResponseJson(
            privateKey = "",
            publicKey = publicOrgKey,
        )
            .asSuccess()
        coEvery {
            organizationService.organizationResetPasswordEnroll(
                organizationId = organizationId,
                userId = profile.userId,
                passwordHash = passwordHash,
                resetPasswordKey = resetPasswordKey,
            )
        } returns Unit.asSuccess()
        coEvery {
            vaultSdkSource.getResetPasswordKey(
                orgPublicKey = publicOrgKey,
                userId = profile.userId,
            )
        } returns resetPasswordKey.asSuccess()
        coEvery {
            vaultRepository.unlockVaultWithMasterPassword(password)
        } returns VaultUnlockResult.Success

        val result = repository.setPassword(
            organizationIdentifier = organizationIdentifier,
            password = password,
            passwordHint = passwordHint,
        )

        assertEquals(SetPasswordResult.Success, result)
        fakeAuthDiskSource.assertMasterPasswordHash(
            userId = USER_ID_1,
            passwordHash = passwordHash,
        )
        fakeAuthDiskSource.assertPrivateKey(userId = USER_ID_1, privateKey = null)
        fakeAuthDiskSource.assertAccountKeys(userId = USER_ID_1, accountKeys = null)
        fakeAuthDiskSource.assertUserKey(userId = USER_ID_1, userKey = encryptedUserKey)
        fakeAuthDiskSource.assertUserState(SINGLE_USER_STATE_1_WITH_PASS)
        coVerify(exactly = 1) {
            authSdkSource.hashPassword(
                email = EMAIL,
                password = password,
                kdf = kdf,
                purpose = HashPurpose.SERVER_AUTHORIZATION,
            )
            vaultSdkSource.updatePassword(userId = USER_ID_1, newPassword = password)
            accountsService.setPassword(body = setPasswordRequestJson)
            organizationService.getOrganizationAutoEnrollStatus(organizationIdentifier)
            organizationService.getOrganizationKeys(organizationId)
            organizationService.organizationResetPasswordEnroll(
                organizationId = organizationId,
                userId = profile.userId,
                passwordHash = passwordHash,
                resetPasswordKey = resetPasswordKey,
            )
            vaultRepository.unlockVaultWithMasterPassword(password)
            vaultSdkSource.getResetPasswordKey(
                orgPublicKey = publicOrgKey,
                userId = profile.userId,
            )
        }
    }

    @Test
    fun `setPassword with unlockVaultWithMasterPassword error should return Failure`() = runTest {
        val password = "password"
        val passwordHash = "passwordHash"
        val passwordHint = "passwordHint"
        val organizationIdentifier = ORGANIZATION_IDENTIFIER
        val organizationId = "orgId"
        val encryptedUserKey = "encryptedUserKey"
        val privateRsaKey = "privateRsaKey"
        val publicRsaKey = "publicRsaKey"
        val publicOrgKey = "publicOrgKey"
        val resetPasswordKey = "resetPasswordKey"
        val profile = SINGLE_USER_STATE_1.activeAccount.profile
        val kdf = profile.toSdkParams()
        val registerKeyResponse = RegisterKeyResponse(
            masterPasswordHash = passwordHash,
            encryptedUserKey = encryptedUserKey,
            keys = RsaKeyPair(public = publicRsaKey, private = privateRsaKey),
        )
        val setPasswordRequestJson = SetPasswordRequestJson(
            passwordHash = passwordHash,
            passwordHint = passwordHint,
            organizationIdentifier = organizationIdentifier,
            kdfIterations = profile.kdfIterations,
            kdfMemory = profile.kdfMemory,
            kdfParallelism = profile.kdfParallelism,
            kdfType = profile.kdfType,
            key = encryptedUserKey,
            keys = RegisterRequestJson.Keys(
                publicKey = publicRsaKey,
                encryptedPrivateKey = privateRsaKey,
            ),
        )
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        coEvery {
            authSdkSource.hashPassword(
                email = EMAIL,
                password = password,
                kdf = kdf,
                purpose = HashPurpose.SERVER_AUTHORIZATION,
            )
        } returns passwordHash.asSuccess()
        coEvery {
            authSdkSource.makeRegisterKeys(email = EMAIL, password = password, kdf = kdf)
        } returns registerKeyResponse.asSuccess()
        coEvery {
            accountsService.setPassword(body = setPasswordRequestJson)
        } returns Unit.asSuccess()
        val error = Throwable("Fail")
        coEvery {
            vaultRepository.unlockVaultWithMasterPassword(password)
        } returns VaultUnlockResult.GenericError(error = error)

        val result = repository.setPassword(
            organizationIdentifier = organizationIdentifier,
            password = password,
            passwordHint = passwordHint,
        )

        assertEquals(SetPasswordResult.Error(error = error), result)
        fakeAuthDiskSource.assertMasterPasswordHash(userId = USER_ID_1, passwordHash = null)
        fakeAuthDiskSource.assertPrivateKey(userId = USER_ID_1, privateKey = privateRsaKey)
        fakeAuthDiskSource.assertUserKey(userId = USER_ID_1, userKey = encryptedUserKey)
        fakeAuthDiskSource.assertUserState(SINGLE_USER_STATE_1)
        coVerify {
            authSdkSource.hashPassword(
                email = EMAIL,
                password = password,
                kdf = kdf,
                purpose = HashPurpose.SERVER_AUTHORIZATION,
            )
            authSdkSource.makeRegisterKeys(email = EMAIL, password = password, kdf = kdf)
            accountsService.setPassword(body = setPasswordRequestJson)
            vaultRepository.unlockVaultWithMasterPassword(password)
        }
        coVerify(exactly = 0) {
            organizationService.getOrganizationAutoEnrollStatus(organizationIdentifier)
            organizationService.getOrganizationKeys(organizationId)
            organizationService.organizationResetPasswordEnroll(
                organizationId = organizationId,
                userId = profile.userId,
                passwordHash = passwordHash,
                resetPasswordKey = resetPasswordKey,
            )
            vaultSdkSource.getResetPasswordKey(
                orgPublicKey = publicOrgKey,
                userId = profile.userId,
            )
        }
    }

    @Test
    fun `passwordHintRequest with valid email should return Success`() = runTest {
        val email = "valid@example.com"
        coEvery {
            accountsService.requestPasswordHint(email)
        } returns PasswordHintResponseJson.Success.asSuccess()

        val result = repository.passwordHintRequest(email)

        assertEquals(PasswordHintResult.Success, result)
    }

    @Test
    fun `passwordHintRequest with error response should return Error`() = runTest {
        val email = "error@example.com"
        val errorMessage = "Error message"
        coEvery {
            accountsService.requestPasswordHint(email)
        } returns PasswordHintResponseJson.Error(errorMessage).asSuccess()

        val result = repository.passwordHintRequest(email)

        assertEquals(PasswordHintResult.Error(message = errorMessage, error = null), result)
    }

    @Test
    fun `passwordHintRequest with failure should return Error with null message`() = runTest {
        val email = "failure@example.com"
        val error = RuntimeException("Network error")
        coEvery { accountsService.requestPasswordHint(email) } returns error.asFailure()

        val result = repository.passwordHintRequest(email)

        assertEquals(PasswordHintResult.Error(message = null, error = error), result)
    }

    @Test
    fun `setDuoCallbackToken should change the value of duoTokenResultFlow`() = runTest {
        repository.duoTokenResultFlow.test {
            repository.setDuoCallbackTokenResult(DuoCallbackTokenResult.Success("mockk"))
            assertEquals(
                DuoCallbackTokenResult.Success("mockk"),
                awaitItem(),
            )
        }
    }

    @Test
    fun `setSsoCallbackResult should change the value of ssoCallbackResultFlow`() = runTest {
        repository.ssoCallbackResultFlow.test {
            repository.setSsoCallbackResult(
                SsoCallbackResult.Success(state = "mockk_state", code = "mockk_code"),
            )
            assertEquals(
                SsoCallbackResult.Success(state = "mockk_state", code = "mockk_code"),
                awaitItem(),
            )
        }
    }

    @Test
    fun `setYubiKeyResult should change the value of yubiKeyResultFlow`() = runTest {
        val yubiKeyResult = YubiKeyResult("mockk")
        repository.yubiKeyResultFlow.test {
            repository.setYubiKeyResult(yubiKeyResult)
            assertEquals(yubiKeyResult, awaitItem())
        }
    }

    @Test
    fun `setWebAuthResult should change the value of webAuthResultFlow`() = runTest {
        val webAuthResult = WebAuthResult.Success("mockk")
        repository.webAuthResultFlow.test {
            repository.setWebAuthResult(webAuthResult)
            assertEquals(webAuthResult, awaitItem())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `showWelcomeCarousel should return value from settings repository`() {
        every { settingsRepository.hasUserLoggedInOrCreatedAccount } returns false
        assertTrue(repository.showWelcomeCarousel)

        every { settingsRepository.hasUserLoggedInOrCreatedAccount } returns true
        assertFalse(repository.showWelcomeCarousel)
    }

    @Test
    fun `getVerifiedOrganizationDomainSsoDetails Success should return Success`() = runTest {
        val email = "test@gmail.com"
        coEvery {
            organizationService.getVerifiedOrganizationDomainSsoDetails(email)
        } returns VerifiedOrganizationDomainSsoDetailsResponse(
            verifiedOrganizationDomainSsoDetails = listOf(
                VerifiedOrganizationDomainSsoDetailsResponse.VerifiedOrganizationDomainSsoDetail(
                    organizationIdentifier = "Test Identifier",
                    organizationName = "Bitwarden",
                    domainName = "bitwarden.com",
                ),
            ),
        ).asSuccess()
        val result = repository.getVerifiedOrganizationDomainSsoDetails(email)
        assertEquals(
            VerifiedOrganizationDomainSsoDetailsResult.Success(
                verifiedOrganizationDomainSsoDetails = listOf(
                    @Suppress("MaxLineLength")
                    VerifiedOrganizationDomainSsoDetailsResponse.VerifiedOrganizationDomainSsoDetail(
                        organizationIdentifier = "Test Identifier",
                        organizationName = "Bitwarden",
                        domainName = "bitwarden.com",
                    ),
                ),
            ),
            result,
        )
    }

    @Test
    fun `getVerifiedOrganizationDomainSsoDetails Failure should return Failure `() = runTest {
        val email = "test@gmail.com"
        val throwable = Throwable("Fail!")
        coEvery {
            organizationService.getVerifiedOrganizationDomainSsoDetails(email)
        } returns throwable.asFailure()
        val result = repository.getVerifiedOrganizationDomainSsoDetails(email)
        assertEquals(VerifiedOrganizationDomainSsoDetailsResult.Failure(error = throwable), result)
    }

    @Test
    fun `prevalidateSso Failure should return Failure`() = runTest {
        val organizationId = "organizationid"
        val throwable = Throwable()
        coEvery {
            identityService.prevalidateSso(organizationId)
        } returns throwable.asFailure()
        val result = repository.prevalidateSso(organizationId)
        assertEquals(PrevalidateSsoResult.Failure(error = throwable), result)
    }

    @Test
    fun `prevalidateSso Failure response should return Failure with message`() = runTest {
        val organizationId = "organizationid"
        coEvery {
            identityService.prevalidateSso(organizationId)
        } returns PrevalidateSsoResponseJson.Error(message = "Fail").asSuccess()
        val result = repository.prevalidateSso(organizationId)
        assertEquals(PrevalidateSsoResult.Failure(message = "Fail", error = null), result)
    }

    @Test
    fun `prevalidateSso Success with a blank token should return Failure`() = runTest {
        val organizationId = "organizationid"
        coEvery {
            identityService.prevalidateSso(organizationId)
        } returns PrevalidateSsoResponseJson.Success(token = "").asSuccess()
        val result = repository.prevalidateSso(organizationId)
        assertEquals(
            PrevalidateSsoResult.Failure(error = MissingPropertyException("Token")),
            result,
        )
    }

    @Test
    fun `prevalidateSso Success with a valid token should return Success`() = runTest {
        val organizationId = "organizationid"
        coEvery {
            identityService.prevalidateSso(organizationId)
        } returns PrevalidateSsoResponseJson.Success(token = "token").asSuccess()
        val result = repository.prevalidateSso(organizationId)
        assertEquals(PrevalidateSsoResult.Success(token = "token"), result)
    }

    @Test
    fun `logout for an inactive account should call logout on the UserLogoutManager`() {
        val userId = USER_ID_2
        val reason = LogoutReason.Timeout
        fakeAuthDiskSource.userState = MULTI_USER_STATE

        repository.logout(userId = userId, reason = reason)

        verify { userLogoutManager.logout(userId = userId, reason = reason) }
    }

    @Test
    fun `requestOneTimePasscode with success response should return Success`() = runTest {
        coEvery {
            accountsService.requestOneTimePasscode()
        } returns Unit.asSuccess()

        val result = repository.requestOneTimePasscode()

        assertEquals(RequestOtpResult.Success, result)
    }

    @Test
    fun `requestOneTimePasscode with error response should return Error`() = runTest {
        val errorMessage = "Error message"
        val error = Throwable(errorMessage)
        coEvery {
            accountsService.requestOneTimePasscode()
        } returns error.asFailure()

        val result = repository.requestOneTimePasscode()

        assertEquals(RequestOtpResult.Error(message = errorMessage, error = error), result)
    }

    @Test
    fun `verifyOneTimePasscode with success response should return Verified result`() = runTest {
        val passcode = "passcode"
        coEvery {
            accountsService.verifyOneTimePasscode(passcode)
        } returns Unit.asSuccess()

        val result = repository.verifyOneTimePasscode(passcode)

        assertEquals(VerifyOtpResult.Verified, result)
    }

    @Test
    fun `verifyOneTimePasscode with error response should return NotVerified result`() = runTest {
        val errorMessage = "Error message"
        val passcode = "passcode"
        val error = Throwable(errorMessage)
        coEvery {
            accountsService.verifyOneTimePasscode(passcode)
        } returns error.asFailure()

        val result = repository.verifyOneTimePasscode(passcode)

        assertEquals(
            VerifyOtpResult.NotVerified(errorMessage = errorMessage, error = error),
            result,
        )
    }

    @Test
    fun `resendVerificationCodeEmail uses cached request data to make api call`() = runTest {
        // Attempt a normal login with a two factor error first, so that the necessary
        // data will be cached.
        coEvery { identityService.preLogin(EMAIL) } returns PRE_LOGIN_SUCCESS.asSuccess()
        coEvery {
            identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.MasterPassword(
                    username = EMAIL,
                    password = PASSWORD_HASH,
                ),
                uniqueAppId = UNIQUE_APP_ID,
            )
        } returns GetTokenResponseJson
            .TwoFactorRequired(
                authMethodsData = TWO_FACTOR_AUTH_METHODS_DATA,
                ssoToken = null,
                twoFactorProviders = null,
            )
            .asSuccess()
        val firstResult = repository.login(email = EMAIL, password = PASSWORD)
        assertEquals(LoginResult.TwoFactorRequired, firstResult)
        coVerify { identityService.preLogin(email = EMAIL) }
        coVerify {
            identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.MasterPassword(
                    username = EMAIL,
                    password = PASSWORD_HASH,
                ),
                uniqueAppId = UNIQUE_APP_ID,
            )
        }

        // Resend the verification code email.
        coEvery {
            accountsService.resendVerificationCodeEmail(
                body = ResendEmailRequestJson(
                    deviceIdentifier = UNIQUE_APP_ID,
                    email = EMAIL,
                    passwordHash = PASSWORD_HASH,
                    ssoToken = null,
                ),
            )
        } returns VerificationCodeResponseJson.Success.asSuccess()
        val resendEmailResult = repository.resendVerificationCodeEmail()
        assertEquals(ResendEmailResult.Success, resendEmailResult)
        coVerify {
            accountsService.resendVerificationCodeEmail(
                body = ResendEmailRequestJson(
                    deviceIdentifier = UNIQUE_APP_ID,
                    email = EMAIL,
                    passwordHash = PASSWORD_HASH,
                    ssoToken = null,
                ),
            )
        }
    }

    @Test
    fun `resendVerificationCodeEmail uses cached request data to make api call with error`() =
        runTest {
            // Attempt a normal login with a two factor error first, so that the necessary
            // data will be cached.
            coEvery { identityService.preLogin(EMAIL) } returns PRE_LOGIN_SUCCESS.asSuccess()
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns GetTokenResponseJson
                .TwoFactorRequired(
                    authMethodsData = TWO_FACTOR_AUTH_METHODS_DATA,
                    ssoToken = null,
                    twoFactorProviders = null,
                )
                .asSuccess()
            val firstResult = repository.login(email = EMAIL, password = PASSWORD)
            assertEquals(LoginResult.TwoFactorRequired, firstResult)
            coVerify { identityService.preLogin(email = EMAIL) }
            coVerify {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            }

            // Resend the verification code email.
            val message = "Failure Message"
            coEvery {
                accountsService.resendVerificationCodeEmail(
                    body = ResendEmailRequestJson(
                        deviceIdentifier = UNIQUE_APP_ID,
                        email = EMAIL,
                        passwordHash = PASSWORD_HASH,
                        ssoToken = null,
                    ),
                )
            } returns VerificationCodeResponseJson
                .Invalid(message = message, validationErrors = null)
                .asSuccess()
            val resendEmailResult = repository.resendVerificationCodeEmail()
            assertEquals(
                ResendEmailResult.Error(message = message, error = null),
                resendEmailResult,
            )
            coVerify {
                accountsService.resendVerificationCodeEmail(
                    body = ResendEmailRequestJson(
                        deviceIdentifier = UNIQUE_APP_ID,
                        email = EMAIL,
                        passwordHash = PASSWORD_HASH,
                        ssoToken = null,
                    ),
                )
            }
        }

    @Test
    fun `resendVerificationCodeEmail returns error if no request data cached`() = runTest {
        val result = repository.resendVerificationCodeEmail()
        assertEquals(
            ResendEmailResult.Error(
                message = null,
                error = MissingPropertyException("Resend Email Request"),
            ),
            result,
        )
    }

    @Test
    fun `switchAccount when there is no saved UserState should do nothing`() {
        val updatedUserId = USER_ID_2

        fakeAuthDiskSource.userState = null

        assertEquals(
            SwitchAccountResult.NoChange,
            repository.switchAccount(userId = updatedUserId),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `switchAccount when the given userId is the same as the current activeUserId should reset any pending account additions`() {
        val originalUserId = USER_ID_1
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        fakeEnvironmentRepository.environment = Environment.Eu
        repository.hasPendingAccountAddition = true

        assertEquals(
            SwitchAccountResult.NoChange,
            repository.switchAccount(userId = originalUserId),
        )

        assertEquals(Environment.Us, fakeEnvironmentRepository.environment)
        verify(exactly = 1) {
            userStateManager.hasPendingAccountAddition = false
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `switchAccount when the given userId does not correspond to a saved account should do nothing`() {
        val invalidId = "invalidId"
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        fakeEnvironmentRepository.environment = Environment.Eu

        assertEquals(
            SwitchAccountResult.NoChange,
            repository.switchAccount(userId = invalidId),
        )

        assertEquals(Environment.Us, fakeEnvironmentRepository.environment)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `switchAccount when the userId is valid should update the current UserState and reset any pending account additions`() {
        val updatedUserId = USER_ID_2
        fakeAuthDiskSource.userState = MULTI_USER_STATE
        fakeEnvironmentRepository.environment = Environment.Eu
        repository.hasPendingAccountAddition = true

        assertEquals(
            SwitchAccountResult.AccountSwitched,
            repository.switchAccount(userId = updatedUserId),
        )

        assertEquals(Environment.Eu, fakeEnvironmentRepository.environment)
        verify(exactly = 1) {
            userStateManager.hasPendingAccountAddition = false
        }
    }

    @Test
    fun `getIsKnownDevice should return failure when service returns failure`() = runTest {
        val error = Throwable("Fail!")
        coEvery {
            devicesService.getIsKnownDevice(EMAIL, UNIQUE_APP_ID)
        } returns error.asFailure()

        val result = repository.getIsKnownDevice(EMAIL)

        coVerify(exactly = 1) {
            devicesService.getIsKnownDevice(EMAIL, UNIQUE_APP_ID)
        }
        assertEquals(KnownDeviceResult.Error(error = error), result)
    }

    @Test
    fun `getIsKnownDevice should return success when service returns success`() = runTest {
        val isKnownDevice = true
        coEvery {
            devicesService.getIsKnownDevice(EMAIL, UNIQUE_APP_ID)
        } returns isKnownDevice.asSuccess()

        val result = repository.getIsKnownDevice(EMAIL)

        coVerify(exactly = 1) {
            devicesService.getIsKnownDevice(EMAIL, UNIQUE_APP_ID)
        }
        assertEquals(KnownDeviceResult.Success(isKnownDevice), result)
    }

    @Test
    fun `getPasswordBreachCount should return failure when service returns failure`() = runTest {
        val password = "password"
        val error = Throwable("Fail")
        coEvery {
            haveIBeenPwnedService.getPasswordBreachCount(password)
        } returns error.asFailure()

        val result = repository.getPasswordBreachCount(password)

        coVerify(exactly = 1) {
            haveIBeenPwnedService.getPasswordBreachCount(password)
        }
        assertEquals(BreachCountResult.Error(error = error), result)
    }

    @Test
    fun `getPasswordBreachCount should return success when service returns success`() = runTest {
        val password = "password"
        val breachCount = 5
        coEvery {
            haveIBeenPwnedService.getPasswordBreachCount(password)
        } returns breachCount.asSuccess()

        val result = repository.getPasswordBreachCount(password)

        coVerify(exactly = 1) {
            haveIBeenPwnedService.getPasswordBreachCount(password)
        }
        assertEquals(BreachCountResult.Success(breachCount), result)
    }

    @Test
    fun `getPasswordStrength returns expected results for various strength levels`() = runTest {
        coEvery {
            authSdkSource.passwordStrength(any(), eq("level_0"))
        } returns LEVEL_0.asSuccess()

        coEvery {
            authSdkSource.passwordStrength(any(), eq("level_1"))
        } returns LEVEL_1.asSuccess()

        coEvery {
            authSdkSource.passwordStrength(any(), eq("level_2"))
        } returns LEVEL_2.asSuccess()

        coEvery {
            authSdkSource.passwordStrength(any(), eq("level_3"))
        } returns LEVEL_3.asSuccess()

        coEvery {
            authSdkSource.passwordStrength(any(), eq("level_4"))
        } returns LEVEL_4.asSuccess()

        assertEquals(
            PasswordStrengthResult.Success(LEVEL_0),
            repository.getPasswordStrength(EMAIL, "level_0"),
        )

        assertEquals(
            PasswordStrengthResult.Success(LEVEL_1),
            repository.getPasswordStrength(EMAIL, "level_1"),
        )

        assertEquals(
            PasswordStrengthResult.Success(LEVEL_2),
            repository.getPasswordStrength(EMAIL, "level_2"),
        )

        assertEquals(
            PasswordStrengthResult.Success(LEVEL_3),
            repository.getPasswordStrength(EMAIL, "level_3"),
        )

        assertEquals(
            PasswordStrengthResult.Success(LEVEL_4),
            repository.getPasswordStrength(EMAIL, "level_4"),
        )
    }

    @Test
    fun `validatePassword with no current user returns ValidatePasswordResult Error`() = runTest {
        val password = "password"
        fakeAuthDiskSource.userState = null

        val result = repository.validatePassword(password = password)

        assertEquals(
            ValidatePasswordResult.Error(error = NoActiveUserException()),
            result,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `validatePassword with no stored password hash and no stored user key returns ValidatePasswordResult Error`() =
        runTest {
            val userId = USER_ID_1
            val password = "password"
            val passwordHash = "passwordHash"
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            coEvery {
                vaultSdkSource.validatePassword(
                    userId = userId,
                    password = password,
                    passwordHash = passwordHash,
                )
            } returns true.asSuccess()

            val result = repository.validatePassword(password = password)

            assertEquals(
                ValidatePasswordResult.Error(error = MissingPropertyException("UserKey")),
                result,
            )
        }

    @Test
    fun `validatePassword with sdk failure returns a ValidatePasswordResult Error`() = runTest {
        val userId = USER_ID_1
        val password = "password"
        val passwordHash = "passwordHash"
        val error = Throwable("Fail!")
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        fakeAuthDiskSource.storeMasterPasswordHash(userId = userId, passwordHash = passwordHash)
        coEvery {
            vaultSdkSource.validatePassword(
                userId = userId,
                password = password,
                passwordHash = passwordHash,
            )
        } returns error.asFailure()

        val result = repository.validatePassword(password = password)

        assertEquals(
            ValidatePasswordResult.Error(error = error),
            result,
        )
    }

    @Test
    fun `validatePassword with sdk success returns a ValidatePasswordResult Success`() = runTest {
        val userId = USER_ID_1
        val password = "password"
        val passwordHash = "passwordHash"
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        fakeAuthDiskSource.storeMasterPasswordHash(userId = userId, passwordHash = passwordHash)
        coEvery {
            vaultSdkSource.validatePassword(
                userId = userId,
                password = password,
                passwordHash = passwordHash,
            )
        } returns true.asSuccess()

        val result = repository.validatePassword(password = password)

        assertEquals(
            ValidatePasswordResult.Success(isValid = true),
            result,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `validatePassword with no stored password hash and a stored user key with sdk failure returns ValidatePasswordResult Success invalid`() =
        runTest {
            val userId = USER_ID_1
            val password = "password"
            val userKey = "userKey"
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            fakeAuthDiskSource.storeUserKey(userId = userId, userKey = userKey)
            coEvery {
                vaultSdkSource.validatePasswordUserKey(
                    userId = userId,
                    password = password,
                    encryptedUserKey = userKey,
                )
            } returns Throwable("Fail").asFailure()

            val result = repository.validatePassword(password = password)

            assertEquals(ValidatePasswordResult.Success(isValid = false), result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `validatePassword with no stored password hash and a stored user key with sdk success returns ValidatePasswordResult Success valid`() =
        runTest {
            val userId = USER_ID_1
            val password = "password"
            val userKey = "userKey"
            val passwordHash = "passwordHash"
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            fakeAuthDiskSource.storeUserKey(userId = userId, userKey = userKey)
            coEvery {
                vaultSdkSource.validatePasswordUserKey(
                    userId = userId,
                    password = password,
                    encryptedUserKey = userKey,
                )
            } returns passwordHash.asSuccess()

            val result = repository.validatePassword(password = password)

            assertEquals(ValidatePasswordResult.Success(isValid = true), result)
            fakeAuthDiskSource.assertMasterPasswordHash(
                userId = userId,
                passwordHash = passwordHash,
            )
        }

    @Test
    fun `validatePin returns ValidatePinResult Error when no active account found`() = runTest {
        val pin = "PIN"
        fakeAuthDiskSource.userState = null

        val result = repository.validatePinUserKey(pin = pin)

        assertEquals(
            ValidatePinResult.Error(error = NoActiveUserException()),
            result,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `validatePinUserKey returns ValidatePinResult Error when no pin protected user key found`() =
        runTest {
            val pin = "PIN"
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            fakeAuthDiskSource.storePinProtectedUserKey(
                userId = SINGLE_USER_STATE_1.activeUserId,
                pinProtectedUserKey = null,
            )

            val result = repository.validatePinUserKey(pin = pin)

            assertEquals(
                ValidatePinResult.Error(MissingPropertyException("Pin Protected User Key")),
                result,
            )
        }

    @Test
    fun `validatePinUserKey returns ValidatePinResult Error when SDK validatePin fails`() =
        runTest {
            val pin = "PIN"
            val pinProtectedUserKey = "pinProtectedUserKey"
            val error = Throwable("Fail!")
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            fakeAuthDiskSource.storePinProtectedUserKeyEnvelope(
                userId = SINGLE_USER_STATE_1.activeUserId,
                pinProtectedUserKeyEnvelope = pinProtectedUserKey,
            )
            coEvery {
                vaultSdkSource.validatePinUserKey(
                    userId = SINGLE_USER_STATE_1.activeUserId,
                    pin = pin,
                    pinProtectedUserKeyEnvelope = pinProtectedUserKey,
                )
            } returns error.asFailure()

            val result = repository.validatePinUserKey(pin = pin)

            assertEquals(
                ValidatePinResult.Error(error = error),
                result,
            )
            coVerify(exactly = 1) {
                vaultSdkSource.validatePinUserKey(
                    userId = SINGLE_USER_STATE_1.activeUserId,
                    pin = pin,
                    pinProtectedUserKeyEnvelope = pinProtectedUserKey,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `validatePinUserKey returns ValidatePinResult Success with valid false when SDK validatePin returns false`() =
        runTest {
            val pin = "PIN"
            val pinProtectedUserKey = "pinProtectedUserKey"
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            fakeAuthDiskSource.storePinProtectedUserKeyEnvelope(
                userId = SINGLE_USER_STATE_1.activeUserId,
                pinProtectedUserKeyEnvelope = pinProtectedUserKey,
            )
            coEvery {
                vaultSdkSource.validatePinUserKey(
                    userId = SINGLE_USER_STATE_1.activeUserId,
                    pin = pin,
                    pinProtectedUserKeyEnvelope = pinProtectedUserKey,
                )
            } returns false.asSuccess()

            val result = repository.validatePinUserKey(pin = pin)

            assertEquals(
                ValidatePinResult.Success(isValid = false),
                result,
            )
            coVerify(exactly = 1) {
                vaultSdkSource.validatePinUserKey(
                    userId = SINGLE_USER_STATE_1.activeUserId,
                    pin = pin,
                    pinProtectedUserKeyEnvelope = pinProtectedUserKey,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `validatePinUserKey returns ValidatePinResult Success with valid true when SDK validatePin returns true`() =
        runTest {
            val pin = "PIN"
            val pinProtectedUserKey = "pinProtectedUserKey"
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            fakeAuthDiskSource.storePinProtectedUserKeyEnvelope(
                userId = SINGLE_USER_STATE_1.activeUserId,
                pinProtectedUserKeyEnvelope = pinProtectedUserKey,
            )
            coEvery {
                vaultSdkSource.validatePinUserKey(
                    userId = SINGLE_USER_STATE_1.activeUserId,
                    pin = pin,
                    pinProtectedUserKeyEnvelope = pinProtectedUserKey,
                )
            } returns true.asSuccess()

            val result = repository.validatePinUserKey(pin = pin)

            assertEquals(
                ValidatePinResult.Success(isValid = true),
                result,
            )
            coVerify(exactly = 1) {
                vaultSdkSource.validatePinUserKey(
                    userId = SINGLE_USER_STATE_1.activeUserId,
                    pin = pin,
                    pinProtectedUserKeyEnvelope = pinProtectedUserKey,
                )
            }
        }

    @Test
    fun `logOutFlow emission for action account should call logout on the UserLogoutManager`() {
        val userId = USER_ID_1
        fakeAuthDiskSource.userState = MULTI_USER_STATE

        mutableLogoutFlow.tryEmit(NotificationLogoutData(userId = userId))

        coVerify(exactly = 1) {
            userLogoutManager.logout(userId = userId, reason = LogoutReason.Notification)
        }
    }

    @Test
    fun `syncOrgKeysFlow emissions for active user should refresh access token and force sync`() {
        fakeAuthDiskSource.userState = MULTI_USER_STATE
        fakeAuthDiskSource.storeAccountTokens(userId = USER_ID_1, accountTokens = ACCOUNT_TOKENS_1)
        every { vaultRepository.sync(forced = true) } just runs

        mutableSyncOrgKeysFlow.tryEmit(USER_ID_1)

        verify(exactly = 1) {
            vaultRepository.sync(forced = true)
        }
        fakeAuthDiskSource.assertAccountTokens(
            userId = USER_ID_1,
            accountTokens = ACCOUNT_TOKENS_1.copy(expiresAtSec = 0L),
        )
    }

    @Test
    fun `syncOrgKeysFlow emissions for inactive user should clear the last sync time`() {
        fakeAuthDiskSource.userState = MULTI_USER_STATE
        fakeAuthDiskSource.storeAccountTokens(userId = USER_ID_2, accountTokens = ACCOUNT_TOKENS_2)
        fakeSettingsDiskSource.storeLastSyncTime(
            userId = USER_ID_2,
            lastSyncTime = FIXED_CLOCK.instant(),
        )
        every { vaultRepository.sync(forced = true) } just runs

        mutableSyncOrgKeysFlow.tryEmit(USER_ID_2)

        verify(exactly = 0) {
            vaultRepository.sync(forced = true)
        }
        fakeSettingsDiskSource.assertLastSyncTime(userId = USER_ID_2, null)
        fakeAuthDiskSource.assertAccountTokens(
            userId = USER_ID_2,
            accountTokens = ACCOUNT_TOKENS_2.copy(expiresAtSec = 0L),
        )
    }

    @Test
    fun `validatePasswordAgainstPolicy validates password against policy requirements`() = runTest {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1

        // A helper method to set a policy with the given parameters.
        fun setPolicy(
            minLength: Int = 0,
            minComplexity: Int? = null,
            requireUpper: Boolean = false,
            requireLower: Boolean = false,
            requireNumbers: Boolean = false,
            requireSpecial: Boolean = false,
        ) {
            every {
                policyManager.getActivePolicies(type = PolicyTypeJson.MASTER_PASSWORD)
            } returns listOf(
                createMockPolicy(
                    type = PolicyTypeJson.MASTER_PASSWORD,
                    isEnabled = true,
                    data = buildJsonObject {
                        put(key = "minLength", value = minLength)
                        put(key = "minComplexity", value = minComplexity)
                        put(key = "requireUpper", value = requireUpper)
                        put(key = "requireLower", value = requireLower)
                        put(key = "requireNumbers", value = requireNumbers)
                        put(key = "requireSpecial", value = requireSpecial)
                        put(key = "enforceOnLogin", value = true)
                    },
                ),
            )
        }

        setPolicy(minLength = 10)
        assertFalse(repository.validatePasswordAgainstPolicies(password = "123"))

        val password = "simple"
        coEvery {
            authSdkSource.passwordStrength(
                email = SINGLE_USER_STATE_1.activeAccount.profile.email,
                password = password,
            )
        } returns LEVEL_0.asSuccess()
        setPolicy(minComplexity = 10)
        assertFalse(repository.validatePasswordAgainstPolicies(password = password))

        setPolicy(requireUpper = true)
        assertFalse(repository.validatePasswordAgainstPolicies(password = "lower"))

        setPolicy(requireLower = true)
        assertFalse(repository.validatePasswordAgainstPolicies(password = "UPPER"))

        setPolicy(requireNumbers = true)
        assertFalse(repository.validatePasswordAgainstPolicies(password = "letters"))

        setPolicy(requireSpecial = true)
        assertFalse(repository.validatePasswordAgainstPolicies(password = "letters"))
    }

    @Test
    fun `sendVerificationEmail success should return success`() = runTest {
        coEvery {
            identityService.sendVerificationEmail(
                SendVerificationEmailRequestJson(
                    email = EMAIL,
                    name = NAME,
                    receiveMarketingEmails = true,
                ),
            )
        } returns SendVerificationEmailResponseJson.Success(EMAIL_VERIFICATION_TOKEN).asSuccess()

        val result = repository.sendVerificationEmail(
            email = EMAIL,
            name = NAME,
            receiveMarketingEmails = true,
        )
        assertEquals(
            SendVerificationEmailResult.Success(EMAIL_VERIFICATION_TOKEN),
            result,
        )
    }

    @Test
    fun `sendVerificationEmail success with invalid email should return error`() = runTest {
        val errorMessage = "Failure"
        coEvery {
            identityService.sendVerificationEmail(
                SendVerificationEmailRequestJson(
                    email = EMAIL,
                    name = NAME,
                    receiveMarketingEmails = true,
                ),
            )
        } returns SendVerificationEmailResponseJson
            .Invalid(errorMessage = errorMessage, validationErrors = null)
            .asSuccess()

        val result = repository.sendVerificationEmail(
            email = EMAIL,
            name = NAME,
            receiveMarketingEmails = true,
        )
        assertEquals(
            SendVerificationEmailResult.Error(errorMessage = errorMessage, error = null),
            result,
        )
    }

    @Test
    fun `sendVerificationEmail failure should return success if body null`() = runTest {
        coEvery {
            identityService.sendVerificationEmail(
                SendVerificationEmailRequestJson(
                    email = EMAIL,
                    name = NAME,
                    receiveMarketingEmails = true,
                ),
            )
        } returns SendVerificationEmailResponseJson.Success(null).asSuccess()

        val result = repository.sendVerificationEmail(
            email = EMAIL,
            name = NAME,
            receiveMarketingEmails = true,
        )
        assertEquals(
            SendVerificationEmailResult.Success(null),
            result,
        )
    }

    @Test
    fun `sendVerificationEmail with empty name should use null and return success`() = runTest {
        coEvery {
            identityService.sendVerificationEmail(
                SendVerificationEmailRequestJson(
                    email = EMAIL,
                    name = null,
                    receiveMarketingEmails = true,
                ),
            )
        } returns SendVerificationEmailResponseJson.Success(EMAIL_VERIFICATION_TOKEN).asSuccess()

        val result = repository.sendVerificationEmail(
            email = EMAIL,
            name = "",
            receiveMarketingEmails = true,
        )
        assertEquals(
            SendVerificationEmailResult.Success(EMAIL_VERIFICATION_TOKEN),
            result,
        )
    }

    @Test
    fun `sendVerificationEmail failure should return error`() = runTest {
        val error = Throwable("fail")
        coEvery {
            identityService.sendVerificationEmail(
                SendVerificationEmailRequestJson(
                    email = EMAIL,
                    name = NAME,
                    receiveMarketingEmails = true,
                ),
            )
        } returns error.asFailure()

        val result = repository.sendVerificationEmail(
            email = EMAIL,
            name = NAME,
            receiveMarketingEmails = true,
        )
        assertEquals(
            SendVerificationEmailResult.Error(errorMessage = null, error = error),
            result,
        )
    }

    @Test
    fun `validateEmailToken should return success result when service returns success`() = runTest {
        coEvery {
            identityService
                .verifyEmailRegistrationToken(
                    body = VerifyEmailTokenRequestJson(
                        email = EMAIL,
                        token = EMAIL_VERIFICATION_TOKEN,
                    ),
                )
        } returns VerifyEmailTokenResponseJson.Valid.asSuccess()

        val emailTokenResult = repository.validateEmailToken(EMAIL, EMAIL_VERIFICATION_TOKEN)

        assertEquals(
            EmailTokenResult.Success,
            emailTokenResult,
        )
    }

    @Test
    fun `validateEmailToken should return expired result when service returns TokenExpired`() =
        runTest {
            coEvery {
                identityService
                    .verifyEmailRegistrationToken(
                        body = VerifyEmailTokenRequestJson(
                            email = EMAIL,
                            token = EMAIL_VERIFICATION_TOKEN,
                        ),
                    )
            } returns VerifyEmailTokenResponseJson.TokenExpired.asSuccess()

            val emailTokenResult = repository.validateEmailToken(EMAIL, EMAIL_VERIFICATION_TOKEN)

            assertEquals(
                EmailTokenResult.Expired,
                emailTokenResult,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `validateEmailToken should return error result when service returns error without expired message`() =
        runTest {
            val errorMessage = "I haven't heard of second breakfast."
            coEvery {
                identityService.verifyEmailRegistrationToken(
                    body = VerifyEmailTokenRequestJson(
                        email = EMAIL,
                        token = EMAIL_VERIFICATION_TOKEN,
                    ),
                )
            } returns VerifyEmailTokenResponseJson.Invalid(message = errorMessage).asSuccess()

            val emailTokenResult = repository.validateEmailToken(EMAIL, EMAIL_VERIFICATION_TOKEN)

            assertEquals(
                EmailTokenResult.Error(message = errorMessage, error = null),
                emailTokenResult,
            )
        }

    @Test
    fun `validateEmailToken should return error result when service returns failure`() = runTest {
        val error = Throwable("Fail!")
        coEvery {
            identityService.verifyEmailRegistrationToken(
                body = VerifyEmailTokenRequestJson(
                    email = EMAIL,
                    token = EMAIL_VERIFICATION_TOKEN,
                ),
            )
        } returns error.asFailure()

        val emailTokenResult = repository.validateEmailToken(EMAIL, EMAIL_VERIFICATION_TOKEN)

        assertEquals(
            EmailTokenResult.Error(message = null, error = error),
            emailTokenResult,
        )
    }

    @Test
    fun `setOnboardingStatus should save the onboarding status to disk`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        repository.setOnboardingStatus(status = OnboardingStatus.NOT_STARTED)
        assertEquals(
            OnboardingStatus.NOT_STARTED,
            fakeAuthDiskSource.getOnboardingStatus(USER_ID_1),
        )
    }

    @Test
    fun `setOnboardingStatus with no userId does not change the onboarding state`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        repository.setOnboardingStatus(status = OnboardingStatus.NOT_STARTED)
        assertEquals(
            OnboardingStatus.NOT_STARTED,
            fakeAuthDiskSource.getOnboardingStatus(USER_ID_1),
        )

        fakeAuthDiskSource.userState = null
        repository.setOnboardingStatus(status = OnboardingStatus.COMPLETE)
        val activeUserId = SINGLE_USER_STATE_1.activeUserId
        assertFalse(
            fakeAuthDiskSource.getOnboardingStatus(activeUserId) == OnboardingStatus.COMPLETE,
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `on successful login a new user should have onboarding status set if has not previously logged in`() =
        runTest {
            val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS
            coEvery {
                identityService.preLogin(email = EMAIL)
            } returns PRE_LOGIN_SUCCESS.asSuccess()
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns successResponse.asSuccess()
            coEvery {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .wrappedPrivateKey,
                        securityState = successResponse.accountKeys
                            ?.securityState
                            ?.securityState,
                        signedPublicKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .signedPublicKey,
                        signingKey = successResponse.accountKeys
                            ?.signatureKeyPair
                            ?.wrappedSigningKey,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                        password = PASSWORD,
                        masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK,
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.Success
            coEvery { vaultRepository.syncIfNecessary() } just runs
            every {
                GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.toUserState(
                    previousUserState = null,
                    environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
                )
            } returns SINGLE_USER_STATE_1
            every { settingsRepository.getUserHasLoggedInValue(USER_ID_1) } returns false
            val result = repository.login(email = EMAIL, password = PASSWORD)
            assertEquals(LoginResult.Success, result)
            assertEquals(AuthState.Authenticated(ACCESS_TOKEN), repository.authStateFlow.value)
            coVerify { identityService.preLogin(email = EMAIL) }
            fakeAuthDiskSource.assertPrivateKey(
                userId = USER_ID_1,
                privateKey = "privateKey",
            )
            fakeAuthDiskSource.assertAccountKeys(
                userId = USER_ID_1,
                accountKeys = ACCOUNT_KEYS,
            )
            fakeAuthDiskSource.assertUserKey(
                userId = USER_ID_1,
                userKey = "key",
            )
            fakeAuthDiskSource.assertMasterPasswordHash(
                userId = USER_ID_1,
                passwordHash = PASSWORD_HASH,
            )
            // This should only be set after they complete a registration and not based on login.
            assertNull(fakeAuthDiskSource.getOnboardingStatus(USER_ID_1))
            verify(exactly = 1) {
                userStateManager.hasPendingAccountAddition = false
            }
        }

    @Test
    fun `on successful login does not set onboarding status if user has previously logged in`() =
        runTest {
            val successResponse = GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS
            coEvery {
                identityService.preLogin(email = EMAIL)
            } returns PRE_LOGIN_SUCCESS.asSuccess()
            coEvery {
                identityService.getToken(
                    email = EMAIL,
                    authModel = IdentityTokenAuthModel.MasterPassword(
                        username = EMAIL,
                        password = PASSWORD_HASH,
                    ),
                    uniqueAppId = UNIQUE_APP_ID,
                )
            } returns successResponse.asSuccess()
            coEvery {
                vaultRepository.unlockVault(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .wrappedPrivateKey,
                        securityState = successResponse.accountKeys
                            ?.securityState
                            ?.securityState,
                        signedPublicKey = successResponse.accountKeys!!
                            .publicKeyEncryptionKeyPair
                            .signedPublicKey,
                        signingKey = successResponse.accountKeys
                            ?.signatureKeyPair
                            ?.wrappedSigningKey,
                    ),
                    userId = USER_ID_1,
                    email = EMAIL,
                    kdf = ACCOUNT_1.profile.toSdkParams(),
                    initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                        password = PASSWORD,
                        masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK,
                    ),
                    organizationKeys = null,
                )
            } returns VaultUnlockResult.Success
            coEvery { vaultRepository.syncIfNecessary() } just runs
            every {
                GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS.toUserState(
                    previousUserState = null,
                    environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
                )
            } returns SINGLE_USER_STATE_1
            every { settingsRepository.getUserHasLoggedInValue(USER_ID_1) } returns true
            val result = repository.login(email = EMAIL, password = PASSWORD)
            assertEquals(LoginResult.Success, result)
            assertEquals(AuthState.Authenticated(ACCESS_TOKEN), repository.authStateFlow.value)
            coVerify { identityService.preLogin(email = EMAIL) }
            fakeAuthDiskSource.assertPrivateKey(
                userId = USER_ID_1,
                privateKey = "privateKey",
            )
            fakeAuthDiskSource.assertAccountKeys(
                userId = USER_ID_1,
                accountKeys = ACCOUNT_KEYS,
            )
            fakeAuthDiskSource.assertUserKey(
                userId = USER_ID_1,
                userKey = "key",
            )
            fakeAuthDiskSource.assertMasterPasswordHash(
                userId = USER_ID_1,
                passwordHash = PASSWORD_HASH,
            )
            verify(exactly = 1) {
                userStateManager.hasPendingAccountAddition = false
                settingsRepository.setDefaultsIfNecessary(userId = USER_ID_1)
            }
            assertNull(fakeAuthDiskSource.getOnboardingStatus(USER_ID_1))
        }

    @Test
    fun `cancelKeyConnectorLogin should clear keyConnectorResponse`() =
        runTest {
            assertDoesNotThrow { repository.cancelKeyConnectorLogin() }
        }

    @Test
    fun `continueKeyConnectorLogin returns error if keyConnectorResponse is null`() =
        runTest {
            val continueResult = repository.continueKeyConnectorLogin()
            assertEquals(
                LoginResult.Error(
                    errorMessage = null,
                    error = MissingPropertyException("Key Connector Response"),
                ),
                continueResult,
            )
        }

    @Test
    @Suppress("MaxLineLength")
    fun `leaveOrganization should return success when organizationService leaveOrganization succeeds`() =
        runTest {
            coEvery {
                organizationService.leaveOrganization(any())
            } returns Unit.asSuccess()

            val continueResult = repository.leaveOrganization("mockId-1")
            coVerify {
                organizationService.leaveOrganization(any())
            }
            assertEquals(
                LeaveOrganizationResult.Success, continueResult,
            )
        }

    @Test
    fun `leaveOrganization should return error when organizationService leaveOrganization fails`() =
        runTest {
            val error = Throwable("Fail")
            coEvery {
                organizationService.leaveOrganization(any())
            } returns error.asFailure()

            val continueResult = repository.leaveOrganization("mockId-1")
            coVerify {
                organizationService.leaveOrganization(any())
            }
            assertEquals(
                LeaveOrganizationResult.Error(error = error),
                continueResult,
            )
        }

    companion object {
        private val FIXED_CLOCK: Clock = Clock.fixed(
            Instant.parse("2023-10-27T12:00:00Z"),
            ZoneOffset.UTC,
        )
        private const val UNIQUE_APP_ID = "testUniqueAppId"
        private const val NAME = "Example Name"
        private const val EMAIL = "test@bitwarden.com"
        private const val EMAIL_2 = "test2@bitwarden.com"
        private const val EMAIL_VERIFICATION_TOKEN = "thisisanawesometoken"
        private const val PASSWORD = "password"
        private const val PASSWORD_HASH = "passwordHash"
        private const val ACCESS_TOKEN = "accessToken"
        private const val ACCESS_TOKEN_2 = "accessToken2"
        private const val REFRESH_TOKEN = "refreshToken"
        private const val REFRESH_TOKEN_2 = "refreshToken2"
        private const val ACCESS_TOKEN_2_EXPIRES_IN = 3600
        private const val TWO_FACTOR_CODE = "123456"
        private val TWO_FACTOR_METHOD = TwoFactorAuthMethod.EMAIL
        private const val TWO_FACTOR_REMEMBER = true
        private val TWO_FACTOR_DATA = TwoFactorDataModel(
            code = TWO_FACTOR_CODE,
            method = TWO_FACTOR_METHOD.value.toString(),
            remember = TWO_FACTOR_REMEMBER,
        )
        private const val SSO_CODE = "ssoCode"
        private const val SSO_CODE_VERIFIER = "ssoCodeVerifier"
        private const val SSO_REDIRECT_URI = "bitwarden://sso-test"
        private const val DEVICE_ACCESS_CODE = "accessCode"
        private const val DEVICE_REQUEST_ID = "authRequestId"
        private const val DEVICE_ASYMMETRICAL_KEY = "asymmetricalKey"
        private const val DEVICE_REQUEST_PRIVATE_KEY = "requestPrivateKey"

        private const val DEFAULT_KDF_ITERATIONS = 600000
        private const val ENCRYPTED_USER_KEY = "encryptedUserKey"
        private const val PUBLIC_KEY = "PublicKey"
        private const val PRIVATE_KEY = "privateKey"
        private const val USER_ID_1 = "2a135b23-e1fb-42c9-bec3-573857bc8181"
        private const val USER_ID_2 = "b9d32ec0-6497-4582-9798-b350f53bfa02"
        private const val ORGANIZATION_IDENTIFIER = "organizationIdentifier"
        private val ORGANIZATIONS = listOf(createMockOrganization(number = 0))
        private val ACCOUNT_KEYS = createMockAccountKeysJson(number = 1)
        private val ACCOUNT_KEYS_WITH_NULL_FIELDS =
            createMockAccountKeysJsonWithNullFields(number = 1)
        private val TWO_FACTOR_AUTH_METHODS_DATA = mapOf(
            TwoFactorAuthMethod.EMAIL to JsonObject(
                mapOf("Email" to JsonPrimitive("ex***@email.com")),
            ),
            TwoFactorAuthMethod.AUTHENTICATOR_APP to JsonObject(mapOf("Email" to JsonNull)),
        )
        private val PRE_LOGIN_SUCCESS = PreLoginResponseJson(
            kdfParams = PreLoginResponseJson.KdfParams.Pbkdf2(iterations = 1u),
        )
        private val AUTH_REQUEST_RESPONSE = AuthRequestResponse(
            privateKey = PRIVATE_KEY,
            publicKey = PUBLIC_KEY,
            accessCode = "accessCode",
            fingerprint = "fingerprint",
        )
        private val REFRESH_TOKEN_RESPONSE_JSON = RefreshTokenResponseJson.Success(
            accessToken = ACCESS_TOKEN_2,
            expiresIn = ACCESS_TOKEN_2_EXPIRES_IN,
            refreshToken = REFRESH_TOKEN_2,
            tokenType = "Bearer",
        )
        private val TRUSTED_DEVICE_DECRYPTION_OPTIONS = TrustedDeviceUserDecryptionOptionsJson(
            encryptedPrivateKey = null,
            encryptedUserKey = null,
            hasAdminApproval = false,
            hasLoginApprovingDevice = false,
            hasManageResetPasswordPermission = false,
        )
        private val USER_DECRYPTION_OPTIONS = UserDecryptionOptionsJson(
            hasMasterPassword = false,
            trustedDeviceUserDecryptionOptions = TRUSTED_DEVICE_DECRYPTION_OPTIONS,
            keyConnectorUserDecryptionOptions = null,
            masterPasswordUnlock = null,
        )

        @Deprecated(
            message = "Use GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS instead",
            replaceWith = ReplaceWith(
                expression = "GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS",
            ),
        )
        private val GET_TOKEN_RESPONSE_SUCCESS = GetTokenResponseJson.Success(
            accessToken = ACCESS_TOKEN,
            refreshToken = "refreshToken",
            tokenType = "Bearer",
            expiresInSeconds = 3600,
            key = "key",
            kdfType = KdfTypeJson.ARGON2_ID,
            kdfIterations = 600000,
            kdfMemory = 16,
            kdfParallelism = 4,
            privateKey = "privateKey",
            accountKeys = null,
            shouldForcePasswordReset = true,
            twoFactorToken = null,
            masterPasswordPolicyOptions = null,
            userDecryptionOptions = null,
            keyConnectorUrl = null,
        )
        private val GET_TOKEN_WITH_ACCOUNT_KEYS_RESPONSE_SUCCESS = GetTokenResponseJson.Success(
            accessToken = ACCESS_TOKEN,
            refreshToken = "refreshToken",
            tokenType = "Bearer",
            expiresInSeconds = 3600,
            key = "key",
            kdfType = KdfTypeJson.ARGON2_ID,
            kdfIterations = 600000,
            kdfMemory = 16,
            kdfParallelism = 4,
            privateKey = "privateKey",
            accountKeys = ACCOUNT_KEYS,
            shouldForcePasswordReset = true,
            twoFactorToken = null,
            masterPasswordPolicyOptions = null,
            userDecryptionOptions = UserDecryptionOptionsJson(
                hasMasterPassword = true,
                trustedDeviceUserDecryptionOptions = null,
                keyConnectorUserDecryptionOptions = null,
                masterPasswordUnlock = MasterPasswordUnlockDataJson(
                    kdf = KdfJson(
                        kdfType = KdfTypeJson.ARGON2_ID,
                        iterations = 600000,
                        memory = 16,
                        parallelism = 4,
                    ),
                    masterKeyWrappedUserKey = "key",
                    salt = "mockSalt",
                ),
            ),
            keyConnectorUrl = null,
        )
        private val BASE_PROFILE_1 = AccountJson.Profile(
            userId = USER_ID_1,
            email = EMAIL,
            isEmailVerified = true,
            name = "Bitwarden Tester",
            hasPremium = false,
            stamp = null,
            organizationId = null,
            avatarColorHex = null,
            forcePasswordResetReason = null,
            kdfType = KdfTypeJson.ARGON2_ID,
            kdfIterations = 600000,
            kdfMemory = 16,
            kdfParallelism = 4,
            userDecryptionOptions = null,
            isTwoFactorEnabled = false,
            creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
        )

        private val PROFILE_1 = BASE_PROFILE_1.copy(
            userDecryptionOptions = UserDecryptionOptionsJson(
                hasMasterPassword = true,
                trustedDeviceUserDecryptionOptions = null,
                keyConnectorUserDecryptionOptions = null,
                masterPasswordUnlock = MasterPasswordUnlockDataJson(
                    kdf = BASE_PROFILE_1.toSdkParams().toKdfRequestModel(),
                    masterKeyWrappedUserKey = "key",
                    salt = "mockSalt",
                ),
            ),
        )
        private val ACCOUNT_1 = AccountJson(
            profile = PROFILE_1,
            settings = AccountJson.Settings(
                environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
            ),
        )
        private val ACCOUNT_2 = AccountJson(
            profile = AccountJson.Profile(
                userId = USER_ID_2,
                email = EMAIL_2,
                isEmailVerified = true,
                name = "Bitwarden Tester 2",
                hasPremium = false,
                stamp = null,
                organizationId = null,
                avatarColorHex = null,
                forcePasswordResetReason = null,
                kdfType = KdfTypeJson.PBKDF2_SHA256,
                kdfIterations = 400000,
                kdfMemory = null,
                kdfParallelism = null,
                userDecryptionOptions = null,
                isTwoFactorEnabled = true,
                creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
            ),
            settings = AccountJson.Settings(
                environmentUrlData = EnvironmentUrlDataJson.DEFAULT_EU,
            ),
        )
        private val SINGLE_USER_STATE_1 = UserStateJson(
            activeUserId = USER_ID_1,
            accounts = mapOf(
                USER_ID_1 to ACCOUNT_1,
            ),
        )
        private val SINGLE_USER_STATE_1_WITH_PASS = UserStateJson(
            activeUserId = USER_ID_1,
            accounts = mapOf(
                USER_ID_1 to ACCOUNT_1.copy(
                    profile = ACCOUNT_1.profile.copy(
                        userDecryptionOptions = UserDecryptionOptionsJson(
                            hasMasterPassword = true,
                            keyConnectorUserDecryptionOptions = null,
                            trustedDeviceUserDecryptionOptions = null,
                            masterPasswordUnlock = MasterPasswordUnlockDataJson(
                                kdf = BASE_PROFILE_1.toSdkParams().toKdfRequestModel(),
                                masterKeyWrappedUserKey = "key",
                                salt = "mockSalt",
                            ),
                        ),
                    ),
                ),
            ),
        )

        private val MOCK_MASTER_PASSWORD_UNLOCK = MasterPasswordUnlockData(
            kdf = ACCOUNT_1.profile.toSdkParams(),
            masterKeyWrappedUserKey = "key",
            salt = "mockSalt",
        )

        private val SINGLE_USER_STATE_2 = UserStateJson(
            activeUserId = USER_ID_2,
            accounts = mapOf(
                USER_ID_2 to ACCOUNT_2,
            ),
        )
        private val MULTI_USER_STATE = UserStateJson(
            activeUserId = USER_ID_1,
            accounts = mapOf(
                USER_ID_1 to ACCOUNT_1,
                USER_ID_2 to ACCOUNT_2,
            ),
        )
        private val ACCOUNT_TOKENS_1: AccountTokensJson = AccountTokensJson(
            accessToken = ACCESS_TOKEN,
            refreshToken = REFRESH_TOKEN,
        )
        private val ACCOUNT_TOKENS_2: AccountTokensJson = AccountTokensJson(
            accessToken = ACCESS_TOKEN_2,
            refreshToken = "refreshToken",
        )
        private val VAULT_UNLOCK_DATA = listOf(
            VaultUnlockData(
                userId = USER_ID_1,
                status = VaultUnlockData.Status.UNLOCKED,
            ),
        )

        private val SERVER_CONFIG_DEFAULT = ServerConfig(
            lastSync = 0L,
            serverData = ConfigResponseJson(
                type = "mockType",
                version = "mockVersion",
                gitHash = "mockGitHash",
                server = null,
                environment = ConfigResponseJson.EnvironmentJson(
                    cloudRegion = "mockCloudRegion",
                    vaultUrl = "mockVaultUrl",
                    apiUrl = "mockApiUrl",
                    identityUrl = "mockIdentityUrl",
                    notificationsUrl = "mockNotificationsUrl",
                    ssoUrl = "mockSsoUrl",
                ),
                featureStates = emptyMap(),
            ),
        )

        private val SERVER_CONFIG_UNOFFICIAL = SERVER_CONFIG_DEFAULT
            .copy(
                serverData = SERVER_CONFIG_DEFAULT.serverData.copy(
                    server = ConfigResponseJson.ServerJson(
                        name = "mockUnofficialServerName",
                        url = "mockUnofficialServerUrl",
                    ),
                ),
            )

        private val UPDATE_KDF_RESPONSE = UpdateKdfResponse(
            masterPasswordAuthenticationData = MasterPasswordAuthenticationData(
                kdf = mockk<Kdf>(relaxed = true),
                salt = "mockSalt",
                masterPasswordAuthenticationHash = "mockHash",
            ),
            masterPasswordUnlockData = MasterPasswordUnlockData(
                kdf = mockk<Kdf>(relaxed = true),
                masterKeyWrappedUserKey = "mockKey",
                salt = "mockSalt",
            ),
            oldMasterPasswordAuthenticationData = MasterPasswordAuthenticationData(
                kdf = mockk<Kdf>(relaxed = true),
                salt = "mockSalt",
                masterPasswordAuthenticationHash = "mockHash",
            ),
        )
    }
}
