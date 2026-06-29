package com.x8bit.bitwarden.ui.platform.feature.overlaynav

import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.data.platform.manager.CookieAcquisitionRequestManager
import com.x8bit.bitwarden.data.platform.manager.model.CookieAcquisitionRequest
import com.x8bit.bitwarden.data.platform.manager.network.NetworkPermissionManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OverlayNavViewModelTest : BaseViewModelTest() {
    private val mutableAccessibilityDisclaimerFlow = MutableStateFlow(true)
    private val settingsRepository: SettingsRepository = mockk {
        every { hasShownAccessibilityDisclaimerFlow } returns mutableAccessibilityDisclaimerFlow
    }
    private val mutableLocalNetworkAccessFlow = MutableStateFlow(false)
    private val networkPermissionManager: NetworkPermissionManager = mockk {
        every { isLocalNetworkAccessRequiredStateFlow } returns mutableLocalNetworkAccessFlow
    }
    private val mutableCookieAcquisitionFlow = MutableStateFlow<CookieAcquisitionRequest?>(null)
    private val cookieAcquisitionRequestManager: CookieAcquisitionRequestManager = mockk {
        every { cookieAcquisitionRequestFlow } returns mutableCookieAcquisitionFlow
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when accessibility disclaimer flow is false should emit NavigateToAccessibilityDisclosure`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                expectNoEvents()
                mutableAccessibilityDisclaimerFlow.value = false
                assertEquals(OverlayNavEvent.NavigateToAccessibilityDisclosure, awaitItem())
            }
        }

    @Test
    fun `when local network access flow is true should emit NavigateToLocalNetworkAccess`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                expectNoEvents()
                mutableLocalNetworkAccessFlow.value = true
                assertEquals(OverlayNavEvent.NavigateToLocalNetworkAccess, awaitItem())
            }
        }

    @Test
    fun `when a cookie acquisition request is ready should emit NavigateToCookieAcquisition`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                expectNoEvents()
                mutableCookieAcquisitionFlow.value = CookieAcquisitionRequest(
                    hostname = "vault.bitwarden.com",
                )
                assertEquals(OverlayNavEvent.NavigateToCookieAcquisition, awaitItem())
            }
        }

    private fun createViewModel(): OverlayNavViewModel = OverlayNavViewModel(
        cookieAcquisitionRequestManager = cookieAcquisitionRequestManager,
        networkPermissionManager = networkPermissionManager,
        settingsRepository = settingsRepository,
    )
}
