package com.bitwarden.authenticatorbridge.factory

import android.content.Context
import com.bitwarden.authenticatorbridge.manager.AuthenticatorBridgeManager
import com.bitwarden.authenticatorbridge.manager.AuthenticatorBridgeManagerImpl
import com.bitwarden.authenticatorbridge.manager.model.AuthenticatorBridgeConnectionType
import com.bitwarden.authenticatorbridge.provider.SymmetricKeyStorageProvider

/**
 * Factory for supplying implementation instances of Authenticator Bridge SDK interfaces.
 */
class AuthenticatorBridgeFactory(
    context: Context,
) {

    private val applicationContext = context.applicationContext

    /**
     * Gets a new instance of [AuthenticatorBridgeManager].
     *
     * @param connectionType Specifies which build variant to connect to.
     * @param symmetricKeyStorageProvider Provides access to local storage of the symmetric
     * encryption key.
     */
    fun getAuthenticatorBridgeManager(
        connectionType: AuthenticatorBridgeConnectionType,
        symmetricKeyStorageProvider: SymmetricKeyStorageProvider,
    ): AuthenticatorBridgeManager = AuthenticatorBridgeManagerImpl(
        context = applicationContext,
        connectionType = connectionType,
        symmetricKeyStorageProvider = symmetricKeyStorageProvider,
    )
}
