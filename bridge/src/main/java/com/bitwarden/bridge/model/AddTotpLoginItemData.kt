package com.bitwarden.bridge.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Domain level model for a TOTP item to be added to the Bitwarden app.
 *
 * @param totpUri A TOTP code URI to be added to the Bitwarden app.
 */
data class AddTotpLoginItemData(
    val totpUri: String,
)
