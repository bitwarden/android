package com.x8bit.bitwarden.ui.auth.feature.completeregistration

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenPlurals
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.util.asText

/**
 * Draws a password indicator that displays password strength based on the given [state].
 */
@Suppress("LongMethod", "CyclomaticComplexMethod", "MagicNumber")
@Composable
fun PasswordStrengthIndicator(
    state: PasswordStrengthState,
    currentCharacterCount: Int,
    modifier: Modifier = Modifier,
    minimumCharacterCount: Int? = null,
) {
    val minimumRequirementMet = (minimumCharacterCount == null) ||
        (currentCharacterCount >= minimumCharacterCount)
    val widthPercent by animateFloatAsState(
        targetValue = if (minimumRequirementMet) {
            when (state) {
                PasswordStrengthState.NONE -> 0f
                PasswordStrengthState.WEAK_1 -> .25f
                PasswordStrengthState.WEAK_2 -> .5f
                PasswordStrengthState.WEAK_3 -> .66f
                PasswordStrengthState.GOOD -> .82f
                PasswordStrengthState.STRONG -> 1f
            }
        } else {
            0f
        },
        label = "Width Percent State",
    )
    val indicatorColor = when (state) {
        PasswordStrengthState.NONE -> BitwardenTheme.colorScheme.status.error
        PasswordStrengthState.WEAK_1 -> BitwardenTheme.colorScheme.status.error
        PasswordStrengthState.WEAK_2 -> BitwardenTheme.colorScheme.status.weak2
        PasswordStrengthState.WEAK_3 -> BitwardenTheme.colorScheme.status.weak2
        PasswordStrengthState.GOOD -> BitwardenTheme.colorScheme.status.good
        PasswordStrengthState.STRONG -> BitwardenTheme.colorScheme.status.strong
    }
    val animatedIndicatorColor by animateColorAsState(
        targetValue = indicatorColor,
        label = "Indicator Color State",
    )
    val label = when (state) {
        PasswordStrengthState.NONE -> "".asText()
        PasswordStrengthState.WEAK_1 -> BitwardenString.weak.asText()
        PasswordStrengthState.WEAK_2 -> BitwardenString.weak.asText()
        PasswordStrengthState.WEAK_3 -> BitwardenString.weak.asText()
        PasswordStrengthState.GOOD -> BitwardenString.good.asText()
        PasswordStrengthState.STRONG -> BitwardenString.strong.asText()
    }
    Column(
        modifier = modifier,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(shape = BitwardenTheme.shapes.progressIndicator)
                .background(BitwardenTheme.colorScheme.sliderButton.unfilled),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(pivotFractionX = 0f, pivotFractionY = 0f)
                        scaleX = widthPercent
                    }
                    .drawBehind {
                        drawRect(animatedIndicatorColor)
                    },
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            minimumCharacterCount?.let { minCount ->
                MinimumCharacterCount(
                    minimumRequirementMet = currentCharacterCount >= minCount,
                    minimumCharacterCount = minCount,
                )
            }
            if (minimumRequirementMet) {
                Text(
                    text = label(),
                    style = BitwardenTheme.typography.labelSmall,
                    color = indicatorColor,
                )
            }
        }
    }
}

@Composable
private fun MinimumCharacterCount(
    modifier: Modifier = Modifier,
    minimumRequirementMet: Boolean,
    minimumCharacterCount: Int,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = if (minimumRequirementMet) {
                BitwardenDrawable.ic_plain_checkmark
            } else {
                BitwardenDrawable.ic_circle
            },
            label = "iconForMinimumCharacterCount",
        ) {
            Icon(
                painter = rememberVectorPainter(id = it),
                contentDescription = null,
                tint = BitwardenTheme.colorScheme.text.secondary,
                modifier = Modifier.size(12.dp),
            )
        }
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = pluralStringResource(
                id = BitwardenPlurals.minimum_characters,
                count = minimumCharacterCount,
                formatArgs = arrayOf(minimumCharacterCount),
            ),
            color = BitwardenTheme.colorScheme.text.secondary,
            style = BitwardenTheme.typography.labelSmall,
        )
    }
}

/**
 * Models various levels of password strength that can be displayed by [PasswordStrengthIndicator].
 */
enum class PasswordStrengthState {
    NONE,
    WEAK_1,
    WEAK_2,
    WEAK_3,
    GOOD,
    STRONG,
}

@Preview(showBackground = true)
@Composable
private fun PasswordStrengthIndicatorPreview_minCharMet() {
    BitwardenTheme {
        PasswordStrengthIndicator(
            state = PasswordStrengthState.WEAK_3,
            currentCharacterCount = 12,
            minimumCharacterCount = 12,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PasswordStrengthIndicatorPreview_minCharNotMet() {
    BitwardenTheme {
        PasswordStrengthIndicator(
            state = PasswordStrengthState.WEAK_3,
            currentCharacterCount = 11,
            minimumCharacterCount = 12,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PasswordStrengthIndicatorPreview_noMinChar() {
    BitwardenTheme {
        PasswordStrengthIndicator(
            state = PasswordStrengthState.WEAK_3,
            currentCharacterCount = 12,
        )
    }
}
