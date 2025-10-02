package com.bitwarden.ui.platform.components.fab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.nullableTestTag
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.util.Text
import kotlinx.collections.immutable.ImmutableList

/**
 * A FAB that expands, when clicked, to display a collection of options that can be clicked.
 *
 * @param expandableFabIcon The icon to display and how to display it.
 * @param items [ExpandableFabOption] buttons displayed when the FAB is expanded.
 * @param label [Text] displayed when the FAB is expanded.
 * @param modifier The modifier for this composable.
 * @param expandableFabState [ExpandableFabIcon] displayed in the FAB.
 * @param onStateChange Lambda invoked when the FAB expanded state changes.
 */
@Suppress("LongMethod")
@Composable
fun <T : ExpandableFabOption> BitwardenExpandableFloatingActionButton(
    expandableFabIcon: ExpandableFabIcon,
    items: ImmutableList<T>,
    modifier: Modifier = Modifier,
    label: Text? = null,
    expandableFabState: MutableState<ExpandableFabState> = rememberExpandableFabState(),
    onStateChange: (expandableFabState: ExpandableFabState) -> Unit = { },
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom,
        modifier = modifier,
    ) {
        AnimatedVisibility(
            visible = expandableFabState.value.isExpanded(),
            label = "display_fab_options_animation",
            modifier = Modifier.weight(weight = 1f),
        ) {
            LazyColumn(
                modifier = Modifier
                    .clickable(interactionSource = null, indication = null) {
                        expandableFabState.value = ExpandableFabState.Collapsed
                    }
                    .fillMaxSize(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(
                    space = 12.dp,
                    alignment = Alignment.Bottom,
                ),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(items) { expandableFabOption ->
                    ExpandableFabOption(
                        onFabOptionClick = {
                            expandableFabState.value = expandableFabState.value.toggleValue()
                            onStateChange(expandableFabState.value)
                            expandableFabOption.onFabOptionClick()
                        },
                        expandableFabOption = expandableFabOption,
                    )
                }
            }
        }

        val rotation by animateFloatAsState(
            targetValue = if (expandableFabState.value.isExpanded()) {
                expandableFabIcon.iconRotation ?: 0f
            } else {
                0f
            },
            label = "add_item_rotation",
        )
        ExtendedFloatingActionButton(
            onClick = {
                expandableFabState.value = expandableFabState.value.toggleValue()
                onStateChange(expandableFabState.value)
            },
            expanded = if (label != null) {
                !expandableFabState.value.isExpanded()
            } else {
                false
            },
            containerColor = BitwardenTheme.colorScheme.filledButton.background,
            contentColor = BitwardenTheme.colorScheme.filledButton.foreground,
            shape = BitwardenTheme.shapes.fab,
            text = {
                label?.let {
                    Text(
                        text = it(),
                        style = BitwardenTheme.typography.labelMedium,
                    )
                }
            },
            icon = {
                Icon(
                    painter = rememberVectorPainter(id = expandableFabIcon.icon.iconRes),
                    contentDescription = expandableFabIcon.icon.contentDescription?.invoke(),
                    modifier = Modifier
                        .rotate(degrees = rotation)
                        .nullableTestTag(tag = expandableFabIcon.icon.testTag),
                )
            },
        )
    }
}

@Composable
private fun <T : ExpandableFabOption> ExpandableFabOption(
    expandableFabOption: T,
    onFabOptionClick: (option: T) -> Unit,
    modifier: Modifier = Modifier,
) {
    SmallFloatingActionButton(
        onClick = { onFabOptionClick(expandableFabOption) },
        containerColor = BitwardenTheme.colorScheme.filledButton.background,
        contentColor = BitwardenTheme.colorScheme.filledButton.foreground,
        shape = BitwardenTheme.shapes.fabItem,
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .wrapContentSize()
                .padding(start = 8.dp, end = 16.dp),
        ) {
            Text(
                text = expandableFabOption.label(),
                style = BitwardenTheme.typography.labelSmall,
                modifier = Modifier.padding(all = 8.dp),
            )
            Icon(
                painter = rememberVectorPainter(id = expandableFabOption.icon.iconRes),
                contentDescription = expandableFabOption.icon.contentDescription?.invoke(),
                modifier = Modifier.nullableTestTag(tag = expandableFabOption.icon.testTag),
            )
        }
    }
}

@Composable
private fun rememberExpandableFabState(): MutableState<ExpandableFabState> =
    remember { mutableStateOf(ExpandableFabState.Collapsed) }

/**
 * Represents options displayed when the FAB is expanded.
 */
abstract class ExpandableFabOption(
    val label: Text,
    val icon: IconData.Local,
    val onFabOptionClick: () -> Unit,
)

/**
 * Models data for an expandable FAB icon.
 */
data class ExpandableFabIcon(
    val icon: IconData.Local,
    val iconRotation: Float?,
)

/**
 * Models the state of the expandable FAB.
 */
sealed class ExpandableFabState {
    /**
     * Indicates if the FAB is expanded.
     */
    fun isExpanded(): Boolean = this == Expanded

    /**
     * Invert the state of the FAB.
     */
    fun toggleValue(): ExpandableFabState = if (isExpanded()) {
        Collapsed
    } else {
        Expanded
    }

    /**
     * Indicates the FAB is collapsed.
     */
    data object Collapsed : ExpandableFabState()

    /**
     * Indicates the FAB is expanded.
     */
    data object Expanded : ExpandableFabState()
}
