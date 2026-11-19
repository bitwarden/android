package com.bitwarden.testharness.ui.platform.feature.rootnav

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.NavEntry
import com.bitwarden.ui.platform.base.BaseComposeTest
import com.bitwarden.ui.platform.navigation.util.navigationControllerOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for the [RootNavScreen] composable in the testharness module.
 */
class RootNavScreenTest : BaseComposeTest() {
    private val navigationController = navigationControllerOf<RootNavGraph>(
        routes = arrayOf(RootNavGraph.LandingRoute),
    )
    private val rootNavStateFlow = MutableStateFlow<RootNavState>(RootNavState.Splash)
    private val viewModel = mockk<RootNavViewModel> {
        every { eventFlow } returns emptyFlow()
        every { stateFlow } returns rootNavStateFlow
    }

    private var isSplashScreenRemoved: Boolean = false

    @Before
    fun setup() {
        setTestContent {
            RootNavScreen(
                viewModel = viewModel,
                navigationController = navigationController,
                entryProvider = { key -> NavEntry(key = key) { Text(text = key.toString()) } },
                onSplashScreenRemoved = { isSplashScreenRemoved = true },
            )
        }
    }

    @Test
    fun `initial state does not invoke splash screen removal`() {
        composeTestRule.runOnIdle {
            assertFalse(isSplashScreenRemoved)
        }
    }

    @Test
    fun `splash screen callback is called when transitioning from Splash state`() {
        composeTestRule.runOnIdle {
            assertFalse(isSplashScreenRemoved)
        }

        // Transition to Landing state
        rootNavStateFlow.value = RootNavState.Landing
        composeTestRule.runOnIdle {
            assertTrue(isSplashScreenRemoved)
        }
    }

    @Test
    fun `nav display displays the landing destination as the initial destination`() {
        composeTestRule
            .onNodeWithText(text = RootNavGraph.LandingRoute.toString())
            .assertIsDisplayed()
        composeTestRule.runOnIdle {
            assertEquals(
                listOf(RootNavGraph.LandingRoute),
                navigationController.backStack.toList(),
            )
        }
    }

    @Test
    fun `nav display displays the destination on top of the back stack`() {
        navigationController.navigate(RootNavGraph.CredentialManagerRoute)

        composeTestRule
            .onNodeWithText(text = RootNavGraph.CredentialManagerRoute.toString())
            .assertIsDisplayed()
        composeTestRule.runOnIdle {
            assertEquals(
                listOf(RootNavGraph.LandingRoute, RootNavGraph.CredentialManagerRoute),
                navigationController.backStack.toList(),
            )
        }
    }

    @Test
    fun `system back should remove the top destination from the back stack`() {
        navigationController.navigate(RootNavGraph.AutofillRoute)
        // Wait for the new destination to be displayed so the back handler has been enabled.
        composeTestRule
            .onNodeWithText(text = RootNavGraph.AutofillRoute.toString())
            .assertIsDisplayed()
        backDispatcher?.onBackPressed()

        composeTestRule
            .onNodeWithText(text = RootNavGraph.LandingRoute.toString())
            .assertIsDisplayed()
        composeTestRule.runOnIdle {
            assertEquals(
                listOf(RootNavGraph.LandingRoute),
                navigationController.backStack.toList(),
            )
        }
    }
}
