package com.bitwarden.ui.platform.util

import android.net.Uri
import androidx.core.net.toUri
import com.bitwarden.ui.platform.base.util.isBase32
import com.bitwarden.ui.platform.model.TotpData

private const val TOTP_HOST_NAME: String = "totp"
private const val TOTP_SCHEME_NAME: String = "otpauth"
private const val PARAM_NAME_ALGORITHM: String = "algorithm"
private const val PARAM_NAME_DIGITS: String = "digits"
private const val PARAM_NAME_ISSUER: String = "issuer"
private const val PARAM_NAME_PERIOD: String = "period"
private const val PARAM_NAME_SECRET: String = "secret"

/**
 * Checks if the given [String] contains valid data for a TOTP. The [TotpData] will be returned
 * when the correct data is present or `null` if data is invalid or missing.
 */
fun String.getTotpDataOrNull(): TotpData? = this.toUri().getTotpDataOrNull()

/**
 * Checks if the given [Uri] contains valid data for a TOTP. The [TotpData] will be returned when
 * the correct data is present or `null` if data is invalid or missing.
 */
fun Uri.getTotpDataOrNull(): TotpData? {
    // Must be a "otpauth" scheme
    if (!this.scheme.equals(other = TOTP_SCHEME_NAME, ignoreCase = true)) return null
    // Must be a "totp" host
    if (!this.host.equals(other = TOTP_HOST_NAME, ignoreCase = true)) return null
    val secret = this.getSecret() ?: return null
    val digits = this.getDigits() ?: return null
    val period = this.getPeriod() ?: return null
    val algorithm = this.getAlgorithm() ?: return null
    val segments = this.pathSegments?.firstOrNull()?.split(":")
    val segmentCount = segments?.size ?: 0
    return TotpData(
        uri = this.toString(),
        issuer = this.getQueryParameter(PARAM_NAME_ISSUER)
            ?: segments?.firstOrNull()?.trim()?.takeIf { segmentCount > 1 },
        accountName = if (segmentCount > 1) {
            segments?.getOrNull(index = 1)?.trim()
        } else {
            segments?.firstOrNull()?.trim()
        },
        secret = secret,
        digits = digits,
        period = period,
        algorithm = algorithm,
    )
}

/**
 * Attempts to extract the algorithm from the given totp [Uri].
 */
private fun Uri.getAlgorithm(): TotpData.CryptoHashAlgorithm? {
    val algorithm = this
        .getQueryParameter(PARAM_NAME_ALGORITHM)
        ?.trim()
        ?.lowercase()
    return if (algorithm == null) {
        // If no value was provided, then we'll default to SHA_1.
        TotpData.CryptoHashAlgorithm.SHA_1
    } else {
        // If the value is unidentifiable, then it's invalid.
        // If it's identifiable, then we return the valid value.
        // We specifically do not use a `let` here, since we do not want to map an unidentified
        // value to the default value.
        TotpData.CryptoHashAlgorithm.parse(value = algorithm)
    }
}

/**
 * Attempts to extract the digits from the given totp [Uri].
 */
@Suppress("MagicNumber")
private fun Uri.getDigits(): Int? {
    val digits = this.getQueryParameter(PARAM_NAME_DIGITS)?.trim()?.toIntOrNull()
    return if (digits == null) {
        // If no value was provided, then we'll default to 6.
        6
    } else if (digits < 1 || digits > 10) {
        // If the value is less than 1 or greater than 10, then it's invalid.
        null
    } else {
        // If the value is valid, then we'll return it.
        digits
    }
}

/**
 * Attempts to extract the period from the given totp [Uri].
 */
@Suppress("MagicNumber")
private fun Uri.getPeriod(): Int? {
    val period = this.getQueryParameter(PARAM_NAME_PERIOD)?.trim()?.toIntOrNull()
    return if (period == null) {
        // If no value was provided, then we'll default to 30.
        30
    } else if (period < 1) {
        // If the value is less than 1, then it's invalid.
        null
    } else {
        // If the value is valid, then we'll return it.
        period
    }
}

/**
 * Attempts to extract the secret from the given totp [Uri].
 */
private fun Uri.getSecret(): String? =
    this
        .getQueryParameter(PARAM_NAME_SECRET)
        ?.trim()
        ?.takeIf { it.isNotEmpty() && it.isBase32() }
