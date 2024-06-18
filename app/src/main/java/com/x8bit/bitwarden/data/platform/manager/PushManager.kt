package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.manager.model.NotificationLogoutData
import com.x8bit.bitwarden.data.platform.manager.model.PasswordlessRequestData
import com.x8bit.bitwarden.data.platform.manager.model.SyncCipherDeleteData
import com.x8bit.bitwarden.data.platform.manager.model.SyncCipherUpsertData
import com.x8bit.bitwarden.data.platform.manager.model.SyncFolderDeleteData
import com.x8bit.bitwarden.data.platform.manager.model.SyncFolderUpsertData
import com.x8bit.bitwarden.data.platform.manager.model.SyncSendDeleteData
import com.x8bit.bitwarden.data.platform.manager.model.SyncSendUpsertData
import kotlinx.coroutines.flow.Flow

/**
 * Manager to handle push notification registration.
 */
interface PushManager {
    /**
     * Flow that represents requests intended for full syncs.
     */
    val fullSyncFlow: Flow<Unit>

    /**
     * Flow that represents requests intended to log a user out.
     */
    val logoutFlow: Flow<NotificationLogoutData>

    /**
     * Flow that represents requests intended to trigger a passwordless request.
     */
    val passwordlessRequestFlow: Flow<PasswordlessRequestData>

    /**
     * Flow that represents requests intended to trigger a sync cipher delete.
     */
    val syncCipherDeleteFlow: Flow<SyncCipherDeleteData>

    /**
     * Flow that represents requests intended to trigger a sync cipher upsert.
     */
    val syncCipherUpsertFlow: Flow<SyncCipherUpsertData>

    /**
     * Flow that represents requests intended to trigger a sync cipher delete.
     */
    val syncFolderDeleteFlow: Flow<SyncFolderDeleteData>

    /**
     * Flow that represents requests intended to trigger a sync folder upsert.
     */
    val syncFolderUpsertFlow: Flow<SyncFolderUpsertData>

    /**
     * Flow that represents requests intended to trigger syncing organization keys.
     */
    val syncOrgKeysFlow: Flow<Unit>

    /**
     * Flow that represents requests intended to trigger a sync send delete.
     */
    val syncSendDeleteFlow: Flow<SyncSendDeleteData>

    /**
     * Flow that represents requests intended to trigger a sync send upsert.
     */
    val syncSendUpsertFlow: Flow<SyncSendUpsertData>

    /**
     * Handles the necessary steps to take when a push notification payload is received.
     */
    fun onMessageReceived(data: Map<String, String>)

    /**
     * Registers a [token] for the current user with Bitwarden's server if needed.
     */
    fun registerPushTokenIfNecessary(token: String)

    /**
     * Attempts to register a push token for the current user retrieved from storage if needed.
     */
    fun registerStoredPushTokenIfNecessary()
}
