package com.x8bit.bitwarden.ui.tools.feature.generator.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * A sealed class representing the mode in which the generator displays.
 */
sealed class GeneratorMode : Parcelable {
    /**
     * Represents the main or default generator mode.
     */
    @Parcelize
    data object Default : GeneratorMode()

    /**
     * A sealed class representing the types of modals in which the generator displays.
     */
    @Parcelize
    sealed class Modal : GeneratorMode() {

        /**
         * Represents the mode for generating passwords.
         */
        @Parcelize
        data object Password : Modal()

        /**
         * Represents the mode for generating usernames.
         *
         * @property website The website corresponding to this username generation, or empty.
         */
        @Parcelize
        data class Username(
            val website: String?,
        ) : Modal()
    }
}
