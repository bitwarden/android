package com.bitwarden.ui.platform.components.coachmark.scope

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.bitwarden.ui.platform.components.coachmark.model.CoachMarkHighlightShape
import com.bitwarden.ui.platform.components.coachmark.model.CoachMarkState
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.tooltip.BitwardenToolTip
import com.bitwarden.ui.platform.components.tooltip.model.BitwardenToolTipState
import com.bitwarden.ui.platform.components.tooltip.model.rememberBitwardenToolTipState
import com.bitwarden.ui.util.Text
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.annotations.VisibleForTesting

/**
 * Creates an instance of [CoachMarkScope] for a given [CoachMarkState].
 */
@OptIn(ExperimentalMaterial3Api::class)
internal class CoachMarkScopeInstance<T : Enum<T>>(
    private val coachMarkState: CoachMarkState<T>,
) : CoachMarkScope<T> {

    @Composable
    override fun CoachMarkHighlight(
        key: T,
        title: String,
        description: String,
        modifier: Modifier,
        shape: CoachMarkHighlightShape,
        onDismiss: (() -> Unit)?,
        leftAction: @Composable() (RowScope.() -> Unit)?,
        rightAction: @Composable() (RowScope.() -> Unit)?,
        anchorContent: @Composable () -> Unit,
    ) {
        val toolTipState = rememberBitwardenToolTipState(
            initialIsVisible = false,
            isPersistent = true,
        )
        CoachMarkHighlightInternal(
            key = key,
            title = title,
            description = description,
            shape = shape,
            onDismiss = onDismiss,
            leftAction = leftAction,
            rightAction = rightAction,
            toolTipState = toolTipState,
            modifier = modifier.onGloballyPositioned {
                coachMarkState.updateHighlight(
                    key = key,
                    bounds = it.boundsInRoot(),
                    toolTipState = toolTipState,
                    shape = shape,
                )
            },
            anchorContent = anchorContent,
        )
    }

    override fun LazyListScope.coachMarkHighlightItem(
        key: T,
        title: Text,
        description: Text,
        modifier: Modifier,
        shape: CoachMarkHighlightShape,
        onDismiss: (() -> Unit)?,
        leftAction: @Composable() (RowScope.() -> Unit)?,
        rightAction: @Composable() (RowScope.() -> Unit)?,
        anchorContent: @Composable () -> Unit,
    ) {
        item(key = key) {
            this@CoachMarkScopeInstance.CoachMarkHighlight(
                key = key,
                title = title(),
                description = description(),
                modifier = modifier,
                shape = shape,
                onDismiss = onDismiss,
                leftAction = leftAction,
                rightAction = rightAction,
                anchorContent = anchorContent,
            )
        }
    }

    override fun <R> LazyListScope.coachMarkHighlightItems(
        key: T,
        title: Text,
        description: Text,
        modifier: Modifier,
        shape: CoachMarkHighlightShape,
        items: List<R>,
        onDismiss: (() -> Unit)?,
        leftAction: @Composable (RowScope.() -> Unit)?,
        rightAction: @Composable (RowScope.() -> Unit)?,
        leadingStaticContent: @Composable (BoxScope.() -> Unit)?,
        leadingContentIsTopCard: Boolean,
        trailingStaticContent: @Composable (BoxScope.() -> Unit)?,
        trailingContentIsBottomCard: Boolean,
        itemContent: @Composable (item: R, cardStyle: CardStyle) -> Unit,
    ) {
        val hasLeadingContent = (leadingStaticContent != null)
        var topCardAlreadyExists = hasLeadingContent && leadingContentIsTopCard
        val bottomCardAlreadyExists = (trailingStaticContent != null) && trailingContentIsBottomCard
        val itemsAdjusted = items
            .drop(if (hasLeadingContent) 0 else 1)
            .toImmutableList()
        item(key = key) {
            this@CoachMarkScopeInstance.CoachMarkHighlightInternal(
                key = key,
                title = title(),
                description = description(),
                shape = shape,
                onDismiss = onDismiss,
                leftAction = leftAction,
                rightAction = rightAction,
            ) {
                Box(
                    modifier = modifier.calculateBoundsAndAddForKey(key = key, isFirstItem = true),
                ) {
                    leadingStaticContent?.invoke(this) ?: run {
                        if (items.isNotEmpty()) {
                            itemContent(
                                items.first(),
                                items.toCoachMarkListItemCardStyle(
                                    index = 0,
                                    topCardAlreadyExists = false,
                                    bottomCardAlreadyExists = bottomCardAlreadyExists,
                                ),
                            )
                            topCardAlreadyExists = true
                        }
                    }
                }
            }
        }
        itemsIndexed(
            itemsAdjusted,
        ) { index, item ->
            Box(
                modifier = modifier.calculateBoundsAndAddForKey(key),
            ) {
                val cardStyle = itemsAdjusted.toCoachMarkListItemCardStyle(
                    index = index,
                    topCardAlreadyExists = topCardAlreadyExists,
                    bottomCardAlreadyExists = bottomCardAlreadyExists,
                )
                itemContent(item, cardStyle)
            }
        }

        trailingStaticContent?.let {
            item {
                Box(
                    modifier = modifier.calculateBoundsAndAddForKey(key),
                    content = it,
                )
            }
        }
    }

    @Composable
    private fun CoachMarkHighlightInternal(
        key: T,
        title: String,
        description: String,
        shape: CoachMarkHighlightShape,
        onDismiss: (() -> Unit)?,
        leftAction: @Composable() (RowScope.() -> Unit)?,
        rightAction: @Composable() (RowScope.() -> Unit)?,
        modifier: Modifier = Modifier,
        toolTipState: BitwardenToolTipState = rememberBitwardenToolTipState(
            initialIsVisible = false,
            isPersistent = true,
        ),
        anchorContent: @Composable () -> Unit,
    ) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                positioning = TooltipAnchorPosition.Above,
                spacingBetweenTooltipAndAnchor = 10.dp,
            ),
            tooltip = {
                BitwardenToolTip(
                    title = title,
                    description = description,
                    onDismiss = {
                        coachMarkState.coachingComplete()
                        onDismiss?.invoke()
                    },
                    leftAction = leftAction,
                    rightAction = rightAction,
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .semantics { isCoachMarkToolTip = true },
                )
            },
            enableUserInput = false,
            focusable = false,
            state = toolTipState,
            modifier = modifier,
            content = anchorContent,
        )

        LaunchedEffect(Unit) {
            coachMarkState.updateHighlight(
                key = key,
                bounds = null,
                toolTipState = toolTipState,
                shape = shape,
            )
        }
    }

    @Composable
    private fun Modifier.calculateBoundsAndAddForKey(
        key: T,
        isFirstItem: Boolean = false,
    ): Modifier {
        var bounds: Rect? by remember {
            mutableStateOf(null)
        }
        LaunchedEffect(bounds) {
            bounds?.let {
                coachMarkState.addToExistingBounds(
                    key = key,
                    isFirstItem = isFirstItem,
                    additionalBounds = it,
                )
            }
        }
        return this.onGloballyPositioned {
            bounds = it.boundsInRoot()
        }
    }
}

/**
 * Returns the appropriate [CardStyle] based on the current [index] in the list being used
 * for a coachMarkHighlightItems list.
 */
private fun <T> Collection<T>.toCoachMarkListItemCardStyle(
    index: Int,
    topCardAlreadyExists: Boolean,
    bottomCardAlreadyExists: Boolean,
    hasDivider: Boolean = true,
    dividerPadding: Dp = 16.dp,
): CardStyle = when {
    topCardAlreadyExists && bottomCardAlreadyExists -> {
        CardStyle.Middle(hasDivider = hasDivider, dividerPadding = dividerPadding)
    }

    topCardAlreadyExists && !bottomCardAlreadyExists -> {
        if (this.size == 1) {
            CardStyle.Bottom
        } else if (index == this.size - 1) {
            CardStyle.Bottom
        } else {
            CardStyle.Middle(hasDivider = hasDivider, dividerPadding = dividerPadding)
        }
    }

    !topCardAlreadyExists && bottomCardAlreadyExists -> {
        if (this.size == 1) {
            CardStyle.Top(hasDivider = hasDivider, dividerPadding = dividerPadding)
        } else if (index == 0) {
            CardStyle.Top(hasDivider = hasDivider, dividerPadding = dividerPadding)
        } else {
            CardStyle.Middle(hasDivider = hasDivider, dividerPadding = dividerPadding)
        }
    }

    else -> this.toListItemCardStyle(
        index = index,
        hasDivider = hasDivider,
        dividerPadding = dividerPadding,
    )
}

/**
 * SemanticPropertyKey used for Unit tests where checking if any displayed CoachMarkToolTips
 */
@VisibleForTesting
val IsCoachMarkToolTipKey = SemanticsPropertyKey<Boolean>("IsCoachMarkToolTip")
private var SemanticsPropertyReceiver.isCoachMarkToolTip by IsCoachMarkToolTipKey
