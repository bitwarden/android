package com.x8bit.bitwarden.data.platform.processor

import android.content.Context
import android.os.Build
import android.os.IInterface
import android.os.RemoteCallbackList
import androidx.core.net.toUri
import com.bitwarden.authenticatorbridge.IAuthenticatorBridgeService
import com.bitwarden.authenticatorbridge.IAuthenticatorBridgeServiceCallback
import com.bitwarden.authenticatorbridge.model.EncryptedAddTotpLoginItemData
import com.bitwarden.authenticatorbridge.model.SymmetricEncryptionKeyData
import com.bitwarden.authenticatorbridge.model.SymmetricEncryptionKeyFingerprintData
import com.bitwarden.authenticatorbridge.util.AUTHENTICATOR_BRIDGE_SDK_VERSION
import com.bitwarden.authenticatorbridge.util.decrypt
import com.bitwarden.authenticatorbridge.util.encrypt
import com.bitwarden.authenticatorbridge.util.toFingerprint
import com.bitwarden.authenticatorbridge.util.toSymmetricEncryptionKeyData
import com.x8bit.bitwarden.data.auth.manager.AddTotpItemFromAuthenticatorManager
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.repository.AuthenticatorBridgeRepository
import com.x8bit.bitwarden.data.platform.util.createAddTotpItemFromAuthenticatorIntent
import com.x8bit.bitwarden.data.platform.util.isBuildVersionBelow
import com.x8bit.bitwarden.ui.vault.util.getTotpDataOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Default implementation of [AuthenticatorBridgeProcessor].
 */
class AuthenticatorBridgeProcessorImpl(
    private val authenticatorBridgeRepository: AuthenticatorBridgeRepository,
    private val addTotpItemFromAuthenticatorManager: AddTotpItemFromAuthenticatorManager,
    private val featureFlagManager: FeatureFlagManager,
    dispatcherManager: DispatcherManager,
    context: Context,
) : AuthenticatorBridgeProcessor {

    private val applicationContext = context.applicationContext
    private val callbacks by lazy { RemoteCallbackList<IAuthenticatorBridgeServiceCallback>() }
    private val scope by lazy { CoroutineScope(dispatcherManager.default) }

    override val binder: IAuthenticatorBridgeService.Stub?
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
    private val defaultBinder = object : IAuthenticatorBridgeService.Stub() {

        override fun getVersionNumber(): String = AUTHENTICATOR_BRIDGE_SDK_VERSION

        override fun checkSymmetricEncryptionKeyFingerprint(
            symmetricKeyFingerprint: SymmetricEncryptionKeyFingerprintData?,
        ): Boolean {
            if (symmetricKeyFingerprint == null) return false
            val localSymmetricKeyFingerprint =
                authenticatorBridgeRepository.authenticatorSyncSymmetricKey
                    ?.toSymmetricEncryptionKeyData()
                    ?.toFingerprint()
                    ?.getOrNull()
            return symmetricKeyFingerprint == localSymmetricKeyFingerprint
        }

        override fun getSymmetricEncryptionKeyData(): SymmetricEncryptionKeyData? =
            authenticatorBridgeRepository
                .authenticatorSyncSymmetricKey
                ?.toSymmetricEncryptionKeyData()

        override fun registerBridgeServiceCallback(callback: IAuthenticatorBridgeServiceCallback?) {
            if (callback == null) return
            callbacks.register(callback)
        }

        override fun unregisterBridgeServiceCallback(
            callback: IAuthenticatorBridgeServiceCallback?,
        ) {
            if (callback == null) return
            callbacks.unregister(callback)
        }

        override fun syncAccounts() {
            val symmetricEncryptionKey = symmetricEncryptionKeyData ?: run {
                Timber.e(
                    t = IllegalStateException(),
                    message = "Unable to sync accounts when symmetricEncryptionKeyData is null.",
                )
                return
            }
            scope.launch {
                // Encrypt the shared account data with the symmetric key:
                val encryptedSharedAccountData = authenticatorBridgeRepository
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

        override fun startAddTotpLoginItemFlow(data: EncryptedAddTotpLoginItemData): Boolean {
            val symmetricEncryptionKey = symmetricEncryptionKeyData ?: run {
                Timber.e(
                    t = IllegalStateException(),
                    message = "Unable to start add TOTP item flow when " +
                        "symmetricEncryptionKeyData is null.",
                )
                return false
            }
            val intent = createAddTotpItemFromAuthenticatorIntent(context = applicationContext)
            val totpData = data.decrypt(symmetricEncryptionKey)
                .onFailure {
                    Timber.e(t = it, message = "Unable to decrypt TOTP data.")
                    return false
                }
                .getOrNull()
                ?.totpUri
                ?.toUri()
                ?.getTotpDataOrNull()
                ?: run {
                    Timber.e(
                        t = IllegalStateException(),
                        message = "Unable to parse TOTP URI.",
                    )
                    return false
                }
            addTotpItemFromAuthenticatorManager.pendingAddTotpLoginItemData = totpData
            applicationContext.startActivity(intent)
            return true
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
