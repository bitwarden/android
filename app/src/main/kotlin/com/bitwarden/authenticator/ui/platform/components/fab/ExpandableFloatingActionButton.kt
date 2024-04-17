package com.bitwarden.authenticator.ui.platform.components.fab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.platform.base.util.Text
import com.bitwarden.authenticator.ui.platform.components.model.IconResource
import com.bitwarden.authenticator.ui.platform.theme.Typography

@Composable
fun <T : ExpandableFabOption> ExpandableFloatingActionButton(
    modifier: Modifier = Modifier,
    label: Text?,
    items: List<T>,
    expandableFabState: MutableState<ExpandableFabState> = rememberExpandableFabState(),
    expandableFabIcon: ExpandableFabIcon,
    onStateChange: (expandableFabState: ExpandableFabState) -> Unit = { },
) {
    val rotation by animateFloatAsState(
        targetValue = if (expandableFabState.value == ExpandableFabState.Expanded) {
            expandableFabIcon.iconRotation ?: 0f
        } else {
            0f
        },
        label = stringResource(R.string.add_item_rotation),
    )
    Column(
        modifier = modifier.wrapContentSize(),
        horizontalAlignment = Alignment.End,
    ) {
        AnimatedVisibility(
            visible = expandableFabState.value.isExpanded(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            LazyColumn(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(items) { expandableFabOption ->
                    ExpandableFabOption(
                        onFabOptionClick = {
                            expandableFabState.value = expandableFabState.value.toggleValue()
                            onStateChange(expandableFabState.value)
                            expandableFabOption.onFabOptionClick()
                        },
                        expandableFabOption = expandableFabOption
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = {
                expandableFabState.value = expandableFabState.value.toggleValue()
                onStateChange(expandableFabState.value)
            },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ) {

            if (label != null) {
                AnimatedVisibility(
                    visible = expandableFabState.value.isExpanded(),
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally(),
                ) {
                    Text(
                        modifier = Modifier.padding(end = 8.dp),
                        text = label(),
                    )
                }
            }

            Icon(
                modifier = Modifier
                    .rotate(rotation)
                    .semantics { expandableFabIcon.iconData.testTag },
                painter = expandableFabIcon.iconData.iconPainter,
                contentDescription = expandableFabIcon.iconData.contentDescription,
            )
        }
    }
}

@Composable
private fun <T : ExpandableFabOption> ExpandableFabOption(
    expandableFabOption: T,
    onFabOptionClick: (option: T) -> Unit,
) {
    SmallFloatingActionButton(
        onClick = { onFabOptionClick(expandableFabOption) },
    ) {
        Row(
            modifier = Modifier
                .wrapContentSize()
                .padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            expandableFabOption.label?.let { label ->
                Text(
                    modifier = Modifier
                        .clip(RoundedCornerShape(size = 8.dp))
                        .padding(all = 8.dp),
                    text = label(),
                    style = Typography.labelSmall,
                )
            }

            Icon(
                painter = expandableFabOption.iconData.iconPainter,
                contentDescription = expandableFabOption.iconData.contentDescription,
            )
        }
    }
}

@Composable
fun rememberExpandableFabState() =
    remember { mutableStateOf<ExpandableFabState>(ExpandableFabState.Collapsed) }

abstract class ExpandableFabOption(
    val label: Text?,
    val iconData: IconResource,
    val onFabOptionClick: () -> Unit,
)

data class ExpandableFabIcon(
    val iconData: IconResource,
    val iconRotation: Float?,
)

sealed class ExpandableFabState {

    fun isExpanded() = this is Expanded

    fun toggleValue() = if (isExpanded()) {
        Collapsed
    } else {
        Expanded
    }

    data object Collapsed : ExpandableFabState()

    data object Expanded : ExpandableFabState()
}
