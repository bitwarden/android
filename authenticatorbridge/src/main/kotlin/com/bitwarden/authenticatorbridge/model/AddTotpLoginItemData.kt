package com.bitwarden.authenticatorbridge.model

/**
 * Domain level model for a TOTP item to be added to the Bitwarden app.
 *
 * @param totpUri A TOTP code URI to be added to the Bitwarden app.
 */
data class AddTotpLoginItemData(
    val totpUri: String,
)
