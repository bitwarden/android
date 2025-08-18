package com.bitwarden.authenticatorbridge.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Models data that will be sent to calling application via IPC.
 *
 * @param initializationVector Cryptographic initialization vector.
 * @param encryptedAccountsJson Encrypted JSON blob containing shared account data. See
 * [SharedAccountDataJson] For the serializable model contained in the blob.
 */
@Parcelize
data class EncryptedSharedAccountData(
    val initializationVector: ByteArrayContainer,
    val encryptedAccountsJson: ByteArrayContainer,
) : Parcelable
