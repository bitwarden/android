package com.x8bit.bitwarden.data.auth.repository.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Summary information about a user's account.
 *
 * @property userId The ID of the user.
 * @property name The full name of the user.
 * @property email The email of the user.
 * @property avatarColorHex Hex color value for a user's avatar in the "#AARRGGBB" format.
 * @property status The current status of the user's account locally.
 */
@Parcelize
data class AccountSummary(
    val userId: String,
    val name: String,
    val email: String,
    val avatarColorHex: String,
    val status: Status,
) : Parcelable {

    /**
     * Describes the status of the given account.
     */
    enum class Status {
        /**
         * The account is currently the active one.
         */
        ACTIVE,

        /**
         * The account is currently locked.
         */
        LOCKED,

        /**
         * The account is currently unlocked.
         */
        UNLOCKED,
    }
}
