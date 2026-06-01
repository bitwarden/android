package com.bitwarden.ui.platform.components.indicator

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A countdown timer displayed to the user.
 *
 * @param timeLeftSeconds The seconds left on the timer.
 * @param periodSeconds The period for the timer countdown.
 * @param modifier A [Modifier] for the composable.
 * @param alertThresholdSeconds The threshold at which the progress indicator should change to an
 * alert color.
 */
@Composable
fun BitwardenCircularCountdownIndicator(
    timeLeftSeconds: Int,
    periodSeconds: Int,
    modifier: Modifier = Modifier,
    alertThresholdSeconds: Int = -1,
) {
    val progressAnimate by animateFloatAsState(
        targetValue = timeLeftSeconds.toFloat() / periodSeconds,
        animationSpec = tween(
            durationMillis = periodSeconds,
            delayMillis = 0,
            easing = LinearOutSlowInEasing,
        ),
        label = "CircularCountDownAnimation",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        CircularProgressIndicator(
            progress = { progressAnimate },
            modifier = Modifier.size(size = 30.dp),
            color = if (timeLeftSeconds > alertThresholdSeconds) {
                BitwardenTheme.colorScheme.icon.secondary
            } else {
                BitwardenTheme.colorScheme.status.error
            },
            trackColor = Color.Transparent,
            strokeWidth = 3.dp,
            strokeCap = StrokeCap.Round,
        )

        Text(
            text = timeLeftSeconds.toString(),
            style = BitwardenTheme.typography.bodySmall,
            color = if (timeLeftSeconds > alertThresholdSeconds) {
                BitwardenTheme.colorScheme.text.primary
            } else {
                BitwardenTheme.colorScheme.status.error
            },
        )
    }
}
