package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isEditable
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onSiblings
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.core.net.toUri
import com.bitwarden.vault.UriMatchType
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.ui.autofill.fido2.manager.Fido2CompletionManager
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricsManager
import com.x8bit.bitwarden.ui.platform.manager.exit.ExitManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.manager.permissions.FakePermissionManager
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorMode
import com.x8bit.bitwarden.ui.util.assertNoDialogExists
import com.x8bit.bitwarden.ui.util.isProgressBar
import com.x8bit.bitwarden.ui.util.onAllNodesWithContentDescriptionAfterScroll
import com.x8bit.bitwarden.ui.util.onAllNodesWithTextAfterScroll
import com.x8bit.bitwarden.ui.util.onNodeWithContentDescriptionAfterScroll
import com.x8bit.bitwarden.ui.util.onNodeWithTextAfterScroll
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.CustomFieldAction
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.CustomFieldType
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriItem
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.VaultCardExpirationMonth
import com.x8bit.bitwarden.ui.vault.model.VaultCollection
import com.x8bit.bitwarden.ui.vault.model.VaultIdentityTitle
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import io.mockk.every
import io.mockk.invoke
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@Suppress("LargeClass")
class VaultAddEditScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateQrCodeScanScreenCalled = false
    private var onNavigateToManualCodeEntryScreenCalled = false
    private var onNavigateToGeneratorModalType: GeneratorMode.Modal? = null
    private var onNavigateToAttachmentsId: String? = null
    private var onNavigateToMoveToOrganizationId: String? = null

    private val mutableEventFlow = bufferedMutableSharedFlow<VaultAddEditEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE_LOGIN)

    private val fakePermissionManager: FakePermissionManager = FakePermissionManager()

    private val viewModel = mockk<VaultAddEditViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }
    private val exitManager: ExitManager = mockk {
        every { exitApplication() } just runs
    }
    private val intentManager: IntentManager = mockk {
        every { launchUri(any()) } just runs
    }
    private val fido2CompletionManager: Fido2CompletionManager = mockk {
        every { completeFido2Registration(any()) } just runs
    }
    private val biometricsManager: BiometricsManager = mockk {
        every { isUserVerificationSupported } returns true
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            VaultAddEditScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToQrCodeScanScreen = { onNavigateQrCodeScanScreenCalled = true },
                onNavigateToManualCodeEntryScreen = {
                    onNavigateToManualCodeEntryScreenCalled = true
                },
                onNavigateToGeneratorModal = { onNavigateToGeneratorModalType = it },
                onNavigateToAttachments = { onNavigateToAttachmentsId = it },
                onNavigateToMoveToOrganization = { id, _ -> onNavigateToMoveToOrganizationId = id },
                viewModel = viewModel,
                permissionsManager = fakePermissionManager,
                exitManager = exitManager,
                intentManager = intentManager,
                fido2CompletionManager = fido2CompletionManager,
                biometricsManager = biometricsManager,
            )
        }
    }

    @Test
    fun `on ExitApp event should call the exitApplication of ExitManager`() {
        mutableEventFlow.tryEmit(VaultAddEditEvent.ExitApp)
        verify { exitManager.exitApplication() }
    }

    @Test
    fun `on NavigateBack event should invoke onNavigateBack`() {
        mutableEventFlow.tryEmit(VaultAddEditEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `on NavigateToTooltipUri Event should invoke IntentManager`() {
        mutableEventFlow.tryEmit(VaultAddEditEvent.NavigateToTooltipUri)
        verify {
            intentManager.launchUri(
                "https://bitwarden.com/help/managing-items/#protect-individual-items".toUri(),
            )
        }
    }

    @Test
    fun `on NavigateToQrCodeScan event should invoke NavigateToQrCodeScan`() {
        mutableEventFlow.tryEmit(VaultAddEditEvent.NavigateToQrCodeScan)
        assertTrue(onNavigateQrCodeScanScreenCalled)
    }

    @Test
    fun `on NavigateToManualCodeEntry event should invoke NavigateToManualCodeEntry`() {
        mutableEventFlow.tryEmit(VaultAddEditEvent.NavigateToManualCodeEntry)
        assertTrue(onNavigateToManualCodeEntryScreenCalled)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on NavigateToGeneratorModal event in password mode should invoke NavigateToGeneratorModal with Password Generator Mode `() {
        mutableEventFlow.tryEmit(
            VaultAddEditEvent.NavigateToGeneratorModal(
                generatorMode = GeneratorMode.Modal.Password,
            ),
        )
        assertEquals(GeneratorMode.Modal.Password, onNavigateToGeneratorModalType)
    }

    @Test
    fun `on NavigateToAttachments event should invoke onNavigateToAttachments`() {
        val cipherId = "cipherId-1234"
        mutableEventFlow.tryEmit(VaultAddEditEvent.NavigateToAttachments(cipherId))
        assertEquals(cipherId, onNavigateToAttachmentsId)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `on NavigateToMoveToOrganization event should invoke onNavigateToMoveToOrganization with the correct ID`() {
        val cipherId = "cipherId-1234"
        mutableEventFlow.tryEmit(VaultAddEditEvent.NavigateToMoveToOrganization(cipherId))
        assertEquals(cipherId, onNavigateToMoveToOrganizationId)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on NavigateToGeneratorModal event in username mode should invoke NavigateToGeneratorModal with Username Generator Mode `() {
        val website = "bitwarden.com"
        mutableEventFlow.tryEmit(
            VaultAddEditEvent.NavigateToGeneratorModal(
                generatorMode = GeneratorMode.Modal.Username(website),
            ),
        )
        assertEquals(GeneratorMode.Modal.Username(website), onNavigateToGeneratorModalType)
    }

    @Test
    fun `on CompleteFido2Create event should invoke Fido2CompletionManager`() {
        val result = Fido2RegisterCredentialResult.Success(
            registrationResponse = "mockRegistrationResponse",
        )
        mutableEventFlow.tryEmit(VaultAddEditEvent.CompleteFido2Registration(result = result))
        verify { fido2CompletionManager.completeFido2Registration(result) }
    }

    @Test
    fun `Fido2Error dialog should display based on state`() {
        mutableStateFlow.value = DEFAULT_STATE_LOGIN.copy(
            dialog = VaultAddEditState.DialogState.Fido2Error("mockMessage".asText()),
        )

        composeTestRule
            .onAllNodesWithText("mockMessage")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `fido2 master password prompt dialog should display based on state`() {
        val dialogTitle = "Master password confirmation"
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText(dialogTitle).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(dialog = VaultAddEditState.DialogState.Fido2MasterPasswordPrompt)
        }

        composeTestRule
            .onNodeWithText(dialogTitle)
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))

        composeTestRule
            .onAllNodesWithText(text = "Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.DismissFido2VerificationDialogClick,
            )
        }

        composeTestRule
            .onAllNodesWithText(text = "Master password")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performTextInput("password")
        composeTestRule
            .onAllNodesWithText(text = "Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.MasterPasswordFido2VerificationSubmit(
                    password = "password",
                ),
            )
        }
    }

    @Test
    fun `fido2 master password error dialog should display based on state`() {
        val dialogMessage = "Invalid master password. Try again."
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText(dialogMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(dialog = VaultAddEditState.DialogState.Fido2MasterPasswordError)
        }

        composeTestRule
            .onNodeWithText(dialogMessage)
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))

        composeTestRule
            .onAllNodesWithText(text = "Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.RetryFido2PasswordVerificationClick,
            )
        }
    }

    @Test
    fun `fido2 pin prompt dialog should display based on state`() {
        val dialogTitle = "Verify PIN"
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText(dialogTitle).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(dialog = VaultAddEditState.DialogState.Fido2PinPrompt)
        }

        composeTestRule
            .onNodeWithText(dialogTitle)
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))

        composeTestRule
            .onAllNodesWithText(text = "Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.DismissFido2VerificationDialogClick,
            )
        }

        composeTestRule
            .onAllNodesWithText(text = "PIN")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performTextInput("PIN")
        composeTestRule
            .onAllNodesWithText(text = "Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.PinFido2VerificationSubmit(
                    pin = "PIN",
                ),
            )
        }
    }

    @Test
    fun `fido2 pin error dialog should display based on state`() {
        val dialogMessage = "Invalid PIN. Try again."
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText(dialogMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(dialog = VaultAddEditState.DialogState.Fido2PinError)
        }

        composeTestRule
            .onNodeWithText(dialogMessage)
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))

        composeTestRule
            .onAllNodesWithText(text = "Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.RetryFido2PinVerificationClick,
            )
        }
    }

    @Test
    fun `fido2 pin set up prompt dialog should display based on state`() {
        val dialogMessage = "Enter your PIN code"
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText(dialogMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(dialog = VaultAddEditState.DialogState.Fido2PinSetUpPrompt)
        }

        composeTestRule
            .onNodeWithText(dialogMessage)
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))

        composeTestRule
            .onAllNodesWithText(text = "Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.DismissFido2VerificationDialogClick,
            )
        }

        composeTestRule
            .onAllNodesWithText(text = "PIN")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performTextInput("1234")
        composeTestRule
            .onAllNodesWithText(text = "Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.PinFido2SetUpSubmit(
                    pin = "1234",
                ),
            )
        }
    }

    @Test
    fun `fido2 pin set up error dialog should display based on state`() {
        val dialogMessage = "The PIN field is required."
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText(dialogMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(dialog = VaultAddEditState.DialogState.Fido2PinSetUpError)
        }

        composeTestRule
            .onNodeWithText(dialogMessage)
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))

        composeTestRule
            .onAllNodesWithText(text = "Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.PinFido2SetUpRetryClick,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking dismiss dialog on Fido2Error dialog should send Fido2ErrorDialogDismissed action`() {
        mutableStateFlow.value = DEFAULT_STATE_LOGIN.copy(
            dialog = VaultAddEditState.DialogState.Fido2Error("mockMessage".asText()),
        )

        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify { viewModel.trySendAction(VaultAddEditAction.Common.Fido2ErrorDialogDismissed) }
    }

    @Test
    fun `close button should update according to state`() {
        composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(shouldShowCloseButton = false)
        }

        composeTestRule.onNodeWithContentDescription("Close").assertDoesNotExist()
    }

    @Test
    fun `clicking close button should send CloseClick action`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Close")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.CloseClick,
            )
        }
    }

    @Test
    fun `clicking save button should send SaveClick action`() {
        composeTestRule
            .onNodeWithText(text = "Save")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.SaveClick,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking dismiss dialog button on InitialAutofillPrompt should send InitialAutofillDialogDismissed action`() {
        mutableStateFlow.value = DEFAULT_STATE_LOGIN_DIALOG.copy(
            dialog = VaultAddEditState.DialogState.InitialAutofillPrompt,
        )

        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.InitialAutofillDialogDismissed,
            )
        }
    }

    @Test
    fun `InitialAutofillPrompt is shown according to state`() {
        mutableStateFlow.value = DEFAULT_STATE_LOGIN

        composeTestRule.assertNoDialogExists()

        mutableStateFlow.value = DEFAULT_STATE_LOGIN_DIALOG.copy(
            dialog = VaultAddEditState.DialogState.InitialAutofillPrompt,
        )

        composeTestRule
            .onAllNodesWithText("Bitwarden Autofill Service")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        mutableStateFlow.value = DEFAULT_STATE_LOGIN_DIALOG.copy(
            dialog = VaultAddEditState.DialogState.Loading("Loading".asText()),
        )
    }

    @Test
    fun `clicking dismiss dialog button should send DismissDialog action`() {
        mutableStateFlow.value = DEFAULT_STATE_LOGIN_DIALOG

        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.DismissDialog,
            )
        }
    }

    @Test
    fun `dialog should display when state is updated to do so`() {
        mutableStateFlow.value = DEFAULT_STATE_LOGIN

        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsNotDisplayed()

        mutableStateFlow.value = DEFAULT_STATE_LOGIN_DIALOG

        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `error text and retry should be displayed according to state`() {
        val message = "error_message"
        mutableStateFlow.update {
            it.copy(viewState = VaultAddEditState.ViewState.Loading)
        }
        composeTestRule.onNodeWithText(message).assertIsNotDisplayed()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultAddEditState.ViewState.Content(
                    common = VaultAddEditState.ViewState.Content.Common(),
                    type = VaultAddEditState.ViewState.Content.ItemType.Login(),
                    isIndividualVaultDisabled = false,
                ),
            )
        }
        composeTestRule.onNodeWithText(message).assertIsNotDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = VaultAddEditState.ViewState.Error(message.asText()))
        }
        composeTestRule.onNodeWithText(message).assertIsDisplayed()
    }

    @Test
    fun `progressbar should be displayed according to state`() {
        mutableStateFlow.update {
            it.copy(viewState = VaultAddEditState.ViewState.Loading)
        }
        // There are 2 because of the pull-to-refresh
        composeTestRule.onAllNodes(isProgressBar).assertCountEquals(2)

        mutableStateFlow.update {
            it.copy(viewState = VaultAddEditState.ViewState.Error("Fail".asText()))
        }
        // Only pull-to-refresh remains
        composeTestRule.onAllNodes(isProgressBar).assertCountEquals(1)

        mutableStateFlow.update {
            it.copy(
                viewState = VaultAddEditState.ViewState.Content(
                    common = VaultAddEditState.ViewState.Content.Common(),
                    type = VaultAddEditState.ViewState.Content.ItemType.Login(),
                    isIndividualVaultDisabled = false,
                ),
            )
        }
        // Only pull-to-refresh remains
        composeTestRule.onAllNodes(isProgressBar).assertCountEquals(1)
    }

    @Test
    fun `clicking a Type Option should send TypeOptionSelect action`() {
        // Opens the menu
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Login. Type")
            .performClick()

        // Choose the option from the menu
        composeTestRule
            .onAllNodesWithText(text = "Login")
            .onLast()
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.TypeOptionSelect(VaultAddEditState.ItemTypeOption.LOGIN),
            )
        }
    }

    @Test
    fun `the Type Option field should display the text of the selected item type`() {
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Login. Type")
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultAddEditState.ViewState.Content(
                    common = VaultAddEditState.ViewState.Content.Common(),
                    type = VaultAddEditState.ViewState.Content.ItemType.Card(),
                    isIndividualVaultDisabled = false,
                ),
            )
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Card. Type")
            .assertIsDisplayed()
    }

    @Test
    fun `in ItemType_Login state the password should change according to state`() {
        composeTestRule
            .onNodeWithTextAfterScroll("Password")
            .assertTextEquals("Password", "")
            .assertIsEnabled()
        composeTestRule
            .onNodeWithContentDescription("Check if password has been exposed.")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Generate password")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Show")
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultAddEditState.ViewState.Content(
                    common = VaultAddEditState.ViewState.Content.Common(),
                    type = VaultAddEditState.ViewState.Content.ItemType.Login(
                        password = "p@ssw0rd",
                    ),
                    isIndividualVaultDisabled = false,
                ),
            )
        }

        composeTestRule
            .onNodeWithText("Password")
            .assertTextEquals("Password", "••••••••")
        composeTestRule
            .onNodeWithContentDescription("Check if password has been exposed.")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Generate password")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Show")
            .assertIsDisplayed()

        // Click on the visibility icon to show the password
        composeTestRule
            .onNodeWithContentDescription("Show")
            .performClick()
        composeTestRule
            .onNodeWithText("Password")
            .assertTextEquals("Password", "p@ssw0rd")
            .assertIsEnabled()
        composeTestRule
            .onNodeWithContentDescription("Hide")
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultAddEditState.ViewState.Content(
                    common = VaultAddEditState.ViewState.Content.Common(),
                    type = VaultAddEditState.ViewState.Content.ItemType.Login(
                        password = "p@ssw0rd",
                        canViewPassword = false,
                    ),
                    isIndividualVaultDisabled = false,
                ),
            )
        }

        composeTestRule
            .onNodeWithText("Password")
            .assertTextEquals("Password", "••••••••")
            .assertIsNotEnabled()
        composeTestRule
            .onNodeWithContentDescription("Check if password has been exposed.")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithContentDescription("Generate password")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithContentDescription("Hide")
            .assertDoesNotExist()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state changing password visibility state should send PasswordVisibilityChange`() {
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Password")
            .assertExists()
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Show")
            .assertExists()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.PasswordVisibilityChange(isVisible = true),
            )
        }
    }

    @Test
    fun `in ItemType_Login state changing Username text field should trigger UsernameTextChange`() {
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Username")
            .performTextInput(text = "TestUsername")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.UsernameTextChange(username = "TestUsername"),
            )
        }
    }

    @Test
    fun `in ItemType_Login the Username control should display the text provided by the state`() {
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Username")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(username = "NewUsername") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Username")
            .assertTextContains("NewUsername")
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state clicking Username text field generator action with non empty username should open dialog that triggers OpenUsernameGeneratorClick`() {
        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(username = "username") }
        }

        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Generate username")
            .performClick()

        composeTestRule
            .onNodeWithText("Yes")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.OpenUsernameGeneratorClick,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state clicking Username generator icon with empty username field should trigger OpenPasswordGeneratorClick`() {
        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(username = "") }
        }

        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Generate username")
            .performClick()

        composeTestRule.assertNoDialogExists()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.OpenUsernameGeneratorClick,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state clicking Password checker action should trigger PasswordCheckerClick`() {
        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(password = "password") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Password")
            .onSiblings()
            .filterToOne(hasContentDescription("Check if password has been exposed."))
            .performClick()

        verify {
            viewModel.trySendAction(VaultAddEditAction.ItemType.LoginType.PasswordCheckerClick)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state clicking Password text field generator action with non empty password field should open dialog that triggers OpenPasswordGeneratorClick`() {
        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(password = "password") }
        }

        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Password")
            .onSiblings()
            .onLast()
            .performClick()

        composeTestRule
            .onNodeWithText("Yes")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.OpenPasswordGeneratorClick,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state clicking Password generator icon with empty password field should trigger OpenPasswordGeneratorClick`() {
        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(password = "") }
        }

        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Password")
            .onSiblings()
            .onLast()
            .performClick()

        composeTestRule.assertNoDialogExists()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.OpenPasswordGeneratorClick,
            )
        }
    }

    @Test
    fun `in ItemType_Login state changing Password text field should trigger PasswordTextChange`() {
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Password")
            .performTextInput(text = "TestPassword")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.PasswordTextChange("TestPassword"),
            )
        }
    }

    @Test
    fun `in ItemType_Login the Password control should display the text provided by the state`() {
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Password")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(password = "NewPassword") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Password")
            .assertTextContains("•••••••••••")
    }

    @Test
    fun `in ItemType_Login state the Passkey should change according to state`() {
        mutableStateFlow.update {
            it.copy(
                viewState = VaultAddEditState.ViewState.Content(
                    common = VaultAddEditState.ViewState.Content.Common(),
                    type = VaultAddEditState.ViewState.Content.ItemType.Login(
                        fido2CredentialCreationDateTime = "fido2Credentials".asText(),
                        canViewPassword = false,
                        canEditItem = false,
                    ),
                    isIndividualVaultDisabled = false,
                ),
            )
        }

        composeTestRule
            .onNodeWithTextAfterScroll("Passkey")
            .assertTextEquals("Passkey", "fido2Credentials")
            .assertIsEnabled()
        composeTestRule
            .onNodeWithContentDescription("Remove passkey")
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultAddEditState.ViewState.Content(
                    common = VaultAddEditState.ViewState.Content.Common(),
                    type = VaultAddEditState.ViewState.Content.ItemType.Login(
                        fido2CredentialCreationDateTime = "fido2Credentials".asText(),
                        canViewPassword = false,
                        canEditItem = true,
                    ),
                    isIndividualVaultDisabled = false,
                ),
            )
        }

        composeTestRule
            .onNodeWithTextAfterScroll("Passkey")
            .assertTextEquals("Passkey", "fido2Credentials")
            .assertIsEnabled()
        composeTestRule
            .onNodeWithContentDescription("Remove passkey")
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultAddEditState.ViewState.Content(
                    common = VaultAddEditState.ViewState.Content.Common(),
                    type = VaultAddEditState.ViewState.Content.ItemType.Login(
                        fido2CredentialCreationDateTime = "fido2Credentials".asText(),
                        canViewPassword = true,
                        canEditItem = true,
                    ),
                    isIndividualVaultDisabled = false,
                ),
            )
        }

        // Click on Remove Passkey button
        composeTestRule
            .onNodeWithTextAfterScroll("Passkey")
            .assertExists()
        composeTestRule
            .onNodeWithContentDescription("Remove passkey")
            .assertIsDisplayed()
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.ClearFido2CredentialClick,
            )
        }
    }

    @Test
    fun `in ItemType_Login state the totp text field should be present based on state`() {
        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(totp = "TestCode") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll("TOTP")
            .assertTextEquals("TOTP", "TestCode")

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(totp = "NewTestCode") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll("TOTP")
            .assertTextEquals("TOTP", "NewTestCode")

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(totp = null) }
        }

        composeTestRule
            .onNodeWithText("TOTP")
            .assertDoesNotExist()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state the totp text field click on trailing icon should call ClearTotpKeyClick`() {
        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(totp = "TestCode") }
        }

        composeTestRule
            .onAllNodesWithContentDescriptionAfterScroll("Delete")
            .onFirst()
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.ClearTotpKeyClick,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state clicking the copy totp code button should trigger CopyTotpKeyClick`() {
        val testCode = "TestCode"

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(totp = testCode) }
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll("Copy TOTP")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.CopyTotpKeyClick(testCode),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state clicking the camera totp code button should trigger SetupTotpClick with result`() {
        fakePermissionManager.checkPermissionResult = false
        fakePermissionManager.getPermissionsResult = true
        val testCode = "TestCode"

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(totp = testCode) }
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll("Camera")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.SetupTotpClick(
                    isGranted = fakePermissionManager.getPermissionsResult,
                ),
            )
        }
    }

    @Test
    fun `in ItemType_Login state SetupTOTP button should be present based on state`() {
        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(totp = null) }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Set up TOTP")
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(totp = "TestCode") }
        }

        composeTestRule
            .onNodeWithText(text = "Set up TOTP")
            .assertIsNotDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state clicking SetupTOTP button with a positive result should send true if permission check returns true`() {
        fakePermissionManager.checkPermissionResult = true

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Set up TOTP")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.SetupTotpClick(true),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state clicking SetupTOTP button with a positive result should send true`() {
        fakePermissionManager.checkPermissionResult = false
        fakePermissionManager.getPermissionsResult = true

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Set up TOTP")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.SetupTotpClick(
                    isGranted = true,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state clicking Set up TOTP button with a negative result should send false`() {
        fakePermissionManager.checkPermissionResult = false
        fakePermissionManager.getPermissionsResult = false

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Set up TOTP")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.SetupTotpClick(
                    isGranted = false,
                ),
            )
        }
    }

    @Test
    fun `in ItemType_Login state changing URI text field should trigger UriValueChange`() {
        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) {
                copy(
                    uriList = listOf(
                        UriItem(id = "TestId", uri = "URI", match = null, checksum = null),
                    ),
                )
            }
        }

        composeTestRule
            .onNodeWithTextAfterScroll("URI")
            .performTextInput("Test")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.UriValueChange(
                    UriItem(id = "TestId", uri = "TestURI", match = null, checksum = null),
                ),
            )
        }
    }

    @Test
    fun `in ItemType_Login the URI control should display the text provided by the state`() {
        composeTestRule
            .onNodeWithTextAfterScroll("URI")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) {
                copy(
                    uriList = listOf(
                        UriItem(id = "TestId", uri = "NewURI", match = null, checksum = null),
                    ),
                )
            }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "URI")
            .assertTextContains("NewURI")
    }

    @Test
    fun `in ItemType_Login Uri settings dialog should be dismissed on cancel click`() {
        composeTestRule
            .onNodeWithTextAfterScroll(text = "URI")
            .onSiblings()
            .filterToOne(hasContentDescription(value = "Options"))
            .performClick()

        composeTestRule
            .onNodeWithText("Cancel")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login Uri settings dialog should send RemoveUriClick action if remove is clicked`() {
        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) {
                copy(
                    uriList = listOf(
                        UriItem(id = "TestId", uri = null, match = null, checksum = null),
                    ),
                )
            }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "URI")
            .onSiblings()
            .filterToOne(hasContentDescription(value = "Options"))
            .performClick()

        composeTestRule
            .onNodeWithText("Remove")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.RemoveUriClick(
                    UriItem(id = "TestId", uri = null, match = null, checksum = null),
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login Uri settings dialog with open match detection click should open list of options`() {
        composeTestRule
            .onNodeWithTextAfterScroll(text = "URI")
            .onSiblings()
            .filterToOne(hasContentDescription(value = "Options"))
            .performClick()

        composeTestRule
            .onNodeWithText("Match detection")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onAllNodesWithText("URI match detection")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Default")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Base domain")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Host")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Starts with")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Regular expression")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Exact")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login on URI settings click and on match detection click and option click should emit UriValueChange action`() {
        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) {
                copy(
                    uriList = listOf(
                        UriItem(id = "TestId", uri = null, match = null, checksum = null),
                    ),
                )
            }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "URI")
            .onSiblings()
            .filterToOne(hasContentDescription(value = "Options"))
            .performClick()

        composeTestRule
            .onNodeWithText("Match detection")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onNodeWithText("URI match detection")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Exact")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.UriValueChange(
                    UriItem(id = "TestId", uri = null, match = UriMatchType.EXACT, checksum = null),
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login on URI settings click and on match detection click and cancel click should dismiss the dialog`() {
        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) {
                copy(
                    uriList = listOf(
                        UriItem(id = "TestId", uri = null, match = null, checksum = null),
                    ),
                )
            }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "URI")
            .onSiblings()
            .filterToOne(hasContentDescription(value = "Options"))
            .performClick()

        composeTestRule
            .onNodeWithText("Match detection")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onAllNodesWithText("URI match detection")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Cancel")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `in ItemType_Login state clicking the New URI button should trigger AddNewUriClick`() {
        composeTestRule
            .onNodeWithTextAfterScroll(text = "New URI")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.AddNewUriClick,
            )
        }
    }

    @Test
    fun `in ItemType_Identity selecting a title should trigger TitleSelected`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        // Opens the menu
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "-- Select --. Title")
            .performClick()

        // Choose the option from the menu
        composeTestRule
            .onAllNodesWithText(text = "Mx")
            .onLast()
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.IdentityType.TitleSelect(
                    title = VaultIdentityTitle.MX,
                ),
            )
        }
    }

    @Test
    fun `in ItemType_Identity the Title should display the selected title from the state`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "-- Select --. Title")
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) {
                copy(
                    selectedTitle = VaultIdentityTitle.MX,
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Mx. Title")
            .assertIsDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Identity changing the first name text field should trigger FirstNameTextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "First name")
            .performTextInput(text = "TestFirstName")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.IdentityType.FirstNameTextChange(
                    firstName = "TestFirstName",
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Identity the first name text field should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "First name")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) { copy(firstName = "NewFirstName") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "First name")
            .assertTextContains("NewFirstName")
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Identity changing the middle name text field should trigger MiddleNameTextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Middle name")
            .performTextInput(text = "TestMiddleName")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.IdentityType.MiddleNameTextChange(
                    middleName = "TestMiddleName",
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Identity the middle name text field should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Middle name")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) { copy(middleName = "NewMiddleName") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Middle name")
            .assertTextContains("NewMiddleName")
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Identity changing the last name text field should trigger LastNameTextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Last name")
            .performTextInput(text = "TestLastName")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.IdentityType.LastNameTextChange(
                    lastName = "TestLastName",
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Identity the last name text field should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Last name")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) { copy(lastName = "NewLastName") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Last name")
            .assertTextContains("NewLastName")
    }

    @Test
    fun `in ItemType_Identity changing username text field should trigger UsernameTextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Username")
            .performTextInput(text = "TestUsername")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.IdentityType.UsernameTextChange(
                    username = "TestUsername",
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Identity the username text field should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Username")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) { copy(username = "NewUsername") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Username")
            .assertTextContains("NewUsername")
    }

    @Test
    fun `in ItemType_Identity changing company text field should trigger CompanyTextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Company")
            .performTextInput(text = "TestCompany")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.IdentityType.CompanyTextChange(
                    company = "TestCompany",
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Identity the company text field should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Company")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) { copy(company = "NewCompany") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Company")
            .assertTextContains("NewCompany")
    }

    @Test
    fun `in ItemType_Identity changing SSN text field should trigger CompanyTextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Social Security number")
            .performTextInput(text = "TestSsn")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.IdentityType.SsnTextChange(
                    ssn = "TestSsn",
                ),
            )
        }
    }

    @Test
    fun `in ItemType_Identity the SSN text field should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Social Security number")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) { copy(ssn = "NewSsn") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Social Security number")
            .assertTextContains("NewSsn")
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Identity changing passport number text field should trigger PassportNumberTextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Passport number")
            .performTextInput(text = "TestPassportNumber")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.IdentityType.PassportNumberTextChange(
                    passportNumber = "TestPassportNumber",
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Identity the passport number text field should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Passport number")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) { copy(passportNumber = "NewPassportNumber") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Passport number")
            .assertTextContains("NewPassportNumber")
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Identity changing license number text field should trigger LicenseNumberTextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "License number")
            .performTextInput(text = "TestLicenseNumber")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.IdentityType.LicenseNumberTextChange(
                    licenseNumber = "TestLicenseNumber",
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Identity the license number text field should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "License number")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) { copy(licenseNumber = "NewLicenseNumber") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "License number")
            .assertTextContains("NewLicenseNumber")
    }

    @Test
    fun `in ItemType_Identity changing email text field should trigger EmailTextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Email")
            .performTextInput(text = "TestEmail")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.IdentityType.EmailTextChange(
                    email = "TestEmail",
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Identity the email text field should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Email")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) { copy(email = "NewEmail") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Email")
            .assertTextContains("NewEmail")
    }

    @Test
    fun `in ItemType_Identity changing address1 text field should trigger Address1TextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Address 1")
            .performTextInput(text = "TestAddress1")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.IdentityType.Address1TextChange(
                    address1 = "TestAddress1",
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Identity the address1 text field should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Address 1")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) { copy(address1 = "NewAddress1") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Address 1")
            .assertTextContains("NewAddress1")
    }

    @Test
    fun `in ItemType_Identity changing address2 text field should trigger Address2TextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Address 2")
            .performTextInput(text = "TestAddress2")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.IdentityType.Address2TextChange(
                    address2 = "TestAddress2",
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Identity the address2 text field should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Address 2")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) { copy(address2 = "NewAddress2") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Address 2")
            .assertTextContains("NewAddress2")
    }

    @Test
    fun `in ItemType_Identity changing address3 text field should trigger Address3TextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Address 3")
            .performTextInput(text = "TestAddress3")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.IdentityType.Address3TextChange(
                    address3 = "TestAddress3",
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Identity the address3 text field should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Address 3")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) { copy(address3 = "NewAddress3") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Address 3")
            .assertTextContains("NewAddress3")
    }

    @Test
    fun `in ItemType_Identity changing city text field should trigger CityTextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "City / Town")
            .performTextInput(text = "TestCity")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.IdentityType.CityTextChange(
                    city = "TestCity",
                ),
            )
        }
    }

    @Test
    fun `in ItemType_Identity the city text field should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "City / Town")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) { copy(city = "NewCity") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "City / Town")
            .assertTextContains("NewCity")
    }

    @Test
    fun `in ItemType_Identity changing zip text field should trigger ZipTextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Zip / Postal code")
            .performTextInput(text = "TestZip")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.IdentityType.ZipTextChange(
                    zip = "TestZip",
                ),
            )
        }
    }

    @Test
    fun `in ItemType_Identity the zip text field should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Zip / Postal code")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) { copy(zip = "NewZip") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Zip / Postal code")
            .assertTextContains("NewZip")
    }

    @Test
    fun `in ItemType_Identity changing country text field should trigger CountryTextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Country")
            .performTextInput(text = "TestCountry")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.IdentityType.CountryTextChange(
                    country = "TestCountry",
                ),
            )
        }
    }

    @Test
    fun `in ItemType_Identity changing state text field should trigger CityTextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "State / Province")
            .performTextInput(text = "TestState")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.IdentityType.StateTextChange(
                    state = "TestState",
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Identity the state province text field should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "State / Province")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) { copy(state = "NewState") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "State / Province")
            .assertTextContains("NewState")
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Identity the country text field should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Country")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) { copy(country = "NewCountry") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Country")
            .assertTextContains("NewCountry")
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Card changing the card holder name text field should trigger CardHolderNameTextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_CARD
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Cardholder name")
            .performTextInput(text = "TestCardHolderName")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.CardType.CardHolderNameTextChange(
                    cardHolderName = "TestCardHolderName",
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Card the card holder name text field should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_CARD
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Cardholder name")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateCardType(currentState) { copy(cardHolderName = "NewCardHolderName") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Cardholder name")
            .assertTextContains("NewCardHolderName")
    }

    @Test
    fun `in ItemType_Card changing the number text field should trigger NumberTextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_CARD
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Number")
            .performTextInput(text = "TestNumber")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.CardType.NumberTextChange(
                    number = "TestNumber",
                ),
            )
        }
    }

    @Test
    fun `in ItemType_Card changing number visibility should trigger NumberVisibilityChange`() {
        mutableStateFlow.value = DEFAULT_STATE_CARD.copy(
            viewState = VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(),
                type = VaultAddEditState.ViewState.Content.ItemType.Card(number = "12345"),
                isIndividualVaultDisabled = false,
            ),
        )
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Number")
            .assertExists()
            .onChildren()
            .filterToOne(hasContentDescription(value = "Show"))
            .assertExists()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.CardType.NumberVisibilityChange(isVisible = true),
            )
        }
    }

    @Test
    fun `in ItemType_Card the number text field should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_CARD
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Number")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateCardType(currentState) { copy(number = "123") }
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll("Show")
            .performClick()

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Number")
            .assertTextContains("123")
    }

    @Test
    fun `in ItemType_Card selecting a brand should trigger BrandSelected`() {
        mutableStateFlow.value = DEFAULT_STATE_CARD
        // Opens the menu
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "-- Select --. Brand")
            .performClick()

        // Choose the option from the menu
        composeTestRule
            .onAllNodesWithText(text = "Visa")
            .onLast()
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.CardType.BrandSelect(
                    brand = VaultCardBrand.VISA,
                ),
            )
        }
    }

    @Test
    fun `in ItemType_Card the Brand should display the selected brand from the state`() {
        mutableStateFlow.value = DEFAULT_STATE_CARD
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "-- Select --. Brand")
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateCardType(currentState) {
                copy(
                    brand = VaultCardBrand.AMEX,
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "American Express. Brand")
            .assertIsDisplayed()
    }

    @Test
    fun `in ItemType_Card selecting an expiration month should trigger ExpirationMonthSelected`() {
        mutableStateFlow.value = DEFAULT_STATE_CARD
        // Opens the menu
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "-- Select --. Expiration month")
            .performClick()

        // Choose the option from the menu
        composeTestRule
            .onAllNodesWithText(text = "02 - February")
            .onLast()
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.CardType.ExpirationMonthSelect(
                    expirationMonth = VaultCardExpirationMonth.FEBRUARY,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Card the Expiration month should display the selected expiration month from the state`() {
        mutableStateFlow.value = DEFAULT_STATE_CARD
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "-- Select --. Expiration month")
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateCardType(currentState) {
                copy(
                    expirationMonth = VaultCardExpirationMonth.MARCH,
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "03 - March. Expiration month")
            .assertIsDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Card changing the expiration year text field should trigger ExpirationYearTextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_CARD
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Expiration year")
            .performTextInput(text = "TestExpirationYear")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.CardType.ExpirationYearTextChange(
                    expirationYear = "TestExpirationYear",
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Card the expiration year text field should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_CARD
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Expiration year")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateCardType(currentState) { copy(expirationYear = "NewExpirationYear") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Expiration year")
            .assertTextContains("NewExpirationYear")
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Card changing the security code text field should trigger SecurityCodeTextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_CARD
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Security code")
            .performTextInput(text = "TestSecurityCode")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.CardType.SecurityCodeTextChange(
                    securityCode = "TestSecurityCode",
                ),
            )
        }
    }

    @Test
    fun `in ItemType_Card changing code visibility should trigger SecurityCodeVisibilityChange`() {
        mutableStateFlow.value = DEFAULT_STATE_CARD.copy(
            viewState = VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(),
                type = VaultAddEditState.ViewState.Content.ItemType.Card(number = "12345"),
                isIndividualVaultDisabled = false,
            ),
        )
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Security code")
            .assertExists()
            .onChildren()
            .filterToOne(hasContentDescription(value = "Show"))
            .assertExists()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.CardType.SecurityCodeVisibilityChange(isVisible = true),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Card the security code text field should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_CARD
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Security code")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateCardType(currentState) { copy(securityCode = "123") }
        }

        composeTestRule
            .onAllNodesWithContentDescription("Show")
            .onLast()
            .performClick()

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Security code")
            .assertTextContains("123")
    }

    @Test
    fun `clicking New Custom Field button should allow creation of Linked type`() {
        mutableStateFlow.value = DEFAULT_STATE_LOGIN

        composeTestRule
            .onNodeWithTextAfterScroll(text = "New custom field")
            .performClick()

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(text = "Linked")
            .performClick()

        composeTestRule
            .onAllNodes(hasAnyAncestor(isDialog()))
            .filterToOne(hasText("Name"))
            .performTextInput("TestLinked")

        composeTestRule
            .onNodeWithText("Ok")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.AddNewCustomFieldClick(
                    customFieldType = CustomFieldType.LINKED,
                    name = "TestLinked",
                ),
            )
        }
    }

    @Test
    fun `clicking a Ownership option should send OwnershipChange action`() {
        updateStateWithOwners()

        // Opens the menu
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(
                label = "placeholder@email.com. Who owns this item?",
            )
            .performClick()

        // Choose the option from the menu
        composeTestRule
            .onAllNodesWithText(text = "mockOwnerName-2")
            .onLast()
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.OwnershipChange(
                    VaultAddEditState.Owner(
                        id = "mockOwnerId-2",
                        name = "mockOwnerName-2",
                        collections = DEFAULT_COLLECTIONS,
                    ),
                ),
            )
        }
    }

    @Test
    fun `the Ownership control should display the text provided by the state`() {
        updateStateWithOwners()
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(
                label = "placeholder@email.com. Who owns this item?",
            )
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateCommonContent(currentState) { copy(selectedOwnerId = "mockOwnerId-2") }
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "mockOwnerName-2. Who owns this item?")
            .assertIsDisplayed()
    }

    @Test
    fun `clicking a Collection should send CollectionSelect action`() {
        updateStateWithOwners(selectedOwnerId = "mockOwnerId-2")

        composeTestRule
            .onNodeWithTextAfterScroll("mockCollectionName-2")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.CollectionSelect(
                    VaultCollection(
                        id = "mockCollectionId-2",
                        name = "mockCollectionName-2",
                        isSelected = false,
                    ),
                ),
            )
        }
    }

    @Test
    fun `ownership section should not be displayed when no organizations present`() {
        updateStateWithOwners(selectedOwnerId = "mockOwnerId-2")

        composeTestRule
            .onNodeWithTextAfterScroll(text = "mockCollectionName-2")
            .assertIsDisplayed()

        updateStateWithOwners(
            selectedOwnerId = null,
            availableOwners = listOf(
                VaultAddEditState.Owner(
                    id = null,
                    name = "placeholder@email.com",
                    collections = DEFAULT_COLLECTIONS,
                ),
            ),
            hasOrganizations = false,
        )

        composeTestRule
            .onNodeWithText(text = "mockCollectionName-2")
            .assertDoesNotExist()
    }

    @Test
    fun `Collection list should display according to state`() {
        updateStateWithOwners(selectedOwnerId = "mockOwnerId-2")

        composeTestRule
            .onNodeWithTextAfterScroll("mockCollectionName-2")
            .assertIsDisplayed()

        updateStateWithOwners(
            selectedOwnerId = "mockOwnerId-2",
            availableOwners = ALTERED_OWNERS,
        )

        composeTestRule
            .onNodeWithTextAfterScroll("mockCollectionName-new")
            .assertIsDisplayed()
    }

    @Test
    fun `changing Name text field should trigger NameTextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Name")
            .performTextInput(text = "TestName")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.NameTextChange(name = "TestName"),
            )
        }
    }

    @Test
    fun `the name control should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Name")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateCommonContent(currentState) { copy(name = "NewName") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Name")
            .assertTextContains("NewName")
    }

    @Test
    fun `clicking a Folder Option should send FolderChange action`() {
        updateStateWithFolders()

        // Opens the menu
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "No Folder. Folder")
            .performClick()

        // Choose the option from the menu
        composeTestRule
            .onAllNodesWithText(text = "mockFolderName-1")
            .onLast()
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.FolderChange(
                    VaultAddEditState.Folder(
                        id = "mockFolderId-1",
                        name = "mockFolderName-1",
                    ),
                ),
            )
        }
    }

    @Test
    fun `the folder control should display the text provided by the state`() {
        updateStateWithFolders()

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "No Folder. Folder")
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateCommonContent(currentState) { copy(selectedFolderId = "mockFolderId-1") }
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "mockFolderName-1. Folder")
            .assertIsDisplayed()
    }

    @Test
    fun `toggling the favorite toggle should send ToggleFavorite action`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithTextAfterScroll("Favorite")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.ToggleFavorite(
                    isFavorite = true,
                ),
            )
        }
    }

    @Test
    fun `the favorite toggle should be enabled or disabled according to state`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithTextAfterScroll("Favorite")
            .assertIsOff()

        mutableStateFlow.update { currentState ->
            updateCommonContent(currentState) { copy(favorite = true) }
        }

        composeTestRule
            .onNodeWithTextAfterScroll("Favorite")
            .assertIsOn()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toggling the Master password re-prompt toggle should send ToggleMasterPasswordReprompt action`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithTextAfterScroll("Master password re-prompt")
            .performTouchInput {
                click(position = Offset(x = 1f, y = center.y))
            }

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.ToggleMasterPasswordReprompt(
                    isMasterPasswordReprompt = true,
                ),
            )
        }
    }

    @Test
    fun `re-prompt toggle should display according to state`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithTextAfterScroll("Master password re-prompt")
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateCommonContent(currentState) { copy(isUnlockWithPasswordEnabled = false) }
        }
        composeTestRule
            .onNodeWithText("Master password re-prompt")
            .assertDoesNotExist()
    }

    @Test
    fun `the master password re-prompt toggle should be enabled or disabled according to state`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithTextAfterScroll("Master password re-prompt")
            .assertIsOff()

        mutableStateFlow.update { currentState ->
            updateCommonContent(currentState) { copy(masterPasswordReprompt = true) }
        }

        composeTestRule
            .onNodeWithTextAfterScroll("Master password re-prompt")
            .assertIsOn()
    }

    @Test
    fun `toggling the Master password re-prompt tooltip button should send TooltipClick action`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Master password re-prompt help")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.TooltipClick,
            )
        }
    }

    @Test
    fun `changing Notes text field should trigger NotesTextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onAllNodesWithTextAfterScroll("Notes")
            .filterToOne(hasSetTextAction())
            .performTextInput("TestNotes")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.NotesTextChange("TestNotes"),
            )
        }
    }

    @Test
    fun `the Notes control should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onAllNodesWithTextAfterScroll("Notes")
            .filterToOne(hasSetTextAction())
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateCommonContent(currentState) { copy(notes = "NewNote") }
        }

        composeTestRule
            .onAllNodesWithTextAfterScroll("Notes")
            .filterToOne(hasSetTextAction())
            .assertTextContains("NewNote")
    }

    @Test
    fun `Ownership option should send OwnershipChange action`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        updateStateWithOwners()

        // Opens the menu
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(
                label = "placeholder@email.com. Who owns this item?",
            )
            .performClick()

        // Choose the option from the menu
        composeTestRule
            .onAllNodesWithText(text = "mockOwnerName-2")
            .onLast()
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.OwnershipChange(
                    VaultAddEditState.Owner(
                        id = "mockOwnerId-2",
                        name = "mockOwnerName-2",
                        collections = DEFAULT_COLLECTIONS,
                    ),
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_SecureNotes the Ownership control should display the text provided by the state`() {
        updateStateWithOwners()

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(
                label = "placeholder@email.com. Who owns this item?",
            )
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateCommonContent(currentState) { copy(selectedOwnerId = "mockOwnerId-2") }
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(
                label = "mockOwnerName-2. Who owns this item?",
            )
            .assertIsDisplayed()
    }

    @Test
    fun `clicking New Custom Field button should allow creation of Text type`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithTextAfterScroll(text = "New custom field")
            .performClick()

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(text = "Text")
            .performClick()

        composeTestRule
            .onAllNodes(hasAnyAncestor(isDialog()))
            .filterToOne(hasText("Name"))
            .performTextInput("TestText")

        composeTestRule
            .onNodeWithText("Ok")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.AddNewCustomFieldClick(
                    customFieldType = CustomFieldType.TEXT,
                    name = "TestText",
                ),
            )
        }
    }

    @Test
    fun `clicking New Custom Field button should not display linked type`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithTextAfterScroll(text = "New custom field")
            .performClick()

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(text = "Linked")
            .assertIsNotDisplayed()
    }

    @Test
    fun `clicking New Custom Field button should allow creation of Boolean type`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithTextAfterScroll(text = "New custom field")
            .performClick()

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(text = "Boolean")
            .performClick()

        composeTestRule
            .onAllNodes(hasAnyAncestor(isDialog()))
            .filterToOne(hasText("Name"))
            .performTextInput("TestBoolean")

        composeTestRule
            .onNodeWithText("Ok")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.AddNewCustomFieldClick(
                    customFieldType = CustomFieldType.BOOLEAN,
                    name = "TestBoolean",
                ),
            )
        }
    }

    @Test
    fun `clicking New Custom Field button should allow creation of Hidden type`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithTextAfterScroll(text = "New custom field")
            .performClick()

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(text = "Hidden")
            .performClick()

        composeTestRule
            .onAllNodes(hasAnyAncestor(isDialog()))
            .filterToOne(hasText("Name"))
            .performTextInput("TestHidden")

        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.AddNewCustomFieldClick(
                    customFieldType = CustomFieldType.HIDDEN,
                    name = "TestHidden",
                ),
            )
        }
    }

    @Test
    fun `changing hidden field visibility state should send HiddenFieldVisibilityChange`() {
        mutableStateFlow.update {
            it.copy(
                viewState = VaultAddEditState.ViewState.Content(
                    common = VaultAddEditState.ViewState.Content.Common(
                        customFieldData = listOf(
                            VaultAddEditState.Custom.HiddenField(
                                itemId = "itemId",
                                name = "Hidden item",
                                value = "I am hiding",
                            ),
                        ),
                    ),
                    type = VaultAddEditState.ViewState.Content.ItemType.Login(),
                    isIndividualVaultDisabled = false,
                ),
            )
        }
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Hidden item")
            .assertExists()
        composeTestRule
            .onAllNodesWithContentDescriptionAfterScroll(label = "Show")
            .onLast()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultAddEditAction.Common.HiddenFieldVisibilityChange(isVisible = true),
            )
        }
    }

    @Test
    fun `clicking and changing the custom text field will send a CustomFieldValueChange event`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES_CUSTOM_FIELDS

        composeTestRule
            .onNodeWithTextAfterScroll("TestText")
            .performTextClearance()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.CustomFieldValueChange(
                    VaultAddEditState.Custom.TextField("Test ID 2", "TestText", ""),
                ),
            )
        }
    }

    @Test
    fun `clicking and changing the custom hidden field will send a CustomFieldValueChange event`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES_CUSTOM_FIELDS

        composeTestRule
            .onNodeWithTextAfterScroll("TestHidden")
            .performTextClearance()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.CustomFieldValueChange(
                    VaultAddEditState.Custom.HiddenField("Test ID 3", "TestHidden", ""),
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking and changing the custom boolean field will send a CustomFieldValueChange event`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES_CUSTOM_FIELDS

        composeTestRule
            .onNodeWithTextAfterScroll("TestBoolean")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.CustomFieldValueChange(
                    VaultAddEditState.Custom.BooleanField("Test ID 1", "TestBoolean", true),
                ),
            )
        }
    }

    @Test
    fun `clicking custom field edit icon and Edit option sends a CustomFieldValueChange action`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES_CUSTOM_FIELDS

        composeTestRule
            .onAllNodesWithContentDescriptionAfterScroll("Edit")
            .onFirst()
            .performClick()

        composeTestRule
            .onNodeWithText("Edit")
            .performClick()

        composeTestRule
            .onAllNodesWithText("TestBoolean")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Name")
            .performTextClearance()

        composeTestRule
            .onNodeWithText("Name")
            .performTextInput("Boolean")

        composeTestRule
            .onNodeWithText("Ok")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.CustomFieldValueChange(
                    VaultAddEditState.Custom.BooleanField("Test ID 1", "Boolean", false),
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking custom field edit icon and Remove option sends a CustomFieldActionSelect remove action`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES_CUSTOM_FIELDS

        composeTestRule
            .onAllNodesWithContentDescriptionAfterScroll("Edit")
            .onFirst()
            .performClick()

        composeTestRule
            .onNodeWithText("Remove")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.CustomFieldActionSelect(
                    customFieldAction = CustomFieldAction.REMOVE,
                    customField = VaultAddEditState.Custom.BooleanField(
                        itemId = "Test ID 1",
                        name = "TestBoolean",
                        value = false,
                    ),
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking custom field edit icon and Move down option sends a CustomFieldActionSelect move down action`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES_CUSTOM_FIELDS

        composeTestRule
            .onAllNodesWithContentDescriptionAfterScroll("Edit")
            .onFirst()
            .performClick()

        composeTestRule
            .onNodeWithText("Move down")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.CustomFieldActionSelect(
                    customFieldAction = CustomFieldAction.MOVE_DOWN,
                    customField = VaultAddEditState.Custom.BooleanField(
                        itemId = "Test ID 1",
                        name = "TestBoolean",
                        value = false,
                    ),
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking custom field edit icon and Move Up options sends a CustomFieldActionSelect move up action`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES_CUSTOM_FIELDS

        composeTestRule
            .onAllNodesWithContentDescriptionAfterScroll("Edit")
            .onFirst()
            .performClick()

        composeTestRule
            .onNodeWithText("Move Up")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.CustomFieldActionSelect(
                    customFieldAction = CustomFieldAction.MOVE_UP,
                    customField = VaultAddEditState.Custom.BooleanField(
                        itemId = "Test ID 1",
                        name = "TestBoolean",
                        value = false,
                    ),
                ),
            )
        }
    }

    @Test
    fun `Menu should display correct items when cipher is in a collection`() {
        mutableStateFlow.update {
            it.copy(
                vaultAddEditType = VaultAddEditType.EditItem(vaultItemId = "mockId-1"),
                viewState = VaultAddEditState.ViewState.Content(
                    common = VaultAddEditState.ViewState.Content.Common(
                        originalCipher = createMockCipherView(1),
                    ),
                    type = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
                    isIndividualVaultDisabled = false,
                ),
            )
        }
        composeTestRule
            .onNodeWithContentDescription("More")
            .performClick()

        composeTestRule
            .onAllNodesWithText("Attachments")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Collections")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Move to Organization")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertDoesNotExist()

        composeTestRule
            .onAllNodesWithText("Delete")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertIsDisplayed()
    }

    @Test
    fun `Menu Collections should display correctly according to state`() {
        mutableStateFlow.update {
            it.copy(
                vaultAddEditType = VaultAddEditType.EditItem(vaultItemId = "mockId-1"),
                viewState = VaultAddEditState.ViewState.Content(
                    common = VaultAddEditState.ViewState.Content.Common(
                        originalCipher = createMockCipherView(1),
                    ),
                    type = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
                    isIndividualVaultDisabled = false,
                ),
            )
        }
        // Confirm overflow is closed on initial load
        composeTestRule
            .onAllNodesWithText("Collections")
            .filter(hasAnyAncestor(isPopup()))
            .assertCountEquals(0)

        // Open the overflow menu
        composeTestRule
            .onNodeWithContentDescription("More")
            .performClick()

        // Confirm Collections option is present
        composeTestRule
            .onAllNodesWithText("Collections")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertIsDisplayed()

        // Confirm Collections option is not present when canAssignToCollections is false
        mutableStateFlow.update {
            it.copy(
                vaultAddEditType = VaultAddEditType.EditItem(vaultItemId = "mockId-1"),
                viewState = VaultAddEditState.ViewState.Content(
                    common = VaultAddEditState.ViewState.Content.Common(
                        originalCipher = createMockCipherView(1),
                        canAssignToCollections = false,
                    ),
                    type = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
                    isIndividualVaultDisabled = false,
                ),
            )
        }
        composeTestRule
            .onAllNodesWithText("Collections")
            .filter(hasAnyAncestor(isPopup()))
            .assertCountEquals(0)
    }

    @Test
    fun `Menu should display correct items when cipher is not in a collection`() {
        mutableStateFlow.update {
            it.copy(
                vaultAddEditType = VaultAddEditType.EditItem(vaultItemId = "mockId-1"),
                viewState = VaultAddEditState.ViewState.Content(
                    common = VaultAddEditState.ViewState.Content.Common(
                        originalCipher = createMockCipherView(1).copy(
                            collectionIds = emptyList(),
                        ),
                    ),
                    type = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
                    isIndividualVaultDisabled = false,
                ),
            )
        }
        composeTestRule
            .onNodeWithContentDescription("More")
            .performClick()

        composeTestRule
            .onAllNodesWithText("Attachments")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Move to Organization")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Collections")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertDoesNotExist()

        composeTestRule
            .onAllNodesWithText("Delete")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertIsDisplayed()
    }

    @Test
    fun `should display policy warning when personal vault is disabled for add item type`() {
        mutableStateFlow.update {
            it.copy(
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
                viewState = VaultAddEditState.ViewState.Content(
                    common = VaultAddEditState.ViewState.Content.Common(
                        originalCipher = createMockCipherView(1),
                    ),
                    type = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
                    isIndividualVaultDisabled = true,
                ),
            )
        }

        composeTestRule
            .onNodeWithTextAfterScroll(
                text = "An organization policy is affecting your ownership options.",
            )
            .assertIsDisplayed()
    }

    @Test
    fun `should not display policy warning when personal vault is disabled for edit item type`() {
        mutableStateFlow.update {
            it.copy(
                vaultAddEditType = VaultAddEditType.EditItem("mockId-1"),
                viewState = VaultAddEditState.ViewState.Content(
                    common = VaultAddEditState.ViewState.Content.Common(
                        originalCipher = createMockCipherView(1),
                    ),
                    type = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
                    isIndividualVaultDisabled = true,
                ),
            )
        }

        composeTestRule
            .onNodeWithText(text = "An organization policy is affecting your ownership options.")
            .assertDoesNotExist()
    }

    @Test
    fun `Delete dialog ok click should send ConfirmDeleteClick`() {
        mutableStateFlow.update {
            it.copy(
                vaultAddEditType = VaultAddEditType.EditItem(vaultItemId = "mockId-1"),
                viewState = VaultAddEditState.ViewState.Content(
                    common = VaultAddEditState.ViewState.Content.Common(
                        originalCipher = createMockCipherView(1),
                    ),
                    type = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
                    isIndividualVaultDisabled = false,
                ),
            )
        }

        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithContentDescription("More")
            .performClick()

        composeTestRule
            .onAllNodesWithText("Delete")
            .filterToOne(hasAnyAncestor(isPopup()))
            .performClick()

        composeTestRule
            .onAllNodesWithText("Do you really want to send to the trash?")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()

        verify {
            viewModel.trySendAction(VaultAddEditAction.Common.ConfirmDeleteClick)
        }
    }

    @Test
    fun `Delete dialog cancel click should dismiss the dialog`() {
        mutableStateFlow.update {
            it.copy(
                vaultAddEditType = VaultAddEditType.EditItem(vaultItemId = "mockId-1"),
                viewState = VaultAddEditState.ViewState.Content(
                    common = VaultAddEditState.ViewState.Content.Common(
                        originalCipher = createMockCipherView(1),
                    ),
                    type = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
                    isIndividualVaultDisabled = false,
                ),
            )
        }

        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithContentDescription("More")
            .performClick()

        composeTestRule
            .onAllNodesWithText("Delete")
            .filterToOne(hasAnyAncestor(isPopup()))
            .performClick()

        composeTestRule
            .onAllNodesWithText("Do you really want to send to the trash?")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `Fido2UserVerification event should prompt for user verification`() {
        every {
            biometricsManager.promptUserVerification(
                onSuccess = any(),
                onCancel = any(),
                onLockOut = any(),
                onError = any(),
                onNotSupported = any(),
            )
        } just runs
        mutableEventFlow.tryEmit(VaultAddEditEvent.Fido2UserVerification(true))
        verify {
            biometricsManager.promptUserVerification(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `Fido2UserVerification onSuccess should send UserVerificationSuccess action`() {
        every {
            biometricsManager.promptUserVerification(
                onSuccess = captureLambda(),
                onCancel = any(),
                onLockOut = any(),
                onError = any(),
                onNotSupported = any(),
            )
        } answers {
            lambda<() -> Unit>().invoke()
        }
        mutableEventFlow.tryEmit(VaultAddEditEvent.Fido2UserVerification(isRequired = true))
        verify { viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationSuccess) }
    }

    @Test
    fun `Fido2UserVerification onCancel should send UserVerificationCancelled action`() {
        every {
            biometricsManager.promptUserVerification(
                onSuccess = any(),
                onCancel = captureLambda(),
                onLockOut = any(),
                onError = any(),
                onNotSupported = any(),
            )
        } answers {
            lambda<() -> Unit>().invoke()
        }
        mutableEventFlow.tryEmit(VaultAddEditEvent.Fido2UserVerification(isRequired = true))
        verify { viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationCancelled) }
    }

    @Test
    fun `Fido2UserVerification onLockout should send UserVerificationLockOut action`() {
        every {
            biometricsManager.promptUserVerification(
                onSuccess = any(),
                onCancel = any(),
                onLockOut = captureLambda(),
                onError = any(),
                onNotSupported = any(),
            )
        } answers {
            lambda<() -> Unit>().invoke()
        }
        mutableEventFlow.tryEmit(VaultAddEditEvent.Fido2UserVerification(isRequired = true))
        verify { viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationLockOut) }
    }

    @Test
    fun `Fido2UserVerification onError should send UserVerificationFail action`() {
        every {
            biometricsManager.promptUserVerification(
                onSuccess = any(),
                onCancel = any(),
                onLockOut = any(),
                onError = captureLambda(),
                onNotSupported = any(),
            )
        } answers { lambda<() -> Unit>().invoke() }
        mutableEventFlow.tryEmit(VaultAddEditEvent.Fido2UserVerification(isRequired = true))
        verify { viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationFail) }
    }

    @Test
    fun `Fido2UserVerification onNotSupported should send UserVerificationNotSupported action`() {
        every {
            biometricsManager.promptUserVerification(
                onSuccess = any(),
                onCancel = any(),
                onLockOut = any(),
                onError = any(),
                onNotSupported = captureLambda(),
            )
        } answers { lambda<() -> Unit>().invoke() }
        mutableEventFlow.tryEmit(VaultAddEditEvent.Fido2UserVerification(isRequired = true))
        verify { viewModel.trySendAction(VaultAddEditAction.Common.UserVerificationNotSupported) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `OverwritePasskeyConfirmationPrompt should display based on dialog state and send ConfirmOverwriteExistingPasskeyClick on Ok click`() {
        val stateWithDialog = DEFAULT_STATE_LOGIN
            .copy(dialog = VaultAddEditState.DialogState.OverwritePasskeyConfirmationPrompt)

        mutableStateFlow.value = stateWithDialog

        composeTestRule
            .onNodeWithText("Overwrite passkey?")
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
        composeTestRule
            .onNodeWithText("This item already contains a passkey. Are you sure you want to overwrite the current passkey?")
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(VaultAddEditAction.Common.ConfirmOverwriteExistingPasskeyClick)
        }
    }

    @Test
    fun `in ItemType_SshKeys the public key field should be read only`() {
        mutableStateFlow.value = DEFAULT_STATE_SSH_KEYS

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Public key")
            .assertExists()
            .assert(!isEditable())
    }

    @Test
    fun `in ItemType_SshKeys the private key field should be read only`() {
        mutableStateFlow.value = DEFAULT_STATE_SSH_KEYS

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Private key")
            .assertExists()
            .assert(!isEditable())
    }

    @Test
    fun `in ItemType_SshKeys the fingerprint field should be read only`() {
        mutableStateFlow.value = DEFAULT_STATE_SSH_KEYS

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Fingerprint")
            .assertExists()
            .assert(!isEditable())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_SshKeys changing the private key visibility should trigger PrivateKeyVisibilityChange`() {
        mutableStateFlow.value = DEFAULT_STATE_SSH_KEYS

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Private key")
            .assertExists()
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Show")
            .assertExists()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.SshKeyType.PrivateKeyVisibilityChange(true),
            )
        }
    }

    //region Helper functions

    private fun updateLoginType(
        currentState: VaultAddEditState,
        transform: VaultAddEditState.ViewState.Content.ItemType.Login.() ->
        VaultAddEditState.ViewState.Content.ItemType.Login,
    ): VaultAddEditState {
        val updatedType = when (val viewState = currentState.viewState) {
            is VaultAddEditState.ViewState.Content -> {
                when (val type = viewState.type) {
                    is VaultAddEditState.ViewState.Content.ItemType.Login -> {
                        viewState.copy(
                            type = type.transform(),
                        )
                    }

                    else -> viewState
                }
            }

            else -> viewState
        }
        return currentState.copy(viewState = updatedType)
    }

    private fun updateIdentityType(
        currentState: VaultAddEditState,
        transform: VaultAddEditState.ViewState.Content.ItemType.Identity.() ->
        VaultAddEditState.ViewState.Content.ItemType.Identity,
    ): VaultAddEditState {
        val updatedType = when (val viewState = currentState.viewState) {
            is VaultAddEditState.ViewState.Content -> {
                when (val type = viewState.type) {
                    is VaultAddEditState.ViewState.Content.ItemType.Identity -> {
                        viewState.copy(
                            type = type.transform(),
                        )
                    }

                    else -> viewState
                }
            }

            else -> viewState
        }
        return currentState.copy(viewState = updatedType)
    }

    private fun updateCardType(
        currentState: VaultAddEditState,
        transform: VaultAddEditState.ViewState.Content.ItemType.Card.() ->
        VaultAddEditState.ViewState.Content.ItemType.Card,
    ): VaultAddEditState {
        val updatedType = when (val viewState = currentState.viewState) {
            is VaultAddEditState.ViewState.Content -> {
                when (val type = viewState.type) {
                    is VaultAddEditState.ViewState.Content.ItemType.Card -> {
                        viewState.copy(
                            type = type.transform(),
                        )
                    }

                    else -> viewState
                }
            }

            else -> viewState
        }
        return currentState.copy(viewState = updatedType)
    }

    @Suppress("MaxLineLength")
    private fun updateCommonContent(
        currentState: VaultAddEditState,
        transform: VaultAddEditState.ViewState.Content.Common.()
        -> VaultAddEditState.ViewState.Content.Common,
    ): VaultAddEditState {
        val updatedType = when (val viewState = currentState.viewState) {
            is VaultAddEditState.ViewState.Content ->
                viewState.copy(common = viewState.common.transform())

            else -> viewState
        }
        return currentState.copy(viewState = updatedType)
    }

    private fun updateStateWithOwners(
        selectedOwnerId: String? = null,
        availableOwners: List<VaultAddEditState.Owner> = DEFAULT_OWNERS,
        hasOrganizations: Boolean = true,
    ) {
        mutableStateFlow.update { currentState ->
            updateCommonContent(currentState) {
                copy(
                    selectedOwnerId = selectedOwnerId,
                    availableOwners = availableOwners,
                    hasOrganizations = hasOrganizations,
                )
            }
        }
    }

    private fun updateStateWithFolders() {
        mutableStateFlow.update {
            updateCommonContent(it) {
                copy(
                    selectedFolderId = null,
                    availableFolders = DEFAULT_FOLDERS,
                )
            }
        }
    }

    //endregion Helper functions

    companion object {
        private val DEFAULT_STATE_LOGIN_DIALOG = VaultAddEditState(
            viewState = VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(),
                type = VaultAddEditState.ViewState.Content.ItemType.Login(),
                isIndividualVaultDisabled = false,
            ),
            dialog = VaultAddEditState.DialogState.Generic(message = "test".asText()),
            vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
            supportedItemTypes = VaultAddEditState.ItemTypeOption.entries,
        )

        private val DEFAULT_STATE_LOGIN = VaultAddEditState(
            vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
            viewState = VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(),
                type = VaultAddEditState.ViewState.Content.ItemType.Login(),
                isIndividualVaultDisabled = false,
            ),
            dialog = null,
            supportedItemTypes = VaultAddEditState.ItemTypeOption.entries,
        )

        private val DEFAULT_STATE_IDENTITY = VaultAddEditState(
            vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.IDENTITY),
            viewState = VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(),
                type = VaultAddEditState.ViewState.Content.ItemType.Identity(),
                isIndividualVaultDisabled = false,
            ),
            dialog = null,
            supportedItemTypes = VaultAddEditState.ItemTypeOption.entries,
        )

        private val DEFAULT_STATE_CARD = VaultAddEditState(
            vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.CARD),
            viewState = VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(),
                type = VaultAddEditState.ViewState.Content.ItemType.Card(),
                isIndividualVaultDisabled = false,
            ),
            dialog = null,
            supportedItemTypes = VaultAddEditState.ItemTypeOption.entries,
        )

        @Suppress("MaxLineLength")
        private val DEFAULT_STATE_SECURE_NOTES_CUSTOM_FIELDS = VaultAddEditState(
            viewState = VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(
                    customFieldData = listOf(
                        VaultAddEditState.Custom.BooleanField("Test ID 1", "TestBoolean", false),
                        VaultAddEditState.Custom.TextField("Test ID 2", "TestText", "TestTextVal"),
                        VaultAddEditState.Custom.HiddenField(
                            "Test ID 3",
                            "TestHidden",
                            "TestHiddenVal",
                        ),
                    ),
                ),
                type = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
                isIndividualVaultDisabled = false,
            ),
            dialog = null,
            vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.SECURE_NOTE),
            supportedItemTypes = VaultAddEditState.ItemTypeOption.entries,
        )

        private val DEFAULT_STATE_SECURE_NOTES = VaultAddEditState(
            vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.SECURE_NOTE),
            viewState = VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(),
                type = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
                isIndividualVaultDisabled = false,
            ),
            dialog = null,
            supportedItemTypes = VaultAddEditState.ItemTypeOption.entries,
        )

        private val DEFAULT_STATE_SSH_KEYS = VaultAddEditState(
            vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.SSH_KEY),
            viewState = VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(),
                type = VaultAddEditState.ViewState.Content.ItemType.SshKey(),
                isIndividualVaultDisabled = false,
            ),
            dialog = null,
            supportedItemTypes = VaultAddEditState.ItemTypeOption.entries,
        )

        private val ALTERED_COLLECTIONS = listOf(
            VaultCollection(
                id = "mockCollectionId-new",
                name = "mockCollectionName-new",
                isSelected = true,
            ),
        )

        private val ALTERED_OWNERS = listOf(
            VaultAddEditState.Owner(
                id = null,
                name = "placeholder@email.com",
                collections = emptyList(),
            ),
            VaultAddEditState.Owner(
                id = "mockOwnerId-1",
                name = "mockOwnerName-1",
                collections = emptyList(),
            ),
            VaultAddEditState.Owner(
                id = "mockOwnerId-2",
                name = "mockOwnerName-2",
                collections = ALTERED_COLLECTIONS,
            ),
        )

        private val DEFAULT_COLLECTIONS = listOf(
            VaultCollection(
                id = "mockCollectionId-2",
                name = "mockCollectionName-2",
                isSelected = false,
            ),
        )

        private val DEFAULT_OWNERS = listOf(
            VaultAddEditState.Owner(
                id = null,
                name = "placeholder@email.com",
                collections = emptyList(),
            ),
            VaultAddEditState.Owner(
                id = "mockOwnerId-1",
                name = "mockOwnerName-1",
                collections = emptyList(),
            ),
            VaultAddEditState.Owner(
                id = "mockOwnerId-2",
                name = "mockOwnerName-2",
                collections = DEFAULT_COLLECTIONS,
            ),
        )

        private val DEFAULT_FOLDERS = listOf(
            VaultAddEditState.Folder(
                id = null,
                name = "No Folder",
            ),
            VaultAddEditState.Folder(
                id = "mockFolderId-1",
                name = "mockFolderName-1",
            ),
            VaultAddEditState.Folder(
                id = "mockFolderId-2",
                name = "mockFolderName-2",
            ),
        )
    }
}
