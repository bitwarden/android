package com.x8bit.bitwarden.data.platform.repository.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents the timeout period for a user's vault.
 */
@Suppress("MagicNumber")
sealed class VaultTimeout : Parcelable {
    /**
     * The type of timeout.
     */
    abstract val type: Type

    /**
     * The time (in minutes) that the vault can stay unlocked before it will automatically lock
     * itself (or `null` to allow for vault that never locks).
     */
    abstract val vaultTimeoutInMinutes: Int?

    /**
     * The vault should be considered timed-out immediately.
     */
    @Parcelize
    data object Immediately : VaultTimeout() {
        override val type: Type get() = Type.IMMEDIATELY
        override val vaultTimeoutInMinutes: Int get() = 0
    }

    /**
     * The vault should time out after one minute.
     */
    @Parcelize
    data object OneMinute : VaultTimeout() {
        override val type: Type get() = Type.ONE_MINUTE
        override val vaultTimeoutInMinutes: Int get() = 1
    }

    /**
     * The vault should time out after five minutes.
     */
    @Parcelize
    data object FiveMinutes : VaultTimeout() {
        override val type: Type get() = Type.FIVE_MINUTES
        override val vaultTimeoutInMinutes: Int get() = 5
    }

    /**
     * The vault should time out after fifteen minutes.
     */
    @Parcelize
    data object FifteenMinutes : VaultTimeout() {
        override val type: Type get() = Type.FIFTEEN_MINUTES
        override val vaultTimeoutInMinutes: Int get() = 15
    }

    /**
     * The vault should time out after thirty minutes.
     */
    @Parcelize
    data object ThirtyMinutes : VaultTimeout() {
        override val type: Type get() = Type.THIRTY_MINUTES
        override val vaultTimeoutInMinutes: Int get() = 30
    }

    /**
     * The vault should time out after one hour.
     */
    @Parcelize
    data object OneHour : VaultTimeout() {
        override val type: Type get() = Type.ONE_HOUR
        override val vaultTimeoutInMinutes: Int get() = 60
    }

    /**
     * The vault should time out after four hours.
     */
    @Parcelize
    data object FourHours : VaultTimeout() {
        override val type: Type get() = Type.FOUR_HOURS
        override val vaultTimeoutInMinutes: Int get() = 240
    }

    /**
     * The vault should time out after an app restart.
     */
    @Parcelize
    data object OnAppRestart : VaultTimeout() {
        override val type: Type get() = Type.ON_APP_RESTART
        override val vaultTimeoutInMinutes: Int get() = -1
    }

    /**
     * The vault should never automatically timeout.
     */
    @Parcelize
    data object Never : VaultTimeout() {
        override val type: Type get() = Type.NEVER
        override val vaultTimeoutInMinutes: Int? get() = null
    }

    /**
     * The timeout period is a custom value given by the dynamic [vaultTimeoutInMinutes].
     */
    @Parcelize
    data class Custom(
        override val vaultTimeoutInMinutes: Int,
    ) : VaultTimeout() {
        override val type: Type get() = Type.CUSTOM
    }

    /**
     * The specific type of timeout.
     */
    enum class Type {
        IMMEDIATELY,
        ONE_MINUTE,
        FIVE_MINUTES,
        FIFTEEN_MINUTES,
        THIRTY_MINUTES,
        ONE_HOUR,
        FOUR_HOURS,
        ON_APP_RESTART,
        NEVER,
        CUSTOM,
    }
}
