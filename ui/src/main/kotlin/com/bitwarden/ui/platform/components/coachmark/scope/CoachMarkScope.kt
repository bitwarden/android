package com.bitwarden.ui.platform.components.coachmark.scope

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bitwarden.ui.platform.components.coachmark.model.CoachMarkHighlightShape
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.util.Text

/**
 * Defines the scope for creating coach mark highlights within a user interface.
 *
 * This interface provides a way to define and display a highlight that guides the user's
 * attention to a specific part of the UI, often accompanied by a tooltip with
 * explanatory text and actions.
 *
 * @param T The type of the enum used to represent the unique keys for each coach mark highlight.
 */
interface CoachMarkScope<T : Enum<T>> {

    /**
     * Creates a highlight for a specific coach mark.
     *
     * This function defines a region of the UI to be highlighted, along with an
     * associated tooltip that can display a title, description, and actions.
     *
     * @param key The unique key identifying this highlight. This key is used to
     * manage the state and order of the coach mark sequence.
     * @param title The title of the coach mark, displayed in the tooltip.
     * @param description The description of the coach mark, providing more context
     * to the user. Displayed in the tooltip.
     * @param shape The shape of the highlight.
     * @param onDismiss An optional callback that is invoked when the coach mark is dismissed
     * (e.g., by clicking the close button). If provided, this function
     * will be executed after the coach mark is dismissed. If not provided,
     * no action is taken on dismissal.
     * @param leftAction An optional composable to be displayed on the left side of the
     * action row in the tooltip. This can be used to provide
     * additional actions or controls.
     * @param rightAction An optional composable to be displayed on the right side of the
     * action row in the tooltip. This can be used to provide
     * primary actions or navigation.
     * @param anchorContent The composable content to be highlighted. This is the UI element
     * that will be visually emphasized by the coach mark.
     */
    @Composable
    fun CoachMarkHighlight(
        key: T,
        title: String,
        description: String,
        modifier: Modifier = Modifier,
        shape: CoachMarkHighlightShape,
        onDismiss: (() -> Unit)?,
        leftAction: (@Composable RowScope.() -> Unit)?,
        rightAction: (@Composable RowScope.() -> Unit)?,
        anchorContent: @Composable () -> Unit,
    )

    /**
     * Creates a [CoachMarkScope.CoachMarkHighlight] in the context of a [LazyListScope],
     * automatically assigns the value of [key] as the [LazyListScope.item]'s `key` value.
     * This is used to be able to find the item to apply the coach mark to in the LazyList.
     * Analogous with [LazyListScope.item] in the context of adding a coach mark around an entire
     * item.
     *
     * @param key The key used for the CoachMark data as well as the `item.key` to find within
     * the `LazyList`.
     *
     * @see [CoachMarkScope.CoachMarkHighlight]
     *
     * Note: If you are only intending "highlight" part of an `item` you will want to give that
     * item the same `key` as the [key] for the coach mark.
     */
    @Suppress("LongParameterList")
    fun LazyListScope.coachMarkHighlightItem(
        key: T,
        title: Text,
        description: Text,
        modifier: Modifier = Modifier,
        shape: CoachMarkHighlightShape = CoachMarkHighlightShape.RoundedRectangle(),
        onDismiss: (() -> Unit)? = null,
        leftAction: (@Composable RowScope.() -> Unit)? = null,
        rightAction: (@Composable RowScope.() -> Unit)? = null,
        anchorContent: @Composable () -> Unit,
    )

    /**
     * Allows for wrapping an entire list of [items] in a single Coach Mark Highlight. The
     * anchor for the tooltip and the scrolling target will be the start/top of the content.
     *
     * @param items Typed list of items to display in the [LazyListScope.items] block.
     * @param leadingStaticContent Optional static content to slot in a [LazyListScope.item]
     * ahead of the list of items.
     * @param leadingContentIsTopCard To denote that the leading content is the "top" part of a
     * card creating using [CardStyle].
     * @param trailingStaticContent Optional static content to slot in a [LazyListScope.item]
     * after the list of items.
     * @param trailingContentIsBottomCard To denote that the trailing content is the "top" part of
     * a card creating using [CardStyle].
     * @param itemContent The content to draw for each [R] in [items] and the necessary
     * [CardStyle] based on its position and other factors.
     *
     * @see [CoachMarkScope.CoachMarkHighlight]
     */
    @Suppress("LongParameterList")
    fun <R> LazyListScope.coachMarkHighlightItems(
        key: T,
        title: Text,
        description: Text,
        modifier: Modifier = Modifier,
        shape: CoachMarkHighlightShape = CoachMarkHighlightShape.RoundedRectangle(),
        items: List<R>,
        onDismiss: (() -> Unit)? = null,
        leftAction: (@Composable RowScope.() -> Unit)? = null,
        rightAction: (@Composable RowScope.() -> Unit)? = null,
        leadingStaticContent: (@Composable BoxScope.() -> Unit)? = null,
        leadingContentIsTopCard: Boolean = false,
        trailingStaticContent: (@Composable BoxScope.() -> Unit)? = null,
        trailingContentIsBottomCard: Boolean = false,
        itemContent: @Composable (R, CardStyle) -> Unit,
    )
}
