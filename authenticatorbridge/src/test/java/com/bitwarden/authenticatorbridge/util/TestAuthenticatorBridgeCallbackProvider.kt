package com.bitwarden.authenticatorbridge.util

import com.bitwarden.authenticatorbridge.IAuthenticatorBridgeServiceCallback
import com.bitwarden.authenticatorbridge.model.EncryptedSharedAccountData
import com.bitwarden.authenticatorbridge.provider.AuthenticatorBridgeCallbackProvider

/**
 * Test implementation of [AuthenticatorBridgeCallbackProvider] that provides a testable
 * [IAuthenticatorBridgeServiceCallback.Default] implementation.
 */
class TestAuthenticatorBridgeCallbackProvider : AuthenticatorBridgeCallbackProvider {

    override fun getCallback(
        onAccountsSync: (EncryptedSharedAccountData) -> Unit,
    ): IAuthenticatorBridgeServiceCallback = object : IAuthenticatorBridgeServiceCallback.Default() {

        override fun onAccountsSync(data: EncryptedSharedAccountData) = onAccountsSync.invoke(data)
    }
}
