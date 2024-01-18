package com.x8bit.bitwarden.ui.platform.feature.settings.autofill

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AutoFillViewModelTest : BaseViewModelTest() {

    private val mutableIsAutofillEnabledStateFlow = MutableStateFlow(false)
    private val settingsRepository: SettingsRepository = mockk() {
        every { isInlineAutofillEnabled } returns true
        every { isInlineAutofillEnabled = any() } just runs
        every { isAutofillEnabledStateFlow } returns mutableIsAutofillEnabledStateFlow
        every { disableAutofill() } just runs
    }

    @Test
    fun `initial state should be correct when not set`() {
        val viewModel = createViewModel(state = null)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct when set`() {
        mutableIsAutofillEnabledStateFlow.value = true
        val state = DEFAULT_STATE.copy(
            isAutoFillServicesEnabled = true,
            uriDetectionMethod = AutoFillState.UriDetectionMethod.REGULAR_EXPRESSION,
        )
        val viewModel = createViewModel(state = state)
        assertEquals(state, viewModel.stateFlow.value)
    }

    @Test
    fun `changes in autofill enabled status should update the state`() {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)

        mutableIsAutofillEnabledStateFlow.value = true

        assertEquals(
            DEFAULT_STATE.copy(isAutoFillServicesEnabled = true),
            viewModel.stateFlow.value,
        )

        mutableIsAutofillEnabledStateFlow.value = false

        assertEquals(
            DEFAULT_STATE.copy(isAutoFillServicesEnabled = false),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on AskToAddLoginClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AutoFillAction.AskToAddLoginClick(true))
            assertEquals(AutoFillEvent.ShowToast("Not yet implemented.".asText()), awaitItem())
        }
        assertEquals(
            DEFAULT_STATE.copy(isAskToAddLoginEnabled = true),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on AutoFillServicesClick with false should disable autofill`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(AutoFillAction.AutoFillServicesClick(false))
        verify {
            settingsRepository.disableAutofill()
        }
        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on AutoFillServicesClick with true should emit NavigateToAutofillSettings`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AutoFillAction.AutoFillServicesClick(true))
            assertEquals(
                AutoFillEvent.NavigateToAutofillSettings,
                awaitItem(),
            )
        }
        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on BackClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AutoFillAction.BackClick)
            assertEquals(AutoFillEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on CopyTotpAutomaticallyClick should update the isCopyTotpAutomaticallyEnabled state`() =
        runTest {
            val viewModel = createViewModel()
            val isEnabled = true
            viewModel.eventFlow.test {
                viewModel.trySendAction(AutoFillAction.CopyTotpAutomaticallyClick(isEnabled))
                assertEquals(AutoFillEvent.ShowToast("Not yet implemented.".asText()), awaitItem())
            }
            assertEquals(
                DEFAULT_STATE.copy(isCopyTotpAutomaticallyEnabled = isEnabled),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `on UseInlineAutofillClick should update the state and save the new value to settings`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(AutoFillAction.UseInlineAutofillClick(false))
        assertEquals(
            DEFAULT_STATE.copy(isUseInlineAutoFillEnabled = false),
            viewModel.stateFlow.value,
        )
        verify { settingsRepository.isInlineAutofillEnabled = false }
    }

    @Test
    fun `on UriDetectionMethodSelect should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        val method = AutoFillState.UriDetectionMethod.EXACT
        viewModel.eventFlow.test {
            viewModel.trySendAction(AutoFillAction.UriDetectionMethodSelect(method))
            assertEquals(AutoFillEvent.ShowToast("Not yet implemented.".asText()), awaitItem())
        }
        assertEquals(
            DEFAULT_STATE.copy(uriDetectionMethod = method),
            viewModel.stateFlow.value,
        )
    }

    private fun createViewModel(
        state: AutoFillState? = DEFAULT_STATE,
    ): AutoFillViewModel = AutoFillViewModel(
        savedStateHandle = SavedStateHandle().apply { set("state", state) },
        settingsRepository = settingsRepository,
    )
}

private val DEFAULT_STATE: AutoFillState = AutoFillState(
    isAskToAddLoginEnabled = false,
    isAutoFillServicesEnabled = false,
    isCopyTotpAutomaticallyEnabled = false,
    isUseInlineAutoFillEnabled = true,
    uriDetectionMethod = AutoFillState.UriDetectionMethod.DEFAULT,
)
