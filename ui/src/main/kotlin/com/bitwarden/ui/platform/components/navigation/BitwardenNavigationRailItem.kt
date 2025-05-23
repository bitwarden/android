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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.bitwarden.ui.platform.components.badge.NotificationBadge
import com.bitwarden.ui.platform.components.navigation.color.bitwardenNavigationRailItemColors
import com.bitwarden.ui.platform.components.util.rememberVectorPainter

/**
 * A custom Bitwarden-themed bottom app bar.
 *
 * @param labelRes The custom label for the navigation item.
 * @param contentDescriptionRes The custom content description for the navigation item.
 * @param selectedIconRes The icon to be displayed when the navigation item is selected.
 * @param unselectedIconRes The icon to be displayed when the navigation item is not selected.
 * @param isSelected Indicates that the navigation item is selected.
 * @param onClick The lambda to be invoked when the navigation item is clicked.
 * @param modifier A [Modifier] that you can use to apply custom modifications to the composable.
 * @param notificationCount The notification count for the navigation item.
 */
@Composable
fun ColumnScope.BitwardenNavigationRailItem(
    @StringRes labelRes: Int,
    @StringRes contentDescriptionRes: Int,
    @DrawableRes selectedIconRes: Int,
    @DrawableRes unselectedIconRes: Int,
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
                    painter = rememberVectorPainter(
                        id = if (isSelected) selectedIconRes else unselectedIconRes,
                    ),
                    contentDescription = stringResource(id = contentDescriptionRes),
                    tint = Color.Unspecified,
                )
            }
        },
        label = {
            Text(
                text = stringResource(id = labelRes),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        selected = isSelected,
        onClick = onClick,
        colors = bitwardenNavigationRailItemColors(),
        modifier = modifier,
    )
}
