package com.bitwarden.ui.platform.manager.exit

import android.app.Activity
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test

class ExitManagerTest {
    private val activity: Activity = mockk {
        every { finishAndRemoveTask() } just runs
    }

    private val exitManager: ExitManager = ExitManagerImpl(
        activity = activity,
    )

    @Test
    fun `exitApplication should finish the activity`() {
        exitManager.exitApplication()

        verify(exactly = 1) {
            activity.finishAndRemoveTask()
        }
    }
}
