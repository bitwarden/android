package com.bitwarden.ui.platform.components.badge

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Reusable component for displaying a notification badge.
 *
 * @param notificationCount numeric value to display in center of the badge.
 */
@Composable
fun NotificationBadge(
    notificationCount: Int,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    backgroundColor: Color = BitwardenTheme.colorScheme.icon.badgeBackground,
    contentColor: Color = BitwardenTheme.colorScheme.icon.badgeForeground,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
    ) {
        Badge(
            content = {
                Text(
                    text = notificationCount.toString(),
                    style = BitwardenTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                )
            },
            modifier = modifier,
            containerColor = backgroundColor,
            contentColor = contentColor,
        )
    }
}

@Preview
@Composable
private fun NotificationBadge_preview() {
    Column(
        modifier = Modifier
            .background(color = Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NotificationBadge(notificationCount = 0, backgroundColor = Color.Red)
        NotificationBadge(notificationCount = 4, backgroundColor = Color.Red)
        NotificationBadge(notificationCount = 199, backgroundColor = Color.Green)
        NotificationBadge(
            notificationCount = 1999,
            backgroundColor = Color.Blue,
            contentColor = Color.Yellow,
        )
    }
}
