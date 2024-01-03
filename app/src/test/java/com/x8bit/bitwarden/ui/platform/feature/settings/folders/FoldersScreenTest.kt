package com.x8bit.bitwarden.ui.platform.feature.settings.folders

import androidx.compose.ui.test.onNodeWithContentDescription
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

class FoldersScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false
    private val mutableEventFlow = bufferedMutableSharedFlow<FoldersEvent>()
    private val mutableStateFlow = MutableStateFlow(Unit)
    val viewModel = mockk<FoldersViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            FoldersScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
            )
        }
    }

    @Test
    fun `close button click should send CloseButtonClick`() {
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        verify { viewModel.trySendAction(FoldersAction.CloseButtonClick) }
    }

    @Test
    fun `add folder button click should send AddFolderButtonClick`() {
        composeTestRule.onNodeWithContentDescription("Add item").performClick()
        verify {
            viewModel.trySendAction(FoldersAction.AddFolderButtonClick)
        }
    }

    @Test
    fun `NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(FoldersEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }
}
