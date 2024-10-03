package com.x8bit.bitwarden.ui.platform.components.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Direct passthrough to [Scaffold] but contains a few specific override values. Everything is
 * still overridable if necessary.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun BitwardenScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = { },
    bottomBar: @Composable () -> Unit = { },
    snackbarHost: @Composable () -> Unit = { },
    floatingActionButton: @Composable () -> Unit = { },
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    pullToRefreshState: BitwardenPullToRefreshState = rememberBitwardenPullToRefreshState(),
    containerColor: Color = BitwardenTheme.colorScheme.background.primary,
    contentColor: Color = BitwardenTheme.colorScheme.text.primary,
    contentWindowInsets: WindowInsets = ScaffoldDefaults
        .contentWindowInsets
        .exclude(WindowInsets.navigationBars),
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .then(modifier),
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = {
            Box(modifier = Modifier.navigationBarsPadding()) {
                floatingActionButton()
            }
        },
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = contentWindowInsets,
        content = { paddingValues ->
            val internalPullToRefreshState = rememberPullToRefreshState()
            Box(
                modifier = Modifier.pullToRefresh(
                    state = internalPullToRefreshState,
                    isRefreshing = pullToRefreshState.isRefreshing,
                    onRefresh = pullToRefreshState.onRefresh,
                    enabled = pullToRefreshState.isEnabled,
                ),
            ) {
                content(paddingValues)

                PullToRefreshDefaults.Indicator(
                    modifier = Modifier
                        .padding(paddingValues)
                        .align(Alignment.TopCenter),
                    isRefreshing = pullToRefreshState.isRefreshing,
                    state = internalPullToRefreshState,
                    containerColor = BitwardenTheme.colorScheme.background.secondary,
                    color = BitwardenTheme.colorScheme.icon.secondary,
                )
            }
        },
    )
}

/**
 * The state of the pull-to-refresh.
 */
data class BitwardenPullToRefreshState(
    val isEnabled: Boolean,
    val isRefreshing: Boolean,
    val onRefresh: () -> Unit,
)

/**
 * Create and remember the default [BitwardenPullToRefreshState].
 */
@Composable
fun rememberBitwardenPullToRefreshState(
    isEnabled: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = { },
): BitwardenPullToRefreshState = remember(isEnabled, isRefreshing, onRefresh) {
    BitwardenPullToRefreshState(
        isEnabled = isEnabled,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
    )
}
