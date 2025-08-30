package com.bitwarden.network.util

import com.bitwarden.network.model.TwoFactorAuthMethod

/**
 * The priority, used to determine the default method from a list of available methods.
 * (Higher value = preference to use the method if it's available)
 */
@Suppress("MagicNumber")
internal val TwoFactorAuthMethod.priority: Int
    get() = when (this) {
        TwoFactorAuthMethod.AUTHENTICATOR_APP -> 1
        TwoFactorAuthMethod.EMAIL -> 0
        TwoFactorAuthMethod.DUO -> 2
        TwoFactorAuthMethod.YUBI_KEY -> 3
        TwoFactorAuthMethod.DUO_ORGANIZATION -> 20
        TwoFactorAuthMethod.WEB_AUTH -> 4
        else -> -1
    }
