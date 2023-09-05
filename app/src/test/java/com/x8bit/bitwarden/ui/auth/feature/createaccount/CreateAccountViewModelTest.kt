package com.x8bit.bitwarden.ui.auth.feature.createaccount

import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountEvent
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountViewModel
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class CreateAccountViewModelTest : BaseViewModelTest() {

    @Test
    fun `SubmitClick should emit ShowToast`() = runTest {
        val viewModel = CreateAccountViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(CreateAccountAction.SubmitClick)
            assert(awaitItem() is CreateAccountEvent.ShowToast)
        }
    }
}
