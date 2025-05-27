package com.x8bit.bitwarden.ui.vault.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents the data for TOTP deeplink.
 *
 * @property uri The raw uri as a string.
 * @property issuer The issuer parameter is a string value indicating the provider or service this
 * account is associated with, URL-encoded according to RFC 3986.
 * @property accountName The users email address.
 * @property secret The secret parameter is an arbitrary key value encoded in Base32 according to
 * RFC 3548. The padding specified in RFC 3548 section 2.2 is not required and should be omitted.
 * @property digits The digits parameter may have the values 6 or 8, and determines how long of a
 * one-time passcode to display to the user.
 * @property period The period parameter defines a period that a TOTP code will be valid for, in
 * seconds.
 * @property algorithm The algorithm may have the values.
 */
@Parcelize
data class TotpData(
    val uri: String,
    val issuer: String?,
    val accountName: String?,
    val secret: String,
    val digits: Int,
    val period: Int,
    val algorithm: CryptoHashAlgorithm,
) : Parcelable {
    /**
     * A representation of the various cryptographic hash algorithms used by TOTP.
     */
    enum class CryptoHashAlgorithm(val value: String) {
        SHA_1(value = "sha1"),
        SHA_256(value = "sha256"),
        SHA_512(value = "sha512"),
        MD_5(value = "md5"),
        ;

        @Suppress("UndocumentedPublicClass")
        companion object {
            /**
             * Attempts to convert the string [value] to a valid [CryptoHashAlgorithm] or null if
             * a match could not be found.
             */
            fun parse(
                value: String?,
            ): CryptoHashAlgorithm? =
                CryptoHashAlgorithm
                    .entries
                    .firstOrNull { it.value.equals(other = value, ignoreCase = true) }
        }
    }
}
