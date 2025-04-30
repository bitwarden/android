package com.x8bit.bitwarden.data.platform.manager.network

import com.bitwarden.data.manager.DispatcherManager
import com.bitwarden.data.repository.ServerConfigRepository
import com.bitwarden.network.BitwardenServiceClient
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private const val ENVIRONMENT_DEBOUNCE_TIMEOUT_MS: Long = 500L

/**
 * Primary implementation of [NetworkConfigManager].
 */
class NetworkConfigManagerImpl(
    authRepository: AuthRepository,
    environmentRepository: EnvironmentRepository,
    serverConfigRepository: ServerConfigRepository,
    bitwardenServiceClient: BitwardenServiceClient,
    dispatcherManager: DispatcherManager,
) : NetworkConfigManager {

    private val collectionScope = CoroutineScope(dispatcherManager.unconfined)

    init {
        @Suppress("OPT_IN_USAGE")
        environmentRepository
            .environmentStateFlow
            .debounce(timeoutMillis = ENVIRONMENT_DEBOUNCE_TIMEOUT_MS)
            .onEach { _ ->
                // This updates the stored service configuration by performing a network request.
                // We debounce it to avoid rapid repeated requests.
                serverConfigRepository.getServerConfig(forceRefresh = true)
            }
            .launchIn(collectionScope)
        bitwardenServiceClient.setRefreshTokenProvider(authRepository)
    }
}
