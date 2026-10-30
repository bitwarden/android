package com.bitwarden.ui.platform.components.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.bitwarden.ui.platform.components.badge.NotificationBadge
import com.bitwarden.ui.platform.components.navigation.color.bitwardenNavigationRailItemColors
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenPlurals

/**
 * A custom Bitwarden-themed navigation rail item.
 *
 * @param labelRes The custom label for the navigation item.
 * @param iconRes The icon to be displayed on the navigation item.
 * @param isSelected Indicates that the navigation item is selected.
 * @param onClick The lambda to be invoked when the navigation item is clicked.
 * @param modifier A [Modifier] that you can use to apply custom modifications to the composable.
 * @param notificationCount The notification count for the navigation item.
 */
@Composable
fun ColumnScope.BitwardenNavigationRailItem(
    @StringRes labelRes: Int,
    @DrawableRes iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    notificationCount: Int = 0,
) {
    NavigationRailItem(
        icon = {
            BadgedBox(
                badge = {
                    NotificationBadge(
                        notificationCount = notificationCount,
                        isVisible = notificationCount > 0,
                    )
                },
            ) {
                Icon(
                    painter = rememberVectorPainter(id = iconRes),
                    contentDescription = null,
                )
            }
        },
        label = {
            val label = stringResource(id = labelRes)
            val notifications = pluralStringResource(
                id = BitwardenPlurals.notifications_content_description,
                count = notificationCount,
                formatArgs = arrayOf(notificationCount),
            )
            Text(
                text = stringResource(id = labelRes),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics {
                    // The NavigationRailItem will clear any icon semantics when the label is
                    // present, so we have to add the notification count manually here.
                    contentDescription = if (notificationCount > 0) {
                        "$label, $notifications"
                    } else {
                        label
                    }
                },
            )
        },
        selected = isSelected,
        onClick = onClick,
        colors = bitwardenNavigationRailItemColors(),
        modifier = modifier,
    )
}
