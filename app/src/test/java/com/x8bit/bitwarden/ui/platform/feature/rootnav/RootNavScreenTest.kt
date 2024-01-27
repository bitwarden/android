package com.x8bit.bitwarden.ui.platform.feature.rootnav

import androidx.navigation.navOptions
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.FakeNavHostController
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RootNavScreenTest : BaseComposeTest() {
    private val fakeNavHostController = FakeNavHostController()

    private val expectedNavOptions = navOptions {
        // When changing root navigation state, pop everything else off the back stack:
        popUpTo(fakeNavHostController.graphId) {
            inclusive = false
            saveState = false
        }
        launchSingleTop = true
        restoreState = false
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
        var isSplashScreenRemoved = false
        composeTestRule.setContent {
            RootNavScreen(
                viewModel = viewModel,
                navController = fakeNavHostController,
                onSplashScreenRemoved = { isSplashScreenRemoved = true },
            )
        }
        composeTestRule.runOnIdle {
            fakeNavHostController.assertCurrentRoute("splash")
        }
        assertFalse(isSplashScreenRemoved)

        // Make sure navigating to Auth works as expected:
        rootNavStateFlow.value = RootNavState.Auth
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "auth_graph",
                navOptions = expectedNavOptions,
            )
        }
        assertTrue(isSplashScreenRemoved)

        // Make sure navigating to vault locked works as expected:
        rootNavStateFlow.value = RootNavState.VaultLocked
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "vault_unlock",
                navOptions = expectedNavOptions,
            )
        }

        // Make sure navigating to vault unlocked works as expected:
        rootNavStateFlow.value = RootNavState.VaultUnlocked(activeUserId = "userId")
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "vault_unlocked_graph",
                navOptions = expectedNavOptions,
            )
        }

        // Make sure navigating to vault unlocked for new sends works as expected:
        rootNavStateFlow.value = RootNavState.VaultUnlockedForNewSend
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "add_send_item/add",
                navOptions = expectedNavOptions,
            )
        }

        // Make sure navigating to vault unlocked for autofill works as expected:
        rootNavStateFlow.value =
            RootNavState.VaultUnlockedForAutofillSelection(
                type = AutofillSelectionData.Type.LOGIN,
            )
        composeTestRule.runOnIdle {
            fakeNavHostController.assertLastNavigation(
                route = "vault_item_listing_as_root/login",
                navOptions = expectedNavOptions,
            )
        }
    }
}
