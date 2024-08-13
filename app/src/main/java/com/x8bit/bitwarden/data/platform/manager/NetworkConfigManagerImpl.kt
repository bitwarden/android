package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.AuthState
import com.x8bit.bitwarden.data.platform.datasource.network.authenticator.RefreshAuthenticator
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.AuthTokenInterceptor
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.BaseUrlInterceptors
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.ServerConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private const val ENVIRONMENT_DEBOUNCE_TIMEOUT_MS: Long = 500L

/**
 * Primary implementation of [NetworkConfigManager].
 */
@Suppress("LongParameterList")
class NetworkConfigManagerImpl(
    authRepository: AuthRepository,
    private val authTokenInterceptor: AuthTokenInterceptor,
    environmentRepository: EnvironmentRepository,
    serverConfigRepository: ServerConfigRepository,
    private val baseUrlInterceptors: BaseUrlInterceptors,
    refreshAuthenticator: RefreshAuthenticator,
    dispatcherManager: DispatcherManager,
) : NetworkConfigManager {

    private val collectionScope = CoroutineScope(dispatcherManager.unconfined)

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
            .launchIn(collectionScope)

        @Suppress("OPT_IN_USAGE")
        environmentRepository
            .environmentStateFlow
            .onEach { environment ->
                baseUrlInterceptors.environment = environment
            }
            .debounce(timeoutMillis = ENVIRONMENT_DEBOUNCE_TIMEOUT_MS)
            .onEach { _ ->
                // This updates the stored service configuration by performing a network request.
                // We debounce it to avoid rapid repeated requests.
                serverConfigRepository.getServerConfig(forceRefresh = true)
            }
            .launchIn(collectionScope)

        refreshAuthenticator.authenticatorProvider = authRepository
    }
}
