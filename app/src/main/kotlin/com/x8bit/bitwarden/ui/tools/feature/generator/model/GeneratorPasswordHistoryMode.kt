package com.x8bit.bitwarden.ui.tools.feature.generator.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Represents the different modes the password history screen can be in.
 */
@Serializable
sealed class GeneratorPasswordHistoryMode : Parcelable {

    /**
     * Represents the main or default password history mode.
     */
    @Parcelize
    @Serializable
    data object Default : GeneratorPasswordHistoryMode()

    /**
     * Represents the item password history mode.
     */
    @Parcelize
    @Serializable
    data class Item(val itemId: String) : GeneratorPasswordHistoryMode()
}
