package com.x8bit.bitwarden.data.auth.datasource.network.util

import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TwoFactorAuthMethod
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Return the list of two-factor auth methods available to the user.
 */
val GetTokenResponseJson.TwoFactorRequired?.availableAuthMethods: List<TwoFactorAuthMethod>
    get() = (
        this
            ?.authMethodsData
            ?.keys
            ?.toList()
            ?: listOf(TwoFactorAuthMethod.EMAIL)
        )
        .plus(TwoFactorAuthMethod.RECOVERY_CODE)

/**
 * The preferred two-factor auth method to be used as a default on the two-factor login screen.
 */
val GetTokenResponseJson.TwoFactorRequired?.preferredAuthMethod: TwoFactorAuthMethod
    get() = this
        ?.authMethodsData
        ?.keys
        ?.maxByOrNull { it.priority }
        ?: TwoFactorAuthMethod.EMAIL

/**
 * If it exists, return the value of the Duo auth url.
 */
val GetTokenResponseJson.TwoFactorRequired?.twoFactorDuoAuthUrl: String?
    get() = this
        ?.authMethodsData
        ?.duo
        ?.get("AuthUrl")
        ?.jsonPrimitive
        ?.contentOrNull

/**
 * If it exists, return the value to display for the email used with two-factor authentication.
 */
val GetTokenResponseJson.TwoFactorRequired?.twoFactorDisplayEmail: String
    get() = this
        ?.authMethodsData
        ?.get(TwoFactorAuthMethod.EMAIL)
        ?.get("Email")
        ?.jsonPrimitive
        ?.contentOrNull
        .orEmpty()

/**
 * Gets the [TwoFactorAuthMethod.DUO] [JsonObject], if it exists, else the
 * [TwoFactorAuthMethod.DUO_ORGANIZATION] [JsonObject], if that exists.
 */
private val Map<TwoFactorAuthMethod, JsonObject?>.duo: JsonObject?
    get() = get(TwoFactorAuthMethod.DUO) ?: get(TwoFactorAuthMethod.DUO_ORGANIZATION)
