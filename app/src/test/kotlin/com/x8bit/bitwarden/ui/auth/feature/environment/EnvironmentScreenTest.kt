package com.x8bit.bitwarden.ui.auth.feature.environment

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.model.FileData
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.auth.feature.environment.EnvironmentState.DialogState
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.manager.keychain.KeyChainManager
import com.x8bit.bitwarden.ui.platform.manager.keychain.model.PrivateKeyAliasSelectionResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EnvironmentScreenTest : BitwardenComposeTest() {
    private var onNavigateBackCalled = false
    private val mutableEventFlow = bufferedMutableSharedFlow<EnvironmentEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mockIntentManager = mockk<IntentManager>(relaxed = true)
    private val mockKeyChainManager = mockk<KeyChainManager> {
        coEvery {
            choosePrivateKeyAlias(any())
        } returns PrivateKeyAliasSelectionResult.Success("mockAlias")
    }
    private val viewModel = mockk<EnvironmentViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        setContent(
            intentManager = mockIntentManager,
            keyChainManager = mockKeyChainManager,
        ) {
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
    fun `on ShowSnackbar should display snackbar content`() {
        val message = "message"
        val data = BitwardenSnackbarData(message = message.asText())
        composeTestRule.onNodeWithText(text = message).assertDoesNotExist()
        mutableEventFlow.tryEmit(EnvironmentEvent.ShowSnackbar(data = data))
        composeTestRule.onNodeWithText(text = message).assertIsDisplayed()
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
    fun `error dialog should be shown or hidden according to the state`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialog = DialogState.Error(
                    ("One or more of the URLs entered are invalid. " +
                        "Please revise it and try to save again.")
                        .asText(),
                ),
            )
        }

        composeTestRule.onNode(isDialog()).assertIsDisplayed()

        composeTestRule
            .onNodeWithText(text = "An error has occurred")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                "One or more of the URLs entered are invalid. " +
                    "Please revise it and try to save again.",
            )
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "Okay")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `error dialog OK click should send ErrorDialogDismiss action`() {
        mutableStateFlow.update {
            it.copy(
                dialog = DialogState.Error("Error".asText()),
            )
        }
        composeTestRule
            .onAllNodesWithText(text = "Okay")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(EnvironmentAction.DialogDismiss) }
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
    fun `use system certificate click should send UseSystemKeyCertificateClick`() {
        composeTestRule
            .onNodeWithText("Choose system certificate")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        verify {
            viewModel.trySendAction(EnvironmentAction.ChooseSystemCertificateClick)
        }
    }

    @Test
    fun `ShowSystemCertificateSelection event should show system certificate selection dialog`() {
        mutableEventFlow.tryEmit(
            EnvironmentEvent.ShowSystemCertificateSelectionDialog(serverUrl = ""),
        )
        coVerify { mockKeyChainManager.choosePrivateKeyAlias(null) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `system certificate selection should send SystemCertificateSelectionResultReceive action`() {
        coEvery {
            mockKeyChainManager.choosePrivateKeyAlias(null)
        } returns PrivateKeyAliasSelectionResult.Success("alias")

        mutableEventFlow.tryEmit(
            EnvironmentEvent.ShowSystemCertificateSelectionDialog(serverUrl = ""),
        )

        verify {
            viewModel.trySendAction(
                EnvironmentAction.SystemCertificateSelectionResultReceive(
                    privateKeyAliasSelectionResult = PrivateKeyAliasSelectionResult.Success(
                        alias = "alias",
                    ),
                ),
            )
        }
    }

    @Test
    fun `key alias should change according to the state`() {
        composeTestRule
            .onNodeWithText("Certificate alias")
            .assertTextEquals("Certificate alias", "")

        mutableStateFlow.update { it.copy(keyAlias = "mock-alias") }

        composeTestRule
            .onNodeWithText("Certificate alias")
            .assertTextEquals("Certificate alias", "mock-alias")
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

    @Test
    fun `ConfirmOverwriteCertificate dialog should display based on state`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialog = DialogState.ConfirmOverwriteAlias(
                    title = "Confirm overwrite".asText(),
                    message = "Overwrite existing certificate?".asText(),
                    triggeringAction = EnvironmentAction.SetCertificateInfoResultReceive(
                        certificateFileData = mockk(),
                        alias = "mockAlias",
                        password = "mockPassword",
                    ),
                ),
            )
        }

        composeTestRule.onNode(isDialog()).assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Confirm overwrite")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Overwrite existing certificate?")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Cancel")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Replace certificate")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ConfirmOverwriteCertificate dialog Replace certificate click should send ConfirmOverwriteCertificate action`() {
        val mockFileData = mockk<FileData>()
        mutableStateFlow.update {
            it.copy(
                dialog = DialogState.ConfirmOverwriteAlias(
                    title = "Confirm overwrite".asText(),
                    message = "Overwrite existing certificate?".asText(),
                    triggeringAction = EnvironmentAction.SetCertificateInfoResultReceive(
                        certificateFileData = mockFileData,
                        alias = "mockAlias",
                        password = "mockPassword",
                    ),
                ),
            )
        }

        composeTestRule
            .onAllNodesWithText("Replace certificate")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                EnvironmentAction.ConfirmOverwriteCertificateClick(
                    EnvironmentAction.SetCertificateInfoResultReceive(
                        certificateFileData = mockFileData,
                        alias = "mockAlias",
                        password = "mockPassword",
                    ),
                ),
            )
        }
    }

    @Test
    fun `ConfirmOverwriteCertificate dialog Cancel click should send DismissDialog action`() {
        mutableStateFlow.update {
            it.copy(
                dialog = DialogState.ConfirmOverwriteAlias(
                    title = "Confirm overwrite".asText(),
                    message = "Overwrite existing certificate?".asText(),
                    triggeringAction = EnvironmentAction.SetCertificateInfoResultReceive(
                        certificateFileData = mockk(),
                        alias = "mockAlias",
                        password = "mockPassword",
                    ),
                ),
            )
        }

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                EnvironmentAction.DialogDismiss,
            )
        }
    }

    companion object {
        val DEFAULT_STATE = EnvironmentState(
            serverUrl = "",
            keyAlias = "",
            webVaultServerUrl = "",
            apiServerUrl = "",
            identityServerUrl = "",
            iconsServerUrl = "",
            keyHost = null,
            dialog = null,
        )
    }
}
