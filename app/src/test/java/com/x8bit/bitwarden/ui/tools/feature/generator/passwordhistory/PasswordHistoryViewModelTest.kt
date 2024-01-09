package com.x8bit.bitwarden.ui.tools.feature.generator.passwordhistory

import app.cash.turbine.test
import com.bitwarden.core.PasswordHistoryView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.repository.model.LocalDataState
import com.x8bit.bitwarden.data.tools.generator.repository.util.FakeGeneratorRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.util.toFormattedPattern
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class PasswordHistoryViewModelTest : BaseViewModelTest() {

    private val initialState = PasswordHistoryState(PasswordHistoryState.ViewState.Loading)

    private val clipboardManager: BitwardenClipboardManager = mockk()
    private val fakeGeneratorRepository = FakeGeneratorRepository()

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
        }
    }

    @Test
    fun `when repository emits Loading state the state updates correctly`() = runTest {
        fakeGeneratorRepository.emitPasswordHistoryState(LocalDataState.Loading)
        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            val expectedState = PasswordHistoryState(PasswordHistoryState.ViewState.Loading)
            val actualState = awaitItem()
            assertEquals(expectedState, actualState)
        }
    }

    @Test
    fun `when repository emits Error state the state updates correctly`() = runTest {
        fakeGeneratorRepository.emitPasswordHistoryState(
            state = LocalDataState.Error(Exception("An error has occurred.")),
        )
        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            val expectedState = PasswordHistoryState(
                PasswordHistoryState.ViewState.Error(R.string.an_error_has_occurred.asText()),
            )
            val actualState = awaitItem()
            assertEquals(expectedState, actualState)
        }
    }

    @Test
    fun `when repository emits Empty state the state updates correctly`() = runTest {
        fakeGeneratorRepository.emitPasswordHistoryState(LocalDataState.Loaded(emptyList()))
        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            val expectedState = PasswordHistoryState(PasswordHistoryState.ViewState.Empty)
            val actualState = awaitItem()
            assertEquals(expectedState, actualState)
        }
    }

    @Test
    fun `when password history updates the state updates correctly`() = runTest {
        val viewModel = createViewModel()

        val passwordHistoryView = PasswordHistoryView("password", Instant.now())
        fakeGeneratorRepository.storePasswordHistory(passwordHistoryView)

        val expectedState = PasswordHistoryState(
            viewState = PasswordHistoryState.ViewState.Content(
                passwords = listOf(
                    PasswordHistoryState.GeneratedPassword(
                        password = "password",
                        date = passwordHistoryView.lastUsedDate.toFormattedPattern(
                            pattern = "MM/dd/yy h:mm a",
                        ),
                    ),
                ),
            ),
        )

        viewModel.stateFlow.test {
            val actualState = awaitItem()
            assertEquals(expectedState, actualState)
        }
    }

    @Test
    fun `CloseClick action should emit NavigateBack event`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(PasswordHistoryAction.CloseClick)
            assertEquals(PasswordHistoryEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `PasswordCopyClick action should call setText on the ClipboardManager`() {
        val viewModel = createViewModel()
        val generatedPassword = PasswordHistoryState.GeneratedPassword(
            password = "testPassword",
            date = "01/01/23",
        )
        every { clipboardManager.setText(text = generatedPassword.password) } just runs

        viewModel.actionChannel.trySend(PasswordHistoryAction.PasswordCopyClick(generatedPassword))

        verify(exactly = 1) {
            clipboardManager.setText(text = generatedPassword.password)
        }
    }

    @Test
    fun `PasswordClearClick action should update to Empty ViewState`() = runTest {
        val viewModel = createViewModel()

        val passwordHistoryView = PasswordHistoryView("password", Instant.now())
        fakeGeneratorRepository.storePasswordHistory(passwordHistoryView)

        viewModel.actionChannel.trySend(PasswordHistoryAction.PasswordClearClick)

        assertTrue(fakeGeneratorRepository.passwordHistoryStateFlow.value is LocalDataState.Loaded)
        assertTrue(
            (fakeGeneratorRepository.passwordHistoryStateFlow.value as LocalDataState.Loaded)
                .data
                .isEmpty(),
        )
    }

    //region Helper Functions

    private fun createViewModel(): PasswordHistoryViewModel = PasswordHistoryViewModel(
        clipboardManager = clipboardManager,
        generatorRepository = fakeGeneratorRepository,
    )

    //endregion Helper Functions
}
