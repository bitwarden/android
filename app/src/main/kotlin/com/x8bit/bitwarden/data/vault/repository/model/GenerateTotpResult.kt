package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Models the result of generating a totp code.
 */
sealed class GenerateTotpResult {

    /**
     * The code was generated successfully.
     */
    data class Success(
        val code: String,
        val periodSeconds: Int,
    ) : GenerateTotpResult()

    /**
     * An error occurred while generating the code.
     */
    data class Error(val error: Throwable) : GenerateTotpResult()
}
