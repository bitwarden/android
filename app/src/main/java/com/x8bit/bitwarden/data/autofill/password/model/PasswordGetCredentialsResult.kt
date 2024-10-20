package com.x8bit.bitwarden.data.autofill.password.model

import androidx.credentials.provider.BeginGetPasswordOption
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher

/**
 * Represents the result of a Password Get Credentials request.
 */
sealed class PasswordGetCredentialsResult {
    /**
     * Indicates credentials were successfully queried.
     */
    data class Success(
        val userId: String,
        val option: BeginGetPasswordOption,
        val credentials: List<AutofillCipher.Login>,
    ) : PasswordGetCredentialsResult()

    /**
     * Indicates an error was encountered when querying for matching credentials.
     */
    data object Error : PasswordGetCredentialsResult()
}
