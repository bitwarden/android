package com.x8bit.bitwarden.ui.platform.components.appbar

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.bottomDivider
import com.x8bit.bitwarden.ui.platform.base.util.scrolledContainerBottomDivider
import com.x8bit.bitwarden.ui.platform.components.appbar.color.bitwardenTopAppBarColors
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A custom Bitwarden-themed medium top app bar with support for actions.
 *
 * This app bar wraps around Material 3's [MediumTopAppBar] and customizes its appearance
 * and behavior according to the app theme.
 * It provides a title and an optional set of actions on the trailing side.
 * These actions are arranged within a custom action row tailored to the app's design requirements.
 *
 * @param title The text to be displayed as the title of the app bar.
 * @param scrollBehavior Defines the scrolling behavior of the app bar. It controls how the app bar
 * behaves in conjunction with scrolling content.
 * @param isBottomDividerEnabled Determines if the bottom divider should be displayed on scroll or
 * not.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * in the app bar's trailing side. This lambda extends [RowScope], allowing flexibility in
 * defining the layout of the actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitwardenMediumTopAppBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    isBottomDividerEnabled: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    MediumTopAppBar(
        colors = bitwardenTopAppBarColors(),
        scrollBehavior = scrollBehavior,
        title = {
            Text(
                text = title,
                style = BitwardenTheme.typography.titleLarge,
                modifier = Modifier.testTag(tag = "PageTitleLabel"),
            )
        },
        modifier = modifier
            .testTag(tag = "HeaderBarComponent")
            .scrolledContainerBottomDivider(
                topAppBarScrollBehavior = scrollBehavior,
                enabled = isBottomDividerEnabled,
            )
            // When the scrolling divider is disabled, we show the static divider
            .bottomDivider(
                enabled = !isBottomDividerEnabled,
                thickness = (0.5).dp,
            ),
        actions = actions,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun BitwardenMediumTopAppBar_preview() {
    BitwardenTheme {
        BitwardenMediumTopAppBar(
            title = "Preview Title",
            scrollBehavior = TopAppBarDefaults
                .exitUntilCollapsedScrollBehavior(
                    rememberTopAppBarState(),
                ),
            actions = {
                BitwardenStandardIconButton(
                    vectorIconRes = R.drawable.ic_ellipsis_vertical,
                    contentDescription = "",
                    onClick = { },
                )
            },
        )
    }
}
