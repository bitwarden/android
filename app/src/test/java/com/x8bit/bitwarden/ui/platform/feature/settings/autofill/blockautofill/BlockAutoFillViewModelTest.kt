package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.blockautofill

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BlockAutoFillViewModelTest : BaseViewModelTest() {

    private val settingsRepository: SettingsRepository = mockk {
        every { blockedAutofillUris } returns listOf("blockedUri")
    }

    @Suppress("MaxLineLength")
    @Test
    fun `initial state with blocked URIs updates state to ViewState Content`() =
        runTest {
            val viewModel = createViewModel()
            val expectedState = BlockAutoFillState(
                viewState = BlockAutoFillState.ViewState.Content(
                    blockedUris = listOf("blockedUri"),
                ),
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `initial state with empty blocked URIs maintains state as ViewState Empty`() =
        runTest {
            every { settingsRepository.blockedAutofillUris } returns emptyList()
            val viewModel = createViewModel()
            val expectedState = BlockAutoFillState(
                viewState = BlockAutoFillState.ViewState.Empty,
            )

            assertEquals(expectedState, viewModel.stateFlow.value)
        }

    @Test
    fun `on AddUriClick should open AddEdit dialog with empty URI`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(BlockAutoFillAction.AddUriClick)

        val expectedDialogState = BlockAutoFillState.DialogState.AddEdit(uri = "")
        assertEquals(expectedDialogState, viewModel.stateFlow.value.dialog)
    }

    @Test
    fun `on UriTextChange should update dialog URI`() = runTest {
        val viewModel = createViewModel()
        val testUri = "http://test.com"
        viewModel.trySendAction(BlockAutoFillAction.UriTextChange(uri = testUri))

        val expectedState = BlockAutoFillState(
            dialog = BlockAutoFillState.DialogState.AddEdit(
                uri = testUri,
                originalUri = null,
                errorMessage = null,
            ),
            viewState = BlockAutoFillState.ViewState.Content(blockedUris = listOf("blockedUri")),
        )

        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    fun `on EditUriClick should open AddEdit dialog with specified URI`() = runTest {
        val viewModel = createViewModel()
        val testUri = "http://edit.com"
        viewModel.trySendAction(BlockAutoFillAction.EditUriClick(uri = testUri))

        val expectedState = BlockAutoFillState(
            dialog = BlockAutoFillState.DialogState.AddEdit(
                uri = testUri,
                originalUri = testUri,
                errorMessage = null,
            ),
            viewState = BlockAutoFillState.ViewState.Content(listOf("blockedUri")),
        )

        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    fun `on RemoveUriClick action should remove specified URI from list`() = runTest {
        val blockedUris = mutableListOf("http://a.com", "http://b.com")

        every { settingsRepository.blockedAutofillUris } answers { blockedUris.toList() }
        every { settingsRepository.blockedAutofillUris = any() } answers {
            blockedUris.clear()
            blockedUris.addAll(firstArg())
        }

        val viewModel = createViewModel()
        viewModel.trySendAction(BlockAutoFillAction.RemoveUriClick(uri = "http://a.com"))

        val expectedState = BlockAutoFillState(
            dialog = null,
            viewState = BlockAutoFillState.ViewState.Content(
                blockedUris = listOf("http://b.com"),
            ),
        )

        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    fun `on SaveUri action with valid URI should add URI to list`() = runTest {
        val blockedUris = mutableListOf("http://existing.com")

        every { settingsRepository.blockedAutofillUris } answers { blockedUris.toList() }
        every { settingsRepository.blockedAutofillUris = any() } answers {
            blockedUris.clear()
            blockedUris.addAll(firstArg())
        }

        val viewModel = createViewModel()
        val testUri = "http://new.com"
        viewModel.trySendAction(BlockAutoFillAction.SaveUri(newUri = testUri))

        val expectedState = BlockAutoFillState(
            dialog = null,
            viewState = BlockAutoFillState.ViewState.Content(
                blockedUris = blockedUris,
            ),
        )

        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    fun `on SaveUri action with valid URIs in a list should add URIs to list`() = runTest {
        val initialUris = mutableListOf("http://existing.com")
        val newUris = "http://new.com, http://another.com"

        every { settingsRepository.blockedAutofillUris } answers { initialUris.toList() }
        every { settingsRepository.blockedAutofillUris = any() } answers {
            initialUris.clear()
            initialUris.addAll(firstArg())
        }

        val viewModel = createViewModel()
        viewModel.trySendAction(BlockAutoFillAction.SaveUri(newUri = newUris))

        val expectedState = BlockAutoFillState(
            dialog = null,
            viewState = BlockAutoFillState.ViewState.Content(
                blockedUris = listOf("http://existing.com", "http://new.com", "http://another.com"),
            ),
        )

        assertEquals(expectedState, viewModel.stateFlow.value)
    }

    @Test
    fun `on BackClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(BlockAutoFillAction.BackClick)
            assertEquals(BlockAutoFillEvent.NavigateBack, awaitItem())
        }
    }

    private fun createViewModel(
        state: BlockAutoFillState? = DEFAULT_STATE,
    ): BlockAutoFillViewModel = BlockAutoFillViewModel(
        savedStateHandle = SavedStateHandle().apply { set("state", state) },
        settingsRepository = settingsRepository,
    )
}

private val DEFAULT_STATE: BlockAutoFillState = BlockAutoFillState(
    viewState = BlockAutoFillState.ViewState.Empty,
)
