package com.x8bit.bitwarden.ui.platform.feature.rootnav

import androidx.navigation.navOptions
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.FakeNavHostController
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RootNavScreenTest : BaseComposeTest() {
    private val fakeNavHostController = FakeNavHostController()

    private val expectedNavOptions = navOptions {
        // When changing root navigation state, pop everything else off the back stack:
        popUpTo(fakeNavHostController.graphId) {
            inclusive = false
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }

    @Test
    fun `initial route should be splash`() {
        val viewModel = mockk<RootNavViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns MutableStateFlow(RootNavState.Splash)
        }
        composeTestRule.setContent {
            RootNavScreen(
                viewModel = viewModel,
                navController = fakeNavHostController,
            )
        }
        composeTestRule.runOnIdle {
            fakeNavHostController.assertCurrentRoute("splash")
        }
    }

    @Test
    fun `when root nav destination changes, navigation should follow`() = runTest {
        val rootNavStateFlow = MutableStateFlow<RootNavState>(RootNavState.Splash)
        val viewModel = mockk<RootNavViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns rootNavStateFlow
        }
        composeTestRule.setContent {
            RootNavScreen(
                viewModel = viewModel,
                navController = fakeNavHostController,
            )
        }
        composeTestRule.runOnIdle {
            fakeNavHostController.assertCurrentRoute("splash")
        }

        // Make sure navigating to Auth works as expected:
        rootNavStateFlow.value = RootNavState.Auth
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "auth",
                navOptions = expectedNavOptions,
            )
        }

        // Make sure navigating to vault unlocked works as expected:
        rootNavStateFlow.value = RootNavState.VaultUnlocked
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "VaultUnlocked",
                navOptions = expectedNavOptions,
            )
        }
    }
}
