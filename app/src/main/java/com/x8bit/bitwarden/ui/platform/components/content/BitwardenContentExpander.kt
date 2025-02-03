package com.x8bit.bitwarden.ui.platform.components.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Displays expandable content when the user clicks the title.
 *
 * @param title The title to be displayed.
 * @param modifier The modifier.
 * @param content The composable content to animate in or out based on the state.
 */
@Composable
fun BitwardenContentExpander(
    modifier: Modifier = Modifier,
    title: String = stringResource(id = R.string.additional_options),
    content: @Composable() AnimatedVisibilityScope.() -> Unit,
) {
    var isExpanded by rememberSaveable { mutableStateOf(value = false) }
    Row(
        modifier = modifier
            .clickable(
                onClickLabel = stringResource(
                    id = if (isExpanded) R.string.options_expanded else R.string.options_collapsed
                ),
                onClick = { isExpanded = !isExpanded },
            )
            .clip(shape = BitwardenTheme.shapes.content)
            .minimumInteractiveComponentSize()
            .padding(top = 16.dp, bottom = 8.dp)
            .padding(horizontal = 16.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = BitwardenTheme.colorScheme.text.interaction,
            style = BitwardenTheme.typography.labelLarge,
            modifier = Modifier.padding(end = 8.dp),
        )
        val iconRotationDegrees by animateFloatAsState(
            targetValue = if (isExpanded) 0f else 180f,
            label = "expanderIconRotationAnimation",
        )
        Icon(
            painter = rememberVectorPainter(id = R.drawable.ic_chevron_up_small),
            contentDescription = null,
            tint = BitwardenTheme.colorScheme.icon.secondary,
            modifier = Modifier.rotate(degrees = iconRotationDegrees)
        )
    }
    AnimatedVisibility(
        modifier = Modifier.clipToBounds(),
        visible = isExpanded,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
        content = content,
    )
}
