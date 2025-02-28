package com.bitwarden.authenticator.ui.platform.feature.debugmenu.manager

import android.view.KeyEvent
import android.view.MotionEvent
import com.bitwarden.authenticator.data.platform.repository.DebugMenuRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DebugLaunchManagerTest {

    private val mockDebugMenuRepository = mockk<DebugMenuRepository>(relaxed = true) {
        every { isDebugMenuEnabled } returns true
    }

    private val mockKeyEvent = mockk<KeyEvent>(relaxed = true) {
        every { action } returns KeyEvent.ACTION_DOWN
        every { keyCode } returns KeyEvent.KEYCODE_GRAVE
        every { isShiftPressed } returns true
    }

    private val mockMotionEvent = mockk<MotionEvent>(relaxed = true) {
        every { action and MotionEvent.ACTION_MASK } returns MotionEvent.ACTION_POINTER_DOWN
        every { pointerCount } returns 3
    }

    private var actionHasBeenCalled = false
    private val action: () -> Unit = { actionHasBeenCalled = true }

    private val debugLaunchManager =
        DebugLaunchManagerImpl(debugMenuRepository = mockDebugMenuRepository)

    @Test
    fun `actionOnInputEvent should return true when KeyEvent is debug trigger`() {
        assertFalse(actionHasBeenCalled)
        val result = debugLaunchManager.actionOnInputEvent(event = mockKeyEvent, action = action)
        assertTrue(result)
        assertTrue(actionHasBeenCalled)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `actionOnInputEvent should return true when TouchEvent is debug trigger done 3 times in a row`() {
        assertFalse(actionHasBeenCalled)
        debugLaunchManager.actionOnInputEvent(event = mockMotionEvent, action = action)
        debugLaunchManager.actionOnInputEvent(event = mockMotionEvent, action = action)
        val result = debugLaunchManager.actionOnInputEvent(event = mockMotionEvent, action = action)
        assertTrue(result)
        assertTrue(actionHasBeenCalled)
    }

    @Test
    fun `actionOnInputEvent should return false when debug menu is not enabled`() {
        every { mockDebugMenuRepository.isDebugMenuEnabled } returns false
        assertFalse(actionHasBeenCalled)
        val result = debugLaunchManager.actionOnInputEvent(event = mockKeyEvent, action = action)
        assertFalse(result)
        assertFalse(actionHasBeenCalled)
    }

    @Test
    fun `actionOnInputEvent should return false when key event is not debug trigger`() {
        assertFalse(actionHasBeenCalled)
        val result = debugLaunchManager
            .actionOnInputEvent(
                event = mockKeyEvent.apply {
                    every { action } returns KeyEvent.ACTION_UP
                },
                action = action,
            )
        assertFalse(result)
        assertFalse(actionHasBeenCalled)
    }

    @Test
    fun `actionOnInputEvent should return false when touch event is not debug trigger`() {
        assertFalse(actionHasBeenCalled)
        debugLaunchManager.actionOnInputEvent(event = mockMotionEvent, action = action)
        debugLaunchManager.actionOnInputEvent(event = mockMotionEvent, action = action)
        val result = debugLaunchManager.actionOnInputEvent(
            event = mockMotionEvent.apply {
                every { pointerCount } returns 100
            },
            action = action,
        )
        assertFalse(result)
        assertFalse(actionHasBeenCalled)
    }

    @Test
    fun `if touch action input takes place too slow should return false`() {
        val eventTimeMillis = 100L
        assertFalse(actionHasBeenCalled)
        debugLaunchManager.actionOnInputEvent(event = mockMotionEvent, action = action)
        debugLaunchManager.actionOnInputEvent(event = mockMotionEvent.apply {
            every { eventTime } returns eventTimeMillis
        }, action = action)
        val result = debugLaunchManager.actionOnInputEvent(
            event = mockMotionEvent.apply {
                every { eventTime } returns eventTimeMillis + 501
            },
            action = action,
        )
        assertFalse(result)
        assertFalse(actionHasBeenCalled)
    }
}
