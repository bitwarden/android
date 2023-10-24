package com.x8bit.bitwarden.data.platform.repository

import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.AuthState
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.AuthTokenInterceptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Primary implementation of [NetworkConfigRepository].
 */
class NetworkConfigRepositoryImpl(
    private val authRepository: AuthRepository,
    private val authTokenInterceptor: AuthTokenInterceptor,
    private val environmentRepository: EnvironmentRepository,
    dispatcher: CoroutineDispatcher,
) : NetworkConfigRepository {

    private val scope = CoroutineScope(dispatcher)

    init {
        authRepository
            .authStateFlow
            .onEach { authState ->
                authTokenInterceptor.authToken = when (authState) {
                    is AuthState.Authenticated -> authState.accessToken
                    is AuthState.Unauthenticated -> null
                    is AuthState.Uninitialized -> null
                }
            }
            .launchIn(scope)

        environmentRepository
            .environmentStateFlow
            .onEach { environment ->
                // TODO: Update base URL interceptors (BIT-725)
            }
            .launchIn(scope)
    }
}
