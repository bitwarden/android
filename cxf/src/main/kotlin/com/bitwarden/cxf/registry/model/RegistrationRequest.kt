package com.bitwarden.cxf.registry.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * Represents a request to register as a credential provider that allows exporting credentials.
 *
 * @property appNameRes The name of the application as it will be displayed to the user.
 * @property credentialTypes The types of credentials that can be exported.
 * @property iconResId Resource ID of a 36x36 pixel drawable to be displayed alongside the
 * [appNameRes] when credential import is requested.
 */
data class RegistrationRequest(
    @field:StringRes
    val appNameRes: Int,
    @field:DrawableRes
    val iconResId: Int,
    val credentialTypes: Set<String>,
)
