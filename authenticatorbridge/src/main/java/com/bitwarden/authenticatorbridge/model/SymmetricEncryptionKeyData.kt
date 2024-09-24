package com.bitwarden.authenticatorbridge.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Wrapper for a symmetric encryption key.
 *
 * @param symmetricEncryptionKey The symmetric encryption key.
 */
@Parcelize
data class SymmetricEncryptionKeyData(
    val symmetricEncryptionKey: ByteArrayContainer,
) : Parcelable
