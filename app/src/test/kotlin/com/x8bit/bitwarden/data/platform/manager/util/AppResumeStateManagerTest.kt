package com.x8bit.bitwarden.data.platform.manager.util

import com.x8bit.bitwarden.data.platform.manager.model.AppResumeScreenData
import org.junit.Assert
import org.junit.Test

class AppResumeStateManagerTest {
    private val appStateManager = AppResumeStateManagerImpl()

    @Test
    fun `AppResumeStateManagerImpl should update and retrieve screen data`() {
        val screenData = AppResumeScreenData.GeneratorScreen

        appStateManager.updateScreenData(screenData)
        Assert.assertEquals(screenData, appStateManager.appResumeState.value)
    }

    @Test
    fun `AppResumeStateManagerImpl should retrieve null if not set`() {
        Assert.assertEquals(null, appStateManager.appResumeState.value)
    }
}
