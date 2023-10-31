package com.x8bit.bitwarden.data.auth.repository

import com.bitwarden.core.Kdf
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson.CaptchaRequired
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson.Success
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.service.AccountsService
import com.x8bit.bitwarden.data.auth.datasource.network.service.HaveIBeenPwnedService
import com.x8bit.bitwarden.data.auth.datasource.network.service.IdentityService
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toKdfTypeJson
import com.x8bit.bitwarden.data.auth.repository.model.AuthState
import com.x8bit.bitwarden.data.auth.repository.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.model.RegisterResult
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.toUserState
import com.x8bit.bitwarden.data.auth.util.toSdkParams
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.platform.util.flatMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

private const val DEFAULT_KDF_ITERATIONS = 600000

/**
 * Default implementation of [AuthRepository].
 */
@Suppress("LongParameterList")
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val accountsService: AccountsService,
    private val haveIBeenPwnedService: HaveIBeenPwnedService,
    private val identityService: IdentityService,
    private val authSdkSource: AuthSdkSource,
    private val authDiskSource: AuthDiskSource,
    dispatcherManager: DispatcherManager,
) : AuthRepository {
    private val scope = CoroutineScope(dispatcherManager.io)

    override val authStateFlow: StateFlow<AuthState> = authDiskSource
        .userStateFlow
        .map { userState ->
            userState
                ?.let {
                    @Suppress("UnsafeCallOnNullableType")
                    AuthState.Authenticated(
                        userState
                            .activeAccount
                            .tokens
                            .accessToken,
                    )
                }
                ?: AuthState.Unauthenticated
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = AuthState.Uninitialized,
        )

    private val mutableCaptchaTokenFlow =
        MutableSharedFlow<CaptchaCallbackTokenResult>(extraBufferCapacity = Int.MAX_VALUE)
    override val captchaTokenResultFlow: Flow<CaptchaCallbackTokenResult> =
        mutableCaptchaTokenFlow.asSharedFlow()

    override var rememberedEmailAddress: String?
        get() = authDiskSource.rememberedEmailAddress
        set(value) {
            authDiskSource.rememberedEmailAddress = value
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
            )
        }
        .flatMap { passwordHash ->
            identityService.getToken(
                email = email,
                passwordHash = passwordHash,
                captchaToken = captchaToken,
            )
        }
        .fold(
            onFailure = { LoginResult.Error(errorMessage = null) },
            onSuccess = {
                when (it) {
                    is CaptchaRequired -> LoginResult.CaptchaRequired(it.captchaKey)
                    is Success -> {
                        authDiskSource.userState = it
                            .toUserState(
                                previousUserState = authDiskSource.userState,
                            )
                        LoginResult.Success
                    }

                    is GetTokenResponseJson.Invalid -> {
                        LoginResult.Error(errorMessage = it.errorModel.errorMessage)
                    }
                }
            },
        )

    override fun logout() {
        val currentUserState = authDiskSource.userState ?: return

        val activeUserId = currentUserState.activeUserId

        // Remove the active user from the accounts map
        val updatedAccounts = currentUserState
            .accounts
            .filterKeys { it != activeUserId }

        // Check if there is a new active user
        if (updatedAccounts.isNotEmpty()) {
            val (updatedActiveUserId, updatedActiveAccount) =
                updatedAccounts.entries.first()

            // Update the user information and emit an updated token
            authDiskSource.userState = currentUserState.copy(
                activeUserId = updatedActiveUserId,
                accounts = updatedAccounts,
            )
        } else {
            // Update the user information and log out
            authDiskSource.userState = null
        }
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
        val kdf = Kdf.Pbkdf2(DEFAULT_KDF_ITERATIONS.toUInt())
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

    override fun setCaptchaCallbackTokenResult(tokenResult: CaptchaCallbackTokenResult) {
        mutableCaptchaTokenFlow.tryEmit(tokenResult)
    }

    @Suppress("MagicNumber")
    override suspend fun getPasswordStrength(
        email: String,
        password: String,
    ): Result<PasswordStrength> {
        // TODO: Replace with SDK call (BIT-964)
        // Ex: return authSdkSource.passwordStrength(email, password)
        val length = password.length
        return when {
            length <= 3 -> PasswordStrength.LEVEL_0
            length <= 6 -> PasswordStrength.LEVEL_1
            length <= 9 -> PasswordStrength.LEVEL_2
            length <= 11 -> PasswordStrength.LEVEL_3
            else -> PasswordStrength.LEVEL_4
        }
            .asSuccess()
    }
}
