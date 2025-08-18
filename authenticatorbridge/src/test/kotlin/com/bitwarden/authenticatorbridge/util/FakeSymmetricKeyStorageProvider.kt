package com.bitwarden.authenticatorbridge.util

import com.bitwarden.authenticatorbridge.model.SymmetricEncryptionKeyData
import com.bitwarden.authenticatorbridge.provider.SymmetricKeyStorageProvider

/**
 * A fake implementation of [SymmetricKeyStorageProvider] for testing purposes.
 */
class FakeSymmetricKeyStorageProvider : SymmetricKeyStorageProvider {
    override var symmetricKey: SymmetricEncryptionKeyData? = null
}
