package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.blockautofill

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BlockAutoFillScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false

    private val mutableEventFlow = bufferedMutableSharedFlow<BlockAutoFillEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<BlockAutoFillViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            BlockAutoFillScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `on back click should send BackClick`() {
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify { viewModel.trySendAction(BlockAutoFillAction.BackClick) }
    }

    @Test
    fun `on NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(BlockAutoFillEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `Screen should display empty state view when in ViewState Empty`() {
        composeTestRule
            .onNodeWithText("Auto-fill will not be offered for these URIs.")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("New blocked URI")
            .assertIsDisplayed()
    }

    @Test
    fun `Screen should display content state view when in ViewState Content`() {
        mutableStateFlow.value = BlockAutoFillState(
            viewState = BlockAutoFillState.ViewState.Content(listOf("uri1", "uri2")),
        )

        listOf("uri1", "uri2").forEach { uri ->
            composeTestRule
                .onNodeWithText(uri)
                .assertIsDisplayed()
        }
    }
}

private val DEFAULT_STATE: BlockAutoFillState = BlockAutoFillState(
    BlockAutoFillState.ViewState.Empty,
)
