package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SetupAutoFillViewModelTest : BaseViewModelTest() {

    private val mutableAutoFillEnabledStateFlow = MutableStateFlow(false)
    private val settingsRepository = mockk<SettingsRepository> {
        every { isAutofillEnabledStateFlow } returns mutableAutoFillEnabledStateFlow
        every { disableAutofill() } just runs
    }

    private val mutableFirstTimeStateFlow = MutableStateFlow(FirstTimeState())
    private val firstTimeActionManager: FirstTimeActionManager = mockk {
        every { firstTimeStateFlow } returns mutableFirstTimeStateFlow
        every { storeShowAutoFillSettingBadge(any()) } just runs
    }

    private val mockUserState = mockk<UserState> {
        every { activeUserId } returns DEFAULT_USER_ID
    }
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(mockUserState)
    private val authRepository: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
        every { setOnboardingStatus(any(), any()) } just runs
    }

    @Test
    fun `handleAutofillEnabledUpdateReceive updates autofillEnabled state`() {
        val viewModel = createViewModel()
        assertFalse(viewModel.stateFlow.value.autofillEnabled)
        mutableAutoFillEnabledStateFlow.value = true

        assertTrue(viewModel.stateFlow.value.autofillEnabled)
    }

    @Test
    fun `handleAutofillServiceChanged with autofillEnabled true navigates to autofill settings`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(SetupAutoFillAction.AutofillServiceChanged(true))
                assertEquals(
                    SetupAutoFillEvent.NavigateToAutofillSettings,
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `handleAutofillServiceChanged with autofillEnabled false disables autofill`() =
        runTest {
            val viewModel = createViewModel()
            mutableAutoFillEnabledStateFlow.value = true
            assertTrue(viewModel.stateFlow.value.autofillEnabled)
            viewModel.eventFlow.test {
                viewModel.trySendAction(SetupAutoFillAction.AutofillServiceChanged(false))
                expectNoEvents()
            }
            verify { settingsRepository.disableAutofill() }
        }

    @Test
    fun `handleTurnOnLater click sets dialogState to TurnOnLaterDialog`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(SetupAutoFillAction.TurnOnLaterClick)
        assertEquals(
            SetupAutoFillDialogState.TurnOnLaterDialog,
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Test
    fun `handleDismissDialog sets dialogState to null`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(SetupAutoFillAction.TurnOnLaterClick)
        assertEquals(
            SetupAutoFillDialogState.TurnOnLaterDialog,
            viewModel.stateFlow.value.dialogState,
        )
        viewModel.trySendAction(SetupAutoFillAction.DismissDialog)
        assertNull(viewModel.stateFlow.value.dialogState)
    }

    @Test
    fun `handleAutoFillServiceFallback sets dialogState to AutoFillFallbackDialog`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(SetupAutoFillAction.AutoFillServiceFallback)
        assertEquals(
            SetupAutoFillDialogState.AutoFillFallbackDialog,
            viewModel.stateFlow.value.dialogState,
        )
    }

    @Test
    fun `handleTurnOnLaterConfirmClick sets onboarding status to FINAL_STEP`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(SetupAutoFillAction.TurnOnLaterConfirmClick)
        verify {
            authRepository.setOnboardingStatus(
                DEFAULT_USER_ID,
                OnboardingStatus.FINAL_STEP,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `handleContinueClick sets onboarding status to FINAL_STEP and sets first time flag to false`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(SetupAutoFillAction.ContinueClick)
        verify(exactly = 1) {
            authRepository.setOnboardingStatus(
                DEFAULT_USER_ID,
                OnboardingStatus.FINAL_STEP,
            )
            firstTimeActionManager.storeShowAutoFillSettingBadge(showBadge = false)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `handleContinueClick send NavigateBack event when not initial setup and sets first time flag to false`() =
        runTest {
            val viewModel =
                createViewModel(initialState = DEFAULT_STATE.copy(isInitialSetup = false))
            viewModel.eventFlow.test {
                viewModel.trySendAction(SetupAutoFillAction.ContinueClick)
                assertEquals(
                    SetupAutoFillEvent.NavigateBack,
                    awaitItem(),
                )
            }
            verify(exactly = 1) {
                firstTimeActionManager.storeShowAutoFillSettingBadge(showBadge = false)
            }
            verify(exactly = 0) {
                authRepository.setOnboardingStatus(
                    DEFAULT_USER_ID,
                    OnboardingStatus.FINAL_STEP,
                )
            }
        }

    @Test
    fun `handleTurnOnLaterConfirmClick sets showAutoFillSettingBadge to true`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(SetupAutoFillAction.TurnOnLaterConfirmClick)
        verify {
            firstTimeActionManager.storeShowAutoFillSettingBadge(
                showBadge = true,
            )
        }
    }

    @Test
    fun `handleClose click sends NavigateBack event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SetupAutoFillAction.CloseClick)
            assertEquals(SetupAutoFillEvent.NavigateBack, awaitItem())
        }
    }

    private fun createViewModel(
        initialState: SetupAutoFillState? = null,
    ) = SetupAutoFillViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(
                "state" to initialState,
                "isInitialSetup" to true,
            ),
        ),
        settingsRepository = settingsRepository,
        authRepository = authRepository,
        firstTimeActionManager = firstTimeActionManager,
    )
}

private const val DEFAULT_USER_ID = "userId"

private val DEFAULT_STATE = SetupAutoFillState(
    userId = DEFAULT_USER_ID,
    dialogState = null,
    autofillEnabled = false,
    isInitialSetup = true,
)
