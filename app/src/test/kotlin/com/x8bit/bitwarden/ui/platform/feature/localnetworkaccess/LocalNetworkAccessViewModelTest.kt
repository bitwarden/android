package com.x8bit.bitwarden.ui.platform.feature.localnetworkaccess

import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.data.platform.manager.network.NetworkPermissionManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LocalNetworkAccessViewModelTest : BaseViewModelTest() {

    private val networkPermissionManager = mockk<NetworkPermissionManager> {
        every { clearIsLocalNetworkAccessRequired() } just runs
    }

    @Test
    fun `initial state is LocalNetworkAccessState`() {
        val viewModel = createViewModel()
        assertEquals(LocalNetworkAccessState, viewModel.stateFlow.value)
    }

    @Test
    fun `CloseClick clears permission and sends NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(LocalNetworkAccessAction.CloseClick)
            assertEquals(LocalNetworkAccessEvent.NavigateBack, awaitItem())
        }
        verify(exactly = 1) { networkPermissionManager.clearIsLocalNetworkAccessRequired() }
    }

    @Test
    fun `ContinueWithoutPermissionClick clears permission and sends NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(LocalNetworkAccessAction.ContinueWithoutPermissionClick)
            assertEquals(LocalNetworkAccessEvent.NavigateBack, awaitItem())
        }
        verify(exactly = 1) { networkPermissionManager.clearIsLocalNetworkAccessRequired() }
    }

    @Test
    fun `SettingsClick sends NavigateToSettings`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(LocalNetworkAccessAction.SettingsClick)
            assertEquals(LocalNetworkAccessEvent.NavigateToSettings, awaitItem())
        }
    }

    @Test
    fun `Resumed with permission granted clears permission and sends NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                LocalNetworkAccessAction.Resumed(hasLocalNetworkAccessPermission = true),
            )
            assertEquals(LocalNetworkAccessEvent.NavigateBack, awaitItem())
        }
        verify(exactly = 1) { networkPermissionManager.clearIsLocalNetworkAccessRequired() }
    }

    @Test
    fun `Resumed with permission not granted does not send any event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                LocalNetworkAccessAction.Resumed(hasLocalNetworkAccessPermission = false),
            )
            expectNoEvents()
        }
        verify(exactly = 0) { networkPermissionManager.clearIsLocalNetworkAccessRequired() }
    }

    private fun createViewModel(): LocalNetworkAccessViewModel = LocalNetworkAccessViewModel(
        networkPermissionManager = networkPermissionManager,
    )
}
