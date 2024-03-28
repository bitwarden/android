package com.x8bit.bitwarden.authenticator.ui.platform.components.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * A class to denote the type of icon being passed.
 */
sealed class IconData : Parcelable {

    /**
     * Data class representing the resources required for an icon.
     *
     * @property iconRes the resource for the local icon.
     */
    @Parcelize
    data class Local(
        val iconRes: Int,
    ) : IconData()

    /**
     * Data class representing the resources required for a network-based icon.
     *
     * @property uri the link for the icon.
     * @property fallbackIconRes fallback resource if the image cannot be loaded.
     */
    @Parcelize
    data class Network(
        val uri: String,
        val fallbackIconRes: Int,
    ) : IconData()
}
