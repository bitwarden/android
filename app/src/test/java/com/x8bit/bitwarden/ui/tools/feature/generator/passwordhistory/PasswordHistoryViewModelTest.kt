package com.x8bit.bitwarden.ui.tools.feature.generator.passwordhistory

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.PasswordHistoryView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.model.LocalDataState
import com.x8bit.bitwarden.data.tools.generator.repository.util.FakeGeneratorRepository
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.util.toFormattedPattern
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorPasswordHistoryMode
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class PasswordHistoryViewModelTest : BaseViewModelTest() {

    private val initialState = createPasswordHistoryState()

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )
    private val clipboardManager: BitwardenClipboardManager = mockk()
    private val fakeGeneratorRepository = FakeGeneratorRepository()
    private val mutableVaultItemFlow = MutableStateFlow<DataState<CipherView?>>(DataState.Loading)
    private val fakeVaultRepository: VaultRepository = mockk {
        every { getVaultItemStateFlow("mockId-1") } returns mutableVaultItemFlow
    }

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
            val expectedState = createPasswordHistoryState()
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
            val expectedState = createPasswordHistoryState(
                viewState = PasswordHistoryState.ViewState.Error(
                    message = R.string.an_error_has_occurred.asText(),
                ),
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
            val expectedState = createPasswordHistoryState(
                viewState = PasswordHistoryState.ViewState.Empty,
            )
            val actualState = awaitItem()
            assertEquals(expectedState, actualState)
        }
    }

    @Test
    fun `when VaultRepository emits Loading state the state updates correctly`() = runTest {
        mutableVaultItemFlow.value = DataState.Loading
        val viewModel = createViewModel(
            initialState = createPasswordHistoryState(
                passwordHistoryMode = GeneratorPasswordHistoryMode.Item(itemId = "mockId-1"),
            ),
        )

        viewModel.stateFlow.test {
            val expectedState = createPasswordHistoryState(
                passwordHistoryMode = GeneratorPasswordHistoryMode.Item(itemId = "mockId-1"),
            )
            val actualState = awaitItem()
            assertEquals(expectedState, actualState)
        }
    }

    @Test
    fun `when VaultRepository emits Error state the state updates correctly`() = runTest {
        mutableVaultItemFlow.value = DataState.Error(error = IllegalStateException())
        val viewModel = createViewModel(
            initialState = createPasswordHistoryState(
                passwordHistoryMode = GeneratorPasswordHistoryMode.Item(itemId = "mockId-1"),
            ),
        )
        viewModel.stateFlow.test {
            val expectedState = createPasswordHistoryState(
                viewState = PasswordHistoryState.ViewState.Error(
                    message = R.string.an_error_has_occurred.asText(),
                ),
                passwordHistoryMode = GeneratorPasswordHistoryMode.Item(itemId = "mockId-1"),
            )
            val actualState = awaitItem()
            assertEquals(expectedState, actualState)
        }
    }

    @Test
    fun `when VaultRepository emits Empty state the state updates correctly`() = runTest {
        mutableVaultItemFlow.value = DataState.Loaded(null)
        val viewModel = createViewModel(
            initialState = createPasswordHistoryState(
                passwordHistoryMode = GeneratorPasswordHistoryMode.Item(itemId = "mockId-1"),
            ),
        )

        viewModel.stateFlow.test {
            val expectedState = createPasswordHistoryState(
                viewState = PasswordHistoryState.ViewState.Empty,
                passwordHistoryMode = GeneratorPasswordHistoryMode.Item(itemId = "mockId-1"),
            )
            val actualState = awaitItem()
            assertEquals(expectedState, actualState)
        }
    }

    @Test
    fun `when VaultRepository emits Pending state the state updates correctly`() = runTest {
        mutableVaultItemFlow.value = DataState.Pending(createMockCipherView(1))
        val viewModel = createViewModel(
            initialState = createPasswordHistoryState(
                passwordHistoryMode = GeneratorPasswordHistoryMode.Item(itemId = "mockId-1"),
            ),
        )

        viewModel.stateFlow.test {
            val expectedState = createPasswordHistoryState(
                viewState = PasswordHistoryState.ViewState.Content(
                    passwords = listOf(
                        PasswordHistoryState.GeneratedPassword(
                            password = "mockPassword-1",
                            date = "10/27/23 12:00 PM",
                        ),
                    ),
                ),
                passwordHistoryMode = GeneratorPasswordHistoryMode.Item(itemId = "mockId-1"),
            )
            val actualState = awaitItem()
            assertEquals(expectedState, actualState)
        }
    }

    @Test
    fun `when password history updates the state updates correctly`() = runTest {
        val viewModel = createViewModel()

        val passwordHistoryView = PasswordHistoryView("password", Instant.now())
        fakeGeneratorRepository.storePasswordHistory(passwordHistoryView)

        val expectedState = createPasswordHistoryState(
            viewState = PasswordHistoryState.ViewState.Content(
                passwords = listOf(
                    PasswordHistoryState.GeneratedPassword(
                        password = "password",
                        date = passwordHistoryView.lastUsedDate.toFormattedPattern(
                            pattern = "MM/dd/yy h:mm a",
                            clock = fixedClock,
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
            viewModel.trySendAction(PasswordHistoryAction.CloseClick)
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

        viewModel.trySendAction(PasswordHistoryAction.PasswordCopyClick(generatedPassword))

        verify(exactly = 1) {
            clipboardManager.setText(text = generatedPassword.password)
        }
    }

    @Test
    fun `PasswordClearClick action should update to Empty ViewState`() = runTest {
        val viewModel = createViewModel()

        val passwordHistoryView = PasswordHistoryView("password", Instant.now())
        fakeGeneratorRepository.storePasswordHistory(passwordHistoryView)

        viewModel.trySendAction(PasswordHistoryAction.PasswordClearClick)

        assertTrue(fakeGeneratorRepository.passwordHistoryStateFlow.value is LocalDataState.Loaded)
        assertTrue(
            (fakeGeneratorRepository.passwordHistoryStateFlow.value as LocalDataState.Loaded)
                .data
                .isEmpty(),
        )
    }

    //region Helper Functions

    private fun createViewModel(
        initialState: PasswordHistoryState = createPasswordHistoryState(),
    ): PasswordHistoryViewModel = PasswordHistoryViewModel(
        clock = fixedClock,
        clipboardManager = clipboardManager,
        generatorRepository = fakeGeneratorRepository,
        vaultRepository = fakeVaultRepository,
        savedStateHandle = createSavedStateHandleWithState(state = initialState),
    )

    private fun createPasswordHistoryState(
        viewState: PasswordHistoryState.ViewState = PasswordHistoryState.ViewState.Loading,
        passwordHistoryMode: GeneratorPasswordHistoryMode = GeneratorPasswordHistoryMode.Default,
    ): PasswordHistoryState =
        PasswordHistoryState(
            viewState = viewState,
            passwordHistoryMode = passwordHistoryMode,
        )

    private fun createSavedStateHandleWithState(
        state: PasswordHistoryState? = createPasswordHistoryState(),
        passwordHistoryMode: GeneratorPasswordHistoryMode = GeneratorPasswordHistoryMode.Default,
    ) = SavedStateHandle().apply {
        set("state", state)
        set(
            "password_history_mode",
            when (passwordHistoryMode) {
                is GeneratorPasswordHistoryMode.Default -> "default"
                is GeneratorPasswordHistoryMode.Item -> "item"
            },
        )
        set(
            "password_history_id",
            (passwordHistoryMode as? GeneratorPasswordHistoryMode.Item)?.itemId,
        )
    }

    //endregion Helper Functions
}
