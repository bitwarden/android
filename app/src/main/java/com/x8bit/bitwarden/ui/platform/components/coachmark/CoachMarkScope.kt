package com.x8bit.bitwarden.ui.platform.components.coachmark

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipScope
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Defines the scope for creating coach mark highlights within a user interface.
 *
 * This interface provides a way to define and display a highlight that guides the user's
 * attention to a specific part of the UI, often accompanied by a tooltip with
 * explanatory text and actions.
 *
 * @param T The type of the enum used to represent the unique keys for each coach mark highlight.
 */
interface CoachMarkScope<T : Enum<T>> {

    /**
     * Creates a highlight for a specific coach mark.
     *
     * This function defines a region of the UI to be highlighted, along with an
     * associated tooltip that can display a title, description, and actions.
     *
     * @param key The unique key identifying this highlight. This key is used to
     * manage the state and order of the coach mark sequence.
     * @param title The title of the coach mark, displayed in the tooltip.
     * @param description The description of the coach mark, providing more context
     * to the user. Displayed in the tooltip.
     * @param shape The shape of the highlight. Defaults to [CoachMarkHighlightShape.SQUARE].
     * Use [CoachMarkHighlightShape.OVAL] for a circular highlight.
     * @param onDismiss An optional callback that is invoked when the coach mark is dismissed
     * (e.g., by clicking the close button). If provided, this function
     * will be executed after the coach mark is dismissed. If not provided,
     * no action is taken on dismissal.
     * @param leftAction An optional composable to be displayed on the left side of the
     * action row in the tooltip. This can be used to provide
     * additional actions or controls.
     * @param rightAction An optional composable to be displayed on the right side of the
     * action row in the tooltip. This can be used to provide
     * primary actions or navigation.
     * @param anchorContent The composable content to be highlighted. This is the UI element
     * that will be visually emphasized by the coach mark.
     */
    @Composable
    fun CoachMarkHighlight(
        key: T,
        title: String,
        description: String,
        shape: CoachMarkHighlightShape = CoachMarkHighlightShape.SQUARE,
        onDismiss: (() -> Unit)? = null,
        leftAction: (@Composable RowScope.() -> Unit)? = null,
        rightAction: (@Composable RowScope.() -> Unit)? = null,
        anchorContent: @Composable () -> Unit,
    )
}

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
        shape: CoachMarkHighlightShape,
        onDismiss: (() -> Unit)?,
        leftAction: @Composable() (RowScope.() -> Unit)?,
        rightAction: @Composable() (RowScope.() -> Unit)?,
        anchorContent: @Composable () -> Unit,
    ) {
        val anchorBounds = remember { mutableStateOf<Rect?>(null) }
        val toolTipState = rememberTooltipState(
            initialIsVisible = false,
            isPersistent = true,
        )
        TooltipBox(
            positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(
                spacingBetweenTooltipAndAnchor = 10.dp,
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
            modifier = Modifier.onGloballyPositioned {
                anchorBounds.value = it.boundsInRoot()
            },
        ) {
            anchorContent()
        }
        LaunchedEffect(anchorBounds) {
            anchorBounds.value?.let {
                coachMarkState.updateHighlight(
                    key = key,
                    bounds = it,
                    toolTipState = toolTipState,
                    shape = shape,
                )
            }
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
            containerColor = BitwardenTheme.colorScheme.background.primary,
        ),
    ) {
        Text(
            text = description,
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
        )
    }
}
