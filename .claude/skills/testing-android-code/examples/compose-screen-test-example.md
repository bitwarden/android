/**
 * Complete Compose Screen Test Example
 *
 * Key patterns demonstrated:
 * - Extending BitwardenComposeTest
 * - Mocking ViewModel with flows
 * - Testing UI interactions
 * - Testing navigation callbacks
 * - Using bufferedMutableSharedFlow for events
 * - Testing dialogs with isDialog() and hasAnyAncestor()
 */
package com.bitwarden.example.feature

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.util.assertNoDialogExists
import com.bitwarden.ui.util.isProgressBar
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test

class ExampleScreenTest : BitwardenComposeTest() {

    // Track navigation callbacks
    private var haveCalledNavigateBack = false
    private var haveCalledNavigateToNext = false

    // Use bufferedMutableSharedFlow for events (default replay = 0)
    private val mutableEventFlow = bufferedMutableSharedFlow<ExampleEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)

    // Mock ViewModel with relaxed = true
    private val viewModel = mockk<ExampleViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        haveCalledNavigateBack = false
        haveCalledNavigateToNext = false

        setContent {
            ExampleScreen(
                onNavigateBack = { haveCalledNavigateBack = true },
                onNavigateToNext = { haveCalledNavigateToNext = true },
                viewModel = viewModel,
            )
        }
    }

    /**
     * Test: Back button sends action to ViewModel
     */
    @Test
    fun `on back click should send BackClick action`() {
        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()

        verify { viewModel.trySendAction(ExampleAction.BackClick) }
    }

    /**
     * Test: Submit button sends action to ViewModel
     */
    @Test
    fun `on submit click should send SubmitClick action`() {
        composeTestRule
            .onNodeWithText("Submit")
            .performClick()

        verify { viewModel.trySendAction(ExampleAction.SubmitClick) }
    }

    /**
     * Test: Loading state shows progress indicator
     */
    @Test
    fun `loading state should display progress indicator`() {
        mutableStateFlow.update { it.copy(isLoading = true) }

        composeTestRule
            .onNode(isProgressBar)
            .assertIsDisplayed()
    }

    /**
     * Test: Data state shows content
     */
    @Test
    fun `data state should display content`() {
        mutableStateFlow.update { it.copy(data = "Test Data") }

        composeTestRule
            .onNodeWithText("Test Data")
            .assertIsDisplayed()
    }

    /**
     * Test: Error state shows error message
     */
    @Test
    fun `error state should display error message`() {
        mutableStateFlow.update { it.copy(errorMessage = "Something went wrong") }

        composeTestRule
            .onNodeWithText("Something went wrong")
            .assertIsDisplayed()
    }

    /**
     * Test: NavigateBack event triggers navigation callback
     */
    @Test
    fun `NavigateBack event should call onNavigateBack`() {
        mutableEventFlow.tryEmit(ExampleEvent.NavigateBack)

        assertTrue(haveCalledNavigateBack)
    }

    /**
     * Test: NavigateToNext event triggers navigation callback
     */
    @Test
    fun `NavigateToNext event should call onNavigateToNext`() {
        mutableEventFlow.tryEmit(ExampleEvent.NavigateToNext)

        assertTrue(haveCalledNavigateToNext)
    }

    /**
     * Test: Item in list can be clicked
     */
    @Test
    fun `on item click should send ItemClick action`() {
        val itemId = "item-123"
        mutableStateFlow.update {
            it.copy(items = listOf(ExampleItem(id = itemId, name = "Test Item")))
        }

        composeTestRule
            .onNodeWithText("Test Item")
            .performClick()

        verify { viewModel.trySendAction(ExampleAction.ItemClick(itemId)) }
    }

    // ==================== DIALOG TESTS ====================

    /**
     * Test: No dialog exists when dialogState is null
     */
    @Test
    fun `no dialog should exist when dialogState is null`() {
        mutableStateFlow.update { it.copy(dialogState = null) }

        composeTestRule.assertNoDialogExists()
    }

    /**
     * Test: Loading dialog displays when state updates
     * PATTERN: Use isDialog() to check dialog exists
     */
    @Test
    fun `loading dialog should display when dialogState is Loading`() {
        mutableStateFlow.update {
            it.copy(dialogState = ExampleState.DialogState.Loading("Please wait..."))
        }

        composeTestRule
            .onNode(isDialog())
            .assertIsDisplayed()

        // Verify loading text within dialog using hasAnyAncestor(isDialog())
        composeTestRule
            .onAllNodesWithText("Please wait...")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    /**
     * Test: Error dialog displays title and message
     * PATTERN: Use filterToOne(hasAnyAncestor(isDialog())) to find text within dialogs
     */
    @Test
    fun `error dialog should display title and message`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = ExampleState.DialogState.Error(
                    title = "An error has occurred",
                    message = "Something went wrong. Please try again.",
                ),
            )
        }

        // Verify dialog exists
        composeTestRule
            .onNode(isDialog())
            .assertIsDisplayed()

        // Verify title within dialog
        composeTestRule
            .onAllNodesWithText("An error has occurred")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        // Verify message within dialog
        composeTestRule
            .onAllNodesWithText("Something went wrong. Please try again.")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    /**
     * Test: Dialog button click sends action
     * PATTERN: Find button with hasAnyAncestor(isDialog()) then performClick()
     */
    @Test
    fun `error dialog dismiss button should send DismissDialog action`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = ExampleState.DialogState.Error(
                    title = "Error",
                    message = "An error occurred",
                ),
            )
        }

        // Click dismiss button within dialog
        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify { viewModel.trySendAction(ExampleAction.DismissDialog) }
    }

    /**
     * Test: Confirmation dialog with multiple buttons
     * PATTERN: Test both confirm and cancel actions
     */
    @Test
    fun `confirmation dialog confirm button should send ConfirmAction`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = ExampleState.DialogState.Confirmation(
                    title = "Confirm Action",
                    message = "Are you sure you want to proceed?",
                ),
            )
        }

        // Click confirm button
        composeTestRule
            .onAllNodesWithText("Confirm")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify { viewModel.trySendAction(ExampleAction.ConfirmAction) }
    }

    @Test
    fun `confirmation dialog cancel button should send DismissDialog action`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = ExampleState.DialogState.Confirmation(
                    title = "Confirm Action",
                    message = "Are you sure?",
                ),
            )
        }

        // Click cancel button
        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify { viewModel.trySendAction(ExampleAction.DismissDialog) }
    }
}

private val DEFAULT_STATE = ExampleState(
    isLoading = false,
    data = null,
    errorMessage = null,
    items = emptyList(),
    dialogState = null,
)

// Example types (normally in separate files)
data class ExampleState(
    val isLoading: Boolean = false,
    val data: String? = null,
    val errorMessage: String? = null,
    val items: List<ExampleItem> = emptyList(),
    val dialogState: DialogState? = null,
) {
    /**
     * PATTERN: Nested sealed class for dialog states.
     * Common dialog types: Loading, Error, Confirmation
     */
    sealed class DialogState {
        data class Loading(val message: String) : DialogState()
        data class Error(val title: String, val message: String) : DialogState()
        data class Confirmation(val title: String, val message: String) : DialogState()
    }
}

data class ExampleItem(val id: String, val name: String)

sealed class ExampleAction {
    data object BackClick : ExampleAction()
    data object SubmitClick : ExampleAction()
    data class ItemClick(val itemId: String) : ExampleAction()
    data object DismissDialog : ExampleAction()
    data object ConfirmAction : ExampleAction()
}

sealed class ExampleEvent {
    data object NavigateBack : ExampleEvent()
    data object NavigateToNext : ExampleEvent()
}
