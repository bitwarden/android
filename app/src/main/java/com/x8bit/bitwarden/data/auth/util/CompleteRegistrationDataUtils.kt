package com.x8bit.bitwarden.data.auth.util

import android.content.Intent
import com.x8bit.bitwarden.data.platform.manager.model.CompleteRegistrationData
import com.x8bit.bitwarden.data.platform.repository.model.Environment

/**
 * Checks if the given [Intent] contains data to complete registration.
 * The [CompleteRegistrationData] will be returned when present.
 */
fun Intent.getCompleteRegistrationDataIntentOrNull(): CompleteRegistrationData?  {
    val uri = data ?: return null
    val host = uri.host ?: return null
    val email = uri?.getQueryParameter("email") ?: return null
    val verificationToken = uri.getQueryParameter("verificationtoken") ?: return null
    if (!host.contains("bitwarden.eu") && !host.contains("bitwarden.com")) return null

    val region = if (host.contains("bitwarden.eu")) Environment.Type.EU else Environment.Type.US
    return CompleteRegistrationData(
        email = email,
        verificationToken = verificationToken,
        region = region
    )
}

