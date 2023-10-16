package com.x8bit.bitwarden.data.auth.datasource.sdk.model

/**
 * An estimate of password strength.
 *
 * Adapted from [zxcvbn](https://github.com/dropbox/zxcvbn#usage).
 */
enum class PasswordStrength {
    /**
     * Too guessable; very risky.
     */
    LEVEL_0,

    /**
     * Very guessable; limited protection.
     */
    LEVEL_1,

    /**
     * Somewhat guessable; some protection.
     */
    LEVEL_2,

    /**
     * Safely unguessable; moderate protection.
     */
    LEVEL_3,

    /**
     * Very unguessable; strong protection.
     */
    LEVEL_4,
}
