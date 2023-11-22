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
 * @property status The current status of the user's account locally.
 */
@Parcelize
data class AccountSummary(
    val userId: String,
    val name: String?,
    val email: String,
    val avatarColorHex: String,
    val status: Status,
) : Parcelable {

    /**
     * The [avatarColorHex] represented as a [Color].
     */
    val avatarColor: Color
        get() = avatarColorHex.hexToColor()

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
