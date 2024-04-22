package com.x8bit.bitwarden.data.auth.datasource.network.util

import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TwoFactorAuthMethod
import com.x8bit.bitwarden.data.platform.datasource.network.util.base64UrlDecodeOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
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

/**
 * If it exists, return the identifier for the relying party used with Web AuthN two-factor
 * authentication.
 */
val GetTokenResponseJson.TwoFactorRequired?.webAuthRpId: String?
    get() = this
        ?.authMethodsData
        ?.get(TwoFactorAuthMethod.WEB_AUTH)
        ?.get("rpId")
        ?.jsonPrimitive
        ?.contentOrNull

/**
 * If it exists, return the type of user verification needed to complete the Web AuthN two-factor
 * authentication.
 */
val GetTokenResponseJson.TwoFactorRequired?.webAuthUserVerification: String?
    get() = this
        ?.authMethodsData
        ?.get(TwoFactorAuthMethod.WEB_AUTH)
        ?.get("userVerification")
        ?.jsonPrimitive
        ?.contentOrNull

/**
 * If it exists, return the challenge that the authenticator need to solve to complete the
 * Web AuthN two-factor authentication.
 */
val GetTokenResponseJson.TwoFactorRequired?.webAuthChallenge: String?
    get() = this
        ?.authMethodsData
        ?.get(TwoFactorAuthMethod.WEB_AUTH)
        ?.get("challenge")
        ?.jsonPrimitive
        ?.contentOrNull

/**
 * If it exists, return the credentials allowed to be used to solve the challenge to complete the
 * Web AuthN two-factor authentication.
 */
val GetTokenResponseJson.TwoFactorRequired?.webAuthAllowCredentials: List<String>?
    get() = this
        ?.authMethodsData
        ?.get(TwoFactorAuthMethod.WEB_AUTH)
        ?.get("allowCredentials")
        ?.jsonArray
        ?.mapNotNull {
            it.jsonObject["id"]?.jsonPrimitive?.contentOrNull?.base64UrlDecodeOrNull()
        }
