package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test

class SetupCompleteViewModelTest : BaseViewModelTest() {

    private val authRepository: AuthRepository = mockk {
        every { goToNextStep() } just runs
    }

    @Test
    fun `When CompleteSetup action is sent user state is updated with onboarding COMPLETE`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(SetupCompleteAction.CompleteSetup)
        verify(exactly = 1) {
            authRepository.goToNextStep()
        }
    }

    private fun createViewModel(): SetupCompleteViewModel = SetupCompleteViewModel(
        authRepository = authRepository,
    )
}
