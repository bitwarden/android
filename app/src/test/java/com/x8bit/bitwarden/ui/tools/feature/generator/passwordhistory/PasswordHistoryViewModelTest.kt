package com.x8bit.bitwarden.ui.tools.feature.generator.passwordhistory

import app.cash.turbine.test
import com.bitwarden.core.PasswordHistoryView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.LocalDataState
import com.x8bit.bitwarden.data.tools.generator.repository.util.FakeGeneratorRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.tools.feature.generator.util.toFormattedPattern
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class PasswordHistoryViewModelTest : BaseViewModelTest() {

    private val initialState = PasswordHistoryState(PasswordHistoryState.ViewState.Loading)

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
        }
    }

    @Test
    fun `when repository emits Loading state the state updates correctly`() = runTest {
        val fakeRepository = FakeGeneratorRepository().apply {
            emitPasswordHistoryState(LocalDataState.Loading)
        }
        val viewModel = PasswordHistoryViewModel(generatorRepository = fakeRepository)

        viewModel.stateFlow.test {
            val expectedState = PasswordHistoryState(PasswordHistoryState.ViewState.Loading)
            val actualState = awaitItem()
            assertEquals(expectedState, actualState)
        }
    }

    @Test
    fun `when repository emits Error state the state updates correctly`() = runTest {
        val fakeRepository = FakeGeneratorRepository().apply {
            emitPasswordHistoryState(LocalDataState.Error(Exception("An error has occurred.")))
        }
        val viewModel = PasswordHistoryViewModel(generatorRepository = fakeRepository)

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
        val fakeRepository = FakeGeneratorRepository().apply {
            emitPasswordHistoryState(LocalDataState.Loaded(emptyList()))
        }
        val viewModel = PasswordHistoryViewModel(generatorRepository = fakeRepository)

        viewModel.stateFlow.test {
            val expectedState = PasswordHistoryState(PasswordHistoryState.ViewState.Empty)
            val actualState = awaitItem()
            assertEquals(expectedState, actualState)
        }
    }

    @Test
    fun `when password history updates the state updates correctly`() = runTest {
        val fakeRepository = FakeGeneratorRepository()
        val viewModel = PasswordHistoryViewModel(generatorRepository = fakeRepository)

        val passwordHistoryView = PasswordHistoryView("password", Instant.now())
        fakeRepository.storePasswordHistory(passwordHistoryView)

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
    fun `PasswordCopyClick action should emit CopyTextToClipboard event`() = runTest {
        val viewModel = createViewModel()
        val generatedPassword = PasswordHistoryState.GeneratedPassword(
            password = "testPassword",
            date = "01/01/23",
        )

        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(
                PasswordHistoryAction.PasswordCopyClick(generatedPassword),
            )
            assertEquals(
                PasswordHistoryEvent.CopyTextToClipboard(generatedPassword.password),
                awaitItem(),
            )
        }
    }

    @Test
    fun `PasswordClearClick action should update to Empty ViewState`() = runTest {
        val fakeRepository = FakeGeneratorRepository()
        val viewModel = PasswordHistoryViewModel(generatorRepository = fakeRepository)

        val passwordHistoryView = PasswordHistoryView("password", Instant.now())
        fakeRepository.storePasswordHistory(passwordHistoryView)

        viewModel.actionChannel.trySend(PasswordHistoryAction.PasswordClearClick)

        assertTrue(fakeRepository.passwordHistoryStateFlow.value is LocalDataState.Loaded)
        assertTrue(
            (fakeRepository.passwordHistoryStateFlow.value as LocalDataState.Loaded).data.isEmpty(),
        )
    }

    //region Helper Functions

    private fun createViewModel(): PasswordHistoryViewModel {
        return PasswordHistoryViewModel(generatorRepository = FakeGeneratorRepository())
    }

    //endregion Helper Functions
}
