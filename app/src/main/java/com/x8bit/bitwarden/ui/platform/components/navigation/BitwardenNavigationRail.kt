package com.x8bit.bitwarden.ui.platform.components.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.ui.platform.base.util.endDivider
import com.x8bit.bitwarden.ui.platform.components.model.NavigationItem
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.ImmutableList

/**
 * A custom Bitwarden-themed navigation rail.
 */
@Composable
fun BitwardenNavigationRail(
    navigationItems: ImmutableList<NavigationItem>,
    selectedItem: NavigationItem?,
    onClick: (NavigationItem) -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = NavigationRailDefaults.windowInsets
        .union(WindowInsets.displayCutout.only(WindowInsetsSides.Start)),
) {
    Surface(
        color = BitwardenTheme.colorScheme.background.secondary,
        contentColor = Color.Unspecified,
        modifier = modifier.endDivider(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .windowInsetsPadding(insets = windowInsets)
                .widthIn(min = 80.dp)
                .padding(vertical = 4.dp)
                .selectableGroup(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                space = 4.dp,
                alignment = Alignment.CenterVertically,
            ),
        ) {
            navigationItems.forEach { navigationItem ->
                BitwardenNavigationRailItem(
                    labelRes = navigationItem.labelRes,
                    contentDescriptionRes = navigationItem.contentDescriptionRes,
                    selectedIconRes = navigationItem.iconResSelected,
                    unselectedIconRes = navigationItem.iconRes,
                    notificationCount = navigationItem.notificationCount,
                    isSelected = navigationItem == selectedItem,
                    onClick = { onClick(navigationItem) },
                    modifier = Modifier.testTag(tag = navigationItem.testTag),
                )
            }
        }
    }
}
