package com.x8bit.bitwarden.ui.platform.util

import android.content.Intent

/**
 * Returns `true` if the [Intent] is a deeplink to the vault, `false` otherwise.
 */
val Intent.isMyVaultShortcut: Boolean
    get() = dataString?.equals("bitwarden://my_vault") == true

/**
 * Returns `true` if the [Intent] is a deeplink to the password generator, `false` otherwise.
 */
val Intent.isPasswordGeneratorShortcut: Boolean
    get() = dataString?.equals("bitwarden://password_generator") == true

/**
 * Returns `true` if the [Intent] is a deeplink to the account security screen, `false` otherwise.
 */
val Intent.isAccountSecurityShortcut: Boolean
    get() = dataString?.equals("bitwarden://settings/account_security") == true

/**
 * Returns `true` if the [Intent] is a deep link callback from a Premium
 * checkout session, `false` otherwise.
 */
val Intent.isPremiumCheckoutCallback: Boolean
    get() = dataString?.equals("bitwarden://premium-upgrade-callback") == true
