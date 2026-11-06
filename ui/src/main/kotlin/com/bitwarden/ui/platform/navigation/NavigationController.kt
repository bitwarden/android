package com.bitwarden.ui.platform.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow

/**
 * Controls navigation within a single backstack of [NavKey] routes.
 *
 * The [backStack] is observable state that should be displayed by a `NavDisplay`, while all
 * changes to it should be made through this controller.
 */
interface NavigationController<T : NavKey> {
    /**
     * The current backstack, ordered from the root entry to the top entry. This list is backed by
     * snapshot state, so reading it within a composition will observe every change.
     */
    val backStack: List<T>

    /**
     * Creates an observable snapshot flow of the backstack, indicating any changes that is made to
     * it.
     */
    val backStackSnapshot: Flow<List<T>>

    /**
     * Navigates to the given [route] by adding it to the top of the backstack.
     */
    fun navigate(route: T)

    /**
     * Navigates back by removing the top entry from the backstack.
     *
     * The last remaining entry is never removed, since a backstack being displayed must never be
     * empty.
     *
     * @return `true` if an entry was removed from the backstack.
     */
    fun navigateBack(): Boolean

    /**
     * Adds the given [route] to the top of the backstack unless an entry of the same type is
     * already on top of the backstack. This ensures there is at most one copy of a given
     * destination on the top of the backstack.
     */
    fun addSingleTop(route: T)

    /**
     * Pops entries off of the backstack until the most recent entry matching the [predicate] is
     * on top. If [inclusive] is `true`, the matching entry is removed as well (unless it is the
     * last remaining entry).
     *
     * @return `true` if an entry matching the [predicate] was found in the backstack.
     */
    fun popUpTo(inclusive: Boolean = false, predicate: (T) -> Boolean): Boolean

    /**
     * Pops all entries off the backstack until only the root entry remains. This does nothing if
     * only the root entry exists.
     */
    fun popToRoot()

    /**
     * Atomically pops entries off of the backstack until the most recent entry matching the
     * [predicate] is on top (or removed as well, if [inclusive] is `true`) and then adds the given
     * [route].
     *
     * If there is no entry matching the [predicate] in the backstack, the [route] is simply added.
     */
    fun navigatePoppingUpTo(route: T, inclusive: Boolean = false, predicate: (T) -> Boolean)

    /**
     * Navigates to the given top-level [route], such as a tab of a navigation bar, where the
     * backstack only ever contains top-level routes.
     *
     * The first route of the backstack (the start route) is always retained beneath the given
     * [route], so navigating back from any other top-level route returns to the start route.
     */
    fun navigateToTopLevelRoute(route: T)

    /**
     * Atomically replaces the entire contents of the backstack with the given [routes].
     */
    fun replaceAll(routes: List<T>)

    /**
     * Atomically replaces the entire contents of the backstack with the given [routes].
     */
    fun replaceAll(vararg routes: T)

    /**
     * Atomically replaces the top of the backstack with the given [route].
     */
    fun replaceLast(route: T)
}
