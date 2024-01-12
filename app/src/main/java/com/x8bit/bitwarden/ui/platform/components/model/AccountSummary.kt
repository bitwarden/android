package com.x8bit.bitwarden.ui.platform.components.model

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import com.x8bit.bitwarden.ui.platform.base.util.hexToColor
import kotlinx.parcelize.Parcelize

/**
 * Summary information about a user's account.
 *
 * @property userId The ID of the user.
 * @property name The full name of the user (if applicable).
 * @property email The email of the user.
 * @property avatarColorHex Hex color value for a user's avatar in the "#AARRGGBB" format.
 * @property environmentLabel Label for the environment associated with the user's account
 * (ex: "bitwarden.com"). This is purely for display purposes.
 * @property isActive Whether or not the account is currently the active one.
 * @property isVaultUnlocked Whether or not the account's vault is currently unlocked.
 */
@Parcelize
data class AccountSummary(
    val userId: String,
    val name: String?,
    val email: String,
    val avatarColorHex: String,
    val environmentLabel: String,
    val isActive: Boolean,
    val isLoggedIn: Boolean,
    val isVaultUnlocked: Boolean,
) : Parcelable {

    /**
     * The [avatarColorHex] represented as a [Color].
     */
    val avatarColor: Color
        get() = avatarColorHex.hexToColor()

    /**
     *  The current status of the user's account locally.
     */
    val status: Status
        get() = when {
            isActive -> Status.ACTIVE
            !isLoggedIn -> Status.LOGGED_OUT
            isVaultUnlocked -> Status.UNLOCKED
            else -> Status.LOCKED
        }

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
         * The account is currently logged out.
         */
        LOGGED_OUT,

        /**
         * The account is currently unlocked.
         */
        UNLOCKED,
    }
}
