@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.platform.base.util

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.CombinedModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.util.isPortrait

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
            drawLine(
                alpha = alpha,
                color = color,
                strokeWidth = thickness.toPx(),
                start = Offset(
                    x = paddingStart.toPx(),
                    y = size.height - thickness.toPx() / 2,
                ),
                end = Offset(
                    x = size.width - paddingEnd.toPx(),
                    y = size.height - thickness.toPx() / 2,
                ),
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
fun Modifier.standardHorizontalMargin(
    portrait: Dp = 16.dp,
    landscape: Dp = 48.dp,
): Modifier =
    this then StandardHorizontalMarginElement(portrait = portrait, landscape = landscape)

private data class StandardHorizontalMarginElement(
    private val portrait: Dp,
    private val landscape: Dp,
) : ModifierNodeElement<StandardHorizontalMarginElement.StandardHorizontalMarginConsumerNode>() {
    override fun create(): StandardHorizontalMarginConsumerNode =
        StandardHorizontalMarginConsumerNode(
            portrait = portrait,
            landscape = landscape,
        )

    override fun update(node: StandardHorizontalMarginConsumerNode) {
        node.portrait = portrait
        node.landscape = landscape
    }

    class StandardHorizontalMarginConsumerNode(
        var portrait: Dp,
        var landscape: Dp,
    ) : Modifier.Node(),
        LayoutModifierNode,
        CompositionLocalConsumerModifierNode {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints,
        ): MeasureResult {
            val currentConfig = currentValueOf(LocalConfiguration)
            val paddingPx = (if (currentConfig.isPortrait) portrait else landscape).roundToPx()
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
        paddingStart = paddingHorizontal,
        paddingTop = paddingVertical,
        paddingEnd = paddingHorizontal,
        paddingBottom = paddingVertical,
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
            paddingValues = PaddingValues(
                start = paddingStart,
                top = paddingTop,
                end = paddingEnd,
                bottom = paddingBottom,
            ),
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
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    return this
        .clip(shape = shape)
        .background(color = color, shape = shape)
        .bottomDivider(
            paddingStart = if (isLtr) cardStyle.dividerPadding else 0.dp,
            paddingEnd = if (isLtr) 0.dp else cardStyle.dividerPadding,
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
