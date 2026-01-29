/**
 * Complete Compose Screen Test Example
 *
 * Key patterns demonstrated:
 * - Extending BitwardenComposeTest
 * - Mocking ViewModel with flows
 * - Testing UI interactions
 * - Testing navigation callbacks
 * - Using bufferedMutableSharedFlow for events
 */
package com.bitwarden.example.feature

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.util.isProgressBar
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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

    @BeforeEach
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
}

private val DEFAULT_STATE = ExampleState(
    isLoading = false,
    data = null,
    errorMessage = null,
    items = emptyList(),
)

// Example types (normally in separate files)
data class ExampleState(
    val isLoading: Boolean = false,
    val data: String? = null,
    val errorMessage: String? = null,
    val items: List<ExampleItem> = emptyList(),
)

data class ExampleItem(val id: String, val name: String)

sealed class ExampleAction {
    data object BackClick : ExampleAction()
    data object SubmitClick : ExampleAction()
    data class ItemClick(val itemId: String) : ExampleAction()
}

sealed class ExampleEvent {
    data object NavigateBack : ExampleEvent()
    data object NavigateToNext : ExampleEvent()
}
