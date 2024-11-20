package com.x8bit.bitwarden.ui.platform.components.appbar

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.bottomDivider
import com.x8bit.bitwarden.ui.platform.base.util.mirrorIfRtl
import com.x8bit.bitwarden.ui.platform.base.util.scrolledContainerBottomDivider
import com.x8bit.bitwarden.ui.platform.components.appbar.color.bitwardenTopAppBarColors
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.model.TopAppBarDividerStyle
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
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
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets
        .union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
    dividerStyle: TopAppBarDividerStyle = TopAppBarDividerStyle.ON_SCROLL,
    actions: @Composable RowScope.() -> Unit = { },
) {
    BitwardenTopAppBar(
        title = title,
        scrollBehavior = scrollBehavior,
        navigationIcon = NavigationIcon(
            navigationIcon = navigationIcon,
            navigationIconContentDescription = navigationIconContentDescription,
            onNavigationIconClick = onNavigationIconClick,
        ),
        modifier = modifier,
        windowInsets = windowInsets,
        dividerStyle = dividerStyle,
        actions = actions,
    )
}

/**
 * Represents a Bitwarden styled [TopAppBar] that assumes the following components:
 *
 * - an optional single navigation control in the upper-left defined by [navigationIcon].
 * - a [title] in the middle.
 * - a [actions] lambda containing the set of actions (usually icons or similar) to display
 *  in the app bar's trailing side. This lambda extends [RowScope], allowing flexibility in
 *  defining the layout of the actions.
 * - if the title text causes an overflow in the standard material [TopAppBar] a [MediumTopAppBar]
 *   will be used instead, droping the title text to a second row beneath the [navigationIcon] and
 *   [actions].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun BitwardenTopAppBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    navigationIcon: NavigationIcon?,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets
        .union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
    dividerStyle: TopAppBarDividerStyle = TopAppBarDividerStyle.ON_SCROLL,
    actions: @Composable RowScope.() -> Unit = {},
    minimunHeight: Dp = 48.dp,
) {
    var titleTextHasOverflow by remember {
        mutableStateOf(false)
    }

    val navigationIconContent: @Composable () -> Unit = remember(navigationIcon) {
        {
            navigationIcon?.let {
                BitwardenStandardIconButton(
                    painter = it.navigationIcon,
                    contentDescription = it.navigationIconContentDescription,
                    onClick = it.onNavigationIconClick,
                    modifier = Modifier
                        .testTag(tag = "CloseButton")
                        .mirrorIfRtl(),
                )
            }
        }
    }
    val customModifier = modifier
        .testTag(tag = "HeaderBarComponent")
        .scrolledContainerBottomDivider(
            topAppBarScrollBehavior = scrollBehavior,
            enabled = when (dividerStyle) {
                TopAppBarDividerStyle.NONE -> false
                TopAppBarDividerStyle.STATIC -> false
                TopAppBarDividerStyle.ON_SCROLL -> true
            },
        )
        .bottomDivider(
            enabled = when (dividerStyle) {
                TopAppBarDividerStyle.NONE -> false
                TopAppBarDividerStyle.STATIC -> true
                TopAppBarDividerStyle.ON_SCROLL -> false
            },
        )

    if (titleTextHasOverflow) {
        MediumTopAppBar(
            windowInsets = windowInsets,
            colors = bitwardenTopAppBarColors(),
            scrollBehavior = scrollBehavior,
            navigationIcon = navigationIconContent,
            collapsedHeight = minimunHeight,
            expandedHeight = 96.dp,
            title = {
                // The height of the component is controlled and will only allow for 1 extra row,
                // making adding any arguments for softWrap and minLines superfluous.
                Text(
                    text = title,
                    style = BitwardenTheme.typography.titleLarge,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag(tag = "PageTitleLabel"),
                )
            },
            modifier = customModifier,
            actions = actions,
        )
    } else {
        TopAppBar(
            windowInsets = windowInsets,
            colors = bitwardenTopAppBarColors(),
            scrollBehavior = scrollBehavior,
            navigationIcon = navigationIconContent,
            expandedHeight = minimunHeight,
            title = {
                Text(
                    text = title,
                    style = BitwardenTheme.typography.titleLarge,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag(tag = "PageTitleLabel"),
                    onTextLayout = {
                        titleTextHasOverflow = it.hasVisualOverflow
                    },
                )
            },
            modifier = customModifier,
            actions = actions,
        )
    }
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
            navigationIcon = NavigationIcon(
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = { },
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun BitwardenTopAppBarOverflow_preview() {
    BitwardenTheme {
        BitwardenTopAppBar(
            title = "Title that is too long for the top line",
            scrollBehavior = TopAppBarDefaults
                .exitUntilCollapsedScrollBehavior(
                    rememberTopAppBarState(),
                ),
            navigationIcon = NavigationIcon(
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = { },
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun BitwardenTopAppBarOverflowCutoff_preview() {
    BitwardenTheme {
        BitwardenTopAppBar(
            title = "Title that is too long for the top line and the bottom line",
            scrollBehavior = TopAppBarDefaults
                .exitUntilCollapsedScrollBehavior(
                    rememberTopAppBarState(),
                ),
            navigationIcon = NavigationIcon(
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = { },
            ),
        )
    }
}

/**
 * Represents all data required to display a [navigationIcon].
 *
 * @property navigationIcon The [Painter] displayed as part of the icon.
 * @property navigationIconContentDescription The content description associated with the icon.
 * @property onNavigationIconClick The click action that is invoked when the icon is tapped.
 */
data class NavigationIcon(
    val navigationIcon: Painter,
    val navigationIconContentDescription: String,
    val onNavigationIconClick: () -> Unit,
)
