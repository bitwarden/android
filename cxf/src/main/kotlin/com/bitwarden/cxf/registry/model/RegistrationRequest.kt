package com.bitwarden.cxf.registry.model

import androidx.annotation.DrawableRes

/**
 * Represents a request to register as a credential provider that allows exporting credentials.
 *
 * @property appName The name of the application as it will be displayed to the user.
 * @property credentialTypes The types of credentials that can be exported.
 * @property iconResId Resource ID of a 36x36 pixel drawable to be displayed alongside the [appName]
 * when credential import is requested.
 */
data class RegistrationRequest(
    val appName: String,
    val credentialTypes: Set<String>,
    @field:DrawableRes
    val iconResId: Int,
)
