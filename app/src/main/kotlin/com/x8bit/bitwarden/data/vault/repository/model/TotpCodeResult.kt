package com.x8bit.bitwarden.data.vault.repository.model

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
    data class CodeScanningError(val error: Throwable? = null) : TotpCodeResult()
}
