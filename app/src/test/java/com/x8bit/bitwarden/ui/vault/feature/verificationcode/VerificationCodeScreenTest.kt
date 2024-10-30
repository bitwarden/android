package com.x8bit.bitwarden.ui.vault.feature.verificationcode

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.baseIconUrl
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

class VerificationCodeScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToSearchCalled = false
    private var onNavigateToVaultItemId: String? = null

    private val mutableEventFlow = bufferedMutableSharedFlow<VerificationCodeEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<VerificationCodeViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            VerificationCodeScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToVaultItemScreen = { onNavigateToVaultItemId = it },
                onNavigateToSearch = { onNavigateToSearchCalled = true },
            )
        }
    }

    @Test
    fun `NavigateBack event should invoke onNavigateBack`() {
        mutableEventFlow.tryEmit(VerificationCodeEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `NavigateToVaultSearchScreen event should invoke onNavigateToSearch`() {
        mutableEventFlow.tryEmit(VerificationCodeEvent.NavigateToVaultSearchScreen)
        assertTrue(onNavigateToSearchCalled)
    }

    @Test
    fun `NavigateToVaultItem event should call onNavigateToVaultItemScreen`() {
        val id = "id4321"
        mutableEventFlow.tryEmit(VerificationCodeEvent.NavigateToVaultItem(id = id))
        assertEquals(id, onNavigateToVaultItemId)
    }

    @Test
    fun `clicking back button should send BackClick action`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Back")
            .performClick()

        verify { viewModel.trySendAction(VerificationCodeAction.BackClick) }
    }

    @Test
    fun `search icon click should send SearchIconClick action`() {
        composeTestRule
            .onNodeWithContentDescription("Search vault")
            .performClick()

        verify { viewModel.trySendAction(VerificationCodeAction.SearchIconClick) }
    }

    @Test
    fun `refresh button click should send RefreshClick action`() {
        mutableStateFlow.update {
            it.copy(viewState = VerificationCodeState.ViewState.Error(message = "".asText()))
        }

        composeTestRule
            .onNodeWithText("Try again")
            .performClick()

        verify { viewModel.trySendAction(VerificationCodeAction.RefreshClick) }
    }

    @Test
    fun `error text and retry should be displayed according to state`() {
        val message = "error_message"
        mutableStateFlow.update { DEFAULT_STATE }
        composeTestRule
            .onNodeWithText(message)
            .assertIsNotDisplayed()

        mutableStateFlow.update { it.copy(viewState = VerificationCodeState.ViewState.Loading) }

        composeTestRule
            .onNodeWithText(message)
            .assertIsNotDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = VerificationCodeState.ViewState.Error(message.asText()))
        }
        composeTestRule
            .onNodeWithText(message)
            .assertIsDisplayed()
    }

    @Test
    fun `auth code and copy button should be displayed according to state`() {
        val authCode = "123 456"
        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                viewState = VerificationCodeState.ViewState.Content(
                    verificationCodeDisplayItems = listOf(
                        createDisplayItem(
                            number = 1,
                            hideAuthCode = false,
                        ),
                    ),
                ),
            )
        }
        composeTestRule
            .onNodeWithText(authCode)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Copy")
            .assertIsDisplayed()

        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                viewState = VerificationCodeState.ViewState.Content(
                    verificationCodeDisplayItems = listOf(
                        createDisplayItem(
                            number = 1,
                            hideAuthCode = true,
                        ),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(authCode)
            .assertIsNotDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Copy")
            .assertIsNotDisplayed()
    }

    @Test
    fun `Items text should be displayed according to state`() {
        mutableStateFlow.update { DEFAULT_STATE }

        composeTestRule
            .onNodeWithText(text = "ITEMS (2)")
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = VerificationCodeState.ViewState.Content(
                    verificationCodeDisplayItems = listOf(
                        createDisplayItem(1),
                        createDisplayItem(2),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(text = "ITEMS (2)")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `Items text count should be displayed according to state`() {
        mutableStateFlow.update { DEFAULT_STATE }
        composeTestRule
            .onNodeWithText(text = "ITEMS (1)")
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = VerificationCodeState.ViewState.Content(
                    verificationCodeDisplayItems = listOf(
                        createDisplayItem(number = 1),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(text = "ITEMS (1)")
            .performScrollTo()
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                viewState = VerificationCodeState.ViewState.Content(
                    verificationCodeDisplayItems = listOf(
                        createDisplayItem(number = 1),
                        createDisplayItem(number = 2),
                        createDisplayItem(number = 3),
                        createDisplayItem(number = 4),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(text = "ITEMS (4)")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `displayItems should be displayed according to state`() {
        mutableStateFlow.update {
            it.copy(
                viewState = VerificationCodeState.ViewState.Content(
                    verificationCodeDisplayItems = listOf(
                        createDisplayItem(number = 1),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(text = "Label 1")
            .assertIsDisplayed()
    }

    @Test
    fun `clicking on a display item should send ItemClick action`() {
        mutableStateFlow.update {
            it.copy(
                viewState = VerificationCodeState.ViewState.Content(
                    verificationCodeDisplayItems = listOf(
                        createDisplayItem(number = 1),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(text = "Label 1")
            .assertIsDisplayed()
            .performClick()

        verify {
            viewModel.trySendAction(VerificationCodeAction.ItemClick("1"))
        }
    }

    @Test
    fun `clicking on copy button should send CopyClick action`() {
        mutableStateFlow.update {
            it.copy(
                viewState = VerificationCodeState.ViewState.Content(
                    verificationCodeDisplayItems = listOf(
                        createDisplayItem(number = 1),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithContentDescription(label = "Copy")
            .assertIsDisplayed()
            .performClick()

        verify {
            viewModel.trySendAction(VerificationCodeAction.CopyClick("123456"))
        }
    }

    @Test
    fun `on overflow item click should display menu`() {
        composeTestRule
            .onNodeWithContentDescription(label = "More")
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Sync")
            .filterToOne(hasAnyAncestor(isPopup()))
            .isDisplayed()

        composeTestRule
            .onAllNodesWithText(text = "Lock")
            .filterToOne(hasAnyAncestor(isPopup()))
            .isDisplayed()
    }

    @Test
    fun `on sync click should send SyncClick`() {
        composeTestRule
            .onNodeWithContentDescription(label = "More")
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Sync")
            .filterToOne(hasAnyAncestor(isPopup()))
            .performClick()

        verify {
            viewModel.trySendAction(VerificationCodeAction.SyncClick)
        }
    }

    @Test
    fun `on lock click should send LockClick`() {
        composeTestRule
            .onNodeWithContentDescription(label = "More")
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Lock")
            .filterToOne(hasAnyAncestor(isPopup()))
            .performClick()

        verify {
            viewModel.trySendAction(VerificationCodeAction.LockClick)
        }
    }

    @Test
    fun `loading dialog should be displayed according to state`() {
        val loadingMessage = "syncing"
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText(loadingMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = VerificationCodeState.DialogState.Loading(
                    message = loadingMessage.asText(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(loadingMessage)
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
    }
}

private fun createDisplayItem(
    number: Int,
    hideAuthCode: Boolean = false,
): VerificationCodeDisplayItem =
    VerificationCodeDisplayItem(
        id = number.toString(),
        authCode = "123456",
        hideAuthCode = hideAuthCode,
        label = "Label $number",
        supportingLabel = "Supporting Label $number",
        periodSeconds = 30,
        timeLeftSeconds = 15,
    )

private val DEFAULT_STATE = VerificationCodeState(
    viewState = VerificationCodeState.ViewState.Loading,
    vaultFilterType = VaultFilterType.AllVaults,
    isIconLoadingDisabled = false,
    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
    isPullToRefreshSettingEnabled = false,
    dialogState = null,
    isRefreshing = false,
)
