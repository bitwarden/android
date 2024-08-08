package com.x8bit.bitwarden.ui.platform.base.util

/**
 * Almost all the events in the app involve navigation or toasts. To prevent accidentally
 * navigating to the same view twice, by default, events are ignored if the view is not currently
 * resumed. To avoid that restriction, specific events can implement [BackgroundEvent].
 */
interface BackgroundEvent
