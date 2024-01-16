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
            .onNodeWithContentDescriptionAfterScroll(label = "Organization, Organization 1")
            .performClick()
        // Choose the option from the menu
        composeTestRule
            .onAllNodesWithText(text = "Organization 2")
            .onLast()
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultMoveToOrganizationAction.OrganizationSelect(
                    VaultMoveToOrganizationState.ViewState.Content.Organization(
                        id = "2",
                        name = "Organization 2",
                        collections = listOf(
                            VaultMoveToOrganizationState.ViewState.Content.Collection(
                                id = "1",
                                name = "Collection 1",
                                isSelected = true,
                            ),
                            VaultMoveToOrganizationState.ViewState.Content.Collection(
                                id = "2",
                                name = "Collection 2",
                                isSelected = false,
                            ),
                            VaultMoveToOrganizationState.ViewState.Content.Collection(
                                id = "3",
                                name = "Collection 3",
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
            .onNodeWithContentDescriptionAfterScroll(label = "Organization, Organization 1")
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = VaultMoveToOrganizationState.ViewState.Content(
                    organizations = createMockOrganizationList(),
                    selectedOrganizationId = "2",
                ),
            )
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Organization, Organization 2")
            .assertIsDisplayed()
    }

    @Test
    fun `selecting a collection should send CollectionSelect action`() {
        composeTestRule
            .onNodeWithText(text = "Collection 2")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultMoveToOrganizationAction.CollectionSelect(
                    VaultMoveToOrganizationState.ViewState.Content.Collection(
                        id = "2",
                        name = "Collection 2",
                        isSelected = false,
                    ),
                ),
            )
        }
    }

    @Test
    fun `the collection list should display according to state`() {
        composeTestRule
            .onNodeWithText("Collection 1")
            .assertIsOn()
        composeTestRule
            .onNodeWithText("Collection 2")
            .assertIsOff()
        composeTestRule
            .onNodeWithText("Collection 3")
            .assertIsOff()

        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = VaultMoveToOrganizationState.ViewState.Content(
                    organizations = createMockOrganizationList()
                        .map { organization ->
                            organization.copy(
                                collections =
                                if (organization.id == "1") {
                                    organization
                                        .collections
                                        .map { collection ->
                                            collection.copy(isSelected = collection.id != "1")
                                        }
                                } else {
                                    organization.collections
                                },
                            )
                        },
                    selectedOrganizationId = "1",
                ),
            )
        }

        composeTestRule
            .onNodeWithText("Collection 1")
            .assertIsOff()
        composeTestRule
            .onNodeWithText("Collection 2")
            .assertIsOn()
        composeTestRule
            .onNodeWithText("Collection 3")
            .assertIsOn()
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
            selectedOrganizationId = "1",
        ),
        dialogState = null,
    )
