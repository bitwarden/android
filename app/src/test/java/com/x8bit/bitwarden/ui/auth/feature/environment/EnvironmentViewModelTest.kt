package com.x8bit.bitwarden.ui.auth.feature.environment

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EnvironmentViewModelTest : BaseViewModelTest() {

    @Test
    fun `initial state should be correct when there is no saved state`() {
        val viewModel = createViewModel()
        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `initial state should be correct when restoring from the save state handle`() {
        val savedState = EnvironmentState(
            serverUrl = "saved-server",
            webVaultServerUrl = "saved-web-vault",
            apiServerUrl = "saved-api",
            identityServerUrl = "saved-identity",
            iconsServerUrl = "saved-icons",
        )
        val viewModel = createViewModel(
            savedStateHandle = SavedStateHandle(
                initialState = mapOf(
                    "state" to savedState,
                ),
            ),
        )
        assertEquals(
            EnvironmentState(
                serverUrl = "saved-server",
                webVaultServerUrl = "saved-web-vault",
                apiServerUrl = "saved-api",
                identityServerUrl = "saved-identity",
                iconsServerUrl = "saved-icons",
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(EnvironmentAction.CloseClick)
            assertEquals(EnvironmentEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `SaveClick should emit ShowTest`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(EnvironmentAction.SaveClick)
            assertEquals(
                EnvironmentEvent.ShowToast("Not yet implemented.".asText()),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ServerUrlChange should update the server URL`() {
        val viewModel = createViewModel()
        viewModel.actionChannel.trySend(
            EnvironmentAction.ServerUrlChange(serverUrl = "updated-server-url"),
        )
        assertEquals(
            DEFAULT_STATE.copy(serverUrl = "updated-server-url"),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `WebVaultServerUrlChange should update the web vault server URL`() {
        val viewModel = createViewModel()
        viewModel.actionChannel.trySend(
            EnvironmentAction.WebVaultServerUrlChange(webVaultServerUrl = "updated-web-vault-url"),
        )
        assertEquals(
            DEFAULT_STATE.copy(webVaultServerUrl = "updated-web-vault-url"),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `ApiServerUrlChange should update the API server URL`() {
        val viewModel = createViewModel()
        viewModel.actionChannel.trySend(
            EnvironmentAction.ApiServerUrlChange(apiServerUrl = "updated-api-url"),
        )
        assertEquals(
            DEFAULT_STATE.copy(apiServerUrl = "updated-api-url"),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `IdentityServerUrlChange should update the identity server URL`() {
        val viewModel = createViewModel()
        viewModel.actionChannel.trySend(
            EnvironmentAction.IdentityServerUrlChange(identityServerUrl = "updated-identity-url"),
        )
        assertEquals(
            DEFAULT_STATE.copy(identityServerUrl = "updated-identity-url"),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `IconsServerUrlChange should update the icons server URL`() {
        val viewModel = createViewModel()
        viewModel.actionChannel.trySend(
            EnvironmentAction.IconsServerUrlChange(iconsServerUrl = "updated-icons-url"),
        )
        assertEquals(
            DEFAULT_STATE.copy(iconsServerUrl = "updated-icons-url"),
            viewModel.stateFlow.value,
        )
    }

    //region Helper methods

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): EnvironmentViewModel =
        EnvironmentViewModel(
            savedStateHandle = savedStateHandle,
        )

    //endregion Helper methods

    companion object {
        private val DEFAULT_STATE = EnvironmentState(
            serverUrl = "",
            webVaultServerUrl = "",
            apiServerUrl = "",
            identityServerUrl = "",
            iconsServerUrl = "",
        )
    }
}
