package com.x8bit.bitwarden.ui.platform.components.coachmark

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipScope
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import org.jetbrains.annotations.VisibleForTesting

/**
 * Creates an instance of [CoachMarkScope] for a given [CoachMarkState].
 */
@OptIn(ExperimentalMaterial3Api::class)
class CoachMarkScopeInstance<T : Enum<T>>(
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
        val toolTipState = rememberTooltipState(
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

    override fun LazyListScope.coachMarkHighlight(
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
        val topCardAlreadyExists = hasLeadingContent && leadingContentIsTopCard
        val bottomCardAlreadyExists = (trailingStaticContent != null) && trailingContentIsBottomCard
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
                    leadingStaticContent?.let { it() } ?: run {
                        if (items.isNotEmpty()) {
                            itemContent(
                                items[0],
                                items.toCoachMarkListItemCardStyle(
                                    index = 0,
                                    topCardAlreadyExists = false,
                                    bottomCardAlreadyExists = bottomCardAlreadyExists,
                                ),
                            )
                        }
                    }
                }
            }
        }
        itemsIndexed(items) { index, item ->
            // if there is no leading content we already added the first item.
            if (!hasLeadingContent && index == 0) return@itemsIndexed
            Box(
                modifier = modifier.calculateBoundsAndAddForKey(key),
            ) {
                val cardStyle = items.toCoachMarkListItemCardStyle(
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
        toolTipState: TooltipState = rememberTooltipState(
            initialIsVisible = false,
            isPersistent = true,
        ),
        anchorContent: @Composable () -> Unit,
    ) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(
                spacingBetweenTooltipAndAnchor = 12.dp,
            ),
            tooltip = {
                CoachMarkToolTip(
                    title = title,
                    description = description,
                    onDismiss = {
                        coachMarkState.coachingComplete()
                        onDismiss?.invoke()
                    },
                    leftAction = leftAction,
                    rightAction = rightAction,
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

    private fun Modifier.calculateBoundsAndAddForKey(
        key: T,
        isFirstItem: Boolean = false,
    ): Modifier = composed {
        var bounds by remember {
            mutableStateOf(Rect.Zero)
        }
        LaunchedEffect(bounds) {
            if (bounds != Rect.Zero) {
                coachMarkState.addToExistingBounds(
                    key = key,
                    isFirstItem = isFirstItem,
                    additionalBounds = bounds,
                )
            }
        }
        this.onGloballyPositioned {
            bounds = it.boundsInRoot()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TooltipScope.CoachMarkToolTip(
    title: String,
    description: String,
    onDismiss: (() -> Unit),
    leftAction: (@Composable RowScope.() -> Unit)?,
    rightAction: (@Composable RowScope.() -> Unit)?,
) {
    RichTooltip(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .semantics { isCoachMarkToolTip = true },
        caretSize = DpSize(width = 24.dp, height = 16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = title,
                    style = BitwardenTheme.typography.eyebrowMedium,
                    color = BitwardenTheme.colorScheme.text.secondary,
                )
                Spacer(modifier = Modifier.weight(1f))
                BitwardenStandardIconButton(
                    painter = rememberVectorPainter(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.close),
                    onClick = onDismiss,
                    modifier = Modifier.size(16.dp),
                )
            }
        },
        action = {
            Row(
                Modifier.fillMaxWidth(),
            ) {
                leftAction?.invoke(this)
                Spacer(modifier = Modifier.weight(1f))
                rightAction?.invoke(this)
            }
        },
        colors = TooltipDefaults.richTooltipColors(
            containerColor = BitwardenTheme.colorScheme.background.secondary,
        ),
    ) {
        Text(
            text = description,
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
        )
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
