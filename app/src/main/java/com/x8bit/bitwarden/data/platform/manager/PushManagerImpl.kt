package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.PushDiskSource
import com.x8bit.bitwarden.data.platform.datasource.network.model.PushTokenRequest
import com.x8bit.bitwarden.data.platform.datasource.network.service.PushService
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.manager.model.BitwardenNotification
import com.x8bit.bitwarden.data.platform.manager.model.NotificationPayload
import com.x8bit.bitwarden.data.platform.manager.model.NotificationType
import com.x8bit.bitwarden.data.platform.manager.model.PasswordlessRequestData
import com.x8bit.bitwarden.data.platform.manager.model.SyncCipherDeleteData
import com.x8bit.bitwarden.data.platform.manager.model.SyncCipherUpsertData
import com.x8bit.bitwarden.data.platform.manager.model.SyncFolderDeleteData
import com.x8bit.bitwarden.data.platform.manager.model.SyncFolderUpsertData
import com.x8bit.bitwarden.data.platform.manager.model.SyncSendDeleteData
import com.x8bit.bitwarden.data.platform.manager.model.SyncSendUpsertData
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import java.time.Clock
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Primary implementation of [PushManager].
 */
class PushManagerImpl @Inject constructor(
    private val authDiskSource: AuthDiskSource,
    private val pushDiskSource: PushDiskSource,
    private val pushService: PushService,
    private val clock: Clock,
    private val json: Json,
    dispatcherManager: DispatcherManager,
) : PushManager {
    private val ioScope = CoroutineScope(dispatcherManager.io)
    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    private val mutableFullSyncSharedFlow = bufferedMutableSharedFlow<Unit>()
    private val mutableLogoutSharedFlow = bufferedMutableSharedFlow<Unit>()
    private val mutablePasswordlessRequestSharedFlow =
        bufferedMutableSharedFlow<PasswordlessRequestData>()
    private val mutableSyncCipherDeleteSharedFlow =
        bufferedMutableSharedFlow<SyncCipherDeleteData>()
    private val mutableSyncCipherUpsertSharedFlow =
        bufferedMutableSharedFlow<SyncCipherUpsertData>()
    private val mutableSyncFolderDeleteSharedFlow =
        bufferedMutableSharedFlow<SyncFolderDeleteData>()
    private val mutableSyncFolderUpsertSharedFlow =
        bufferedMutableSharedFlow<SyncFolderUpsertData>()
    private val mutableSyncOrgKeysSharedFlow = bufferedMutableSharedFlow<Unit>()
    private val mutableSyncSendDeleteSharedFlow =
        bufferedMutableSharedFlow<SyncSendDeleteData>()
    private val mutableSyncSendUpsertSharedFlow =
        bufferedMutableSharedFlow<SyncSendUpsertData>()

    override val fullSyncFlow: SharedFlow<Unit>
        get() = mutableFullSyncSharedFlow.asSharedFlow()

    override val logoutFlow: SharedFlow<Unit>
        get() = mutableLogoutSharedFlow.asSharedFlow()

    override val passwordlessRequestFlow: SharedFlow<PasswordlessRequestData>
        get() = mutablePasswordlessRequestSharedFlow.asSharedFlow()

    override val syncCipherDeleteFlow: SharedFlow<SyncCipherDeleteData>
        get() = mutableSyncCipherDeleteSharedFlow.asSharedFlow()

    override val syncCipherUpsertFlow: SharedFlow<SyncCipherUpsertData>
        get() = mutableSyncCipherUpsertSharedFlow.asSharedFlow()

    override val syncFolderDeleteFlow: SharedFlow<SyncFolderDeleteData>
        get() = mutableSyncFolderDeleteSharedFlow.asSharedFlow()

    override val syncFolderUpsertFlow: SharedFlow<SyncFolderUpsertData>
        get() = mutableSyncFolderUpsertSharedFlow.asSharedFlow()

    override val syncOrgKeysFlow: SharedFlow<Unit>
        get() = mutableSyncOrgKeysSharedFlow.asSharedFlow()

    override val syncSendDeleteFlow: SharedFlow<SyncSendDeleteData>
        get() = mutableSyncSendDeleteSharedFlow.asSharedFlow()

    override val syncSendUpsertFlow: SharedFlow<SyncSendUpsertData>
        get() = mutableSyncSendUpsertSharedFlow.asSharedFlow()

    init {
        authDiskSource
            .userStateFlow
            .mapNotNull { it?.activeUserId }
            .distinctUntilChanged()
            .onEach { registerStoredPushTokenIfNecessary() }
            .launchIn(unconfinedScope)
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod", "ReturnCount")
    override fun onMessageReceived(data: String) {
        val notification = try {
            json.decodeFromString<BitwardenNotification>(data)
        } catch (exception: IllegalArgumentException) {
            return
        }

        if (authDiskSource.uniqueAppId == notification.contextId) return

        val userId = authDiskSource.userState?.activeUserId

        when (val type = notification.notificationType) {
            NotificationType.AUTH_REQUEST,
            NotificationType.AUTH_REQUEST_RESPONSE,
            -> {
                val payload: NotificationPayload.PasswordlessRequestNotification =
                    json.decodeFromJsonElement(notification.payload)
                mutablePasswordlessRequestSharedFlow.tryEmit(
                    PasswordlessRequestData(
                        loginRequestId = payload.id,
                        userId = payload.userId,
                    ),
                )
            }

            NotificationType.LOG_OUT -> {
                if (userId == null) return
                mutableLogoutSharedFlow.tryEmit(Unit)
            }

            NotificationType.SYNC_CIPHER_CREATE,
            NotificationType.SYNC_CIPHER_UPDATE,
            -> {
                val payload: NotificationPayload.SyncCipherNotification =
                    json.decodeFromJsonElement(notification.payload)
                if (!payload.userMatchesNotification(userId)) return
                mutableSyncCipherUpsertSharedFlow.tryEmit(
                    SyncCipherUpsertData(
                        cipherId = payload.id,
                        revisionDate = payload.revisionDate,
                        isUpdate = type == NotificationType.SYNC_CIPHER_UPDATE,
                    ),
                )
            }

            NotificationType.SYNC_CIPHER_DELETE,
            NotificationType.SYNC_LOGIN_DELETE,
            -> {
                val payload: NotificationPayload.SyncCipherNotification =
                    json.decodeFromJsonElement(notification.payload)
                if (!payload.userMatchesNotification(userId)) return
                mutableSyncCipherDeleteSharedFlow.tryEmit(
                    SyncCipherDeleteData(payload.id),
                )
            }

            NotificationType.SYNC_CIPHERS,
            NotificationType.SYNC_SETTINGS,
            NotificationType.SYNC_VAULT,
            -> {
                if (userId == null) return
                mutableFullSyncSharedFlow.tryEmit(Unit)
            }

            NotificationType.SYNC_FOLDER_CREATE,
            NotificationType.SYNC_FOLDER_UPDATE,
            -> {
                val payload: NotificationPayload.SyncFolderNotification =
                    json.decodeFromJsonElement(notification.payload)
                if (!payload.userMatchesNotification(userId)) return
                mutableSyncFolderUpsertSharedFlow.tryEmit(
                    SyncFolderUpsertData(
                        folderId = payload.id,
                        revisionDate = payload.revisionDate,
                        isUpdate = type == NotificationType.SYNC_FOLDER_UPDATE,
                    ),
                )
            }

            NotificationType.SYNC_FOLDER_DELETE -> {
                val payload: NotificationPayload.SyncFolderNotification =
                    json.decodeFromJsonElement(notification.payload)
                if (!payload.userMatchesNotification(userId)) return

                mutableSyncFolderDeleteSharedFlow.tryEmit(
                    SyncFolderDeleteData(payload.id),
                )
            }

            NotificationType.SYNC_ORG_KEYS -> {
                if (userId == null) return
                mutableSyncOrgKeysSharedFlow.tryEmit(Unit)
            }

            NotificationType.SYNC_SEND_CREATE,
            NotificationType.SYNC_SEND_UPDATE,
            -> {
                val payload: NotificationPayload.SyncSendNotification =
                    json.decodeFromJsonElement(notification.payload)
                if (!payload.userMatchesNotification(userId)) return
                mutableSyncSendUpsertSharedFlow.tryEmit(
                    SyncSendUpsertData(
                        sendId = payload.id,
                        revisionDate = payload.revisionDate,
                        isUpdate = type == NotificationType.SYNC_SEND_UPDATE,
                    ),
                )
            }

            NotificationType.SYNC_SEND_DELETE -> {
                val payload: NotificationPayload.SyncSendNotification =
                    json.decodeFromJsonElement(notification.payload)
                if (!payload.userMatchesNotification(userId)) return
                mutableSyncSendDeleteSharedFlow.tryEmit(
                    SyncSendDeleteData(payload.id),
                )
            }
        }
    }

    override fun registerPushTokenIfNecessary(token: String) {
        pushDiskSource.registeredPushToken = token
        val userId = authDiskSource.userState?.activeUserId ?: return
        ioScope.launch {
            registerPushTokenIfNecessaryInternal(
                userId = userId,
                token = token,
            )
        }
    }

    override fun registerStoredPushTokenIfNecessary() {
        val userId = authDiskSource.userState?.activeUserId ?: return

        // If the last registered token is from less than a day before, skip this for now
        val lastRegistration = pushDiskSource.getLastPushTokenRegistrationDate(userId)?.toInstant()
        val dayBefore = clock.instant().minus(1, ChronoUnit.DAYS)
        if (lastRegistration?.isAfter(dayBefore) == true) return

        ioScope.launch {
            pushDiskSource.registeredPushToken?.let {
                registerPushTokenIfNecessaryInternal(
                    userId = userId,
                    token = it,
                )
            }
        }
    }

    private suspend fun registerPushTokenIfNecessaryInternal(userId: String, token: String) {
        val currentToken = pushDiskSource.getCurrentPushToken(userId)

        if (token == currentToken) {
            // Our token is up-to-date, so just update the last registration date
            pushDiskSource.storeLastPushTokenRegistrationDate(
                userId,
                ZonedDateTime.ofInstant(clock.instant(), ZoneOffset.UTC),
            )
            return
        }

        pushService
            .putDeviceToken(
                PushTokenRequest(token),
            )
            .fold(
                onSuccess = {
                    pushDiskSource.storeLastPushTokenRegistrationDate(
                        userId,
                        ZonedDateTime.ofInstant(clock.instant(), ZoneOffset.UTC),
                    )
                    pushDiskSource.storeCurrentPushToken(
                        userId = userId,
                        pushToken = token,
                    )
                },
                onFailure = {
                    // Silently fail. This call will be attempted again the next time the token
                    // registration is done.
                },
            )
    }
}

private fun NotificationPayload.userMatchesNotification(userId: String?): Boolean {
    return this.userId != null && this.userId == userId
}
