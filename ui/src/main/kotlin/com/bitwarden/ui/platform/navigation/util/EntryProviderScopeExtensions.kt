package com.bitwarden.ui.platform.navigation.util

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.bitwarden.ui.platform.theme.EntryTransitions
import com.bitwarden.ui.platform.theme.RootTransition

/**
 * Adds the [Composable] [content] entry to the [EntryProviderScope] using a
 * [EntryTransitions.push] transition.
 */
inline fun <reified T : NavKey> EntryProviderScope<in T>.pushEntry(
    noinline clazzContentKey: (key: T) -> Any = { defaultContentKey(key = it) },
    noinline content: @Composable (T) -> Unit,
) {
    entry<T>(
        clazzContentKey = clazzContentKey,
        metadata = EntryTransitions.push,
        content = content,
    )
}

/**
 * Adds the [Composable] [content] entry to the [EntryProviderScope] using a
 * [EntryTransitions.slideUp] transition.
 */
inline fun <reified T : NavKey> EntryProviderScope<in T>.slideUpEntry(
    noinline clazzContentKey: (key: T) -> Any = { defaultContentKey(key = it) },
    noinline content: @Composable (T) -> Unit,
) {
    entry<T>(
        clazzContentKey = clazzContentKey,
        metadata = EntryTransitions.slideUp,
        content = content,
    )
}

/**
 * Adds the [Composable] [content] entry to the [EntryProviderScope] using a
 * [EntryTransitions.stay] transition.
 */
inline fun <reified T : NavKey> EntryProviderScope<in T>.stayEntry(
    noinline clazzContentKey: (key: T) -> Any = { defaultContentKey(key = it) },
    noinline content: @Composable (T) -> Unit,
) {
    entry<T>(
        clazzContentKey = clazzContentKey,
        metadata = EntryTransitions.stay,
        content = content,
    )
}

/**
 * Adds the [Composable] [content] entry to the [EntryProviderScope] using a
 * [EntryTransitions.rootPush] transition. This should be used for the root of a flow.
 */
inline fun <reified T : NavKey> EntryProviderScope<in T>.rootPushEntry(
    noinline clazzContentKey: (key: T) -> Any = { defaultContentKey(key = it) },
    noinline content: @Composable (T) -> Unit,
) {
    entry<T>(
        clazzContentKey = clazzContentKey,
        metadata = EntryTransitions.rootPush,
        content = content,
    )
}

/**
 * Adds the [Composable] [content] entry to the [EntryProviderScope] using a
 * [EntryTransitions.rootTransition] with the specified [RootTransition] type.
 */
inline fun <reified T : NavKey> EntryProviderScope<in T>.rootTransitionEntry(
    rootTransition: RootTransition,
    noinline clazzContentKey: (key: T) -> Any = { defaultContentKey(key = it) },
    noinline content: @Composable (T) -> Unit,
) {
    entry<T>(
        clazzContentKey = clazzContentKey,
        metadata = EntryTransitions.rootTransition(rootTransition = rootTransition),
        content = content,
    )
}

private fun defaultContentKey(key: Any): String = "$key:${key::class}"
