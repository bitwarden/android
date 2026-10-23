package com.bitwarden.ui.platform.components.header

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.interactiveBorder
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString

/**
 * The clickable row shared by the expanding header components. It handles the interaction,
 * accessibility semantics, and the animated expansion indicator, while the [content] provides
 * the header text.
 *
 * @param isExpanded Whether the content controlled by the header is expanded.
 * @param onClick Invoked when the header is clicked.
 * @param shape The [Shape] used to clip the header and draw the interactive border.
 * @param insets Additional padding applied inside the header, after the horizontal padding.
 * @param showExpansionIndicator Whether to show an indicator to expand or collapse the content.
 * @param expansionIndicatorColor The tint applied to the expansion indicator.
 * @param interactionSource A [MutableInteractionSource] for observing and emitting interactions
 * for this component.
 * @param modifier The [Modifier] to be applied to the header.
 * @param content The text content displayed before the expansion indicator.
 */
@Composable
internal fun ExpandingHeaderRow(
    isExpanded: Boolean,
    onClick: () -> Unit,
    shape: Shape,
    insets: PaddingValues,
    showExpansionIndicator: Boolean,
    expansionIndicatorColor: Color,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .clip(shape = shape)
            .interactiveBorder(interactionSource = interactionSource, shape = shape)
            .clickable(
                onClickLabel = stringResource(
                    id = if (isExpanded) BitwardenString.collapse else BitwardenString.expand,
                ),
                onClick = onClick,
                interactionSource = interactionSource,
                role = Role.Button,
            )
            .minimumInteractiveComponentSize()
            .padding(horizontal = 16.dp)
            .padding(paddingValues = insets)
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
        if (showExpansionIndicator) {
            val iconRotationDegrees by animateFloatAsState(
                targetValue = if (isExpanded) 0f else 180f,
                label = "expanderIconRotationAnimation",
            )
            Icon(
                painter = rememberVectorPainter(id = BitwardenDrawable.ic_chevron_up_small),
                contentDescription = null,
                tint = expansionIndicatorColor,
                modifier = Modifier.rotate(degrees = iconRotationDegrees),
            )
        }
    }
}
