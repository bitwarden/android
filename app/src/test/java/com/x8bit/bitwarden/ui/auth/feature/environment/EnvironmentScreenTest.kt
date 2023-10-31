package com.x8bit.bitwarden.ui.auth.feature.environment

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EnvironmentScreenTest : BaseComposeTest() {
    private var onNavigateBackCalled = false
    private val mutableEventFlow = MutableSharedFlow<EnvironmentEvent>(
        extraBufferCapacity = Int.MAX_VALUE,
    )
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<EnvironmentViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            EnvironmentScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `NavigateBack event should invoke onNavigateBack`() {
        mutableEventFlow.tryEmit(EnvironmentEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `close click should send CloseClick`() {
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        verify {
            viewModel.trySendAction(EnvironmentAction.CloseClick)
        }
    }

    @Test
    fun `save click should send SaveClick`() {
        composeTestRule.onNodeWithText("Save").performClick()
        verify {
            viewModel.trySendAction(EnvironmentAction.SaveClick)
        }
    }

    @Test
    fun `server URL should change according to the state`() {
        composeTestRule
            .onNodeWithText("Server URL")
            // Click to focus to see placeholder
            .performClick()
            .assertTextEquals("Server URL", "ex. https://bitwarden.company.com", "")

        mutableStateFlow.update { it.copy(serverUrl = "server-url") }

        composeTestRule
            .onNodeWithText("Server URL")
            .assertTextEquals("Server URL", "server-url")
    }

    @Test
    fun `server URL change should send ServerUrlChange`() {
        composeTestRule.onNodeWithText("Server URL").performTextInput("updated-server-url")
        verify {
            viewModel.trySendAction(
                EnvironmentAction.ServerUrlChange(serverUrl = "updated-server-url"),
            )
        }
    }

    @Test
    fun `web vault URL should change according to the state`() {
        composeTestRule
            .onNodeWithText("Web vault server URL")
            .assertTextEquals("Web vault server URL", "")

        mutableStateFlow.update { it.copy(webVaultServerUrl = "web-vault-url") }

        composeTestRule
            .onNodeWithText("Web vault server URL")
            .assertTextEquals("Web vault server URL", "web-vault-url")
    }

    @Test
    fun `web vault server URL change should send WebVaultServerUrlChange`() {
        composeTestRule
            .onNodeWithText("Web vault server URL")
            .performTextInput("updated-web-vault-url")
        verify {
            viewModel.trySendAction(
                EnvironmentAction.WebVaultServerUrlChange(
                    webVaultServerUrl = "updated-web-vault-url",
                ),
            )
        }
    }

    @Test
    fun `API server URL should change according to the state`() {
        composeTestRule
            .onNodeWithText("API server URL")
            .assertTextEquals("API server URL", "")

        mutableStateFlow.update { it.copy(apiServerUrl = "api-url") }

        composeTestRule
            .onNodeWithText("API server URL")
            .assertTextEquals("API server URL", "api-url")
    }

    @Test
    fun `API server URL change should send ApiServerUrlChange`() {
        composeTestRule
            .onNodeWithText("API server URL")
            .performTextInput("updated-api-url")
        verify {
            viewModel.trySendAction(
                EnvironmentAction.ApiServerUrlChange(apiServerUrl = "updated-api-url"),
            )
        }
    }

    @Test
    fun `identity server URL should change according to the state`() {
        composeTestRule
            .onNodeWithText("Identity server URL")
            .assertTextEquals("Identity server URL", "")

        mutableStateFlow.update { it.copy(identityServerUrl = "identity-url") }

        composeTestRule
            .onNodeWithText("Identity server URL")
            .assertTextEquals("Identity server URL", "identity-url")
    }

    @Test
    fun `identity server URL change should send IdentityServerUrlChange`() {
        composeTestRule
            .onNodeWithText("Identity server URL")
            .performTextInput("updated-identity-url")
        verify {
            viewModel.trySendAction(
                EnvironmentAction.IdentityServerUrlChange(
                    identityServerUrl = "updated-identity-url",
                ),
            )
        }
    }

    @Test
    fun `icons server URL should change according to the state`() {
        composeTestRule
            .onNodeWithText("Icons server URL")
            .assertTextEquals("Icons server URL", "")

        mutableStateFlow.update { it.copy(iconsServerUrl = "icons-url") }

        composeTestRule
            .onNodeWithText("Icons server URL")
            .assertTextEquals("Icons server URL", "icons-url")
    }

    @Test
    fun `icons server URL change should send IconsServerUrlChange`() {
        composeTestRule
            .onNodeWithText("Icons server URL")
            .performTextInput("updated-icons-url")
        verify {
            viewModel.trySendAction(
                EnvironmentAction.IconsServerUrlChange(iconsServerUrl = "updated-icons-url"),
            )
        }
    }

    companion object {
        val DEFAULT_STATE = EnvironmentState(
            serverUrl = "",
            webVaultServerUrl = "",
            apiServerUrl = "",
            identityServerUrl = "",
            iconsServerUrl = "",
        )
    }
}
