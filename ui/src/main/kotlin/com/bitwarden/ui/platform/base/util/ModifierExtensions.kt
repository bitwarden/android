@file:Suppress("TooManyFunctions")

package com.bitwarden.ui.platform.base.util

import android.os.Build
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.CombinedModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.model.WindowSize
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.platform.util.getWindowSize

/**
 * Adds a performance-optimized background color specified by the given [topAppBarScrollBehavior]
 * and its current scroll state.
 */
@OmitFromCoverage
@OptIn(ExperimentalMaterial3Api::class)
@Stable
@Composable
fun Modifier.scrolledContainerBackground(
    topAppBarScrollBehavior: TopAppBarScrollBehavior,
): Modifier {
    val expandedColor = BitwardenTheme.colorScheme.background.secondary
    val collapsedColor = BitwardenTheme.colorScheme.background.secondary
    return CombinedModifier(
        outer = this,
        inner = drawBehind {
            drawRect(
                color = topAppBarScrollBehavior.toScrolledContainerColor(
                    expandedColor = expandedColor,
                    collapsedColor = collapsedColor,
                ),
            )
        },
    )
}

/**
 * Draws a very simple non-intractable scrollbar on the end side of the component.
 */
@OmitFromCoverage
@Composable
fun Modifier.simpleVerticalScrollbar(
    state: ScrollState,
    scrollbarWidth: Dp = 6.dp,
    color: Color = BitwardenTheme.colorScheme.stroke.divider,
    layoutDirection: LayoutDirection = LocalLayoutDirection.current,
): Modifier =
    this then Modifier.drawWithContent {
        drawContent()
        val viewHeight = state.viewportSize.toFloat()
        val contentHeight = state.maxValue + viewHeight
        val scrollbarHeight = (10.dp.toPx()..viewHeight)
            .takeUnless { it.isEmpty() }
            ?.let { (viewHeight * (viewHeight / contentHeight)).coerceIn(range = it) }
            ?: 0f
        val variableZone = viewHeight - scrollbarHeight
        val scrollbarYOffset = (state.value.toFloat() / state.maxValue) * variableZone
        val halfScrollbarWidthPx = scrollbarWidth.toPx() / 2
        drawRoundRect(
            cornerRadius = CornerRadius(x = halfScrollbarWidthPx, y = halfScrollbarWidthPx),
            color = color,
            topLeft = Offset(
                x = when (layoutDirection) {
                    LayoutDirection.Ltr -> this.size.width - scrollbarWidth.toPx()
                    LayoutDirection.Rtl -> 0f
                },
                y = scrollbarYOffset,
            ),
            size = Size(width = scrollbarWidth.toPx(), height = scrollbarHeight),
        )
    }

/**
 * Adds a bottom divider specified by the given [topAppBarScrollBehavior] and its current scroll
 * state.
 */
@OmitFromCoverage
@OptIn(ExperimentalMaterial3Api::class)
@Stable
@Composable
fun Modifier.scrolledContainerBottomDivider(
    topAppBarScrollBehavior: TopAppBarScrollBehavior,
    enabled: Boolean = true,
): Modifier =
    this.bottomDivider(
        alpha = topAppBarScrollBehavior.toScrolledContainerDividerAlpha(),
        enabled = enabled,
    )

/**
 * This is a [Modifier] extension for adding an optional click listener to a composable.
 */
@OmitFromCoverage
@Stable
@Composable
fun Modifier.nullableClickable(
    indicationColor: Color = BitwardenTheme.colorScheme.background.pressed,
    enabled: Boolean = true,
    onClick: (() -> Unit)?,
): Modifier =
    onClick
        ?.let {
            this.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = indicationColor),
                onClick = it,
                enabled = enabled,
            )
        }
        ?: this

/**
 * This is a [Modifier] extension for adding an optional test tag to the composable.
 */
@OmitFromCoverage
@Stable
@Composable
fun Modifier.nullableTestTag(
    tag: String?,
): Modifier = this.run { tag?.let { testTag(tag = it) } ?: this }

/**
 * This is a [Modifier] extension for drawing a divider at the top of the composable.
 */
@OmitFromCoverage
@Stable
@Composable
fun Modifier.topDivider(
    paddingStart: Dp = 0.dp,
    paddingEnd: Dp = 0.dp,
    thickness: Dp = (0.5).dp,
    color: Color = BitwardenTheme.colorScheme.stroke.divider,
    enabled: Boolean = true,
    alpha: Float = 1f,
): Modifier = drawWithCache {
    onDrawWithContent {
        drawContent()
        if (enabled) {
            val (startX, endX) = when (layoutDirection) {
                LayoutDirection.Ltr -> paddingStart.toPx() to (size.width - paddingEnd.toPx())
                LayoutDirection.Rtl -> (size.width - paddingEnd.toPx()) to paddingStart.toPx()
            }
            drawLine(
                alpha = alpha,
                color = color,
                strokeWidth = thickness.toPx(),
                start = Offset(x = startX, y = thickness.toPx() / 2),
                end = Offset(x = endX, y = thickness.toPx() / 2),
            )
        }
    }
}

/**
 * This is a [Modifier] extension for drawing a divider at the bottom of the composable.
 */
@OmitFromCoverage
@Stable
@Composable
fun Modifier.bottomDivider(
    paddingStart: Dp = 0.dp,
    paddingEnd: Dp = 0.dp,
    thickness: Dp = (0.5).dp,
    color: Color = BitwardenTheme.colorScheme.stroke.divider,
    enabled: Boolean = true,
    alpha: Float = 1f,
): Modifier = drawWithCache {
    onDrawWithContent {
        drawContent()
        if (enabled) {
            val (startX, endX) = when (layoutDirection) {
                LayoutDirection.Ltr -> paddingStart.toPx() to (size.width - paddingEnd.toPx())
                LayoutDirection.Rtl -> (size.width - paddingEnd.toPx()) to paddingStart.toPx()
            }
            drawLine(
                alpha = alpha,
                color = color,
                strokeWidth = thickness.toPx(),
                start = Offset(x = startX, y = size.height - thickness.toPx() / 2),
                end = Offset(x = endX, y = size.height - thickness.toPx() / 2),
            )
        }
    }
}

/**
 * This is a [Modifier] extension for drawing a divider at the end of the composable.
 */
@OmitFromCoverage
@Stable
@Composable
fun Modifier.endDivider(
    paddingTop: Dp = 0.dp,
    paddingBottom: Dp = 0.dp,
    thickness: Dp = (0.5).dp,
    color: Color = BitwardenTheme.colorScheme.stroke.divider,
    enabled: Boolean = true,
    alpha: Float = 1f,
): Modifier = drawWithCache {
    onDrawWithContent {
        drawContent()
        if (enabled) {
            val startX = when (layoutDirection) {
                LayoutDirection.Ltr -> size.width - thickness.toPx() / 2
                LayoutDirection.Rtl -> thickness.toPx() / 2
            }
            drawLine(
                alpha = alpha,
                color = color,
                strokeWidth = thickness.toPx(),
                start = Offset(x = startX, y = paddingTop.toPx()),
                end = Offset(x = startX, y = size.height - paddingBottom.toPx()),
            )
        }
    }
}

/**
 * This is a [Modifier] extension for mirroring the contents of a composable when the layout
 * direction is set to [LayoutDirection.Rtl]. Primarily used for directional icons, such as the
 * up button and chevrons.
 */
@OmitFromCoverage
@Stable
@Composable
fun Modifier.mirrorIfRtl(): Modifier =
    if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
        scale(scaleX = -1f, scaleY = 1f)
    } else {
        this
    }

/**
 * This is a [Modifier] extension for ensuring that the tab button navigates properly when using
 * a physical keyboard.
 */
@OmitFromCoverage
@Stable
@Composable
fun Modifier.tabNavigation(): Modifier {
    val focusManager = LocalFocusManager.current
    return onPreviewKeyEvent { keyEvent ->
        if (keyEvent.key == Key.Tab && keyEvent.type == KeyEventType.KeyDown) {
            focusManager.moveFocus(
                if (keyEvent.isShiftPressed) {
                    FocusDirection.Previous
                } else {
                    FocusDirection.Next
                },
            )
            true
        } else {
            false
        }
    }
}

/**
 * This is a [Modifier] extension for ensuring that the content uses the standard horizontal margin.
 */
@OmitFromCoverage
@Stable
@Composable
fun Modifier.standardHorizontalMargin(
    compact: Dp = 16.dp,
    medium: Dp = 48.dp,
): Modifier =
    standardHorizontalMargin(
        compact = compact,
        medium = medium,
        windowAdaptiveInfo = currentWindowAdaptiveInfo(),
    )

/**
 * This is a [Modifier] extension for ensuring that the content uses the standard horizontal margin.
 */
@OmitFromCoverage
@Stable
fun Modifier.standardHorizontalMargin(
    compact: Dp = 16.dp,
    medium: Dp = 48.dp,
    windowAdaptiveInfo: WindowAdaptiveInfo,
): Modifier =
    this then StandardHorizontalMarginElement(
        compact = compact,
        medium = medium,
        windowAdaptiveInfo = windowAdaptiveInfo,
    )

private data class StandardHorizontalMarginElement(
    private val compact: Dp,
    private val medium: Dp,
    private val windowAdaptiveInfo: WindowAdaptiveInfo,
) : ModifierNodeElement<StandardHorizontalMarginElement.StandardHorizontalMarginConsumerNode>() {
    override fun create(): StandardHorizontalMarginConsumerNode =
        StandardHorizontalMarginConsumerNode(
            compact = compact,
            medium = medium,
            windowAdaptiveInfo = windowAdaptiveInfo,
        )

    override fun update(node: StandardHorizontalMarginConsumerNode) {
        node.compact = compact
        node.medium = medium
    }

    class StandardHorizontalMarginConsumerNode(
        var compact: Dp,
        var medium: Dp,
        private val windowAdaptiveInfo: WindowAdaptiveInfo,
    ) : Modifier.Node(),
        LayoutModifierNode {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints,
        ): MeasureResult {
            val paddingPx = when (windowAdaptiveInfo.getWindowSize()) {
                WindowSize.Compact -> compact.roundToPx()
                WindowSize.Medium -> medium.roundToPx()
            }

            // Account for the padding on each side.
            val horizontalPx = paddingPx * 2
            // Measure the placeable within the horizontal space accounting for the padding Px.
            val placeable = measurable.measure(
                constraints = constraints.offset(
                    horizontal = -horizontalPx,
                    vertical = 0,
                ),
            )
            // The width of the placeable plus the total padding, used to create the layout.
            val width = constraints.constrainWidth(width = placeable.width + horizontalPx)
            return layout(width = width, height = placeable.height) {
                placeable.place(x = paddingPx, y = 0)
            }
        }
    }
}

/**
 * This is a [Modifier] extension that applies a card style to the content.
 */
@OmitFromCoverage
@Stable
@Composable
fun Modifier.cardStyle(
    cardStyle: CardStyle?,
    onClick: (() -> Unit)? = null,
    clickEnabled: Boolean = true,
    paddingHorizontal: Dp = 0.dp,
    paddingVertical: Dp = 12.dp,
    containerColor: Color = BitwardenTheme.colorScheme.background.secondary,
    indicationColor: Color = BitwardenTheme.colorScheme.background.pressed,
): Modifier =
    this.cardStyle(
        cardStyle = cardStyle,
        onClick = onClick,
        clickEnabled = clickEnabled,
        padding = PaddingValues(
            horizontal = paddingHorizontal,
            vertical = paddingVertical,
        ),
        containerColor = containerColor,
        indicationColor = indicationColor,
    )

/**
 * This is a [Modifier] extension that applies a card style to the content.
 */
@OmitFromCoverage
@Stable
@Composable
fun Modifier.cardStyle(
    cardStyle: CardStyle?,
    onClick: (() -> Unit)? = null,
    clickEnabled: Boolean = true,
    paddingStart: Dp = 0.dp,
    paddingTop: Dp = 12.dp,
    paddingEnd: Dp = 0.dp,
    paddingBottom: Dp = 12.dp,
    containerColor: Color = BitwardenTheme.colorScheme.background.secondary,
    indicationColor: Color = BitwardenTheme.colorScheme.background.pressed,
): Modifier =
    this.cardStyle(
        cardStyle = cardStyle,
        onClick = onClick,
        clickEnabled = clickEnabled,
        padding = PaddingValues(
            start = paddingStart,
            top = paddingTop,
            end = paddingEnd,
            bottom = paddingBottom,
        ),
        containerColor = containerColor,
        indicationColor = indicationColor,
    )

/**
 * This is a [Modifier] extension that applies a card style to the content.
 */
@OmitFromCoverage
@Stable
@Composable
fun Modifier.cardStyle(
    cardStyle: CardStyle?,
    onClick: (() -> Unit)? = null,
    clickEnabled: Boolean = true,
    padding: PaddingValues = PaddingValues(horizontal = 0.dp, vertical = 12.dp),
    containerColor: Color = BitwardenTheme.colorScheme.background.secondary,
    indicationColor: Color = BitwardenTheme.colorScheme.background.pressed,
): Modifier =
    this
        .cardBackground(
            cardStyle = cardStyle,
            color = containerColor,
        )
        .nullableClickable(
            onClick = onClick,
            enabled = clickEnabled,
            indicationColor = indicationColor,
        )
        .cardPadding(
            cardStyle = cardStyle,
            paddingValues = padding,
        )

/**
 * This is a [Modifier] extension that applies a card background to the content.
 */
@OmitFromCoverage
@Stable
@Composable
fun Modifier.cardBackground(
    cardStyle: CardStyle?,
    color: Color = BitwardenTheme.colorScheme.background.secondary,
): Modifier {
    cardStyle ?: return this
    val shape = if ("robolectric" == Build.FINGERPRINT) {
        // TODO: This is here to ensure our click events work in robolectric tests because of the
        //  uneven rounded corners that need to be clipped. This should be removed when the bug is
        //  resolved: https://issuetracker.google.com/issues/366255137
        RectangleShape
    } else {
        when (cardStyle) {
            is CardStyle.Top -> BitwardenTheme.shapes.contentTop
            is CardStyle.Middle -> BitwardenTheme.shapes.contentMiddle
            CardStyle.Bottom -> BitwardenTheme.shapes.contentBottom
            CardStyle.Full -> BitwardenTheme.shapes.content
        }
    }
    return this
        .clip(shape = shape)
        .background(color = color, shape = shape)
        .bottomDivider(
            paddingStart = cardStyle.dividerPadding,
            enabled = cardStyle.hasDivider,
        )
}

/**
 * This is a [Modifier] extension that applies card padding to the content.
 */
@OmitFromCoverage
@Stable
@Composable
fun Modifier.cardPadding(
    cardStyle: CardStyle?,
    paddingValues: PaddingValues = PaddingValues(vertical = 12.dp),
): Modifier {
    cardStyle ?: return this
    return this.padding(paddingValues = paddingValues)
}
