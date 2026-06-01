package com.bitwarden.authenticator.ui.platform.feature.debugmenu.manager

import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import com.bitwarden.authenticator.data.platform.repository.DebugMenuRepository

private const val TAP_TIME_THRESHOLD_MILLIS = 500
private const val POINTERS_REQUIRED = 3

/**
 * Default implementation of the [DebugMenuLaunchManager]
 */
class DebugLaunchManagerImpl(
    private val debugMenuRepository: DebugMenuRepository,
) : DebugMenuLaunchManager {

    private val tapEventQueue: ArrayDeque<Long> = ArrayDeque()

    override fun actionOnInputEvent(
        event: InputEvent,
        action: () -> Unit,
    ): Boolean {
        val shouldTakeAction = when (event) {
            is KeyEvent -> event.debugTrigger()
            is MotionEvent -> shouldHandleMotionEvent(event)
            else -> false
        }

        if (shouldTakeAction) {
            action()
        }

        return shouldTakeAction
    }

    private fun shouldHandleMotionEvent(event: MotionEvent): Boolean {
        if (!event.debugTrigger()) return false
        // Pop old tap events until we have ones within our threshold
        while (
            tapEventQueue
                .firstOrNull()
                ?.let { event.eventTime - it >= TAP_TIME_THRESHOLD_MILLIS } == true
        ) {
            tapEventQueue.removeFirst()
        }

        // Add this tap event
        tapEventQueue.add(event.eventTime)
        return event.eventTime - tapEventQueue.first() < TAP_TIME_THRESHOLD_MILLIS &&
            tapEventQueue.size >= POINTERS_REQUIRED
    }

    /**
     * This is the equivalent of the entry of `shift` + `~` on a US keyboard.
     */
    private fun KeyEvent.debugTrigger(): Boolean =
        action == KeyEvent.ACTION_DOWN &&
            keyCode == KeyEvent.KEYCODE_GRAVE &&
            isShiftPressed &&
            debugMenuRepository.isDebugMenuEnabled

    private fun MotionEvent.debugTrigger(): Boolean =
        action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_POINTER_DOWN &&
            pointerCount == POINTERS_REQUIRED &&
            debugMenuRepository.isDebugMenuEnabled
}
