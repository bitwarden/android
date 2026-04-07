package com.bitwarden.ui.platform.components.coachmark

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.coachmark.model.CoachMarkHighlightShape
import com.bitwarden.ui.platform.components.coachmark.model.CoachMarkState
import com.bitwarden.ui.platform.components.coachmark.model.rememberCoachMarkState
import com.bitwarden.ui.platform.components.coachmark.scope.CoachMarkScope
import com.bitwarden.ui.platform.components.coachmark.scope.CoachMarkScopeInstance
import com.bitwarden.ui.platform.components.text.BitwardenClickableText
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.coroutines.launch

/**
 * A composable container that manages and displays coach mark highlights.
 *
 * This composable provides a full-screen overlay that can highlight specific
 * areas of the UI and display tooltips to guide the user through a sequence
 * of steps or features.
 *
 * @param T The type of the enum used to represent the unique keys for each coach mark highlight.
 * @param state The [CoachMarkState] that manages the sequence and state of the coach marks.
 * @param modifier The modifier to be applied to the container.
 * @param content The composable content that defines the coach mark highlights within the
 * [CoachMarkScope].
 */
@Composable
@Suppress("LongMethod")
fun <T : Enum<T>> CoachMarkContainer(
    state: CoachMarkState<T>,
    modifier: Modifier = Modifier,
    content: @Composable CoachMarkScope<T>.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
    ) {
        CoachMarkScopeInstance(coachMarkState = state).content()
        val boundedRectangle by state.currentHighlightBounds
        val isVisible by state.isVisible
        val currentHighlightShape by state.currentHighlightShape

        val highlightPath = remember(boundedRectangle, currentHighlightShape) {
            if (boundedRectangle == Rect.Zero) {
                return@remember Path()
            }
            val highlightArea = Rect(
                topLeft = boundedRectangle.topLeft,
                bottomRight = boundedRectangle.bottomRight,
            )
            Path().apply {
                when (val shape = currentHighlightShape) {
                    is CoachMarkHighlightShape.RoundedRectangle -> addRoundRect(
                        RoundRect(
                            rect = highlightArea,
                            cornerRadius = CornerRadius(
                                x = shape.radius,
                            ),
                        ),
                    )

                    CoachMarkHighlightShape.Oval -> addOval(highlightArea)
                }
            }
        }
        if (boundedRectangle != Rect.Zero && isVisible) {
            val backgroundColor = BitwardenTheme.colorScheme.background.scrim
            Box(
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                // NO-OP, this consumes any touch events
                                // while the scrim is showing.
                            },
                        )
                    }
                    .fillMaxSize()
                    .drawBehind {
                        clipPath(
                            path = highlightPath,
                            clipOp = ClipOp.Difference,
                            block = {
                                drawRect(
                                    color = backgroundColor,
                                )
                            },
                        )
                    },
            )
        }
        // Once the bounds and shape update show the tooltip for the active coach mark.
        LaunchedEffect(state.currentHighlightBounds.value, state.currentHighlightShape.value) {
            if (state.currentHighlightBounds.value != Rect.Zero) {
                state.showToolTipForCurrentCoachMark()
            }
        }
        // On the initial composition of the screen check to see if the coach mark was visible and
        // then show the associated coach mark.
        LaunchedEffect(Unit) {
            if (state.isVisible.value) {
                state.currentHighlight.value?.let {
                    state.showCoachMark(it)
                }
            }
        }

        // Consume system back event when the scrim is visible.
        BackHandler(
            enabled = state.isVisible.value,
            onBack = {
                // No-op
            },
        )
    }
}

@Preview
@Composable
@Suppress("LongMethod")
private fun BitwardenCoachMarkContainer_preview() {
    BitwardenTheme {
        val state = rememberCoachMarkState(Foo.entries)
        val scope = rememberCoroutineScope()
        CoachMarkContainer(
            state = state,
        ) {
            Column(
                modifier = Modifier
                    .background(BitwardenTheme.colorScheme.background.primary)
                    .padding(top = 100.dp)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize(),
            ) {

                BitwardenClickableText(
                    label = "Start Coach Mark Flow",
                    onClick = {
                        scope.launch {
                            state.showCoachMark(Foo.Bar)
                        }
                    },
                    style = BitwardenTheme.typography.labelLarge,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth(),
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    CoachMarkHighlight(
                        key = Foo.Bar,
                        title = "1 of 3",
                        description = "Use this button to generate a new unique password.",
                        onDismiss = null,
                        leftAction = null,
                        rightAction = {
                            BitwardenClickableText(
                                label = "Next",
                                onClick = {
                                    scope.launch {
                                        state.showNextCoachMark()
                                    }
                                },
                                style = BitwardenTheme.typography.labelLarge,
                            )
                        },
                        shape = CoachMarkHighlightShape.Oval,
                    ) {
                        BitwardenStandardIconButton(
                            painter = rememberVectorPainter(BitwardenDrawable.ic_puzzle),
                            contentDescription = stringResource(BitwardenString.close),
                            onClick = {},
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                CoachMarkHighlight(
                    key = Foo.Baz,
                    title = "Foo",
                    description = "Baz",
                    shape = CoachMarkHighlightShape.RoundedRectangle(radius = 50f),
                    onDismiss = null,
                    leftAction = {
                        BitwardenClickableText(
                            label = "Back",
                            onClick = {
                                scope.launch {
                                    state.showPreviousCoachMark()
                                }
                            },
                            style = BitwardenTheme.typography.labelLarge,
                        )
                    },
                    rightAction = {
                        BitwardenClickableText(
                            label = "Done",
                            onClick = {
                                scope.launch {
                                    state.coachingComplete()
                                }
                            },
                            style = BitwardenTheme.typography.labelLarge,
                        )
                    },
                ) {
                    Text(text = "Foo Baz")
                }

                Spacer(Modifier.size(100.dp))
            }
        }
    }
}

/**
 * Example enum for demonstration purposes.
 */
private enum class Foo {
    Bar,
    Baz,
}
