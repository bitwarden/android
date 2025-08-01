package com.bitwarden.ui.platform.components.icon.model

import android.os.Parcelable
import androidx.annotation.DrawableRes
import com.bitwarden.ui.util.Text
import kotlinx.parcelize.Parcelize

/**
 * A class to denote the type of icon being passed.
 */
@Parcelize
sealed class IconData : Parcelable {

    /**
     * The icon content description.
     */
    abstract val contentDescription: Text?

    /**
     * The icon test tag.
     */
    abstract val testTag: String?

    /**
     * Data class representing the resources required for an icon.
     *
     * @property iconRes the resource for the local icon.
     */
    @Parcelize
    data class Local(
        @field:DrawableRes val iconRes: Int,
        override val contentDescription: Text? = null,
        override val testTag: String? = null,
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
        @field:DrawableRes val fallbackIconRes: Int,
        override val contentDescription: Text? = null,
        override val testTag: String? = null,
    ) : IconData()
}
