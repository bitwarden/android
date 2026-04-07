package com.bitwarden.authenticator.data.platform.manager.lock.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents the timeout period for the app.
 */
@Suppress("MagicNumber")
sealed class AppTimeout : Parcelable {
    /**
     * The type of timeout.
     */
    abstract val type: Type

    /**
     * The time (in minutes) that the vault can stay unlocked before it will automatically lock
     * itself.
     */
    abstract val timeoutInMinutes: Int

    /**
     * The app should be considered timed-out immediately.
     */
    @Parcelize
    data object Immediately : AppTimeout() {
        override val type: Type get() = Type.IMMEDIATELY
        override val timeoutInMinutes: Int get() = 0
    }

    /**
     * The app should time out after one minute.
     */
    @Parcelize
    data object OneMinute : AppTimeout() {
        override val type: Type get() = Type.ONE_MINUTE
        override val timeoutInMinutes: Int get() = 1
    }

    /**
     * The app should time out after five minutes.
     */
    @Parcelize
    data object FiveMinutes : AppTimeout() {
        override val type: Type get() = Type.FIVE_MINUTES
        override val timeoutInMinutes: Int get() = 5
    }

    /**
     * The app should time out after fifteen minutes.
     */
    @Parcelize
    data object FifteenMinutes : AppTimeout() {
        override val type: Type get() = Type.FIFTEEN_MINUTES
        override val timeoutInMinutes: Int get() = 15
    }

    /**
     * The app should time out after thirty minutes.
     */
    @Parcelize
    data object ThirtyMinutes : AppTimeout() {
        override val type: Type get() = Type.THIRTY_MINUTES
        override val timeoutInMinutes: Int get() = 30
    }

    /**
     * The app should time out after one hour.
     */
    @Parcelize
    data object OneHour : AppTimeout() {
        override val type: Type get() = Type.ONE_HOUR
        override val timeoutInMinutes: Int get() = 60
    }

    /**
     * The app should time out after four hours.
     */
    @Parcelize
    data object FourHours : AppTimeout() {
        override val type: Type get() = Type.FOUR_HOURS
        override val timeoutInMinutes: Int get() = 240
    }

    /**
     * The app should time out after an app restart.
     */
    @Parcelize
    data object OnAppRestart : AppTimeout() {
        override val type: Type get() = Type.ON_APP_RESTART
        override val timeoutInMinutes: Int get() = -1
    }

    /**
     * The app should never automatically timeout.
     */
    @Parcelize
    data object Never : AppTimeout() {
        override val type: Type get() = Type.NEVER
        override val timeoutInMinutes: Int get() = -2
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
    }
}
