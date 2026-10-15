package com.bitwarden.ui.platform.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.contains
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay

typealias ContentTransformProvider<T> =
    AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform

typealias PredictiveContentTransformProvider<T> =
    AnimatedContentTransitionScope<Scene<T>>.(
        swipeEdge: Int,
    ) -> ContentTransform

/**
 * Reusable [NavEntry.metadata] that defines how an entry animates as it is pushed onto and popped
 * off of a backstack.
 */
object EntryTransitions {
    /**
     * Slides the entry up from the bottom of the screen when it is pushed and back down when it is
     * popped. The entry beneath it stays in place.
     */
    val slideUp: Map<String, Any> = entryTransitions(
        enter = TransitionProviders.Enter.slideUp,
        popExit = TransitionProviders.Exit.slideDown,
    )

    /**
     * Keeps the entry in place when it is pushed or popped.
     */
    val stay: Map<String, Any> = entryTransitions(
        enter = TransitionProviders.Enter.stay,
        popExit = TransitionProviders.Exit.stay,
    )

    /**
     * Pushes the entry in from the end of the screen when it is pushed and back towards the end
     * when it is popped.
     *
     * This is suitable for screens deeper within a hierarchy that uses push transitions; the root
     * screen of such a hierarchy should use [rootPush].
     */
    val push: Map<String, Any> = entryTransitions(
        enter = TransitionProviders.Enter.pushToStart,
        popExit = TransitionProviders.Exit.pushToEnd,
    )

    /**
     * Marks the entry as the root of a flow that uses [push] transitions, such as the root screen
     * of a tab. The root is pushed towards the start of the screen when another entry is pushed on
     * top of it and is pushed back in from the start when that entry is popped.
     *
     * Because the entry starts a new flow, navigating to or from the root itself uses the default
     * transitions of the hosting [NavDisplay].
     */
    val rootPush: Map<String, Any> = metadata {
        put(FlowRootKey, true)
        put(CoveredExitTransitionKey, TransitionProviders.Exit.pushToStart)
        put(RevealedEnterTransitionKey, TransitionProviders.Enter.pushToEnd)
    } + entryTransitions(
        enter = TransitionProviders.Enter.stay,
        popExit = TransitionProviders.Exit.fadeOut,
    )

    /**
     * Specifies the [RootTransition] used when the back stack beginning with this entry replaces
     * (or is replaced by) another back stack within a [NavDisplay] using
     * [NavDisplayTransitions.root].
     *
     * This only has an effect when applied to the first entry of a back stack.
     */
    fun rootTransition(rootTransition: RootTransition): Map<String, Any> = metadata {
        put(RootTransitionKey, rootTransition)
    }
}

/**
 * Describes how a back stack animates when it replaces, or is replaced by, another back stack
 * within a [NavDisplay] using [NavDisplayTransitions.root].
 */
enum class RootTransition {
    /**
     * Fades the back stack in and out. This is the default behavior.
     */
    FADE,

    /**
     * The back stack appears without any animation and disappears without any animation.
     */
    NONE,

    /**
     * Pushes the back stack in from the end of the screen. When replacing another back stack using
     * this transition, the replaced back stack stays in place.
     */
    PUSH,

    /**
     * Slides the back stack up from the bottom of the screen while the replaced back stack stays
     * in place, and slides it back down when it is replaced.
     */
    SLIDE_UP,
}

/**
 * Reusable default transitions for a [NavDisplay]. These are used whenever the transitioning
 * entries do not provide their own [EntryTransitions].
 */
object NavDisplayTransitions {
    /**
     * Fades between scenes.
     */
    fun <T : Any> fade(): ContentTransformProvider<T> = {
        TransitionProviders.Enter.fadeIn(this) togetherWith TransitionProviders.Exit.fadeOut(this)
    }

    /**
     * Transitions for a "root" [NavDisplay] whose back stack is entirely replaced as the user moves
     * between the high level flows of the app. Each back stack declares how it animates using
     * [EntryTransitions.rootTransition] on its first entry.
     *
     * Entries without their own [EntryTransitions] do not animate when navigating within a flow.
     */
    fun <T : Any> root(): ContentTransformProvider<T> = {
        if (isSameFlowNavigation) {
            TransitionProviders.Enter.none(this) togetherWith TransitionProviders.Exit.none(this)
        } else {
            val initialTransition = initialState.rootTransition
            val targetTransition = targetState.rootTransition
            val enter = when {
                targetTransition == RootTransition.SLIDE_UP -> TransitionProviders.Enter.slideUp
                targetTransition == RootTransition.PUSH -> TransitionProviders.Enter.pushToStart
                initialTransition == RootTransition.NONE -> TransitionProviders.Enter.none
                else -> TransitionProviders.Enter.fadeIn
            }
            val exit = when {
                initialTransition == RootTransition.NONE -> TransitionProviders.Exit.none
                initialTransition == RootTransition.SLIDE_UP -> TransitionProviders.Exit.slideDown
                targetTransition == RootTransition.SLIDE_UP -> TransitionProviders.Exit.stay
                initialTransition == RootTransition.PUSH &&
                    targetTransition == RootTransition.PUSH -> TransitionProviders.Exit.stay

                else -> TransitionProviders.Exit.fadeOut
            }
            enter(this) togetherWith exit(this)
        }
    }

    /**
     * Converts the given [ContentTransformProvider] into a [PredictiveContentTransformProvider]
     * that uses the same [ContentTransform] regardless of the swipe edge.
     */
    fun <T : Any> predictive(
        provider: ContentTransformProvider<T>,
    ): PredictiveContentTransformProvider<T> = { _ -> provider(this) }
}

/**
 * Indicates whether the navigation is occurring within the same flow, which is a push or pop on the
 * same back stack beneath the same root entry (see [EntryTransitions.rootPush]). This is `false`
 * when the back stack has been replaced or when navigating to or from the root of a flow.
 */
val AnimatedContentTransitionScope<out Scene<*>>.isSameFlowNavigation: Boolean
    get() = initialState.flowRootContentKey == targetState.flowRootContentKey

/**
 * Metadata key indicating that an entry is the root of a flow.
 */
private object FlowRootKey : NavMetadataKey<Boolean>

/**
 * Metadata key for the exit transition used by an entry when another entry is pushed on top of it.
 */
private object CoveredExitTransitionKey : NavMetadataKey<ExitTransitionProvider>

/**
 * Metadata key for the enter transition used by an entry when the entry on top of it is popped.
 */
private object RevealedEnterTransitionKey : NavMetadataKey<EnterTransitionProvider>

/**
 * Metadata key for the [RootTransition] of the back stack beginning with an entry.
 */
private object RootTransitionKey : NavMetadataKey<RootTransition>

/**
 * Creates the metadata for the transitions of an entry that is pushed with the given [enter]
 * transition and popped with the given [popExit] transition.
 */
private fun entryTransitions(
    enter: EnterTransitionProvider,
    popExit: ExitTransitionProvider,
): Map<String, Any> {
    val popTransitionSpec: AnimatedContentTransitionScope<Scene<*>>.() -> ContentTransform? = {
        if (isSameFlowNavigation) {
            targetState.revealedEnterTransition(this) togetherWith popExit(this)
        } else {
            null
        }
    }
    return NavDisplay.transitionSpec {
        if (isSameFlowNavigation) {
            enter(this) togetherWith initialState.coveredExitTransition(this)
        } else {
            null
        }
    } + NavDisplay.popTransitionSpec(popTransitionSpec) +
        NavDisplay.predictivePopTransitionSpec { _ -> popTransitionSpec(this) }
}

/**
 * The exit transition used by the top entry of this scene when another entry is pushed on top.
 */
private val Scene<*>.coveredExitTransition: ExitTransitionProvider
    get() = metadata[CoveredExitTransitionKey] ?: TransitionProviders.Exit.stay

/**
 * The enter transition used by the top entry of this scene when the entry on top is popped.
 */
private val Scene<*>.revealedEnterTransition: EnterTransitionProvider
    get() = metadata[RevealedEnterTransitionKey] ?: TransitionProviders.Enter.stay

/**
 * The [RootTransition] declared by the first entry of the back stack represented by this scene.
 */
private val Scene<*>.rootTransition: RootTransition
    get() = (previousEntries.firstOrNull() ?: entries.first())
        .metadata[RootTransitionKey]
        ?: RootTransition.FADE

/**
 * The content key of the entry that roots the flow currently displayed by this scene. This is the
 * last entry marked with [FlowRootKey] or, if there is none, the first entry of the back stack.
 */
private val Scene<*>.flowRootContentKey: Any
    get() {
        val backStack = previousEntries + entries
        return (backStack.lastOrNull { FlowRootKey in it.metadata } ?: backStack.first()).contentKey
    }
