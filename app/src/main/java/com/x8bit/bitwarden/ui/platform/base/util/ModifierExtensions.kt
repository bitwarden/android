package com.x8bit.bitwarden.ui.platform.base.util

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * Adds a performance-optimized background color specified by the given [topAppBarScrollBehavior]
 * and its current scroll state.
 */
@OmitFromCoverage
@OptIn(ExperimentalMaterial3Api::class)
@Stable
@Composable
fun Modifier.scrolledContainerBackground(
    topAppBarScrollBehavior: TopAppBarScrollBehavior,
): Modifier {
    val expandedColor = MaterialTheme.colorScheme.surface
    val collapsedColor = MaterialTheme.colorScheme.surfaceContainer
    return this then drawBehind {
        drawRect(
            color = topAppBarScrollBehavior.toScrolledContainerColor(
                expandedColor = expandedColor,
                collapsedColor = collapsedColor,
            ),
        )
    }
}
