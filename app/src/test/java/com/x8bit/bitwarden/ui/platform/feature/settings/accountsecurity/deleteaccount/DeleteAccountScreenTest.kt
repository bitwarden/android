package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccount

import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeleteAccountScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false

    private val mutableEventFlow = MutableSharedFlow<DeleteAccountEvent>(
        extraBufferCapacity = Int.MAX_VALUE,
    )
    private val mutableStateFlow = MutableStateFlow(Unit)
    private val viewModel = mockk<DeleteAccountViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            DeleteAccountScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `on NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(DeleteAccountEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }
}
