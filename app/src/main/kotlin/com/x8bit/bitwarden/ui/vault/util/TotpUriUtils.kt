package com.x8bit.bitwarden.ui.vault.util

import android.net.Uri
import com.x8bit.bitwarden.ui.vault.model.TotpData

private const val TOTP_HOST_NAME: String = "totp"
private const val TOTP_SCHEME_NAME: String = "otpauth"
private const val PARAM_NAME_ALGORITHM: String = "algorithm"
private const val PARAM_NAME_DIGITS: String = "digits"
private const val PARAM_NAME_ISSUER: String = "issuer"
private const val PARAM_NAME_PERIOD: String = "period"
private const val PARAM_NAME_SECRET: String = "secret"

/**
 * Checks if the given [Uri] contains valid data for a TOTP. The [TotpData] will be returned when
 * the correct data is present or `null` if data is invalid or missing.
 */
fun Uri.getTotpDataOrNull(): TotpData? {
    // Must be a "otpauth" scheme
    if (!this.scheme.equals(other = TOTP_SCHEME_NAME, ignoreCase = true)) return null
    // Must be a "totp" host
    if (!this.host.equals(other = TOTP_HOST_NAME, ignoreCase = true)) return null
    // Must contain a "secret"
    val secret = this.getQueryParameter(PARAM_NAME_SECRET)?.trim() ?: return null
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
        digits = this.getQueryParameter(PARAM_NAME_DIGITS)?.trim()?.toIntOrNull() ?: 6,
        period = this
            .getQueryParameter(PARAM_NAME_PERIOD)
            ?.trim()
            ?.toIntOrNull()
            ?.takeUnless { it <= 0 }
            ?: 30,
        algorithm = TotpData.CryptoHashAlgorithm
            .parse(value = this.getQueryParameter(PARAM_NAME_ALGORITHM)?.trim())
            ?: TotpData.CryptoHashAlgorithm.SHA_1,
    )
}
