package com.bitwarden.authenticator.data.authenticator.repository.model

/**
 * Models result of the user adding a totp code.
 */
sealed class TotpCodeResult {

    /**
     * Code containing an OTP URI has been successfully scanned.
     */
    data class TotpCodeScan(val code: String) : TotpCodeResult()

    /**
     * Code containing exported data from Google Authenticator was scanned.
     */
    data class GoogleExportScan(val data: String) : TotpCodeResult()

    /**
     * There was an error scanning the code.
     */
    data object CodeScanningError : TotpCodeResult()
}
