package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
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
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.FakePermissionManager
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.toAnnotatedString
import com.x8bit.bitwarden.ui.util.isProgressBar
import com.x8bit.bitwarden.ui.util.onAllNodesWithTextAfterScroll
import com.x8bit.bitwarden.ui.util.onNodeWithContentDescriptionAfterScroll
import com.x8bit.bitwarden.ui.util.onNodeWithTextAfterScroll
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.CustomFieldType
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@Suppress("LargeClass")
class VaultAddEditScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateQrCodeScanScreenCalled = false

    private val clipboardManager = mockk<ClipboardManager>()

    private val mutableEventFlow = bufferedMutableSharedFlow<VaultAddEditEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE_LOGIN)

    private val fakePermissionManager: FakePermissionManager = FakePermissionManager()

    private val viewModel = mockk<VaultAddEditViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            VaultAddEditScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
                permissionsManager = fakePermissionManager,
                clipboardManager = clipboardManager,
                onNavigateToQrCodeScanScreen = {
                    onNavigateQrCodeScanScreenCalled = true
                },
            )
        }
    }

    @Test
    fun `on NavigateBack event should invoke onNavigateBack`() {
        mutableEventFlow.tryEmit(VaultAddEditEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on NavigateToQrCodeScan event should invoke NavigateToQrCodeScan`() {
        mutableEventFlow.tryEmit(VaultAddEditEvent.NavigateToQrCodeScan)
        assertTrue(onNavigateQrCodeScanScreenCalled)
    }

    @Test
    fun `on CopyToClipboard should call setText on ClipboardManager`() {
        val textString = "text"

        every { clipboardManager.setText(textString.toAnnotatedString()) } just runs

        mutableEventFlow.tryEmit(VaultAddEditEvent.CopyToClipboard(textString))

        verify(exactly = 1) {
            clipboardManager.setText(textString.toAnnotatedString())
        }
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
        composeTestRule.onNode(isProgressBar).assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = VaultAddEditState.ViewState.Error("Fail".asText()))
        }
        composeTestRule.onNode(isProgressBar).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultAddEditState.ViewState.Content(
                    common = VaultAddEditState.ViewState.Content.Common(),
                    type = VaultAddEditState.ViewState.Content.ItemType.Login(),
                ),
            )
        }
        composeTestRule.onNode(isProgressBar).assertDoesNotExist()
    }

    @Test
    fun `clicking a Type Option should send TypeOptionSelect action`() {
        // Opens the menu
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Type, Login")
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
            .onNodeWithContentDescriptionAfterScroll(label = "Type, Login")
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultAddEditState.ViewState.Content(
                    common = VaultAddEditState.ViewState.Content.Common(),
                    type = VaultAddEditState.ViewState.Content.ItemType.Card,
                ),
            )
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Type, Card")
            .assertIsDisplayed()
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
    fun `in ItemType_Login state clicking Username generator action should trigger OpenUsernameGeneratorClick`() {
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Generate username")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.OpenUsernameGeneratorClick,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state clicking Password checker action should trigger PasswordCheckerClick`() {
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Password")
            .onSiblings()
            .onFirst()
            .performClick()

        verify {
            viewModel.trySendAction(VaultAddEditAction.ItemType.LoginType.PasswordCheckerClick)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state click Password text field generator action should trigger OpenPasswordGeneratorClick`() {
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Password")
            .onSiblings()
            .onLast()
            .performClick()

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

    @Suppress("MaxLineLength")
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
    fun `in ItemType_Login state changing URI text field should trigger UriTextChange`() {
        composeTestRule
            .onNodeWithTextAfterScroll("URI")
            .performTextInput("TestURI")

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.UriTextChange("TestURI"),
            )
        }
    }

    @Test
    fun `in ItemType_Login the URI control should display the text provided by the state`() {
        composeTestRule
            .onNodeWithTextAfterScroll("URI")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(uri = "NewURI") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "URI")
            .assertTextContains("NewURI")
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state clicking the URI settings action should trigger UriSettingsClick`() {
        composeTestRule
            .onNodeWithTextAfterScroll(text = "URI")
            .onSiblings()
            .filterToOne(hasContentDescription(value = "Options"))
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.LoginType.UriSettingsClick,
            )
        }
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
            .onNodeWithContentDescriptionAfterScroll(label = "Title, Mr")
            .performClick()

        // Choose the option from the menu
        composeTestRule
            .onAllNodesWithText(text = "Mx")
            .onLast()
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.ItemType.IdentityType.TitleSelected(
                    title = VaultAddEditState.ViewState.Content.ItemType.Identity.Title.MX,
                ),
            )
        }
    }

    @Test
    fun `in ItemType_Identity the Title should display the selected title from the state`() {
        mutableStateFlow.value = DEFAULT_STATE_IDENTITY
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Title, Mr")
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) {
                copy(
                    selectedTitle = VaultAddEditState.ViewState.Content.ItemType.Identity.Title.MX,
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Title, Mx")
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

    @Suppress("MaxLineLength")
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

    @Suppress("MaxLineLength")
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
            .onNodeWithText("Name")
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
        // Opens the menu
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(
                label = "Who owns this item?, placeholder@email.com",
            )
            .performClick()

        // Choose the option from the menu
        composeTestRule
            .onAllNodesWithText(text = "a@b.com")
            .onLast()
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.OwnershipChange("a@b.com"),
            )
        }
    }

    @Test
    fun `the Ownership control should display the text provided by the state`() {
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(
                label = "Who owns this item?, placeholder@email.com",
            )
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateCommonContent(currentState) { copy(ownership = "Owner 2") }
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Who owns this item?, Owner 2")
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
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        // Opens the menu
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Folder, No Folder")
            .performClick()

        // Choose the option from the menu
        composeTestRule
            .onAllNodesWithText(text = "Folder 1")
            .onLast()
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.FolderChange("Folder 1".asText()),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `the folder control should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Folder, No Folder")
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateCommonContent(currentState) { copy(folderName = "Folder 2".asText()) }
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Folder, Folder 2")
            .assertIsDisplayed()
    }

    @Suppress("MaxLineLength")
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

    @Suppress("MaxLineLength")
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

    @Suppress("MaxLineLength")
    @Test
    fun `Ownership option should send OwnershipChange action`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        // Opens the menu
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Who owns this item?, placeholder@email.com")
            .performClick()

        // Choose the option from the menu
        composeTestRule
            .onAllNodesWithText(text = "a@b.com")
            .onLast()
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.OwnershipChange("a@b.com"),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_SecureNotes the Ownership control should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Who owns this item?, placeholder@email.com")
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateCommonContent(currentState) { copy(ownership = "Owner 2") }
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Who owns this item?, Owner 2")
            .assertIsDisplayed()
    }

    @Suppress("MaxLineLength")
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
            .onNodeWithText("Name")
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

    @Suppress("MaxLineLength")
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

    @Suppress("MaxLineLength")
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
            .onNodeWithText("Name")
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
            .onNodeWithText("Name")
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

    @Suppress("MaxLineLength")
    @Test
    fun `clicking and changing the custom text field will send a CustomFieldValueChange event`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES_CUSTOM_FIELDS

        composeTestRule
            .onNodeWithTextAfterScroll("TestText")
            .performTextClearance()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.CustomFieldValueChange(
                    VaultAddEditState.Custom.TextField("Test ID", "TestText", ""),
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking and changing the custom hidden field will send a CustomFieldValueChange event`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES_CUSTOM_FIELDS

        composeTestRule
            .onNodeWithTextAfterScroll("TestHidden")
            .performTextClearance()

        verify {
            viewModel.trySendAction(
                VaultAddEditAction.Common.CustomFieldValueChange(
                    VaultAddEditState.Custom.HiddenField("Test ID", "TestHidden", ""),
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
                    VaultAddEditState.Custom.BooleanField("Test ID", "TestBoolean", true),
                ),
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

    //endregion Helper functions

    companion object {
        private val DEFAULT_STATE_LOGIN_DIALOG = VaultAddEditState(
            viewState = VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(),
                type = VaultAddEditState.ViewState.Content.ItemType.Login(),
            ),
            dialog = VaultAddEditState.DialogState.Error("test".asText()),
            vaultAddEditType = VaultAddEditType.AddItem,
        )

        private val DEFAULT_STATE_LOGIN = VaultAddEditState(
            vaultAddEditType = VaultAddEditType.AddItem,
            viewState = VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(),
                type = VaultAddEditState.ViewState.Content.ItemType.Login(),
            ),
            dialog = null,
        )

        private val DEFAULT_STATE_IDENTITY = VaultAddEditState(
            vaultAddEditType = VaultAddEditType.AddItem,
            viewState = VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(),
                type = VaultAddEditState.ViewState.Content.ItemType.Identity(),
            ),
            dialog = null,
        )

        @Suppress("MaxLineLength")
        private val DEFAULT_STATE_SECURE_NOTES_CUSTOM_FIELDS = VaultAddEditState(
            viewState = VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(
                    customFieldData = listOf(
                        VaultAddEditState.Custom.BooleanField("Test ID", "TestBoolean", false),
                        VaultAddEditState.Custom.TextField("Test ID", "TestText", "TestTextVal"),
                        VaultAddEditState.Custom.HiddenField(
                            "Test ID",
                            "TestHidden",
                            "TestHiddenVal",
                        ),
                    ),
                ),
                type = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
            ),
            dialog = null,
            vaultAddEditType = VaultAddEditType.AddItem,
        )

        private val DEFAULT_STATE_SECURE_NOTES = VaultAddEditState(
            vaultAddEditType = VaultAddEditType.AddItem,
            viewState = VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(),
                type = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
            ),
            dialog = null,
        )
    }
}
