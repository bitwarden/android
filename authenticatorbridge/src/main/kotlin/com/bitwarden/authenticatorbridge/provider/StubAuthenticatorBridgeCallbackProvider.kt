package com.bitwarden.authenticatorbridge.provider

import com.bitwarden.authenticatorbridge.IAuthenticatorBridgeServiceCallback
import com.bitwarden.authenticatorbridge.model.EncryptedSharedAccountData

/**
 * Default implementation of [AuthenticatorBridgeCallbackProvider] that provides a live
 * [IAuthenticatorBridgeServiceCallback.Stub] implementation.
 */
class StubAuthenticatorBridgeCallbackProvider : AuthenticatorBridgeCallbackProvider {

    override fun getCallback(
        onAccountsSync: (EncryptedSharedAccountData) -> Unit,
    ): IAuthenticatorBridgeServiceCallback = object : IAuthenticatorBridgeServiceCallback.Stub() {

        override fun onAccountsSync(data: EncryptedSharedAccountData) = onAccountsSync.invoke(data)
    }
}
