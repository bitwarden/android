package com.x8bit.bitwarden.data.auth.repository

import com.bitwarden.core.Kdf
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson.CaptchaRequired
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson.Success
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.service.AccountsService
import com.x8bit.bitwarden.data.auth.datasource.network.service.IdentityService
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toKdfTypeJson
import com.x8bit.bitwarden.data.auth.repository.model.AuthState
import com.x8bit.bitwarden.data.auth.repository.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.model.RegisterResult
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.util.toSdkParams
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.AuthTokenInterceptor
import com.x8bit.bitwarden.data.platform.util.flatMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

private const val DEFAULT_KDF_ITERATIONS = 600000

/**
 * Default implementation of [AuthRepository].
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val accountsService: AccountsService,
    private val identityService: IdentityService,
    private val authSdkSource: AuthSdkSource,
    private val authDiskSource: AuthDiskSource,
    private val authTokenInterceptor: AuthTokenInterceptor,
) : AuthRepository {

    private val mutableAuthStateFlow = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authStateFlow: StateFlow<AuthState> = mutableAuthStateFlow.asStateFlow()

    private val mutableCaptchaTokenFlow =
        MutableSharedFlow<CaptchaCallbackTokenResult>(extraBufferCapacity = Int.MAX_VALUE)
    override val captchaTokenResultFlow: Flow<CaptchaCallbackTokenResult> =
        mutableCaptchaTokenFlow.asSharedFlow()

    override var rememberedEmailAddress: String?
        get() = authDiskSource.rememberedEmailAddress
        set(value) {
            authDiskSource.rememberedEmailAddress = value
        }

    // TODO Handle selected region functionality BIT-725
    override var selectedRegionLabel: String = "bitwarden.us"

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
                        // TODO: Create intermediate class for providing auth token
                        // to interceptor (BIT-411)
                        authTokenInterceptor.authToken = it.accessToken
                        mutableAuthStateFlow.value = AuthState.Authenticated(it.accessToken)
                        LoginResult.Success
                    }

                    is GetTokenResponseJson.Invalid -> {
                        LoginResult.Error(errorMessage = it.errorModel.errorMessage)
                    }
                }
            },
        )

    override fun logout() {
        mutableAuthStateFlow.update { AuthState.Unauthenticated }
    }

    override suspend fun register(
        email: String,
        masterPassword: String,
        masterPasswordHint: String?,
        captchaToken: String?,
    ): RegisterResult {
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
}
