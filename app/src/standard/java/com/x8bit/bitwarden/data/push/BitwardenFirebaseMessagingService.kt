package com.x8bit.bitwarden.data.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.platform.manager.PushManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Handles setup and receiving of push notifications.
 */
@OmitFromCoverage
@AndroidEntryPoint
class BitwardenFirebaseMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var pushManager: PushManager

    override fun onMessageReceived(message: RemoteMessage) {
        pushManager.onMessageReceived(message.data)
    }

    override fun onNewToken(token: String) {
        pushManager.registerPushTokenIfNecessary(token)
    }
}
