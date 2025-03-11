package com.x8bit.bitwarden.data.autofill.fido2.repository

import com.x8bit.bitwarden.data.autofill.fido2.datasource.disk.Fido2PrivilegedAppDiskSource
import com.x8bit.bitwarden.data.autofill.fido2.datasource.disk.entity.Fido2PrivilegedAppInfoEntity
import com.x8bit.bitwarden.data.autofill.fido2.model.PrivilegedAppAllowListJson
import com.x8bit.bitwarden.data.autofill.fido2.model.PrivilegedAppData
import com.x8bit.bitwarden.data.platform.manager.AssetManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.util.combineDataStates
import com.x8bit.bitwarden.data.platform.util.decodeFromStringOrNull
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
    private val fido2PrivilegedAppDiskSource: Fido2PrivilegedAppDiskSource,
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
            val googleAppsDataState = assetManager.readAsset(fileName = GOOGLE_ALLOW_LIST_FILE_NAME)
                .map { json.decodeFromString<PrivilegedAppAllowListJson>(it) }
                .fold(
                    onSuccess = { DataState.Loaded(it) },
                    onFailure = { DataState.Error(it) },
                )

            val communityAppsDataState =
                assetManager.readAsset(fileName = COMMUNITY_ALLOW_LIST_FILE_NAME)
                    .map { json.decodeFromString<PrivilegedAppAllowListJson>(it) }
                    .fold(
                        onSuccess = { DataState.Loaded(it) },
                        onFailure = { DataState.Error(it) },
                    )

            mutableGoogleTrustedAppsFlow.value = googleAppsDataState
            mutableCommunityTrustedPrivilegedAppsFlow.value = communityAppsDataState

            fido2PrivilegedAppDiskSource.userTrustedPrivilegedAppsFlow
                .map { DataState.Loaded(it.toFido2PrivilegedAppAllowListJson()) }
                .onEach {
                    mutableUserTrustedAppsFlow.value = it
                }
                .launchIn(ioScope)
        }
    }

    override suspend fun getUserTrustedPrivilegedAppsOrNull(): PrivilegedAppAllowListJson =
        fido2PrivilegedAppDiskSource
            .getAllUserTrustedPrivilegedApps()
            .toFido2PrivilegedAppAllowListJson()

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

    override suspend fun addTrustedPrivilegedApp(
        packageName: String,
        signature: String,
    ) = fido2PrivilegedAppDiskSource
        .addTrustedPrivilegedApp(
            packageName = packageName,
            signature = signature,
        )

    override suspend fun removeTrustedPrivilegedApp(
        packageName: String,
        signature: String,
    ) = fido2PrivilegedAppDiskSource
        .removeTrustedPrivilegedApp(
            packageName = packageName,
            signature = signature,
        )

    override suspend fun getUserTrustedAllowListJson(): String = json
        .encodeToString(
            fido2PrivilegedAppDiskSource
                .getAllUserTrustedPrivilegedApps()
                .toFido2PrivilegedAppAllowListJson(),
        )
}

@Suppress("MaxLineLength")
private fun List<Fido2PrivilegedAppInfoEntity>.toFido2PrivilegedAppAllowListJson() =
    PrivilegedAppAllowListJson(
        apps = map { it.toFido2PrivilegedAppJson() },
    )

private fun Fido2PrivilegedAppInfoEntity.toFido2PrivilegedAppJson() =
    PrivilegedAppAllowListJson.PrivilegedAppJson(
        type = ANDROID_TYPE,
        info = PrivilegedAppAllowListJson.PrivilegedAppJson.PrivilegedAppInfoJson(
            packageName = packageName,
            signatures = listOf(
                PrivilegedAppAllowListJson
                    .PrivilegedAppJson
                    .PrivilegedAppInfoJson
                    .PrivilegedAppSignatureJson(
                        build = RELEASE_BUILD,
                        certFingerprintSha256 = signature,
                    ),
            ),
        ),
    )
