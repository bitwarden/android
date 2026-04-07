package com.x8bit.bitwarden.data.credentials.repository

import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.repository.util.combineDataStates
import com.bitwarden.core.data.util.decodeFromStringOrNull
import com.x8bit.bitwarden.data.credentials.datasource.disk.PrivilegedAppDiskSource
import com.x8bit.bitwarden.data.credentials.datasource.disk.entity.PrivilegedAppEntity
import com.x8bit.bitwarden.data.credentials.model.PrivilegedAppAllowListJson
import com.x8bit.bitwarden.data.credentials.repository.model.PrivilegedAppData
import com.x8bit.bitwarden.data.platform.manager.AssetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * A "stop timeout delay" in milliseconds used to let a shared coroutine continue to run for the
 * specified period of time after it no longer has subscribers.
 */
private const val STOP_TIMEOUT_DELAY_MS: Long = 1000L
private const val GOOGLE_ALLOW_LIST_FILE_NAME = "fido2_privileged_google.json"
private const val COMMUNITY_ALLOW_LIST_FILE_NAME = "fido2_privileged_community.json"
private const val ANDROID_TYPE = "android"
private const val RELEASE_BUILD = "release"

/**
 * Primary implementation of [PrivilegedAppRepository].
 */
class PrivilegedAppRepositoryImpl(
    private val privilegedAppDiskSource: PrivilegedAppDiskSource,
    private val assetManager: AssetManager,
    dispatcherManager: DispatcherManager,
    private val json: Json,
) : PrivilegedAppRepository {

    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)
    private val ioScope = CoroutineScope(dispatcherManager.io)

    private val mutableUserTrustedAppsFlow =
        MutableStateFlow<DataState<PrivilegedAppAllowListJson>>(DataState.Loading)
    private val mutableGoogleTrustedAppsFlow =
        MutableStateFlow<DataState<PrivilegedAppAllowListJson>>(DataState.Loading)
    private val mutableCommunityTrustedPrivilegedAppsFlow =
        MutableStateFlow<DataState<PrivilegedAppAllowListJson>>(DataState.Loading)

    override val trustedAppDataStateFlow: StateFlow<DataState<PrivilegedAppData>> =
        combine(
            userTrustedAppsFlow,
            googleTrustedPrivilegedAppsFlow,
            communityTrustedAppsFlow,
        ) { userAppsState, googleAppsState, communityAppsState ->
            combineDataStates(
                userAppsState,
                googleAppsState,
                communityAppsState,
            ) { userApps, googleApps, communityApps ->
                PrivilegedAppData(
                    googleTrustedApps = googleApps,
                    communityTrustedApps = communityApps,
                    userTrustedApps = userApps,
                )
            }
        }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_TIMEOUT_DELAY_MS),
                initialValue = DataState.Loading,
            )

    override val userTrustedAppsFlow: StateFlow<DataState<PrivilegedAppAllowListJson>>
        get() = mutableUserTrustedAppsFlow.asStateFlow()

    override val googleTrustedPrivilegedAppsFlow: StateFlow<DataState<PrivilegedAppAllowListJson>>
        get() = mutableGoogleTrustedAppsFlow.asStateFlow()

    override val communityTrustedAppsFlow: StateFlow<DataState<PrivilegedAppAllowListJson>>
        get() = mutableCommunityTrustedPrivilegedAppsFlow.asStateFlow()

    init {
        ioScope.launch {
            mutableGoogleTrustedAppsFlow.value = assetManager
                .readAsset(fileName = GOOGLE_ALLOW_LIST_FILE_NAME)
                .map { json.decodeFromString<PrivilegedAppAllowListJson>(it) }
                .fold(
                    onSuccess = { DataState.Loaded(it) },
                    onFailure = { DataState.Error(it) },
                )

            mutableCommunityTrustedPrivilegedAppsFlow.value = assetManager
                .readAsset(fileName = COMMUNITY_ALLOW_LIST_FILE_NAME)
                .map { json.decodeFromString<PrivilegedAppAllowListJson>(it) }
                .fold(
                    onSuccess = { DataState.Loaded(it) },
                    onFailure = { DataState.Error(it) },
                )
        }

        privilegedAppDiskSource
            .userTrustedPrivilegedAppsFlow
            .map { DataState.Loaded(it.toPrivilegedAppAllowListJson()) }
            .onEach { mutableUserTrustedAppsFlow.value = it }
            .launchIn(ioScope)
    }

    override suspend fun getUserTrustedPrivilegedAppsOrNull(): PrivilegedAppAllowListJson =
        privilegedAppDiskSource
            .getAllUserTrustedPrivilegedApps()
            .toPrivilegedAppAllowListJson()

    override suspend fun getGoogleTrustedPrivilegedAppsOrNull(): PrivilegedAppAllowListJson? =
        withContext(ioScope.coroutineContext) {
            assetManager
                .readAsset(fileName = GOOGLE_ALLOW_LIST_FILE_NAME)
                .map { json.decodeFromStringOrNull<PrivilegedAppAllowListJson>(it) }
                .getOrNull()
        }

    override suspend fun getCommunityTrustedPrivilegedAppsOrNull(): PrivilegedAppAllowListJson? {
        return withContext(ioScope.coroutineContext) {
            assetManager
                .readAsset(fileName = COMMUNITY_ALLOW_LIST_FILE_NAME)
                .map { json.decodeFromStringOrNull<PrivilegedAppAllowListJson>(it) }
                .getOrNull()
        }
    }

    override suspend fun isPrivilegedAppAllowed(
        packageName: String,
        signature: String,
    ): Boolean = privilegedAppDiskSource
        .isPrivilegedAppTrustedByUser(
            packageName = packageName,
            signature = signature,
        )

    override suspend fun addTrustedPrivilegedApp(
        packageName: String,
        signature: String,
    ): Unit = privilegedAppDiskSource
        .addTrustedPrivilegedApp(
            packageName = packageName,
            signature = signature,
        )

    override suspend fun removeTrustedPrivilegedApp(
        packageName: String,
        signature: String,
    ): Unit = privilegedAppDiskSource
        .removeTrustedPrivilegedApp(
            packageName = packageName,
            signature = signature,
        )

    override suspend fun getUserTrustedAllowListJson(): String = json
        .encodeToString(
            privilegedAppDiskSource
                .getAllUserTrustedPrivilegedApps()
                .toPrivilegedAppAllowListJson(),
        )
}

private fun List<PrivilegedAppEntity>.toPrivilegedAppAllowListJson() =
    PrivilegedAppAllowListJson(
        apps = map { it.toPrivilegedAppJson() },
    )

private fun PrivilegedAppEntity.toPrivilegedAppJson() =
    PrivilegedAppAllowListJson.PrivilegedAppJson(
        type = ANDROID_TYPE,
        info = PrivilegedAppAllowListJson.PrivilegedAppJson.InfoJson(
            packageName = packageName,
            signatures = listOf(
                PrivilegedAppAllowListJson
                    .PrivilegedAppJson
                    .InfoJson
                    .SignatureJson(
                        build = RELEASE_BUILD,
                        certFingerprintSha256 = signature,
                    ),
            ),
        ),
    )
