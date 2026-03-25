package com.x8bit.bitwarden.ui.vault.feature.cardscanner

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
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
}
