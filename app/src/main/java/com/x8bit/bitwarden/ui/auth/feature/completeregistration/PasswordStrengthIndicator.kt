package com.x8bit.bitwarden.ui.auth.feature.completeregistration

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.theme.LocalNonMaterialColors

/**
 * Draws a password indicator that displays password strength based on the given [state].
 */
@Suppress("LongMethod", "CyclomaticComplexMethod", "MagicNumber")
@Composable
fun PasswordStrengthIndicator(
    modifier: Modifier = Modifier,
    state: PasswordStrengthState,
) {
    val widthPercent by animateFloatAsState(
        targetValue = when (state) {
            PasswordStrengthState.NONE -> 0f
            PasswordStrengthState.WEAK_1 -> .25f
            PasswordStrengthState.WEAK_2 -> .5f
            PasswordStrengthState.WEAK_3 -> .66f
            PasswordStrengthState.GOOD -> .82f
            PasswordStrengthState.STRONG -> 1f
        },
        label = "Width Percent State",
    )
    val indicatorColor = when (state) {
        PasswordStrengthState.NONE -> MaterialTheme.colorScheme.error
        PasswordStrengthState.WEAK_1 -> MaterialTheme.colorScheme.error
        PasswordStrengthState.WEAK_2 -> MaterialTheme.colorScheme.error
        PasswordStrengthState.WEAK_3 -> LocalNonMaterialColors.current.passwordWeak
        PasswordStrengthState.GOOD -> MaterialTheme.colorScheme.primary
        PasswordStrengthState.STRONG -> LocalNonMaterialColors.current.passwordStrong
    }
    val animatedIndicatorColor by animateColorAsState(
        targetValue = indicatorColor,
        label = "Indicator Color State",
    )
    val label = when (state) {
        PasswordStrengthState.NONE -> "".asText()
        PasswordStrengthState.WEAK_1 -> R.string.weak.asText()
        PasswordStrengthState.WEAK_2 -> R.string.weak.asText()
        PasswordStrengthState.WEAK_3 -> R.string.weak.asText()
        PasswordStrengthState.GOOD -> R.string.good.asText()
        PasswordStrengthState.STRONG -> R.string.strong.asText()
    }
    Column(
        modifier = modifier,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
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
        Text(
            text = label(),
            style = MaterialTheme.typography.labelSmall,
            color = indicatorColor,
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
