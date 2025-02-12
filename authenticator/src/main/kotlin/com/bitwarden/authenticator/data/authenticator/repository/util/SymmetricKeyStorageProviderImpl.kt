package com.bitwarden.authenticator.data.authenticator.repository.util

import com.bitwarden.authenticator.data.auth.datasource.disk.AuthDiskSource
import com.bitwarden.authenticatorbridge.model.SymmetricEncryptionKeyData
import com.bitwarden.authenticatorbridge.provider.SymmetricKeyStorageProvider
import com.bitwarden.authenticatorbridge.util.toSymmetricEncryptionKeyData

/**
 * Implementation of [SymmetricKeyStorageProvider] that stores symmetric key data in encrypted
 * shared preferences.
 */
class SymmetricKeyStorageProviderImpl(
    private val authDiskSource: AuthDiskSource,
) : SymmetricKeyStorageProvider {

    override var symmetricKey: SymmetricEncryptionKeyData?
        get() = authDiskSource.authenticatorBridgeSymmetricSyncKey?.toSymmetricEncryptionKeyData()
        set(value) {
            authDiskSource.authenticatorBridgeSymmetricSyncKey =
                value?.symmetricEncryptionKey?.byteArray
        }
}
