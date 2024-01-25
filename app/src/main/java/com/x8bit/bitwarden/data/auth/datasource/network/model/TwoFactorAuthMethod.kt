package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents different providers that can be used for two-factor login.
 */
@Serializable
@Suppress("MagicNumber")
enum class TwoFactorAuthMethod {
    @SerialName("0")
    AUTHENTICATOR_APP,

    @SerialName("1")
    EMAIL,

    @SerialName("2")
    DUO,

    @SerialName("3")
    YUBI_KEY,

    @SerialName("4")
    U2F,

    @SerialName("5")
    REMEMBER,

    @SerialName("6")
    DUO_ORGANIZATION,

    @SerialName("7")
    FIDO_2_WEB_APP,

    @SerialName("-1")
    RECOVERY_CODE,
}
