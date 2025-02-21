package com.x8bit.bitwarden.ui.platform.components.header

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Reusable header element that is clickable for expanding or collapsing content.
 *
 * @param collapsedText Text to display when the content is collapsed.
 * @param expandedText Text to display when the content is expanded.
 * @param showExpansionIndicator Whether to show an indicator to expand or collapse the content.
 */
@Composable
fun BitwardenExpandingHeader(
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    collapsedText: String = stringResource(id = R.string.additional_options),
    expandedText: String = collapsedText,
    showExpansionIndicator: Boolean = true,
) {
    Row(
        modifier = modifier
            .clip(shape = BitwardenTheme.shapes.content)
            .clickable(
                onClickLabel = stringResource(
                    id = if (isExpanded) R.string.options_expanded else R.string.options_collapsed,
                ),
                onClick = onClick,
            )
            .minimumInteractiveComponentSize()
            .padding(top = 16.dp, bottom = 8.dp)
            .padding(horizontal = 16.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (isExpanded) expandedText else collapsedText,
            color = BitwardenTheme.colorScheme.text.interaction,
            style = BitwardenTheme.typography.labelLarge,
            modifier = Modifier.padding(end = 8.dp),
        )
        if (showExpansionIndicator) {
            val iconRotationDegrees = animateFloatAsState(
                targetValue = if (isExpanded) 0f else 180f,
                label = "expanderIconRotationAnimation",
            )
            Icon(
                painter = rememberVectorPainter(id = R.drawable.ic_chevron_up_small),
                contentDescription = null,
                tint = BitwardenTheme.colorScheme.icon.secondary,
                modifier = Modifier.rotate(degrees = iconRotationDegrees.value),
            )
        }
    }
}
