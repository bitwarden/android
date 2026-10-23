package com.bitwarden.ui.platform.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

typealias EnterTransitionProvider =
    @JvmSuppressWildcards AnimatedContentTransitionScope<*>.() -> EnterTransition

typealias ExitTransitionProvider =
    @JvmSuppressWildcards AnimatedContentTransitionScope<*>.() -> ExitTransition

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
val DEFAULT_STAY_TRANSITION_TIME_MS: Int = maxOf(
    DEFAULT_FADE_TRANSITION_TIME_MS,
    DEFAULT_SLIDE_TRANSITION_TIME_MS,
    DEFAULT_PUSH_TRANSITION_TIME_MS,
)

/**
 * Contains the standard, reusable "transition providers" that are used to build the
 * [EntryTransitions] for navigation entries and the default transitions of a navigation display.
 */
object TransitionProviders {
    /**
     * The standard set of "enter" transition providers.
     */
    object Enter {
        /**
         * Fades the new screen in.
         */
        val fadeIn: EnterTransitionProvider = {
            fadeIn(animationSpec = tween(durationMillis = DEFAULT_FADE_TRANSITION_TIME_MS))
        }

        /**
         * There is no transition for the entering screen.
         */
        val none: EnterTransitionProvider = {
            EnterTransition.None
        }

        /**
         * Slides the new screen in from the start (left) of the screen towards the end (right).
         */
        val pushToEnd: EnterTransitionProvider = {
            val totalTransitionDurationMs = DEFAULT_PUSH_TRANSITION_TIME_MS
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(durationMillis = totalTransitionDurationMs),
                initialOffset = { fullWidth -> fullWidth / 2 },
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = totalTransitionDurationMs / 2,
                    delayMillis = totalTransitionDurationMs / 2,
                ),
            )
        }

        /**
         * Slides the new screen in from the end (right) of the screen towards the start (left).
         */
        val pushToStart: EnterTransitionProvider = {
            val totalTransitionDurationMs = DEFAULT_PUSH_TRANSITION_TIME_MS
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(durationMillis = totalTransitionDurationMs),
                initialOffset = { fullWidth -> fullWidth / 2 },
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
        val slideUp: EnterTransitionProvider = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Up,
                animationSpec = tween(durationMillis = DEFAULT_SLIDE_TRANSITION_TIME_MS),
            )
        }

        /**
         * A "no-op" transition: this changes nothing about the screen but "lasts" as long as
         * other standard transitions in order to leave the screen in place such that it does not
         * immediately appear while the other screen transitions away.
         */
        val stay: EnterTransitionProvider = {
            fadeIn(
                animationSpec = tween(durationMillis = DEFAULT_STAY_TRANSITION_TIME_MS),
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
        val fadeOut: ExitTransitionProvider = {
            fadeOut(animationSpec = tween(durationMillis = DEFAULT_FADE_TRANSITION_TIME_MS))
        }

        /**
         * There is no transition for the exiting screen.
         *
         * Unlike the [TransitionProviders.Exit.stay] transition, this will immediately remove
         * the outgoing screen even if there is an ongoing enter transition happening for the new
         * screen.
         */
        val none: ExitTransitionProvider = {
            ExitTransition.None
        }

        /**
         * Slides the current screen out to the start (left) of the screen towards the end (right).
         */
        @Suppress("MagicNumber")
        val pushToStart: ExitTransitionProvider = {
            val totalTransitionDurationMs = DEFAULT_PUSH_TRANSITION_TIME_MS
            val delayMs = totalTransitionDurationMs / 7
            val slideWithoutDelayMs = totalTransitionDurationMs - delayMs
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(
                    durationMillis = slideWithoutDelayMs,
                    delayMillis = delayMs,
                ),
                targetOffset = { fullWidth -> fullWidth / 2 },
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = totalTransitionDurationMs / 2,
                    delayMillis = delayMs,
                ),
            )
        }

        /**
         * Slides the current screen out to the end (right) of the screen towards the start (left).
         */
        @Suppress("MagicNumber")
        val pushToEnd: ExitTransitionProvider = {
            val totalTransitionDurationMs = DEFAULT_PUSH_TRANSITION_TIME_MS
            val delayMs = totalTransitionDurationMs / 7
            val slideWithoutDelayMs = totalTransitionDurationMs - delayMs
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(
                    durationMillis = slideWithoutDelayMs,
                    delayMillis = delayMs,
                ),
                targetOffset = { fullWidth -> fullWidth / 2 },
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
        val slideDown: ExitTransitionProvider = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Down,
                animationSpec = tween(durationMillis = DEFAULT_SLIDE_TRANSITION_TIME_MS),
            )
        }

        /**
         * A "no-op" transition: this changes nothing about the screen but "lasts" as long as
         * other standard transitions in order to leave the screen in place such that it does not
         * immediately disappear while the other screen transitions into place.
         */
        val stay: ExitTransitionProvider = {
            fadeOut(
                animationSpec = tween(durationMillis = DEFAULT_STAY_TRANSITION_TIME_MS),
                targetAlpha = 0.99f,
            )
        }
    }
}
