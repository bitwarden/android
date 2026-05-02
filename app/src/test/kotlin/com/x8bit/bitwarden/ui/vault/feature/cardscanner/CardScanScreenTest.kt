package com.x8bit.bitwarden.ui.vault.feature.cardscanner

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.feature.cardscanner.util.FakeCardTextAnalyzer
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config

class CardScanScreenTest : BitwardenComposeTest() {

    private var onNavigateBackCalled = false

    private val cardTextAnalyzer = FakeCardTextAnalyzer()

    private val mutableEventFlow = bufferedMutableSharedFlow<CardScanEvent>()

    private val viewModel = mockk<CardScanViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
    }

    @Before
    fun setup() {
        setContent(
            cardTextAnalyzer = cardTextAnalyzer,
        ) {
            CardScanScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `screen should render with close button`() {
        composeTestRule
            .onNodeWithContentDescription("Close")
            .assertIsDisplayed()
    }

    @Test
    fun `close button click should send CloseClick action`() {
        composeTestRule
            .onNodeWithContentDescription("Close")
            .performClick()

        verify {
            viewModel.trySendAction(CardScanAction.CloseClick)
        }
    }

    @Test
    fun `on NavigateBack event should invoke onNavigateBack`() {
        mutableEventFlow.tryEmit(CardScanEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `title should display Scan card`() {
        composeTestRule
            .onNodeWithText("Scan card")
            .assertExists()
    }

    @Config(qualifiers = "land")
    @Test
    fun `instruction text should display in landscape mode`() {
        composeTestRule
            .onNodeWithText("Position your card within the frame to scan it.")
            .assertIsDisplayed()
    }

    @Test
    fun `instruction text should render above the camera scan frame`() {
        val instructionBottom = composeTestRule
            .onNodeWithTag("CardScanInstruction")
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
            .bottom
        val scanFrameTop = composeTestRule
            .onNodeWithTag("CardScanFrame")
            .getUnclippedBoundsInRoot()
            .top
        // The instruction's bottom edge must sit at or above the scan-frame's top edge so
        // the instruction never visually overlaps or sits below the scan-frame region.
        assertTrue(instructionBottom <= scanFrameTop)
    }
}
