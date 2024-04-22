package com.x8bit.bitwarden.data.auth.datasource.network.model

import androidx.annotation.Keep
import com.x8bit.bitwarden.data.platform.datasource.network.serializer.BaseEnumeratedIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents different providers that can be used for two-factor login.
 */
@Serializable(TwoFactorAuthMethodSerializer::class)
enum class TwoFactorAuthMethod(val value: UInt) {
    @SerialName("0")
    AUTHENTICATOR_APP(value = 0U),

    @SerialName("1")
    EMAIL(value = 1U),

    @SerialName("2")
    DUO(value = 2U),

    @SerialName("3")
    YUBI_KEY(value = 3U),

    @SerialName("4")
    U2F(value = 4U),

    @SerialName("5")
    REMEMBER(value = 5U),

    @SerialName("6")
    DUO_ORGANIZATION(value = 6U),

    @SerialName("7")
    WEB_AUTH(value = 7U),

    @SerialName("-1")
    RECOVERY_CODE(value = 100U),
}

@Keep
private class TwoFactorAuthMethodSerializer :
    BaseEnumeratedIntSerializer<TwoFactorAuthMethod>(TwoFactorAuthMethod.entries.toTypedArray())
