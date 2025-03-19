package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedapps.list.model

import android.os.Parcelable
import androidx.annotation.StringRes
import com.x8bit.bitwarden.R
import kotlinx.parcelize.Parcelize

/**
 * Represents a single item in the list of trusted privileged apps.
 *
 * @param packageName The package name of the privileged app.
 * @param signature The signature of the privileged app.
 */
@Parcelize
data class PrivilegedAppListItem(
    val packageName: String,
    val signature: String,
    val trustAuthority: PrivilegedAppTrustAuthority,
    val appName: String? = null,
) : Parcelable {

    val canRevokeTrust: Boolean
        get() = trustAuthority == PrivilegedAppTrustAuthority.USER

    /**
     * Represents the trust authority of a privileged app.
     */
    enum class PrivilegedAppTrustAuthority(
        @StringRes val displayName: Int,
    ) {
        /**
         * The app is trusted by Google.
         */
        GOOGLE(displayName = R.string.google),

        /**
         * The app is trusted by the Bitwarden community.
         */
        COMMUNITY(displayName = R.string.the_community),

        /**
         * The app is trusted by the user.
         */
        USER(displayName = R.string.you),
    }
}
