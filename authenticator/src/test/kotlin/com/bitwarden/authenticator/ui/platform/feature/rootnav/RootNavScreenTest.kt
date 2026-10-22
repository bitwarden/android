package com.bitwarden.authenticator.ui.platform.feature.rootnav

import androidx.compose.material3.Text
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.NavEntry
import com.bitwarden.authenticator.ui.platform.base.AuthenticatorComposeTest
import com.bitwarden.authenticator.ui.platform.manager.biometrics.BiometricsManager
import com.bitwarden.ui.platform.navigation.util.navigationControllerOf
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class RootNavScreenTest : AuthenticatorComposeTest() {

    private var onSplashScreenRemovedCalled = false

    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)

    private val viewModel: RootNavViewModel = mockk {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns emptyFlow()
        every { trySendAction(any()) } just runs
    }

    private val navigationController = navigationControllerOf<RootNavGraph>(
        routes = arrayOf(RootNavGraph.SplashRoute),
    )

    private val biometricsManager: BiometricsManager = mockk {
        every { isBiometricsSupported } returns true
    }

    @Before
    fun setup() {
        onSplashScreenRemovedCalled = false
        setContent(
            biometricsManager = biometricsManager,
        ) {
            RootNavScreen(
                viewModel = viewModel,
                navigationController = navigationController,
                entryProvider = { key ->
                    NavEntry(key = key) { Text(text = key.toString()) }
                },
                biometricsManager = biometricsManager,
                onSplashScreenRemoved = { onSplashScreenRemovedCalled = true },
            )
        }
    }

    @Test
    fun `initial back stack should display the splash screen`() {
        composeTestRule.onNodeWithText(text = RootNavGraph.SplashRoute.toString()).assertExists()
        composeTestRule.runOnIdle {
            assertEquals(listOf(RootNavGraph.SplashRoute), navigationController.backStack.toList())
            assertFalse(onSplashScreenRemovedCalled)
        }
    }

    @Test
    fun `back stack changes should send BackStackUpdate action`() {
        composeTestRule.runOnIdle {
            verify(exactly = 1) { viewModel.trySendAction(RootNavAction.BackStackUpdate) }
        }

        mutableStateFlow.value = DEFAULT_STATE.copy(
            navState = RootNavState.NavState.Tutorial,
        )

        composeTestRule.runOnIdle {
            verify(exactly = 2) { viewModel.trySendAction(RootNavAction.BackStackUpdate) }
        }
    }

    @Test
    fun `when navState is Splash should show splash screen`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            navState = RootNavState.NavState.Splash,
        )

        composeTestRule.runOnIdle {
            assertEquals(listOf(RootNavGraph.SplashRoute), navigationController.backStack.toList())
            assertFalse(onSplashScreenRemovedCalled)
        }
    }

    @Test
    fun `when navState is Tutorial should show tutorial screen`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            navState = RootNavState.NavState.Tutorial,
        )

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(RootNavGraph.TutorialRoute),
                navigationController.backStack.toList(),
            )
            assertTrue(onSplashScreenRemovedCalled)
        }
    }

    @Test
    fun `when navState is Locked should show unlock screen`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            navState = RootNavState.NavState.Locked,
        )

        composeTestRule.runOnIdle {
            assertEquals(listOf(RootNavGraph.UnlockRoute), navigationController.backStack.toList())
            assertTrue(onSplashScreenRemovedCalled)
        }
    }

    @Test
    fun `when navState is Unlocked should show the authenticator section`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            navState = RootNavState.NavState.Unlocked,
        )

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(RootNavGraph.AuthenticatorRoute),
                navigationController.backStack.toList(),
            )
            assertTrue(onSplashScreenRemovedCalled)
        }
    }

    @Test
    fun `back stack should not be replaced when the navState is unchanged`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            navState = RootNavState.NavState.Unlocked,
        )
        composeTestRule.runOnIdle {
            assertEquals(
                listOf(RootNavGraph.AuthenticatorRoute),
                navigationController.backStack.toList(),
            )
        }

        navigationController.navigate(RootNavGraph.TutorialRoute)

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(RootNavGraph.AuthenticatorRoute, RootNavGraph.TutorialRoute),
                navigationController.backStack.toList(),
            )
        }
    }

    @Test
    fun `onSplashScreenRemoved should be called when navState changes from Splash`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            navState = RootNavState.NavState.Splash,
        )

        composeTestRule.runOnIdle {
            assertFalse(onSplashScreenRemovedCalled)
        }

        mutableStateFlow.update {
            it.copy(navState = RootNavState.NavState.Tutorial)
        }

        composeTestRule.runOnIdle {
            assertTrue(onSplashScreenRemovedCalled)
        }
    }

    @Test
    fun `onSplashScreenRemoved should not be called when navState is Splash`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            navState = RootNavState.NavState.Splash,
        )

        composeTestRule.runOnIdle {
            assertFalse(onSplashScreenRemovedCalled)
        }
    }

    @Test
    fun `navigation should handle Splash to Tutorial transition`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            navState = RootNavState.NavState.Splash,
        )

        composeTestRule.runOnIdle {
            assertEquals(listOf(RootNavGraph.SplashRoute), navigationController.backStack.toList())
        }

        mutableStateFlow.update {
            it.copy(navState = RootNavState.NavState.Tutorial)
        }

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(RootNavGraph.TutorialRoute),
                navigationController.backStack.toList(),
            )
        }
    }

    @Test
    fun `navigation should handle Tutorial to Unlocked transition`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            navState = RootNavState.NavState.Tutorial,
        )

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(RootNavGraph.TutorialRoute),
                navigationController.backStack.toList(),
            )
        }

        mutableStateFlow.update {
            it.copy(navState = RootNavState.NavState.Unlocked)
        }

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(RootNavGraph.AuthenticatorRoute),
                navigationController.backStack.toList(),
            )
        }
    }

    @Test
    fun `navigation should handle Splash to Locked transition`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            navState = RootNavState.NavState.Splash,
        )

        composeTestRule.runOnIdle {
            assertEquals(listOf(RootNavGraph.SplashRoute), navigationController.backStack.toList())
        }

        mutableStateFlow.update {
            it.copy(navState = RootNavState.NavState.Locked)
        }

        composeTestRule.runOnIdle {
            assertEquals(listOf(RootNavGraph.UnlockRoute), navigationController.backStack.toList())
        }
    }

    @Test
    fun `navigation should handle Locked to Unlocked transition`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            navState = RootNavState.NavState.Locked,
        )

        composeTestRule.runOnIdle {
            assertEquals(listOf(RootNavGraph.UnlockRoute), navigationController.backStack.toList())
        }

        mutableStateFlow.update {
            it.copy(navState = RootNavState.NavState.Unlocked)
        }

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(RootNavGraph.AuthenticatorRoute),
                navigationController.backStack.toList(),
            )
        }
    }

    @Test
    fun `navigation should handle Unlocked to Locked transition`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            navState = RootNavState.NavState.Unlocked,
        )

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(RootNavGraph.AuthenticatorRoute),
                navigationController.backStack.toList(),
            )
        }

        navigationController.navigate(RootNavGraph.TutorialRoute)
        mutableStateFlow.update {
            it.copy(navState = RootNavState.NavState.Locked)
        }

        composeTestRule.runOnIdle {
            assertEquals(listOf(RootNavGraph.UnlockRoute), navigationController.backStack.toList())
        }
    }

    @Test
    fun `navigation should handle Splash to Unlocked transition`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            navState = RootNavState.NavState.Splash,
        )

        composeTestRule.runOnIdle {
            assertEquals(listOf(RootNavGraph.SplashRoute), navigationController.backStack.toList())
        }

        mutableStateFlow.update {
            it.copy(navState = RootNavState.NavState.Unlocked)
        }

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(RootNavGraph.AuthenticatorRoute),
                navigationController.backStack.toList(),
            )
        }
    }
}

private val DEFAULT_STATE = RootNavState(
    navState = RootNavState.NavState.Splash,
)
