package com.x8bit.bitwarden.ui.vault.feature.itemlisting.util

import androidx.annotation.StringRes
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.data.credentials.model.PasswordRegisterResult

/**
 * Returns the string resource ID corresponding to the error message for the given
 * [PasswordRegisterResult.Error].
 */
val PasswordRegisterResult.Error.messageResourceId: Int
    @StringRes
    get() = when (this) {
        PasswordRegisterResult.Error.InternalError ->
            BitwardenString.password_registration_failed_due_to_an_internal_error
    }
