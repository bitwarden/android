package com.x8bit.bitwarden.data.auth.repository

import com.bitwarden.core.Kdf
import com.bitwarden.sdk.Client
import com.x8bit.bitwarden.data.auth.datasource.network.api.AccountsApi
import com.x8bit.bitwarden.data.auth.datasource.network.api.IdentityApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthState
import com.x8bit.bitwarden.data.auth.datasource.network.model.LoginResult
import com.x8bit.bitwarden.data.auth.datasource.network.model.PreLoginRequestJson
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.AuthTokenInterceptor
import com.x8bit.bitwarden.data.platform.util.flatMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [AuthRepository].
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val accountsApi: AccountsApi,
    private val identityApi: IdentityApi,
    private val bitwardenSdkClient: Client,
    private val authTokenInterceptor: AuthTokenInterceptor,
) : AuthRepository {

    private val mutableAuthStateFlow = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authStateFlow: StateFlow<AuthState> = mutableAuthStateFlow.asStateFlow()

    /**
     * Attempt to login with the given email.
     */
    override suspend fun login(
        email: String,
        password: String,
    ): LoginResult = accountsApi
        .preLogin(PreLoginRequestJson(email))
        .flatMap {
            // TODO: Use KDF enum from pre login correctly (BIT-329)
            val passwordHash = bitwardenSdkClient
                .auth()
                .hashPassword(
                    email = email,
                    password = password,
                    kdfParams = Kdf.Pbkdf2(it.kdfIterations),
                )
            identityApi.getToken(
                email = email,
                passwordHash = passwordHash,
            )
        }
        .fold(
            onFailure = {
                // TODO: Add more detail to these cases to expose server error messages (BIT-320)
                LoginResult.Error
            },
            onSuccess = {
                // TODO: Create intermediate class for providing auth token to interceptor (BIT-411)
                authTokenInterceptor.authToken = it.accessToken
                mutableAuthStateFlow.value = AuthState.Authenticated(it.accessToken)
                LoginResult.Success
            },
        )
}
