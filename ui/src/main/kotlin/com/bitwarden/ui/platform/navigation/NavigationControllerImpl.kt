package com.bitwarden.ui.platform.navigation

import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow

/**
 * The default implementation of the [NavigationController], which manages the given [backStack].
 */
internal class NavigationControllerImpl<T : NavKey>(
    override val backStack: NavBackStack<T>,
) : NavigationController<T> {
    override val backStackSnapshot: Flow<List<T>> get() = snapshotFlow { backStack.toList() }

    override fun navigate(route: T) {
        backStack.add(element = route)
    }

    override fun navigateBack(): Boolean = if (backStack.size <= 1) {
        false
    } else {
        backStack.removeAt(index = backStack.lastIndex)
        true
    }

    override fun addSingleTop(route: T) {
        if (backStack.lastOrNull()?.let { it::class } == route::class) return
        backStack.add(element = route)
    }

    override fun popUpTo(
        inclusive: Boolean,
        predicate: (T) -> Boolean,
    ): Boolean {
        val index = backStack.indexOfLast(predicate = predicate)
        return if (index == -1) {
            false
        } else {
            val newSize = (if (inclusive) index else index + 1).coerceAtLeast(minimumValue = 1)
            if (newSize < backStack.size) backStack.subList(newSize, backStack.size).clear()
            true
        }
    }

    override fun popToRoot() {
        backStack.subList(fromIndex = 1, toIndex = backStack.size).clear()
    }

    override fun navigatePoppingUpTo(
        route: T,
        inclusive: Boolean,
        predicate: (T) -> Boolean,
    ) {
        val index = backStack.indexOfLast(predicate = predicate)
        Snapshot.withMutableSnapshot {
            if (index != -1) {
                backStack
                    .subList(
                        fromIndex = if (inclusive) index else index + 1,
                        toIndex = backStack.size,
                    )
                    .clear()
            }
            backStack.add(element = route)
        }
    }

    override fun navigateToTopLevelRoute(route: T) {
        val startRoute = backStack.first()
        if (route == startRoute) {
            this.replaceAll(routes = listOf(startRoute))
        } else {
            this.replaceAll(routes = listOf(startRoute, route))
        }
    }

    override fun replaceAll(routes: List<T>) {
        require(value = routes.isNotEmpty()) {
            "A back stack cannot be replaced with an empty list."
        }
        Snapshot.withMutableSnapshot {
            backStack.clear()
            backStack.addAll(elements = routes)
        }
    }

    override fun replaceAll(vararg routes: T) {
        this.replaceAll(routes = routes.toList())
    }

    override fun replaceLast(route: T) {
        backStack[backStack.lastIndex] = route
    }
}
