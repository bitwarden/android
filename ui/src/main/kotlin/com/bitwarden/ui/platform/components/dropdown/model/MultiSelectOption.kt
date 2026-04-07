package com.bitwarden.ui.platform.components.dropdown.model

/**
 * Represents an option in a multi-select list, which can either be a header or a selectable row.
 */
sealed class MultiSelectOption {
    /**
     * The text to display for the option.
     */
    abstract val title: String

    /**
     * Represents a header item in a multi-select list. Headers are used to visually group related
     * options within the list.
     */
    data class Header(
        override val title: String,
        val testTag: String? = null,
    ) : MultiSelectOption()

    /**
     * Represents a selectable row item in a multi-select list.
     */
    data class Row(
        override val title: String,
        val testTag: String? = null,
    ) : MultiSelectOption()
}
