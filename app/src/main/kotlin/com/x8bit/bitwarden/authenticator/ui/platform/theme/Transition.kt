package com.x8bit.bitwarden.authenticator.ui.platform.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost

typealias EnterTransitionProvider =
    (@JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)

typealias ExitTransitionProvider =
    (@JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)

typealias NonNullEnterTransitionProvider =
    (@JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition)

typealias NonNullExitTransitionProvider =
    (@JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition)

/**
 * The default transition time (in milliseconds) for all fade transitions in the
 * [TransitionProviders].
 */
const val DEFAULT_FADE_TRANSITION_TIME_MS: Int = 300

/**
 * The default transition time (in milliseconds) for all slide transitions in the
 * [TransitionProviders].
 */
const val DEFAULT_SLIDE_TRANSITION_TIME_MS: Int = 450

/**
 * The default transition time (in milliseconds) for all slide transitions in the
 * [TransitionProviders].
 */
const val DEFAULT_PUSH_TRANSITION_TIME_MS: Int = 350

/**
 * The default transition time (in milliseconds) for all "stay"/no-op transitions in the
 * [TransitionProviders].
 *
 * This should be at least as large as any other transition that might also be happening during a
 * navigation.
 */
val DEFAULT_STAY_TRANSITION_TIME_MS: Int =
    maxOf(
        DEFAULT_FADE_TRANSITION_TIME_MS,
        DEFAULT_SLIDE_TRANSITION_TIME_MS,
        DEFAULT_PUSH_TRANSITION_TIME_MS,
    )

/**
 * Checks if the parent of the destination before and after the navigation is the same. This is
 * useful to ignore certain enter/exit transitions when navigating between distinct, nested flows.
 */
val AnimatedContentTransitionScope<NavBackStackEntry>.isSameGraphNavigation: Boolean
    get() = initialState.destination.parent == targetState.destination.parent

/**
 * Contains standard "transition providers" that may be used to specify the [EnterTransition] and
 * [ExitTransition] used when building a typical composable destination. These may return `null`
 * values in order to allow transitions between nested navigation graphs to be specified by
 * components higher up in the graph.
 */
object TransitionProviders {
    /**
     * The standard set of "enter" transition providers.
     */
    object Enter {
        /**
         * Fades the new screen in.
         *
         * Note that this represents a `null` transition when navigating between different nested
         * navigation graphs.
         */
        val fadeIn: EnterTransitionProvider = {
            RootTransitionProviders
                .Enter
                .fadeIn(this)
                .takeIf { isSameGraphNavigation }
        }

        /**
         * Slides the new screen in from the left of the screen.
         */
        val pushLeft: EnterTransitionProvider = {
            RootTransitionProviders
                .Enter
                .pushLeft(this)
                .takeIf { isSameGraphNavigation }
        }

        /**
         * Slides the new screen in from the right of the screen.
         */
        val pushRight: EnterTransitionProvider = {
            RootTransitionProviders
                .Enter
                .pushRight(this)
                .takeIf { isSameGraphNavigation }
        }

        /**
         * Slides the new screen in from the bottom of the screen.
         *
         * Note that this represents a `null` transition when navigating between different nested
         * navigation graphs.
         */
        val slideUp: EnterTransitionProvider = {
            RootTransitionProviders
                .Enter
                .slideUp(this)
                .takeIf { isSameGraphNavigation }
        }

        /**
         * A "no-op" transition: this changes nothing about the screen but "lasts" as long as
         * other standard transitions in order to leave the screen in place such that it does not
         * immediately appear while the other screen transitions away.
         *
         * Note that this represents a `null` transition when navigating between different nested
         * navigation graphs.
         */
        val stay: EnterTransitionProvider = {
            RootTransitionProviders
                .Enter
                .stay(this)
                .takeIf { isSameGraphNavigation }
        }
    }

    /**
     * The standard set of "exit" transition providers.
     */
    object Exit {
        /**
         * Fades the current screen out.
         *
         * Note that this represents a `null` transition when navigating between different nested
         * navigation graphs.
         */
        val fadeOut: ExitTransitionProvider = {
            RootTransitionProviders
                .Exit
                .fadeOut(this)
                .takeIf { isSameGraphNavigation }
        }

        /**
         * Slides the current screen out to the left of the screen.
         */
        val pushLeft: ExitTransitionProvider = {
            RootTransitionProviders
                .Exit
                .pushLeft(this)
                .takeIf { isSameGraphNavigation }
        }

        /**
         * Slides the current screen out to the right of the screen.
         */
        val pushRight: ExitTransitionProvider = {
            RootTransitionProviders
                .Exit
                .pushRight(this)
                .takeIf { isSameGraphNavigation }
        }

        /**
         * Slides the current screen down to the bottom of the screen.
         *
         * Note that this represents a `null` transition when navigating between different nested
         * navigation graphs.
         */
        val slideDown: ExitTransitionProvider = {
            RootTransitionProviders
                .Exit
                .slideDown(this)
                .takeIf { isSameGraphNavigation }
        }

        /**
         * A "no-op" transition: this changes nothing about the screen but "lasts" as long as
         * other standard transitions in order to leave the screen in place such that it does not
         * immediately disappear while the other screen transitions into place.
         *
         * Note that this represents a `null` transition when navigating between different nested
         * navigation graphs.
         */
        val stay: ExitTransitionProvider = {
            RootTransitionProviders
                .Exit
                .stay(this)
                .takeIf { isSameGraphNavigation }
        }
    }
}

/**
 * Contains standard "transition providers" that may be used to specify the [EnterTransition] and
 * [ExitTransition] used when building a root [NavHost], which requires a non-null value.
 */
object RootTransitionProviders {
    /**
     * The standard set of "enter" transition providers.
     */
    object Enter {
        /**
         * Fades the new screen in.
         */
        val fadeIn: NonNullEnterTransitionProvider = {
            fadeIn(tween(DEFAULT_FADE_TRANSITION_TIME_MS))
        }

        /**
         * There is no transition for the entering screen.
         */
        val none: NonNullEnterTransitionProvider = {
            EnterTransition.None
        }

        /**
         * Slides the new screen in from the left of the screen.
         */
        val pushLeft: NonNullEnterTransitionProvider = {
            val totalTransitionDurationMs = DEFAULT_PUSH_TRANSITION_TIME_MS
            slideInHorizontally(
                animationSpec = tween(durationMillis = totalTransitionDurationMs),
                initialOffsetX = { fullWidth -> fullWidth / 2 },
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = totalTransitionDurationMs / 2,
                    delayMillis = totalTransitionDurationMs / 2,
                ),
            )
        }

        /**
         * Slides the new screen in from the right of the screen.
         */
        val pushRight: NonNullEnterTransitionProvider = {
            val totalTransitionDurationMs = DEFAULT_PUSH_TRANSITION_TIME_MS
            slideInHorizontally(
                animationSpec = tween(durationMillis = totalTransitionDurationMs),
                initialOffsetX = { fullWidth -> -fullWidth / 2 },
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = totalTransitionDurationMs / 2,
                    delayMillis = totalTransitionDurationMs / 2,
                ),
            )
        }

        /**
         * Slides the new screen in from the bottom of the screen.
         */
        val slideUp: NonNullEnterTransitionProvider = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Up,
                animationSpec = tween(DEFAULT_SLIDE_TRANSITION_TIME_MS),
            )
        }

        /**
         * A "no-op" transition: this changes nothing about the screen but "lasts" as long as
         * other standard transitions in order to leave the screen in place such that it does not
         * immediately appear while the other screen transitions away.
         */
        val stay: NonNullEnterTransitionProvider = {
            fadeIn(
                animationSpec = tween(DEFAULT_STAY_TRANSITION_TIME_MS),
                initialAlpha = 1f,
            )
        }
    }

    /**
     * The standard set of "exit" transition providers.
     */
    object Exit {
        /**
         * Fades the current screen out.
         */
        val fadeOut: NonNullExitTransitionProvider = {
            fadeOut(tween(DEFAULT_FADE_TRANSITION_TIME_MS))
        }

        /**
         * There is no transition for the exiting screen.
         *
         * Unlike the [stay] transition, this will immediately remove the outgoing screen even if
         * there is an ongoing enter transition happening for the new screen.
         */
        val none: NonNullExitTransitionProvider = {
            ExitTransition.None
        }

        /**
         * Slides the current screen out to the left of the screen.
         */
        @Suppress("MagicNumber")
        val pushLeft: NonNullExitTransitionProvider = {
            val totalTransitionDurationMs = DEFAULT_PUSH_TRANSITION_TIME_MS
            val delayMs = totalTransitionDurationMs / 7
            val slideWithoutDelayMs = totalTransitionDurationMs - delayMs
            slideOutHorizontally(
                animationSpec = tween(
                    durationMillis = slideWithoutDelayMs,
                    delayMillis = delayMs,
                ),
                targetOffsetX = { fullWidth -> -fullWidth / 2 },
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = totalTransitionDurationMs / 2,
                    delayMillis = delayMs,
                ),
            )
        }

        /**
         * Slides the current screen out to the right of the screen.
         */
        @Suppress("MagicNumber")
        val pushRight: NonNullExitTransitionProvider = {
            val totalTransitionDurationMs = DEFAULT_PUSH_TRANSITION_TIME_MS
            val delayMs = totalTransitionDurationMs / 7
            val slideWithoutDelayMs = totalTransitionDurationMs - delayMs
            slideOutHorizontally(
                animationSpec = tween(
                    durationMillis = slideWithoutDelayMs,
                    delayMillis = delayMs,
                ),
                targetOffsetX = { fullWidth -> fullWidth / 2 },
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = totalTransitionDurationMs / 2,
                    delayMillis = delayMs,
                ),
            )
        }

        /**
         * Slides the current screen down to the bottom of the screen.
         */
        val slideDown: NonNullExitTransitionProvider = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Down,
                animationSpec = tween(DEFAULT_SLIDE_TRANSITION_TIME_MS),
            )
        }

        /**
         * A "no-op" transition: this changes nothing about the screen but "lasts" as long as
         * other standard transitions in order to leave the screen in place such that it does not
         * immediately disappear while the other screen transitions into place.
         */
        val stay: NonNullExitTransitionProvider = {
            fadeOut(
                animationSpec = tween(DEFAULT_STAY_TRANSITION_TIME_MS),
                targetAlpha = 0.99f,
            )
        }
    }
}
