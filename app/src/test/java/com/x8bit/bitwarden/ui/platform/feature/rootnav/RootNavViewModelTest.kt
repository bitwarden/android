package com.x8bit.bitwarden.ui.platform.feature.rootnav

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RootNavViewModelTest : BaseViewModelTest() {

    @Test
    fun `initial state should be splash`() {
        val viewModel = RootNavViewModel(SavedStateHandle())
        assertEquals(viewModel.stateFlow.value, RootNavState.Splash)
    }

    @Test
    fun `initial state should be the state in savedStateHandle`() {
        val handle = SavedStateHandle(mapOf(("nav_state" to RootNavState.VaultUnlocked)))
        val viewModel = RootNavViewModel(handle)
        assertEquals(viewModel.stateFlow.value, RootNavState.VaultUnlocked)
    }

    @Test
    fun `state should move from splash to auth`() = runTest {
        val viewModel = RootNavViewModel(SavedStateHandle())
        viewModel.stateFlow.test {
            assertEquals(awaitItem(), RootNavState.Splash)
            assertEquals(awaitItem(), RootNavState.Auth)
        }
    }
}
