package com.x8bit.bitwarden.data.platform.repository

import com.bitwarden.data.manager.DispatcherManager
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.data.repository.util.toEnvironmentUrls
import com.bitwarden.data.repository.util.toEnvironmentUrlsOrDefault
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.EnvironmentDiskSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

/**
 * Primary implementation of [EnvironmentRepository].
 */
class EnvironmentRepositoryImpl(
    private val environmentDiskSource: EnvironmentDiskSource,
    authDiskSource: AuthDiskSource,
    dispatcherManager: DispatcherManager,
) : EnvironmentRepository {

    private val scope = CoroutineScope(dispatcherManager.io)

    override var environment: Environment
        get() = environmentDiskSource
            .preAuthEnvironmentUrlData
            .toEnvironmentUrlsOrDefault()
        set(value) {
            environmentDiskSource.preAuthEnvironmentUrlData = value.environmentUrlData
        }

    override val environmentStateFlow: StateFlow<Environment> = environmentDiskSource
        .preAuthEnvironmentUrlDataFlow
        .map { it.toEnvironmentUrlsOrDefault() }
        .onEach { Timber.d("Current environment: ${it.type}") }
        .stateIn(
            scope = scope,
            started = SharingStarted.Lazily,
            initialValue = environment,
        )

    init {
        authDiskSource
            .userStateFlow
            .onEach { userState ->
                // If the active account has environment data, set that as the current value.
                userState
                    ?.activeAccount
                    ?.settings
                    ?.environmentUrlData
                    ?.let { environmentDiskSource.preAuthEnvironmentUrlData = it }
            }
            .launchIn(scope)
    }

    override fun loadEnvironmentForEmail(userEmail: String): Boolean {
        val urls = environmentDiskSource
            .getPreAuthEnvironmentUrlDataForEmail(userEmail)
            ?: return false
        environment = urls.toEnvironmentUrls()
        return true
    }

    override fun saveCurrentEnvironmentForEmail(userEmail: String) =
        environmentDiskSource
            .storePreAuthEnvironmentUrlDataForEmail(
                userEmail = userEmail,
                urls = environment.environmentUrlData,
            )
}
