package com.x8bit.bitwarden.ui.platform.feature.rootnav

import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class RootNavViewModelTest : BaseViewModelTest() {

    @Test
    fun `initial state should be splash`() {
        val viewModel = RootNavViewModel()
        assert(viewModel.stateFlow.value is RootNavState.Splash)
    }

    @Test
    fun `state should move from splash to auth`() = runTest {
        val viewModel = RootNavViewModel()
        viewModel.stateFlow.test {
            assert(awaitItem() is RootNavState.Splash)
            assert(awaitItem() is RootNavState.Auth)
        }
    }
}
