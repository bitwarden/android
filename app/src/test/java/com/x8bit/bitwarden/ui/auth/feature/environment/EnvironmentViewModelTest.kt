package com.x8bit.bitwarden.ui.auth.feature.environment

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EnvironmentViewModelTest : BaseViewModelTest() {

    private val fakeEnvironmentRepository = FakeEnvironmentRepository()

    @Suppress("MaxLineLength")
    @Test
    fun `initial state should be correct when there is no saved state and the current environment is not self-hosted`() {
        val viewModel = createViewModel()
        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `initial state should be correct when there is no saved state and the current environment is self-hosted`() {
        val selfHostedEnvironmentUrlData = EnvironmentUrlDataJson(
            base = "self-hosted-base",
            api = "self-hosted-api",
            identity = "self-hosted-identity",
            icon = "self-hosted-icons",
            webVault = "self-hosted-web-vault",
        )
        fakeEnvironmentRepository.environment = Environment.SelfHosted(
            environmentUrlData = selfHostedEnvironmentUrlData,
        )
        val viewModel = createViewModel()
        assertEquals(
            DEFAULT_STATE.copy(
                serverUrl = "self-hosted-base",
                webVaultServerUrl = "self-hosted-web-vault",
                apiServerUrl = "self-hosted-api",
                identityServerUrl = "self-hosted-identity",
                iconsServerUrl = "self-hosted-icons",
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `initial state should be correct when restoring from the save state handle`() {
        val savedState = DEFAULT_STATE.copy(
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
            DEFAULT_STATE.copy(
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
            viewModel.trySendAction(EnvironmentAction.CloseClick)
            assertEquals(EnvironmentEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `SaveClick should show the error dialog when any URLs are invalid`() = runTest {
        assertEquals(
            Environment.Us,
            fakeEnvironmentRepository.environment,
        )

        val viewModel = createViewModel()
        // Update to valid absolute URL
        listOf(
            EnvironmentAction.WebVaultServerUrlChange(
                webVaultServerUrl = "web vault",
            ),
        )
            .forEach { viewModel.trySendAction(it) }

        val initialState = DEFAULT_STATE.copy(webVaultServerUrl = "web vault")
        assertEquals(
            initialState,
            viewModel.stateFlow.value,
        )

        viewModel.trySendAction(EnvironmentAction.SaveClick)

        assertEquals(
            initialState.copy(
                shouldShowErrorDialog = true,
            ),
            viewModel.stateFlow.value,
        )

        // The Environment has not been updated
        assertEquals(
            Environment.Us,
            fakeEnvironmentRepository.environment,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `SaveClick should emit NavigateBack and ShowToast and update the environment when all URLs are valid`() =
        runTest {
            assertEquals(
                Environment.Us,
                fakeEnvironmentRepository.environment,
            )

            val viewModel = createViewModel()
            // Update to valid absolute or relative URLs
            listOf(
                EnvironmentAction.ServerUrlChange(
                    serverUrl = "https://server-url",
                ),
                EnvironmentAction.WebVaultServerUrlChange(
                    webVaultServerUrl = "http://web-vault-url",
                ),
                EnvironmentAction.ApiServerUrlChange(
                    apiServerUrl = "api-url",
                ),
                EnvironmentAction.IdentityServerUrlChange(
                    identityServerUrl = "identity-url",
                ),
                EnvironmentAction.IconsServerUrlChange(
                    iconsServerUrl = "icons-url",
                ),
            )
                .forEach { viewModel.trySendAction(it) }

            viewModel.eventFlow.test {
                viewModel.trySendAction(EnvironmentAction.SaveClick)

                assertEquals(
                    EnvironmentEvent.ShowToast(R.string.environment_saved.asText()),
                    awaitItem(),
                )
                assertEquals(
                    EnvironmentEvent.NavigateBack,
                    awaitItem(),
                )
                // All the updated URLs should be prefixed with "https://" or "http://"
                assertEquals(
                    Environment.SelfHosted(
                        environmentUrlData = EnvironmentUrlDataJson(
                            base = "https://server-url",
                            api = "https://api-url",
                            identity = "https://identity-url",
                            icon = "https://icons-url",
                            notifications = null,
                            webVault = "http://web-vault-url",
                            events = null,
                        ),
                    ),
                    fakeEnvironmentRepository.environment,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `SaveClick should emit NavigateBack and ShowToast and update the environment when some URLs are valid and others are null`() =
        runTest {
            assertEquals(
                Environment.Us,
                fakeEnvironmentRepository.environment,
            )

            val viewModel = createViewModel()
            // Update to valid absolute URL
            listOf(
                EnvironmentAction.WebVaultServerUrlChange(
                    webVaultServerUrl = "http://web-vault-url",
                ),
            )
                .forEach { viewModel.trySendAction(it) }

            viewModel.eventFlow.test {
                viewModel.trySendAction(EnvironmentAction.SaveClick)

                assertEquals(
                    EnvironmentEvent.ShowToast(R.string.environment_saved.asText()),
                    awaitItem(),
                )
                assertEquals(
                    EnvironmentEvent.NavigateBack,
                    awaitItem(),
                )
                // All the updated URLs should be prefixed with "https://" or "http://"
                assertEquals(
                    Environment.SelfHosted(
                        environmentUrlData = EnvironmentUrlDataJson(
                            base = "",
                            api = null,
                            identity = null,
                            icon = null,
                            notifications = null,
                            webVault = "http://web-vault-url",
                            events = null,
                        ),
                    ),
                    fakeEnvironmentRepository.environment,
                )
            }
        }

    @Test
    fun `ServerUrlChange should update the server URL`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(
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
        viewModel.trySendAction(
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
        viewModel.trySendAction(
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
        viewModel.trySendAction(
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
        viewModel.trySendAction(
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
            environmentRepository = fakeEnvironmentRepository,
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
            shouldShowErrorDialog = false,
        )
    }
}
