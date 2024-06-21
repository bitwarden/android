package com.x8bit.bitwarden.data.auth.util

import android.content.Intent
import com.x8bit.bitwarden.data.platform.manager.model.CompleteRegistrationData

/**
 * Checks if the given [Intent] contains data to complete registration.
 * The [CompleteRegistrationData] will be returned when present.
 */
fun Intent.getCompleteRegistrationDataIntentOrNull(): CompleteRegistrationData?  {
    val uri = data ?: return null
    val email = uri?.getQueryParameter("email") ?: return null
    val verificationToken = uri.getQueryParameter("verificationtoken") ?: return null
    return CompleteRegistrationData(
        email = email,
        verificationToken = verificationToken
    )
}

