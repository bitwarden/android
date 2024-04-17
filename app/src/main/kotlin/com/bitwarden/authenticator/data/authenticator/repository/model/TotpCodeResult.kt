package com.bitwarden.authenticator.data.authenticator.repository.model

/**
 * Models result of the user adding a totp code.
 */
sealed class TotpCodeResult {

    /**
     * Code has been successfully added.
     */
    data class Success(val code: String) : TotpCodeResult()

    /**
     * There was an error scanning the code.
     */
    data object CodeScanningError : TotpCodeResult()
}
