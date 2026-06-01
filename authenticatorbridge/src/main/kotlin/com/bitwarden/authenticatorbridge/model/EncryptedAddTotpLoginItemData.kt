package com.bitwarden.authenticatorbridge.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Models an encrypted totp item to be added to the Bitwarden app.
 *
 * @param initializationVector Cryptographic initialization vector.
 * @param encryptedTotpUriJson Encrypted JSON string containing TOTP URI info. See
 * [AddTotpLoginItemDataJson] for the json structure of the string.
 */
@Parcelize
data class EncryptedAddTotpLoginItemData(
    val initializationVector: ByteArrayContainer,
    val encryptedTotpUriJson: ByteArrayContainer,
) : Parcelable
