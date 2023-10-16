package com.x8bit.bitwarden.data.auth.datasource.sdk.util

import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength

/**
 * Converts the given [Int] to a [PasswordStrength]. A `null` value is returned if this value is
 * not in the [0, 4] range.
 */
@Suppress("MagicNumber")
fun Int.toPasswordStrengthOrNull(): PasswordStrength? =
    when (this) {
        0 -> PasswordStrength.LEVEL_0
        1 -> PasswordStrength.LEVEL_1
        2 -> PasswordStrength.LEVEL_2
        3 -> PasswordStrength.LEVEL_3
        4 -> PasswordStrength.LEVEL_4
        else -> null
    }

/**
 * Converts the given [UByte] to a [PasswordStrength]. A `null` value is returned if this value is
 * not in the [0, 4] range.
 */
fun UByte.toPasswordStrengthOrNull(): PasswordStrength? =
    this.toInt().toPasswordStrengthOrNull()

/**
 * Converts the given [UInt] to a [PasswordStrength]. A `null` value is returned if this value is
 * not in the [0, 4] range.
 */
fun UInt.toPasswordStrengthOrNull(): PasswordStrength? =
    this.toInt().toPasswordStrengthOrNull()

/**
 * Converts the given [PasswordStrength] to an [Int].
 */
@Suppress("MagicNumber")
fun PasswordStrength.toInt(): Int =
    when (this) {
        PasswordStrength.LEVEL_0 -> 0
        PasswordStrength.LEVEL_1 -> 1
        PasswordStrength.LEVEL_2 -> 2
        PasswordStrength.LEVEL_3 -> 3
        PasswordStrength.LEVEL_4 -> 4
    }

/**
 * Converts the given [PasswordStrength] to a [UByte].
 */
@Suppress("MagicNumber")
fun PasswordStrength.toUByte(): UByte =
    this.toInt().toUByte()

/**
 * Converts the given [PasswordStrength] to a [UInt].
 */
@Suppress("MagicNumber")
fun PasswordStrength.toUInt(): UInt =
    this.toInt().toUInt()
