package com.bitwarden.ui.platform.navigation.util

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.snapshotFlow
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.NavigationControllerImpl
import timber.log.Timber

/**
 * Pops entries off of the back stack until the most recent entry of type [Route] is on top. If
 * [inclusive] is `true`, the entry of type [Route] is removed as well (unless it is the last
 * remaining entry).
 */
inline fun <reified Route : NavKey> NavigationController<in Route>.popUpTo(
    inclusive: Boolean = false,
): Boolean = this.popUpTo(inclusive = inclusive) { it is Route }

/**
 * Remembers the decorated [NavEntry]s for the back stack of this [NavigationController], which
 * can then be displayed by a `NavDisplay`.
 *
 * The state of each entry (including its saveable state and ViewModels) is owned by the
 * composition this is called from, rather than by the `NavDisplay` itself. This allows the state
 * to be retained while the `NavDisplay` is not being displayed, such as when it is hosted by an
 * unselected tab, and it is cleared once the entry is popped off of the backstack.
 */
@Composable
fun <Route : NavKey> NavigationController<Route>.rememberNavEntries(
    entryProvider: (key: Route) -> NavEntry<Route>,
): List<NavEntry<Route>> = rememberDecoratedNavEntries(
    backStack = this.backStack,
    entryDecorators = rememberBitwardenNavEntryDecorators(),
    entryProvider = entryProvider,
)

/**
 * Remembers the decorated [NavEntry]s for the back stack of this [NavigationController] (see
 * [rememberNavEntries]) as [NavEntry]s of any [NavKey].
 *
 * This allows the entries of multiple back stacks, each belonging to a different navigation graph,
 * to be displayed together by a single `NavDisplay`, such as when following the "multiple back
 * stacks" pattern for the tabs of a navigation bar. The entries of each back stack are still
 * decorated independently, so the state of each back stack is retained while it is not displayed.
 */
@Composable
fun <Route : NavKey> NavigationController<Route>.rememberNavKeyEntries(
    entryProvider: (key: Route) -> NavEntry<Route>,
): List<NavEntry<NavKey>> {
    val keys = this.backStack.toList()
    val entries = this.rememberNavEntries(entryProvider = entryProvider)
    return keys.zip(other = entries) { key, entry ->
        NavEntry(
            key = key,
            contentKey = entry.contentKey,
            metadata = entry.metadata,
            content = { entry.Content() },
        )
    }
}

/**
 * Creates a [NavigationController] whose backstack initially contains the given [routes].
 *
 * The backstack of the returned controller is not saved. Within a composition, use
 * [rememberNavigationController] instead.
 */
@VisibleForTesting
fun <T : NavKey> navigationControllerOf(
    vararg routes: T,
): NavigationController<T> = NavigationControllerImpl(
    backStack = NavBackStack(elements = routes),
)

/**
 * Remember a [NavigationController] across compositions and process death.
 */
@Composable
inline fun <reified T : NavKey> rememberNavigationController(
    vararg routes: T,
): NavigationController<T> {
    val backStack = rememberSerializable(
        serializer = NavBackStackSerializer(elementSerializer = NavKeySerializer()),
    ) {
        NavBackStack(elements = routes)
    }
    LaunchedEffect(key1 = backStack) {
        snapshotFlow { backStack.toList() }.collect {
            val currentStack = it.joinToString(separator = " -> ") { entry ->
                entry::class.simpleName ?: "Unknown"
            }
            Timber.d(message = "${T::class.simpleName}: $currentStack")
        }
    }
    return rememberNavigationController(backStack = backStack)
}

/**
 * Remember a [NavigationController] across compositions and process death.
 */
@Composable
fun <T : NavKey> rememberNavigationController(
    backStack: NavBackStack<T>,
): NavigationController<T> = remember(key1 = backStack) {
    NavigationControllerImpl(backStack = backStack)
}
