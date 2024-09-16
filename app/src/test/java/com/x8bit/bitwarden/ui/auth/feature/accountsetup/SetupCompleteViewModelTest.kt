package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SetupCompleteViewModelTest : BaseViewModelTest() {

    private val mockUserState = mockk<UserState> {
        every { activeUserId } returns DEFAULT_USER_ID
    }
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(mockUserState)
    private val authRepository: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
        every { setOnboardingStatus(any(), any()) } just runs
    }

    @Test
    fun `When user state has no active accounts then throw IllegalStateException`() {
        mutableUserStateFlow.value = null
        assertThrows<IllegalArgumentException> {
            createViewModel()
        }
    }

    @Test
    fun `When user state has active account then ViewModel state should contain active user ID`() {
        val viewModel = createViewModel()
        assertEquals(
            SetupCompleteState(userId = DEFAULT_USER_ID),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `When CompleteSetup action is sent user state is updated with onboarding COMPLETE`() {
        val viewModel = createViewModel()
        assertEquals(
            SetupCompleteState(userId = DEFAULT_USER_ID),
            viewModel.stateFlow.value,
        )
        viewModel.trySendAction(SetupCompleteAction.CompleteSetup)
        verify {
            authRepository.setOnboardingStatus(
                DEFAULT_USER_ID,
                OnboardingStatus.COMPLETE,
            )
        }
    }

    private fun createViewModel() = SetupCompleteViewModel(authRepository = authRepository)
}

private const val DEFAULT_USER_ID = "userId"
