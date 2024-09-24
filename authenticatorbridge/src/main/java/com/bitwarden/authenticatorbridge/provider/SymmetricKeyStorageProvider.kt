package com.bitwarden.authenticatorbridge.provider

import com.bitwarden.authenticatorbridge.model.SymmetricEncryptionKeyData

/**
 * TODO: document me
 */
interface SymmetricKeyStorageProvider {

    var symmetricKey: SymmetricEncryptionKeyData?
}
