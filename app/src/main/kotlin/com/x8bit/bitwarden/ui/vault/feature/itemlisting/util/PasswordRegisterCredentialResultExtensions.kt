package com.x8bit.bitwarden.ui.vault.feature.itemlisting.util

import androidx.annotation.StringRes
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.credentials.model.PasswordRegisterCredentialResult

/**
 * Returns the string resource ID corresponding to the error message for the given
 * [PasswordRegisterCredentialResult.Error].
 */
val PasswordRegisterCredentialResult.Error.messageResourceId: Int
    @StringRes
    get() = when (this) {
        PasswordRegisterCredentialResult.Error.InternalError ->
            R.string.password_registration_failed_due_to_an_internal_error
    }
