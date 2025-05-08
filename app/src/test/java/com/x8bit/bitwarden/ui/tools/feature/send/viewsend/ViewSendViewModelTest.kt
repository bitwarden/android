package com.x8bit.bitwarden.ui.tools.feature.send.viewsend

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ViewSendViewModelTest : BaseViewModelTest() {
    @BeforeEach
    fun setup() {
        mockkStatic(
            SavedStateHandle::toViewSendArgs,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            SavedStateHandle::toViewSendArgs,
        )
    }

    @Test
    fun `initial state should be correct`() {
        val viewModel = createViewModel()
        assertEquals(
            DEFAULT_STATE.copy(viewState = ViewSendState.ViewState.Loading),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on CloseClick should send NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ViewSendAction.CloseClick)
            assertEquals(ViewSendEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on EditClick should send NavigateToEdit`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ViewSendAction.EditClick)
            assertEquals(
                ViewSendEvent.NavigateToEdit(
                    sendType = DEFAULT_STATE.sendType,
                    sendId = DEFAULT_STATE.sendId,
                ),
                awaitItem(),
            )
        }
    }

    private fun createViewModel(
        state: ViewSendState? = null,
    ): ViewSendViewModel = ViewSendViewModel(
        savedStateHandle = SavedStateHandle().apply {
            set(key = "state", value = state)
            every { toViewSendArgs() } returns ViewSendArgs(
                sendId = (state ?: DEFAULT_STATE).sendId,
                sendType = (state ?: DEFAULT_STATE).sendType,
            )
        },
    )
}

private val DEFAULT_STATE = ViewSendState(
    sendType = SendItemType.TEXT,
    sendId = "send_id",
    viewState = ViewSendState.ViewState.Content,
)
