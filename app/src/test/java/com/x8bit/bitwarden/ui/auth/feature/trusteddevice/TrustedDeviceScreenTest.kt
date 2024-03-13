package com.x8bit.bitwarden.ui.auth.feature.trusteddevice

import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue

class TrustedDeviceScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled: Boolean = false

    private val mutableEventFlow = bufferedMutableSharedFlow<TrustedDeviceEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    val viewModel = mockk<TrustedDeviceViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            TrustedDeviceScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
            )
        }
    }

    @Test
    fun `on NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(TrustedDeviceEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }
}

private val DEFAULT_STATE: TrustedDeviceState = TrustedDeviceState
