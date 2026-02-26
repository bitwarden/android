package com.bitwarden.ui.platform.base.util

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import com.bitwarden.ui.platform.theme.TransitionProviders
import kotlin.reflect.KType

/**
 * A wrapper around [NavGraphBuilder.composable] that supplies slide up/down transitions.
 */
inline fun <reified T : Any> NavGraphBuilder.composableWithSlideTransitions(
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    this.composable<T>(
        typeMap = typeMap,
        deepLinks = deepLinks,
        enterTransition = TransitionProviders.Enter.slideUp,
        exitTransition = TransitionProviders.Exit.stay,
        popEnterTransition = TransitionProviders.Enter.stay,
        popExitTransition = TransitionProviders.Exit.slideDown,
        sizeTransform = null,
        content = content,
    )
}

/**
 * A wrapper around [NavGraphBuilder.composable] that supplies "stay" transitions.
 */
inline fun <reified T : Any> NavGraphBuilder.composableWithStayTransitions(
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    this.composable<T>(
        typeMap = typeMap,
        deepLinks = deepLinks,
        enterTransition = TransitionProviders.Enter.stay,
        exitTransition = TransitionProviders.Exit.stay,
        popEnterTransition = TransitionProviders.Enter.stay,
        popExitTransition = TransitionProviders.Exit.stay,
        sizeTransform = null,
        content = content,
    )
}

/**
 * A wrapper around [NavGraphBuilder.composable] that supplies push transitions.
 *
 * This is suitable for screens deeper within a hierarchy that uses push transitions; the root
 * screen of such a hierarchy should use [composableWithRootPushTransitions].
 */
inline fun <reified T : Any> NavGraphBuilder.composableWithPushTransitions(
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    this.composable<T>(
        typeMap = typeMap,
        deepLinks = deepLinks,
        enterTransition = TransitionProviders.Enter.pushToStart,
        exitTransition = TransitionProviders.Exit.stay,
        popEnterTransition = TransitionProviders.Enter.stay,
        popExitTransition = TransitionProviders.Exit.pushToEnd,
        sizeTransform = null,
        content = content,
    )
}

/**
 * A wrapper around [NavGraphBuilder.composable] that supplies push transitions to the root screen
 * in a nested graph that uses push transitions.
 */
inline fun <reified T : Any> NavGraphBuilder.composableWithRootPushTransitions(
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    this.composable<T>(
        typeMap = typeMap,
        deepLinks = deepLinks,
        enterTransition = TransitionProviders.Enter.stay,
        exitTransition = TransitionProviders.Exit.pushToStart,
        popEnterTransition = TransitionProviders.Enter.pushToEnd,
        popExitTransition = TransitionProviders.Exit.fadeOut,
        sizeTransform = null,
        content = content,
    )
}
