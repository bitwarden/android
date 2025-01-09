package com.x8bit.bitwarden.ui.platform.components.coachmark

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.text.BitwardenClickableText
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.coroutines.launch

private const val ROUNDED_RECT_RADIUS = 8f

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
    val scope = rememberCoroutineScope()
    Box(modifier = Modifier
        .fillMaxSize()
        .then(modifier),
    ) {
        CoachMarkScopeInstance(coachMarkState = state).content()
        val boundedRectangle by state.currentHighlightBounds
        if (
            boundedRectangle != Rect.Zero && state.isVisible.value
        ) {
            val highlightArea = Rect(
                topLeft = boundedRectangle.topLeft,
                bottomRight = boundedRectangle.bottomRight,
            )

            Box(
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                if (state.isVisible.value) {
                                    scope.launch {
                                        state.showCoachMark()
                                    }
                                }
                            },
                        )
                    }
                    .fillMaxSize()
                    .drawBehind {
                        clipPath(
                            path = Path().apply {
                                when (state.currentHighlightShape.value) {
                                    CoachMarkHighlightShape.SQUARE -> addRoundRect(
                                        RoundRect(
                                            rect = highlightArea,
                                            cornerRadius = CornerRadius(
                                                x = ROUNDED_RECT_RADIUS,
                                            ),
                                        ),
                                    )

                                    CoachMarkHighlightShape.OVAL -> addOval(highlightArea)
                                }
                            },
                            clipOp = ClipOp.Difference,
                            block = {
                                drawRect(
                                    color = Color.Black,
                                    alpha = 0.5f,
                                )
                            },
                        )
                    },
            )
        }
    }
    LaunchedEffect(Unit) {
        if (state.isVisible.value && (state.currentHighlight.value != null)) {
            state.showCoachMark()
        }
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
                        shape = CoachMarkHighlightShape.OVAL,
                    ) {
                        BitwardenStandardIconButton(
                            painter = rememberVectorPainter(R.drawable.ic_puzzle),
                            contentDescription = stringResource(R.string.close),
                            onClick = {},
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                CoachMarkHighlight(
                    key = Foo.Baz,
                    title = "Foo",
                    description = "Baz",
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
