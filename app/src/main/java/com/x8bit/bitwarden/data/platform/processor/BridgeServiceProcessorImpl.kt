package com.x8bit.bitwarden.data.platform.processor

import android.content.Intent
import android.os.Build
import android.os.IInterface
import android.os.RemoteCallbackList
import com.bitwarden.bridge.IBridgeService
import com.bitwarden.bridge.IBridgeServiceCallback
import com.bitwarden.bridge.model.EncryptedAddTotpLoginItemData
import com.bitwarden.bridge.model.SymmetricEncryptionKeyData
import com.bitwarden.bridge.model.SymmetricEncryptionKeyFingerprintData
import com.bitwarden.bridge.util.NATIVE_BRIDGE_SDK_VERSION
import com.bitwarden.bridge.util.encrypt
import com.bitwarden.bridge.util.toFingerprint
import com.bitwarden.bridge.util.toSymmetricEncryptionKeyData
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.repository.BridgeRepository
import com.x8bit.bitwarden.data.platform.util.isBuildVersionBelow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Default implementation of [BridgeServiceProcessor].
 */
class BridgeServiceProcessorImpl(
    private val bridgeRepository: BridgeRepository,
    private val featureFlagManager: FeatureFlagManager,
    dispatcherManager: DispatcherManager,
) : BridgeServiceProcessor {

    private val callbacks by lazy { RemoteCallbackList<IBridgeServiceCallback>() }
    private val scope by lazy { CoroutineScope(dispatcherManager.default) }

    override val binder: IBridgeService.Stub?
        get() {
            return if (
                !featureFlagManager.getFeatureFlag(FlagKey.AuthenticatorSync) ||
                isBuildVersionBelow(Build.VERSION_CODES.S)
            ) {
                // If the feature flag is not enabled, OR if version is below Android 12,
                // return a null binder which will no-op all service calls
                null
            } else {
                // Otherwise, return real binder implementation:
                defaultBinder
            }
        }

    /**
     * Default implementation of the bridge service binder.
     */
    private val defaultBinder = object : IBridgeService.Stub() {

        override fun getVersionNumber(): String = NATIVE_BRIDGE_SDK_VERSION

        override fun checkSymmetricEncryptionKeyFingerprint(
            symmetricKeyFingerprint: SymmetricEncryptionKeyFingerprintData?,
        ): Boolean {
            if (symmetricKeyFingerprint == null) return false
            val localSymmetricKeyFingerprint =
                bridgeRepository.authenticatorSyncSymmetricKey
                    ?.toSymmetricEncryptionKeyData()
                    ?.toFingerprint()
                    ?.getOrNull()
            return symmetricKeyFingerprint == localSymmetricKeyFingerprint
        }

        override fun getSymmetricEncryptionKeyData(): SymmetricEncryptionKeyData? =
            bridgeRepository.authenticatorSyncSymmetricKey?.toSymmetricEncryptionKeyData()

        override fun registerBridgeServiceCallback(callback: IBridgeServiceCallback?) {
            if (callback == null) return
            callbacks.register(callback)
        }

        override fun unregisterBridgeServiceCallback(callback: IBridgeServiceCallback?) {
            if (callback == null) return
            callbacks.unregister(callback)
        }

        override fun syncAccounts() {
            val symmetricEncryptionKey = symmetricEncryptionKeyData ?: return
            scope.launch {
                // Encrypt the shared account data with the symmetric key:
                val encryptedSharedAccountData = bridgeRepository
                    .getSharedAccounts()
                    .encrypt(symmetricEncryptionKey)
                    .getOrNull()
                    ?: return@launch

                // Report results to callback:
                callbacks.forEach { callback ->
                    callback.onAccountsSync(encryptedSharedAccountData)
                }
            }
        }

        override fun createAddTotpLoginItemIntent(): Intent {
            // TODO: BITAU-112
            return Intent()
        }

        override fun setPendingAddTotpLoginItemData(data: EncryptedAddTotpLoginItemData?) {
            // TODO: BITAU-112
        }
    }
}

/**
 * This function mirrors the hidden "RemoteCallbackList.broadcast" function.
 */
@Suppress("TooGenericExceptionCaught")
private fun <T : IInterface> RemoteCallbackList<T>.forEach(action: (T) -> Unit) {
    val count = this.beginBroadcast()
    try {
        for (index in 0..count) {
            action(this.getBroadcastItem(index))
        }
    } catch (e: Exception) {
        // Broadcast failed
    } finally {
        this.finishBroadcast()
    }
}
