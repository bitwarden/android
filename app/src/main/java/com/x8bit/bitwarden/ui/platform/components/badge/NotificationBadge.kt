package com.x8bit.bitwarden.ui.platform.components.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.ui.platform.theme.LocalNonMaterialColors

/**
 * Reusable component for displaying a notification badge.
 *
 * @param notificationCount numeric value to display in center of the badge.
 */
@Composable
fun NotificationBadge(
    modifier: Modifier = Modifier,
    notificationCount: Int,
    backgroundColor: Color = LocalNonMaterialColors.current.fingerprint,
    contentColor: Color = MaterialTheme.colorScheme.onSecondary,
) {
    Badge(
        content = {
            if (notificationCount > 0) {
                Text(
                    text = notificationCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }
        },
        modifier = modifier,
        containerColor = backgroundColor,
        contentColor = contentColor,
    )
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
