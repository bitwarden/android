package com.x8bit.bitwarden.data.platform.processor

import android.content.Intent
import com.bitwarden.bridge.IBridgeService
import com.bitwarden.bridge.IBridgeServiceCallback
import com.bitwarden.bridge.model.EncryptedAddTotpLoginItemData
import com.bitwarden.bridge.model.SymmetricEncryptionKeyData
import com.bitwarden.bridge.model.SymmetricEncryptionKeyFingerprintData
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey

/**
 * Default implementation of [BridgeServiceProcessor].
 */
class BridgeServiceProcessorImpl(
    private val featureFlagManager: FeatureFlagManager,
) : BridgeServiceProcessor {

    override val binder: IBridgeService.Stub?
        // TODO: Check for Android API level as well: BITAU-102
        get() {
            return if (featureFlagManager.getFeatureFlag(FlagKey.AuthenticatorSync)) {
                defaultBinder
            } else {
                // If the feature flag is not enabled, return a null binder which will no-op all
                // service calls.
                null
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
