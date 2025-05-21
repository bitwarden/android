package com.bitwarden.ui.platform.components.model

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Defines the possible display options for a card background on a composable component.
 */
sealed class CardStyle {
    /**
     * Indicates if a divider should be displayed in this card.
     */
    abstract val hasDivider: Boolean

    /**
     * Provides start padding to the divider when present.
     */
    abstract val dividerPadding: Dp

    /**
     * Defines a top card that has top rounded corners.
     */
    data class Top(
        override val hasDivider: Boolean = true,
        override val dividerPadding: Dp = 16.dp,
    ) : CardStyle()

    /**
     * Defines a middle card that has no rounded corners.
     */
    data class Middle(
        override val hasDivider: Boolean = true,
        override val dividerPadding: Dp = 16.dp,
    ) : CardStyle()

    /**
     * Defines a bottom card that has bottom rounded corners.
     */
    data object Bottom : CardStyle() {
        override val hasDivider: Boolean = false
        override val dividerPadding: Dp = 0.dp
    }

    /**
     * Defines a full card that has rounded corners.
     */
    data object Full : CardStyle() {
        override val hasDivider: Boolean = false
        override val dividerPadding: Dp = 0.dp
    }
}
