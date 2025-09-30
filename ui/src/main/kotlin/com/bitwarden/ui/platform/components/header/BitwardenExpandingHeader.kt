package com.bitwarden.ui.platform.components.header

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Reusable header element that is clickable for expanding or collapsing content.
 *
 * @param collapsedText Text to display when the content is collapsed.
 * @param expandedText Text to display when the content is expanded.
 * @param showExpansionIndicator Whether to show an indicator to expand or collapse the content.
 */
@Suppress("LongMethod")
@Composable
fun BitwardenExpandingHeader(
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    collapsedText: String = stringResource(id = BitwardenString.additional_options),
    expandedText: String = collapsedText,
    showExpansionIndicator: Boolean = true,
    shape: Shape = BitwardenTheme.shapes.content,
    insets: PaddingValues = PaddingValues(top = 16.dp, bottom = 8.dp),
) {
    Row(
        modifier = modifier
            .clip(shape = shape)
            .clickable(
                onClickLabel = stringResource(
                    id = if (isExpanded) {
                        BitwardenString.options_expanded
                    } else {
                        BitwardenString.options_collapsed
                    },
                ),
                onClick = onClick,
            )
            .minimumInteractiveComponentSize()
            .padding(horizontal = 16.dp)
            .padding(paddingValues = insets)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Crossfade(
            targetState = isExpanded,
            label = "BitwardenExpandingHeaderTitle_animation",
            // Make the animation shorter when the text is the same to avoid crossfading the same
            // text.
            animationSpec = tween(
                durationMillis = if (expandedText != collapsedText) {
                    AnimationConstants.DefaultDurationMillis
                } else {
                    0
                },
            ),
        ) { expanded ->
            if (expanded) {
                Text(
                    text = expandedText,
                    color = BitwardenTheme.colorScheme.text.interaction,
                    style = BitwardenTheme.typography.labelLarge,
                    modifier = Modifier.padding(end = 8.dp),
                )
            } else {
                Text(
                    text = collapsedText,
                    color = BitwardenTheme.colorScheme.text.interaction,
                    style = BitwardenTheme.typography.labelLarge,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
        if (showExpansionIndicator) {
            val iconRotationDegrees = animateFloatAsState(
                targetValue = if (isExpanded) 0f else 180f,
                label = "expanderIconRotationAnimation",
            )
            Icon(
                painter = rememberVectorPainter(id = BitwardenDrawable.ic_chevron_up_small),
                contentDescription = null,
                tint = BitwardenTheme.colorScheme.icon.secondary,
                modifier = Modifier.rotate(degrees = iconRotationDegrees.value),
            )
        }
    }
}
