package com.x8bit.bitwarden.ui.vault.feature.movetoorganization

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.util.onNodeWithContentDescriptionAfterScroll
import com.x8bit.bitwarden.ui.vault.feature.movetoorganization.util.createMockOrganizationList
import com.x8bit.bitwarden.ui.vault.model.VaultCollection
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VaultMoveToOrganizationScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false

    private val mutableEventFlow = bufferedMutableSharedFlow<VaultMoveToOrganizationEvent>()
    private val mutableStateFlow = MutableStateFlow(createVaultMoveToOrganizationState())

    private val viewModel = mockk<VaultMoveToOrganizationViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            VaultMoveToOrganizationScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `the app bar title should display according to state`() {
        mutableStateFlow.update { currentState ->
            currentState.copy(viewState = VaultMoveToOrganizationState.ViewState.Loading)
        }

        composeTestRule
            .onNodeWithText(text = "Collections")
            .assertIsNotDisplayed()
        composeTestRule
            .onNodeWithText(text = "Move to Organization")
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            currentState.copy(onlyShowCollections = true)
        }

        composeTestRule
            .onNodeWithText(text = "Move to Organization")
            .assertIsNotDisplayed()
        composeTestRule
            .onNodeWithText(text = "Collections")
            .assertIsDisplayed()
    }

    @Test
    fun `the app bar button text should display according to state`() {
        mutableStateFlow.update { currentState ->
            currentState.copy(viewState = VaultMoveToOrganizationState.ViewState.Loading)
        }

        composeTestRule
            .onNodeWithText(text = "Save")
            .assertIsNotDisplayed()
        composeTestRule
            .onNodeWithText(text = "Move")
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            currentState.copy(onlyShowCollections = true)
        }

        composeTestRule
            .onNodeWithText(text = "Move")
            .assertIsNotDisplayed()
        composeTestRule
            .onNodeWithText(text = "Save")
            .assertIsDisplayed()
    }

    @Test
    fun `the organization option field should update according to state`() {
        composeTestRule
            .onNodeWithContentDescription(label = "mockOrganizationName-1. Organization")
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            currentState.copy(onlyShowCollections = true)
        }

        composeTestRule
            .onNodeWithContentDescription(label = "mockOrganizationName-1. Organization")
            .assertIsNotDisplayed()
    }

    @Test
    fun `the organization option field description should update according to state`() {
        composeTestRule
            .onNodeWithText(text = "Choose an organization that", substring = true)
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            currentState.copy(onlyShowCollections = true)
        }

        composeTestRule
        composeTestRule
            .onNodeWithText(text = "Choose an organization that", substring = true)
            .assertIsNotDisplayed()
    }

    @Test
    fun `on NavigateBack event should invoke onNavigateBack`() {
        mutableEventFlow.tryEmit(VaultMoveToOrganizationEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `clicking close button should send BackClick action`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Close")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultMoveToOrganizationAction.BackClick,
            )
        }
    }

    @Test
    fun `clicking move button should send MoveClick action`() {
        composeTestRule
            .onNodeWithText(text = "Move")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultMoveToOrganizationAction.MoveClick,
            )
        }
    }

    @Test
    fun `selecting an organization should send OrganizationSelect action`() {
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "mockOrganizationName-1. Organization")
            .performClick()
        // Choose the option from the menu
        composeTestRule
            .onAllNodesWithText(text = "mockOrganizationName-2")
            .onLast()
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultMoveToOrganizationAction.OrganizationSelect(
                    VaultMoveToOrganizationState.ViewState.Content.Organization(
                        id = "mockOrganizationId-2",
                        name = "mockOrganizationName-2",
                        collections = listOf(
                            VaultCollection(
                                id = "mockId-2",
                                name = "mockName-2",
                                isSelected = false,
                            ),
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `the organization option field should display according to state`() {
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "mockOrganizationName-1. Organization")
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = VaultMoveToOrganizationState.ViewState.Content(
                    organizations = createMockOrganizationList(),
                    selectedOrganizationId = "mockOrganizationId-2",
                ),
            )
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "mockOrganizationName-2. Organization")
            .assertIsDisplayed()
    }

    @Test
    fun `selecting a collection should send CollectionSelect action`() {
        composeTestRule
            .onNodeWithText(text = "mockName-1")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultMoveToOrganizationAction.CollectionSelect(
                    VaultCollection(
                        id = "mockId-1",
                        name = "mockName-1",
                        isSelected = true,
                    ),
                ),
            )
        }
    }

    @Test
    fun `the collection list should display according to state`() {
        composeTestRule
            .onNodeWithText("mockName-1")
            .assertIsOn()

        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = VaultMoveToOrganizationState.ViewState.Content(
                    organizations = createMockOrganizationList()
                        .map { organization ->
                            organization.copy(
                                collections =
                                if (organization.id == "mockOrganizationId-1") {
                                    organization
                                        .collections
                                        .map { collection ->
                                            collection.copy(
                                                isSelected = collection.id != "mockId-1",
                                            )
                                        }
                                } else {
                                    organization.collections
                                },
                            )
                        },
                    selectedOrganizationId = "mockOrganizationId-1",
                ),
            )
        }

        composeTestRule
            .onNodeWithText("mockName-1")
            .assertIsOff()
    }

    @Test
    fun `loading dialog should display according to state`() {
        composeTestRule
            .onAllNodesWithText("loading")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsNotDisplayed()

        mutableStateFlow.update {
            it.copy(
                dialogState = VaultMoveToOrganizationState.DialogState.Loading("loading".asText()),
            )
        }

        composeTestRule
            .onAllNodesWithText("loading")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `error dialog should display according to state`() {
        composeTestRule
            .onAllNodesWithText("error")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsNotDisplayed()

        mutableStateFlow.update {
            it.copy(
                dialogState = VaultMoveToOrganizationState.DialogState.Error("error".asText()),
            )
        }

        composeTestRule
            .onAllNodesWithText("error")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }
}

private fun createVaultMoveToOrganizationState(): VaultMoveToOrganizationState =
    VaultMoveToOrganizationState(
        vaultItemId = "mockId",
        viewState = VaultMoveToOrganizationState.ViewState.Content(
            organizations = createMockOrganizationList(),
            selectedOrganizationId = "mockOrganizationId-1",
        ),
        dialogState = null,
        onlyShowCollections = false,
    )
