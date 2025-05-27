package com.x8bit.bitwarden.ui.platform.components.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.bitwarden.ui.platform.base.util.toDp
import com.bitwarden.ui.platform.components.navigation.BitwardenBottomAppBar
import com.bitwarden.ui.platform.components.navigation.BitwardenNavigationRail
import com.bitwarden.ui.platform.model.WindowSize
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.platform.util.rememberWindowSize
import com.x8bit.bitwarden.ui.platform.components.model.BitwardenPullToRefreshState
import com.x8bit.bitwarden.ui.platform.components.model.ScaffoldNavigationData
import com.x8bit.bitwarden.ui.platform.components.model.rememberBitwardenPullToRefreshState
import com.x8bit.bitwarden.ui.platform.components.scrim.BitwardenAnimatedScrim

/**
 * Direct passthrough to [Scaffold] but contains a few specific override values. Everything is
 * still overridable if necessary.
 *
 * The [utilityBar] is a nonstandard [Composable] that is placed below the [topBar] and does not
 * scroll.
 * The [overlay] is a nonstandard [Composable] that is placed over top the `utilityBar` and
 * `content`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun BitwardenScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = { },
    utilityBar: @Composable () -> Unit = { },
    overlay: @Composable () -> Unit = { },
    snackbarHost: @Composable () -> Unit = { },
    floatingActionButton: @Composable () -> Unit = { },
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    navigationData: ScaffoldNavigationData? = null,
    pullToRefreshState: BitwardenPullToRefreshState = rememberBitwardenPullToRefreshState(),
    containerColor: Color = BitwardenTheme.colorScheme.background.primary,
    contentColor: Color = BitwardenTheme.colorScheme.text.primary,
    contentWindowInsets: WindowInsets = ScaffoldDefaults
        .contentWindowInsets
        .union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Horizontal),
    content: @Composable () -> Unit,
) {
    val windowSize = rememberWindowSize()
    val hasNavigationItems = !navigationData?.navigationItems.isNullOrEmpty()
    val isNavigationRailVisible = windowSize != WindowSize.Compact && hasNavigationItems
    val isNavigationBarVisible = windowSize == WindowSize.Compact && hasNavigationItems
    Scaffold(
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .then(other = modifier),
        topBar = topBar,
        bottomBar = {
            if (isNavigationBarVisible) {
                ScaffoldBottomAppBar(navigationData = navigationData)
            }
        },
        snackbarHost = {
            Box(modifier = Modifier.imePadding()) {
                snackbarHost()
            }
        },
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = WindowInsets(0.dp),
        content = { paddingValues ->
            Row(
                modifier = Modifier
                    .padding(paddingValues = paddingValues)
                    .consumeWindowInsets(paddingValues = paddingValues)
                    .imePadding(),
            ) {
                if (isNavigationRailVisible) {
                    ScaffoldNavigationRail(navigationData = navigationData)
                }
                Box(
                    modifier = Modifier.run {
                        if (isNavigationRailVisible) {
                            consumeWindowInsets(
                                insets = WindowInsets.displayCutout.only(WindowInsetsSides.Start),
                            )
                        } else if (isNavigationBarVisible) {
                            consumeWindowInsets(
                                insets = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
                            )
                        } else {
                            this
                        }
                    },
                ) {
                    Column {
                        utilityBar()
                        val internalPullToRefreshState = rememberPullToRefreshState()
                        Box(
                            modifier = Modifier
                                .windowInsetsPadding(insets = contentWindowInsets)
                                .pullToRefresh(
                                    state = internalPullToRefreshState,
                                    isRefreshing = pullToRefreshState.isRefreshing,
                                    onRefresh = pullToRefreshState.onRefresh,
                                    enabled = pullToRefreshState.isEnabled,
                                ),
                        ) {
                            content()

                            PullToRefreshDefaults.Indicator(
                                modifier = Modifier.align(Alignment.TopCenter),
                                isRefreshing = pullToRefreshState.isRefreshing,
                                state = internalPullToRefreshState,
                                containerColor = BitwardenTheme.colorScheme.background.secondary,
                                color = BitwardenTheme.colorScheme.icon.secondary,
                            )
                        }
                    }
                    overlay()
                }
            }
        },
    )
}

@Composable
private fun ScaffoldBottomAppBar(
    navigationData: ScaffoldNavigationData,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        var appBarHeightPx by remember { mutableIntStateOf(0) }
        BitwardenBottomAppBar(
            navigationItems = navigationData.navigationItems,
            selectedItem = navigationData.selectedNavigationItem,
            onClick = navigationData.onNavigationClick,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { appBarHeightPx = it.size.height }
                .testTag(tag = "NavigationBarContainer"),
        )
        BitwardenAnimatedScrim(
            isVisible = navigationData.shouldDimNavigation,
            onClick = {
                // Do nothing
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(height = appBarHeightPx.toDp()),
        )
    }
}

@Composable
private fun ScaffoldNavigationRail(
    navigationData: ScaffoldNavigationData,
    modifier: Modifier = Modifier,
) {
    // We set the z-index to 1f in order to make sure the content transitions
    // animate in under the navigation rail.
    Box(
        modifier = modifier
            .fillMaxHeight()
            .zIndex(zIndex = 1f),
    ) {
        var appBarWidthPx by remember { mutableIntStateOf(0) }
        BitwardenNavigationRail(
            navigationItems = navigationData.navigationItems,
            selectedItem = navigationData.selectedNavigationItem,
            onClick = navigationData.onNavigationClick,
            modifier = Modifier
                .fillMaxHeight()
                .wrapContentWidth()
                .onGloballyPositioned { appBarWidthPx = it.size.width }
                .testTag(tag = "NavigationBarContainer"),
        )
        BitwardenAnimatedScrim(
            isVisible = navigationData.shouldDimNavigation,
            onClick = {
                // Do nothing
            },
            modifier = Modifier
                .fillMaxHeight()
                .width(width = appBarWidthPx.toDp()),
        )
    }
}
