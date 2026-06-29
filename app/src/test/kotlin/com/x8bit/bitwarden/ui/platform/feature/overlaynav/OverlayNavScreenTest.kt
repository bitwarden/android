package com.x8bit.bitwarden.ui.platform.feature.overlaynav

import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.base.createMockNavHostController
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.feature.accessibilitydisclosure.AccessibilityDisclosureRoute
import com.x8bit.bitwarden.ui.platform.feature.cookieacquisition.CookieAcquisitionRoute
import com.x8bit.bitwarden.ui.platform.feature.localnetworkaccess.LocalNetworkAccessRoute
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test

class OverlayNavScreenTest : BitwardenComposeTest() {
    private val mockNavHostController = createMockNavHostController()
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
                navController = mockNavHostController,
                onSplashScreenRemoved = {},
            )
        }
    }

    @Test
    fun `on NavigateToCookieAcquisition should navigate to the cookie acquisition screen`() {
        mutableEventFlow.tryEmit(OverlayNavEvent.NavigateToCookieAcquisition)
        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                mockNavHostController.navigate(route = CookieAcquisitionRoute, builder = any())
            }
        }
    }

    @Test
    fun `on NavigateToLocalNetworkAccess should navigate to the local network access screen`() {
        mutableEventFlow.tryEmit(OverlayNavEvent.NavigateToLocalNetworkAccess)
        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                mockNavHostController.navigate(route = LocalNetworkAccessRoute, builder = any())
            }
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on NavigateToAccessibilityDisclosure should navigate to the accessibility disclosure screen`() {
        mutableEventFlow.tryEmit(OverlayNavEvent.NavigateToAccessibilityDisclosure)
        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                mockNavHostController.navigate(
                    route = AccessibilityDisclosureRoute,
                    builder = any(),
                )
            }
        }
    }
}
