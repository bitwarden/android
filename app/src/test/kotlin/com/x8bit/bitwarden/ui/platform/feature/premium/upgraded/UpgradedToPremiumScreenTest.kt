package com.x8bit.bitwarden.ui.platform.feature.premium.upgraded

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.net.toUri
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.manager.IntentManager
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpgradedToPremiumScreenTest : BitwardenComposeTest() {

    private var onDismissCalled = false
    private val mutableEventFlow = bufferedMutableSharedFlow<UpgradedToPremiumEvent>()
    private val viewModel = mockk<UpgradedToPremiumViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
    }
    private val intentManager = mockk<IntentManager> {
        every { launchUri(any()) } just runs
    }

    @Before
    fun setUp() {
        setContent(intentManager = intentManager) {
            UpgradedToPremiumScreen(
                onDismiss = { onDismissCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `title should be displayed`() {
        composeTestRule
            .onNodeWithText("Upgraded to Premium")
            .assertExists()
    }

    @Test
    fun `body should be displayed`() {
        composeTestRule
            .onNodeWithText(
                text = "advanced security features",
                substring = true,
            )
            .assertExists()
    }

    @Test
    fun `learn more button click should send LearnMoreClick action`() {
        composeTestRule
            .onNodeWithText("Learn more")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(UpgradedToPremiumAction.LearnMoreClick) }
    }

    @Test
    fun `close button click should send CloseClick action`() {
        composeTestRule
            .onNodeWithText("Close")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(UpgradedToPremiumAction.CloseClick) }
    }

    @Test
    fun `NavigateBack event should call onDismiss`() {
        mutableEventFlow.tryEmit(UpgradedToPremiumEvent.NavigateBack)
        assertTrue(onDismissCalled)
    }

    @Test
    fun `NavigateToUrl event should call launchUri`() {
        val url = "https://example.com/help"
        mutableEventFlow.tryEmit(UpgradedToPremiumEvent.NavigateToUrl(url = url))
        verify { intentManager.launchUri(uri = url.toUri()) }
    }
}
