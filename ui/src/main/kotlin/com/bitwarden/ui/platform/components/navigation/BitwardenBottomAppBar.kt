package com.bitwarden.ui.platform.components.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.bitwarden.ui.platform.base.util.topDivider
import com.bitwarden.ui.platform.components.navigation.model.NavigationItem
import com.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.ImmutableList

/**
 * A custom Bitwarden-themed bottom app bar.
 */
@Composable
fun BitwardenBottomAppBar(
    navigationItems: ImmutableList<NavigationItem>,
    selectedItem: NavigationItem?,
    onClick: (NavigationItem) -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = BottomAppBarDefaults.windowInsets,
) {
    BottomAppBar(
        containerColor = BitwardenTheme.colorScheme.background.secondary,
        contentColor = Color.Unspecified,
        windowInsets = windowInsets,
        modifier = modifier.topDivider(),
    ) {
        navigationItems.forEach { navigationItem ->
            BitwardenNavigationBarItem(
                labelRes = navigationItem.labelRes,
                contentDescriptionRes = navigationItem.contentDescriptionRes,
                selectedIconRes = navigationItem.iconResSelected,
                unselectedIconRes = navigationItem.iconRes,
                notificationCount = navigationItem.notificationCount,
                isSelected = selectedItem == navigationItem,
                onClick = { onClick(navigationItem) },
                modifier = Modifier.testTag(tag = navigationItem.testTag),
            )
        }
    }
}
