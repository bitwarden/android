package com.x8bit.bitwarden.ui.platform.feature.search.model

/**
 * Possible options available during the autofill process on the Search screen.
 */
enum class AutofillSelectionOption {
    /**
     * The item should be selected for autofill.
     */
    AUTOFILL,

    /**
     * The item should be selected for autofill and updated to be linked to the given URI.
     */
    AUTOFILL_AND_SAVE,

    /**
     * The item should be viewed.
     */
    VIEW,
}
