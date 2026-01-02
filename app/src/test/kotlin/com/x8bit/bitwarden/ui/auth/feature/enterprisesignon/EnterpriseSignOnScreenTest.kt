package com.x8bit.bitwarden.ui.auth.feature.enterprisesignon

import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
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
import androidx.compose.ui.test.performTextInput
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.assertNoDialogExists
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.model.AuthTabLaunchers
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

class EnterpriseSignOnScreenTest : BitwardenComposeTest() {
    private val ssoLauncher: ActivityResultLauncher<Intent> = mockk()
    private var onNavigateBackCalled = false
    private var onNavigateToSetPasswordCalled = false
    private var onNavigateToTwoFactorLoginEmailAndOrgIdentifier: Pair<String, String>? = null
    private val mutableEventFlow = bufferedMutableSharedFlow<EnterpriseSignOnEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<EnterpriseSignOnViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    private val intentManager: IntentManager = mockk {
        every { startAuthTab(uri = any(), redirectScheme = any(), launcher = any()) } just runs
    }

    @Before
    fun setup() {
        setContent(
            authTabLaunchers = AuthTabLaunchers(
                duo = mockk(),
                sso = ssoLauncher,
                webAuthn = mockk(),
            ),
            intentManager = intentManager,
        ) {
            EnterpriseSignOnScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToSetPassword = { onNavigateToSetPasswordCalled = true },
                onNavigateToTwoFactorLogin = { email, orgIdentifier ->
                    onNavigateToTwoFactorLoginEmailAndOrgIdentifier = email to orgIdentifier
                },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `app bar log in click should send LogInClick action`() {
        composeTestRule.onNodeWithText("Log in").performClick()
        verify { viewModel.trySendAction(EnterpriseSignOnAction.LogInClick) }
    }

    @Test
    fun `close button click should send CloseButtonClick action`() {
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        verify {
            viewModel.trySendAction(EnterpriseSignOnAction.CloseButtonClick)
        }
    }

    @Test
    fun `organization identifier input change should send OrgIdentifierInputChange action`() {
        val input = "input"
        composeTestRule.onNodeWithText("Organization identifier").performTextInput(input)
        verify {
            viewModel.trySendAction(EnterpriseSignOnAction.OrgIdentifierInputChange(input))
        }
    }

    @Test
    fun `organization identifier should change according to state`() {
        composeTestRule
            .onNodeWithText("Organization identifier")
            .assertTextEquals("Organization identifier", "")

        mutableStateFlow.update { it.copy(orgIdentifierInput = "test") }

        composeTestRule
            .onNodeWithText("Organization identifier")
            .assertTextEquals("Organization identifier", "test")
    }

    @Test
    fun `NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(EnterpriseSignOnEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `NavigateToSsoLogin should call startCustomTabsActivity`() {
        val ssoUri = Uri.parse("https://identity.bitwarden.com/sso-test")
        val scheme = "bitwarden"
        mutableEventFlow.tryEmit(EnterpriseSignOnEvent.NavigateToSsoLogin(ssoUri, scheme))
        verify(exactly = 1) {
            intentManager.startAuthTab(
                uri = ssoUri,
                redirectScheme = scheme,
                launcher = ssoLauncher,
            )
        }
    }

    @Test
    fun `NavigateToSetPassword should call onNavigateToSetPassword`() {
        mutableEventFlow.tryEmit(EnterpriseSignOnEvent.NavigateToSetPassword)
        assertTrue(onNavigateToSetPasswordCalled)
    }

    @Test
    fun `NavigateToTwoFactorLogin should call onNavigateToTwoFactorLogin`() {
        val email = "test@example.com"
        val orgIdentifier = "org_identifier"
        mutableEventFlow.tryEmit(
            EnterpriseSignOnEvent.NavigateToTwoFactorLogin(email, orgIdentifier),
        )
        assertEquals(email to orgIdentifier, onNavigateToTwoFactorLoginEmailAndOrgIdentifier)
    }

    @Test
    fun `error dialog should be shown or hidden according to the state`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = EnterpriseSignOnState.DialogState.Error(
                    title = "Error dialog title".asText(),
                    message = "Error dialog message".asText(),
                ),
            )
        }

        composeTestRule.onNode(isDialog()).assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Error dialog title")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Error dialog message")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "Okay")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `loading dialog should be displayed according to state`() {
        composeTestRule.assertNoDialogExists()
        composeTestRule.onNodeWithText("Loading").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = EnterpriseSignOnState.DialogState.Loading(
                    message = "Loading".asText(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText("Loading")
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
    }

    @Test
    fun `error dialog OK click should send DialogDismiss action`() {
        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                dialogState = EnterpriseSignOnState.DialogState.Error(
                    title = "title".asText(),
                    message = "message".asText(),
                ),
            )
        }

        composeTestRule
            .onAllNodesWithText(text = "Okay")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(EnterpriseSignOnAction.DialogDismiss) }
    }

    @Test
    fun `ConfirmKeyConnector dialog should be shown or hidden according to the state`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = EnterpriseSignOnState.DialogState.KeyConnectorDomain(
                    keyConnectorDomain = "bitwarden.com",
                ),
            )
        }

        composeTestRule.onNode(isDialog()).assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Confirm Key Connector domain")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                "Please confirm the domain below with your organization administrator." +
                    "\n\n" +
                    "Key Connector domain:" +
                    "\n" +
                    "bitwarden.com",
            )
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Confirm")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Cancel")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `ConfirmKeyConnector Confirm click should send ConfirmKeyConnectorDomainClick action`() {
        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                dialogState = EnterpriseSignOnState.DialogState.KeyConnectorDomain(
                    keyConnectorDomain = "bitwarden.com",
                ),
            )
        }

        composeTestRule
            .onAllNodesWithText("Confirm")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(EnterpriseSignOnAction.ConfirmKeyConnectorDomainClick) }
    }

    @Test
    fun `ConfirmKeyConnector Cancel click should send CancelKeyConnectorDomainClick action`() {
        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                dialogState = EnterpriseSignOnState.DialogState.KeyConnectorDomain(
                    keyConnectorDomain = "bitwarden.com",
                ),
            )
        }

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(EnterpriseSignOnAction.CancelKeyConnectorDomainClick) }
    }

    companion object {
        private val DEFAULT_STATE = EnterpriseSignOnState(
            dialogState = null,
            orgIdentifierInput = "",
        )
    }
}
