package com.bitwarden.authenticator.ui.platform.feature.rootnav

import androidx.navigation.navOptions
import com.bitwarden.authenticator.ui.auth.unlock.UnlockRoute
import com.bitwarden.authenticator.ui.authenticator.feature.navbar.AuthenticatorNavbarRoute
import com.bitwarden.authenticator.ui.platform.base.AuthenticatorComposeTest
import com.bitwarden.authenticator.ui.platform.feature.splash.SplashRoute
import com.bitwarden.authenticator.ui.platform.feature.tutorial.TutorialRoute
import com.bitwarden.authenticator.ui.platform.manager.biometrics.BiometricsManager
import com.bitwarden.ui.platform.base.createMockNavHostController
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test

class RootNavScreenTest : AuthenticatorComposeTest() {

    private var onSplashScreenRemovedCalled = false

    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)

    private val viewModel: RootNavViewModel = mockk {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns emptyFlow()
        every { trySendAction(any()) } just runs
    }

    private val navController = createMockNavHostController()

    private val expectedNavOptions = navOptions {
        // When changing root navigation state, pop everything else off the back stack:
        popUpTo(id = navController.graph.id) {
            inclusive = false
            saveState = false
        }
        launchSingleTop = true
        restoreState = false
    }

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
                navController = navController,
                biometricsManager = biometricsManager,
                onSplashScreenRemoved = { onSplashScreenRemovedCalled = true },
            )
        }
    }

    @Test
    fun `when navState is Splash should show splash screen`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            navState = RootNavState.NavState.Splash,
        )

        composeTestRule.runOnIdle {
            verify {
                navController.navigate(
                    route = SplashRoute,
                    navOptions = expectedNavOptions,
                )
            }
            assertFalse(onSplashScreenRemovedCalled)
        }
    }

    @Test
    fun `when navState is Tutorial should show tutorial screen`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            navState = RootNavState.NavState.Tutorial,
        )

        composeTestRule.runOnIdle {
            verify {
                navController.navigate(
                    route = TutorialRoute,
                    navOptions = expectedNavOptions,
                )
            }
            assertTrue(onSplashScreenRemovedCalled)
        }
    }

    @Test
    fun `when navState is Locked should show unlock screen`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            navState = RootNavState.NavState.Locked,
        )

        composeTestRule.runOnIdle {
            verify {
                navController.navigate(
                    route = UnlockRoute,
                    navOptions = expectedNavOptions,
                )
            }
            assertTrue(onSplashScreenRemovedCalled)
        }
    }

    @Test
    fun `when navState is Unlocked should show authenticator graph`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            navState = RootNavState.NavState.Unlocked,
        )

        composeTestRule.runOnIdle {
            verify {
                navController.navigate(
                    route = AuthenticatorNavbarRoute,
                    navOptions = expectedNavOptions,
                )
            }
            assertTrue(onSplashScreenRemovedCalled)
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
            verify {
                navController.navigate(
                    route = SplashRoute,
                    navOptions = expectedNavOptions,
                )
            }
        }

        mutableStateFlow.update {
            it.copy(navState = RootNavState.NavState.Tutorial)
        }

        composeTestRule.runOnIdle {
            verify {
                navController.navigate(
                    route = TutorialRoute,
                    navOptions = expectedNavOptions,
                )
            }
        }
    }

    @Test
    fun `navigation should handle Tutorial to Unlocked transition`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            navState = RootNavState.NavState.Tutorial,
        )

        composeTestRule.runOnIdle {
            verify {
                navController.navigate(
                    route = TutorialRoute,
                    navOptions = expectedNavOptions,
                )
            }
        }

        mutableStateFlow.update {
            it.copy(navState = RootNavState.NavState.Unlocked)
        }

        composeTestRule.runOnIdle {
            verify {
                navController.navigate(
                    route = AuthenticatorNavbarRoute,
                    navOptions = expectedNavOptions,
                )
            }
        }
    }

    @Test
    fun `navigation should handle Splash to Locked transition`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            navState = RootNavState.NavState.Splash,
        )

        composeTestRule.runOnIdle {
            verify {
                navController.navigate(
                    route = SplashRoute,
                    navOptions = expectedNavOptions,
                )
            }
        }

        mutableStateFlow.update {
            it.copy(navState = RootNavState.NavState.Locked)
        }

        composeTestRule.runOnIdle {
            verify {
                navController.navigate(
                    route = UnlockRoute,
                    navOptions = expectedNavOptions,
                )
            }
        }
    }

    @Test
    fun `navigation should handle Locked to Unlocked transition`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            navState = RootNavState.NavState.Locked,
        )

        composeTestRule.runOnIdle {
            verify {
                navController.navigate(
                    route = UnlockRoute,
                    navOptions = expectedNavOptions,
                )
            }
        }

        mutableStateFlow.update {
            it.copy(navState = RootNavState.NavState.Unlocked)
        }

        composeTestRule.runOnIdle {
            verify {
                navController.navigate(
                    route = AuthenticatorNavbarRoute,
                    navOptions = expectedNavOptions,
                )
            }
        }
    }

    @Test
    fun `navigation should handle Splash to Unlocked transition`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            navState = RootNavState.NavState.Splash,
        )

        composeTestRule.runOnIdle {
            verify {
                navController.navigate(
                    route = SplashRoute,
                    navOptions = expectedNavOptions,
                )
            }
        }

        mutableStateFlow.update {
            it.copy(navState = RootNavState.NavState.Unlocked)
        }

        composeTestRule.runOnIdle {
            verify {
                navController.navigate(
                    route = AuthenticatorNavbarRoute,
                    navOptions = expectedNavOptions,
                )
            }
        }
    }
}

private val DEFAULT_STATE = RootNavState(
    hasSeenWelcomeGuide = false,
    navState = RootNavState.NavState.Splash,
)
