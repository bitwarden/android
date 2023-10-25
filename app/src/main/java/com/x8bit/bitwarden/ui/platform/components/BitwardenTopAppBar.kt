package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a Bitwarden styled [TopAppBar] that assumes the following components:
 *
 * - a single navigation control in the upper-left defined by [navigationIcon],
 *   [navigationIconContentDescription], and [onNavigationIconClick].
 * - a [title] in the middle.
 * - a [actions] lambda containing the set of actions (usually icons or similar) to display
 *  in the app bar's trailing side. This lambda extends [RowScope], allowing flexibility in
 *  defining the layout of the actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitwardenTopAppBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    navigationIcon: Painter,
    navigationIconContentDescription: String,
    onNavigationIconClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            IconButton(
                onClick = { onNavigationIconClick() },
            ) {
                Icon(
                    painter = navigationIcon,
                    contentDescription = navigationIconContentDescription,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        actions = actions,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun BitwardenTopAppBar_preview() {
    BitwardenTheme {
        BitwardenTopAppBar(
            title = "Title",
            scrollBehavior = TopAppBarDefaults
                .exitUntilCollapsedScrollBehavior(
                    rememberTopAppBarState(),
                ),
            navigationIcon = painterResource(id = R.drawable.ic_close),
            navigationIconContentDescription = stringResource(id = R.string.close),
            onNavigationIconClick = {},
        )
    }
}
