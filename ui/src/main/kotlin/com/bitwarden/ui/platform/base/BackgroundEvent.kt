package com.bitwarden.ui.platform.base

/**
 * Almost all the events in the app involve navigation. To prevent accidentally navigating to the
 * same view twice, by default, events are ignored if the view is not currently resumed.
 * To avoid that restriction, specific events can implement [BackgroundEvent].
 */
interface BackgroundEvent

/**
 * This implementation of [BackgroundEvent] allows the event to not be filtered but will force it
 * to wait until the view is resumed before consuming the event.
 */
interface DeferredBackgroundEvent : BackgroundEvent
