package com.bitwarden.cxf.registry.model

import android.graphics.Bitmap

/**
 * Represents a request to register as a credential provider that allows exporting credentials.
 *
 * @property appName The name of the application as it will be displayed to the user.
 * @property credentialTypes The types of credentials that can be exported.
 */
data class RegistrationRequest(
    val appName: String,
    val credentialTypes: Set<String>,
    val bitmap: Bitmap,
)
