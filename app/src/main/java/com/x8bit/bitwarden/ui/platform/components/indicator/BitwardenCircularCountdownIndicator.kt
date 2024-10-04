package com.x8bit.bitwarden.ui.platform.components.indicator

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
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A countdown timer displayed to the user.
 *
 * @param timeLeftSeconds The seconds left on the timer.
 * @param periodSeconds The period for the timer countdown.
 * @param modifier A [Modifier] for the composable.
 */
@Composable
fun BitwardenCircularCountdownIndicator(
    timeLeftSeconds: Int,
    periodSeconds: Int,
    modifier: Modifier = Modifier,
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
            color = BitwardenTheme.colorScheme.icon.secondary,
            trackColor = Color.Transparent,
            strokeWidth = 3.dp,
            strokeCap = StrokeCap.Round,
        )

        Text(
            text = timeLeftSeconds.toString(),
            style = BitwardenTheme.typography.bodySmall,
            color = BitwardenTheme.colorScheme.text.primary,
        )
    }
}
