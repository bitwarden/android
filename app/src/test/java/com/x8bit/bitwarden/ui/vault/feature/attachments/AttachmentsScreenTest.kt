package com.x8bit.bitwarden.ui.vault.feature.attachments

import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class AttachmentsScreenTest : BaseComposeTest() {
    private var onNavigateBackCalled = false

    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mutableEventFlow = bufferedMutableSharedFlow<AttachmentsEvent>()
    val viewModel: AttachmentsViewModel = mockk {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
        every { trySendAction(any()) } just runs
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            AttachmentsScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
            )
        }
    }

    @Test
    fun `NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(AttachmentsEvent.NavigateBack)
        Assert.assertTrue(onNavigateBackCalled)
    }
}

private val DEFAULT_STATE: AttachmentsState = AttachmentsState(
    cipherId = "cipherId-1234",
)
