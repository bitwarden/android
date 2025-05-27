package com.bitwarden.authenticatorbridge.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Wrapper for a symmetric encryption fingerprint (hash) that can be used
 * to verify if callers have the correct symmetric key for encrypting/decrypting IPC data.
 *
 * @param symmetricEncryptionKeyFingerprint The fingerprint of the symmetric encryption key.
 */
@Parcelize
data class SymmetricEncryptionKeyFingerprintData(
    val symmetricEncryptionKeyFingerprint: ByteArrayContainer,
) : Parcelable
