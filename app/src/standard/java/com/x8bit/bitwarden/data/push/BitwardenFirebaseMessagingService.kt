package com.x8bit.bitwarden.data.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.x8bit.bitwarden.data.platform.manager.PushManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Handles setup and receiving of push notifications.
 */
@AndroidEntryPoint
class BitwardenFirebaseMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var pushManager: PushManager

    override fun onMessageReceived(message: RemoteMessage) {
        // TODO handle new messages. See BIT-1362.
    }

    override fun onNewToken(token: String) {
        pushManager.registerPushTokenIfNecessary(token)
    }
}
