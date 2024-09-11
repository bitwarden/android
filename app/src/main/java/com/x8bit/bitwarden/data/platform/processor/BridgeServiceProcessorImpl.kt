package com.x8bit.bitwarden.data.platform.processor

import android.content.Intent
import android.os.Build
import com.bitwarden.bridge.IBridgeService
import com.bitwarden.bridge.IBridgeServiceCallback
import com.bitwarden.bridge.model.EncryptedAddTotpLoginItemData
import com.bitwarden.bridge.model.SymmetricEncryptionKeyData
import com.bitwarden.bridge.model.SymmetricEncryptionKeyFingerprintData
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.util.isBuildVersionBelow

/**
 * Default implementation of [BridgeServiceProcessor].
 */
class BridgeServiceProcessorImpl(
    private val featureFlagManager: FeatureFlagManager,
) : BridgeServiceProcessor {

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
        override fun getVersionNumber(): String {
            // TODO: BITAU-104
            return ""
        }

        override fun checkSymmetricEncryptionKeyFingerprint(
            data: SymmetricEncryptionKeyFingerprintData?,
        ): Boolean {
            // TODO: BITAU-104
            return false
        }

        override fun getSymmetricEncryptionKeyData(): SymmetricEncryptionKeyData? {
            // TODO: BITAU-104
            return null
        }

        override fun registerBridgeServiceCallback(callback: IBridgeServiceCallback?) {
            // TODO: BITAU-104
        }

        override fun unregisterBridgeServiceCallback(callback: IBridgeServiceCallback?) {
            // TODO: BITAU-104
        }

        override fun syncAccounts() {
            // TODO: BITAU-104
        }

        override fun createAddTotpLoginItemIntent(): Intent {
            // TODO: BITAU-104
            return Intent()
        }

        override fun setPendingAddTotpLoginItemData(data: EncryptedAddTotpLoginItemData?) {
            // TODO: BITAU-104
        }
    }
}
