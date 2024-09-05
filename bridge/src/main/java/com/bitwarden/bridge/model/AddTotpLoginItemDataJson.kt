package com.bitwarden.bridge.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Serializable model for a TOTP item to be added to the Bitwarden app. For domain level
 * model, see [AddTotpLoginItemData].
 *
 * @param totpUri A TOTP code URI to be added to the Bitwarden app.
 */
@Serializable
data class AddTotpLoginItemDataJson(
    @SerialName("totpUri")
    val totpUri: String,
)