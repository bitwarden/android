package com.x8bit.bitwarden.ui.platform.feature.overlaynav

import androidx.compose.material3.Text
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.NavEntry
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.navigation.util.navigationControllerOf
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class OverlayNavScreenTest : BitwardenComposeTest() {
    private val navigationController = navigationControllerOf<OverlayNavGraph>(
        routes = arrayOf(OverlayNavGraph.RootNavigationRoute),
    )
    private val mutableEventFlow = bufferedMutableSharedFlow<OverlayNavEvent>()
    private val viewModel = mockk<OverlayNavViewModel> {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns MutableStateFlow(Unit)
    }

    @Before
    fun setup() {
        setContent {
            OverlayNavScreen(
                viewModel = viewModel,
                navigationController = navigationController,
                onSplashScreenRemoved = {},
                entryProvider = { key -> NavEntry(key = key) { Text(text = key.toString()) } },
            )
        }
    }

    @Test
    fun `initial back stack should display the root navigation screen`() {
        composeTestRule
            .onNodeWithText(text = OverlayNavGraph.RootNavigationRoute.toString())
            .assertExists()
        composeTestRule.runOnIdle {
            assertEquals(
                listOf(OverlayNavGraph.RootNavigationRoute),
                navigationController.backStack.toList(),
            )
        }
    }

    @Test
    fun `on NavigateToUpdateBiometrics should navigate to the update biometrics screen`() {
        mutableEventFlow.tryEmit(OverlayNavEvent.NavigateToUpdateBiometrics)
        composeTestRule.runOnIdle {
            assertEquals(
                listOf(OverlayNavGraph.RootNavigationRoute, OverlayNavGraph.UpdateBiometricsRoute),
                navigationController.backStack.toList(),
            )
        }
    }

    @Test
    fun `on NavigateToCookieAcquisition should navigate to the cookie acquisition screen`() {
        mutableEventFlow.tryEmit(OverlayNavEvent.NavigateToCookieAcquisition)
        composeTestRule.runOnIdle {
            assertEquals(
                listOf(OverlayNavGraph.RootNavigationRoute, OverlayNavGraph.CookieAcquisitionRoute),
                navigationController.backStack.toList(),
            )
        }
    }

    @Test
    fun `on NavigateToCookieAcquisition should not navigate again when already displayed`() {
        mutableEventFlow.tryEmit(OverlayNavEvent.NavigateToCookieAcquisition)
        mutableEventFlow.tryEmit(OverlayNavEvent.NavigateToCookieAcquisition)
        composeTestRule.runOnIdle {
            assertEquals(
                listOf(OverlayNavGraph.RootNavigationRoute, OverlayNavGraph.CookieAcquisitionRoute),
                navigationController.backStack.toList(),
            )
        }
    }

    @Test
    fun `on NavigateToLocalNetworkAccess should navigate to the local network access screen`() {
        mutableEventFlow.tryEmit(OverlayNavEvent.NavigateToLocalNetworkAccess)
        composeTestRule.runOnIdle {
            assertEquals(
                listOf(
                    OverlayNavGraph.RootNavigationRoute,
                    OverlayNavGraph.LocalNetworkAccessRoute,
                ),
                navigationController.backStack.toList(),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on NavigateToAccessibilityDisclosure should navigate to the accessibility disclosure screen`() {
        mutableEventFlow.tryEmit(OverlayNavEvent.NavigateToAccessibilityDisclosure)
        composeTestRule.runOnIdle {
            assertEquals(
                listOf(
                    OverlayNavGraph.RootNavigationRoute,
                    OverlayNavGraph.AccessibilityDisclosureRoute,
                ),
                navigationController.backStack.toList(),
            )
        }
    }
}
