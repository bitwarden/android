package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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

class VaultScreenTest : BaseComposeTest() {

    private var onNavigateToVaultAddItemScreenCalled = false

    private val mutableEventFlow = MutableSharedFlow<VaultEvent>(
        extraBufferCapacity = Int.MAX_VALUE,
    )
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<VaultViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            VaultScreen(
                viewModel = viewModel,
                onNavigateToVaultAddItemScreen = { onNavigateToVaultAddItemScreenCalled = true },
            )
        }
    }

    @Test
    fun `search icon click should send SearchIconClick action`() {
        mutableStateFlow.update { it.copy(viewState = VaultState.ViewState.NoItems) }
        composeTestRule.onNodeWithContentDescription("Search vault").performClick()
        verify { viewModel.trySendAction(VaultAction.SearchIconClick) }
    }

    @Test
    fun `floating action button click should send AddItemClick action`() {
        mutableStateFlow.update { it.copy(viewState = VaultState.ViewState.NoItems) }
        composeTestRule.onNodeWithContentDescription("Add item").performClick()
        verify { viewModel.trySendAction(VaultAction.AddItemClick) }
    }

    @Test
    fun `add an item button click should send AddItemClick action`() {
        mutableStateFlow.update { it.copy(viewState = VaultState.ViewState.NoItems) }
        composeTestRule.onNodeWithText("Add an Item").performClick()
        verify { viewModel.trySendAction(VaultAction.AddItemClick) }
    }

    @Test
    fun `NavigateToAddItemScreen event should call onNavigateToVaultAddItemScreen`() {
        mutableEventFlow.tryEmit(VaultEvent.NavigateToAddItemScreen)
        assertTrue(onNavigateToVaultAddItemScreenCalled)
    }

    @Test
    fun `clicking a login item should send LoginGroupClick action`() {
        mutableStateFlow.update {
            it.copy(
                viewState = VaultState.ViewState.Content(
                    loginItemsCount = 0,
                    cardItemsCount = 0,
                    identityItemsCount = 0,
                    secureNoteItemsCount = 0,
                    favoriteItems = emptyList(),
                    folderItems = emptyList(),
                    noFolderItems = emptyList(),
                    trashItemsCount = 0,
                ),
            )
        }

        composeTestRule.onNodeWithText("Login").performScrollTo().performClick()
        verify {
            viewModel.trySendAction(VaultAction.LoginGroupClick)
        }
    }

    @Test
    fun `clicking a card item should send CardGroupClick action`() {
        mutableStateFlow.update {
            it.copy(
                viewState = VaultState.ViewState.Content(
                    loginItemsCount = 0,
                    cardItemsCount = 0,
                    identityItemsCount = 0,
                    secureNoteItemsCount = 0,
                    favoriteItems = emptyList(),
                    folderItems = emptyList(),
                    noFolderItems = emptyList(),
                    trashItemsCount = 0,
                ),
            )
        }

        composeTestRule.onNodeWithText("Card").performScrollTo().performClick()
        verify {
            viewModel.trySendAction(VaultAction.CardGroupClick)
        }
    }

    @Test
    fun `clicking an identity item should send IdentityGroupClick action`() {
        mutableStateFlow.update {
            it.copy(
                viewState = VaultState.ViewState.Content(
                    loginItemsCount = 0,
                    cardItemsCount = 0,
                    identityItemsCount = 0,
                    secureNoteItemsCount = 0,
                    favoriteItems = emptyList(),
                    folderItems = emptyList(),
                    noFolderItems = emptyList(),
                    trashItemsCount = 0,
                ),
            )
        }

        composeTestRule.onNodeWithText("Identity").performScrollTo().performClick()
        verify {
            viewModel.trySendAction(VaultAction.IdentityGroupClick)
        }
    }

    @Test
    fun `clicking a secure note item should send SecureNoteGroupClick action`() {
        mutableStateFlow.update {
            it.copy(
                viewState = VaultState.ViewState.Content(
                    loginItemsCount = 0,
                    cardItemsCount = 0,
                    identityItemsCount = 0,
                    secureNoteItemsCount = 0,
                    favoriteItems = emptyList(),
                    folderItems = emptyList(),
                    noFolderItems = emptyList(),
                    trashItemsCount = 0,
                ),
            )
        }

        composeTestRule.onNodeWithText("Secure note").performScrollTo().performClick()
        verify {
            viewModel.trySendAction(VaultAction.SecureNoteGroupClick)
        }
    }
}

private val DEFAULT_STATE: VaultState = VaultState(
    avatarColorString = "FF0000FF",
    initials = "BW",
    viewState = VaultState.ViewState.Loading,
)
