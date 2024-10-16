package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.util.activeUserIdChangesFlow
import com.x8bit.bitwarden.data.platform.datasource.disk.PushDiskSource
import com.x8bit.bitwarden.data.platform.datasource.network.model.PushTokenRequest
import com.x8bit.bitwarden.data.platform.datasource.network.service.PushService
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.manager.model.BitwardenNotification
import com.x8bit.bitwarden.data.platform.manager.model.NotificationLogoutData
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
import com.x8bit.bitwarden.data.platform.util.decodeFromStringOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
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
    private val mutableLogoutSharedFlow = bufferedMutableSharedFlow<NotificationLogoutData>()
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

    override val logoutFlow: SharedFlow<NotificationLogoutData>
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

    private val activeUserId: String?
        get() = authDiskSource.userState?.activeUserId

    init {
        authDiskSource
            .activeUserIdChangesFlow
            .mapNotNull { it }
            .onEach { registerStoredPushTokenIfNecessary() }
            .launchIn(unconfinedScope)
    }

    override fun onMessageReceived(data: Map<String, String>) {
        val notificationType = data["type"]
            ?.let { json.decodeFromStringOrNull<NotificationType>(string = it) }
            ?: return
        val payload = data["payload"] ?: return
        val notification = BitwardenNotification(
            contextId = data["contextId"],
            notificationType = notificationType,
            payload = payload,
        )
        onMessageReceived(notification)
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun onMessageReceived(notification: BitwardenNotification) {
        if (authDiskSource.uniqueAppId == notification.contextId) return

        val userId = activeUserId ?: return

        when (val type = notification.notificationType) {
            NotificationType.AUTH_REQUEST,
            NotificationType.AUTH_REQUEST_RESPONSE,
                -> {
                json
                    .decodeFromString<NotificationPayload.PasswordlessRequestNotification>(
                        string = notification.payload,
                    )
                    .takeIf { it.loginRequestId != null && it.userId != null }
                    ?.let {
                        mutablePasswordlessRequestSharedFlow.tryEmit(
                            PasswordlessRequestData(
                                loginRequestId = requireNotNull(it.loginRequestId),
                                userId = requireNotNull(it.userId),
                            ),
                        )
                    }
            }

            NotificationType.LOG_OUT -> {
                json
                    .decodeFromString<NotificationPayload.UserNotification>(
                        string = notification.payload,
                    )
                    .userId
                    ?.let { mutableLogoutSharedFlow.tryEmit(NotificationLogoutData(it)) }
            }

            NotificationType.SYNC_CIPHER_CREATE,
            NotificationType.SYNC_CIPHER_UPDATE,
                -> {
                json
                    .decodeFromString<NotificationPayload.SyncCipherNotification>(
                        string = notification.payload,
                    )
                    .takeIf { isLoggedIn(userId) && it.userMatchesNotification(userId) }
                    ?.takeIf { it.cipherId != null && it.revisionDate != null }
                    ?.let {
                        mutableSyncCipherUpsertSharedFlow.tryEmit(
                            SyncCipherUpsertData(
                                cipherId = requireNotNull(it.cipherId),
                                revisionDate = requireNotNull(it.revisionDate),
                                organizationId = it.organizationId,
                                collectionIds = it.collectionIds,
                                isUpdate = type == NotificationType.SYNC_CIPHER_UPDATE,
                            ),
                        )
                    }
            }

            NotificationType.SYNC_CIPHER_DELETE,
            NotificationType.SYNC_LOGIN_DELETE,
                -> {
                json
                    .decodeFromString<NotificationPayload.SyncCipherNotification>(
                        string = notification.payload,
                    )
                    .takeIf { isLoggedIn(userId) && it.userMatchesNotification(userId) }
                    ?.cipherId
                    ?.let { mutableSyncCipherDeleteSharedFlow.tryEmit(SyncCipherDeleteData(it)) }
            }

            NotificationType.SYNC_CIPHERS,
            NotificationType.SYNC_SETTINGS,
            NotificationType.SYNC_VAULT,
                -> {
                mutableFullSyncSharedFlow.tryEmit(Unit)
            }

            NotificationType.SYNC_FOLDER_CREATE,
            NotificationType.SYNC_FOLDER_UPDATE,
                -> {
                json
                    .decodeFromString<NotificationPayload.SyncFolderNotification>(
                        string = notification.payload,
                    )
                    .takeIf { isLoggedIn(userId) && it.userMatchesNotification(userId) }
                    ?.takeIf { it.folderId != null && it.revisionDate != null }
                    ?.let {
                        mutableSyncFolderUpsertSharedFlow.tryEmit(
                            SyncFolderUpsertData(
                                folderId = requireNotNull(it.folderId),
                                revisionDate = requireNotNull(it.revisionDate),
                                isUpdate = type == NotificationType.SYNC_FOLDER_UPDATE,
                            ),
                        )
                    }
            }

            NotificationType.SYNC_FOLDER_DELETE -> {
                json
                    .decodeFromString<NotificationPayload.SyncFolderNotification>(
                        string = notification.payload,
                    )
                    .takeIf { isLoggedIn(userId) && it.userMatchesNotification(userId) }
                    ?.folderId
                    ?.let { mutableSyncFolderDeleteSharedFlow.tryEmit(SyncFolderDeleteData(it)) }
            }

            NotificationType.SYNC_ORG_KEYS -> {
                if (isLoggedIn(userId)) {
                    mutableSyncOrgKeysSharedFlow.tryEmit(Unit)
                }
            }

            NotificationType.SYNC_SEND_CREATE,
            NotificationType.SYNC_SEND_UPDATE,
                -> {
                json
                    .decodeFromString<NotificationPayload.SyncSendNotification>(
                        string = notification.payload,
                    )
                    .takeIf { isLoggedIn(userId) && it.userMatchesNotification(userId) }
                    ?.takeIf { it.sendId != null && it.revisionDate != null }
                    ?.let {
                        mutableSyncSendUpsertSharedFlow.tryEmit(
                            SyncSendUpsertData(
                                sendId = requireNotNull(it.sendId),
                                revisionDate = requireNotNull(it.revisionDate),
                                isUpdate = type == NotificationType.SYNC_SEND_UPDATE,
                            ),
                        )
                    }
            }

            NotificationType.SYNC_SEND_DELETE -> {
                json
                    .decodeFromString<NotificationPayload.SyncSendNotification>(
                        string = notification.payload,
                    )
                    .takeIf { isLoggedIn(userId) && it.userMatchesNotification(userId) }
                    ?.sendId
                    ?.let { mutableSyncSendDeleteSharedFlow.tryEmit(SyncSendDeleteData(it)) }
            }
        }
    }

    override fun registerPushTokenIfNecessary(token: String) {
        pushDiskSource.registeredPushToken = token

        val userId = activeUserId ?: return
        if (!isLoggedIn(userId)) return
        ioScope.launch {
            registerPushTokenIfNecessaryInternal(
                userId = userId,
                token = token,
            )
        }
    }

    override fun registerStoredPushTokenIfNecessary() {
        val userId = activeUserId ?: return
        if (!isLoggedIn(userId)) return

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
                userId = userId,
                registrationDate = ZonedDateTime.ofInstant(clock.instant(), ZoneOffset.UTC),
            )
            return
        }

        pushService
            .putDeviceToken(
                body = PushTokenRequest(token),
            )
            .fold(
                onSuccess = {
                    pushDiskSource.storeLastPushTokenRegistrationDate(
                        userId = userId,
                        registrationDate = ZonedDateTime.ofInstant(clock.instant(), ZoneOffset.UTC),
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

    private fun isLoggedIn(
        userId: String,
    ): Boolean = authDiskSource.getAccountTokens(userId)?.isLoggedIn == true
}

private fun NotificationPayload.userMatchesNotification(userId: String): Boolean {
    return this.userId != null && this.userId == userId
}
