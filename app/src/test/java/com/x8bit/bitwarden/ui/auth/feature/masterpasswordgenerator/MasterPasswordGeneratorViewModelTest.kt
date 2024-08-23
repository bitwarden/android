package com.x8bit.bitwarden.ui.auth.feature.masterpasswordgenerator

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPassphraseResult
import com.x8bit.bitwarden.data.tools.generator.repository.util.FakeGeneratorRepository
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MasterPasswordGeneratorViewModelTest : BaseViewModelTest() {
    private val fakeGeneratorRepository = FakeGeneratorRepository()
    private val mockPolicyManager = mockk<PolicyManager>(relaxed = true) {
        every { getActivePolicies(type = PolicyTypeJson.MASTER_PASSWORD) } returns emptyList()
    }

    @BeforeEach
    fun setup() {
        fakeGeneratorRepository.setMockGeneratePassphraseResult(
            result = GeneratedPassphraseResult.Success(
                generatedString = "generatedString",
            ),
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `With no saved state and failed generator result, initial password state is default value`() {
        fakeGeneratorRepository.setMockGeneratePassphraseResult(
            result = GeneratedPassphraseResult.InvalidRequest,
        )
        val viewModel = createViewModel()

        assertEquals(
            MasterPasswordGeneratorState(generatedPassword = "-"),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `With previous saved state, initial password state is saved value`() {
        val savedPassword = "saved-pw"
        val viewModel = createViewModel(
            initialState = MasterPasswordGeneratorState(generatedPassword = savedPassword),
        )

        assertEquals(
            MasterPasswordGeneratorState(generatedPassword = savedPassword),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `Verify passphrase request is created and attempts to check for policy constraints`() {
        createViewModel()

        verify { mockPolicyManager.getActivePolicies(type = PolicyTypeJson.MASTER_PASSWORD) }
    }

    @Test
    fun `State updates when generate action is sent and repository returns success result`() =
        runTest {
            val viewModel = createViewModel()
            val newPassPhrase = "I am new"
            val expectedResult = MasterPasswordGeneratorState(generatedPassword = newPassPhrase)

            viewModel.stateFlow.test {
                assertNotEquals(expectedResult, awaitItem())
                fakeGeneratorRepository.setMockGeneratePassphraseResult(
                    GeneratedPassphraseResult.Success(generatedString = newPassPhrase),
                )
                viewModel.trySendAction(
                    MasterPasswordGeneratorAction.GeneratePasswordClickAction,
                )
                assertEquals(expectedResult, awaitItem())
            }
        }

    @Test
    fun `ShowSnackbar event is sent when the generate passphrase result is a failure`() = runTest {
        val viewModel = createViewModel()

        fakeGeneratorRepository.setMockGeneratePassphraseResult(
            GeneratedPassphraseResult.InvalidRequest,
        )

        viewModel.eventFlow.test {
            viewModel.trySendAction(MasterPasswordGeneratorAction.GeneratePasswordClickAction)
            assertEquals(
                MasterPasswordGeneratorEvent.ShowSnackbar(R.string.an_error_has_occurred.asText()),
                awaitItem(),
            )
        }
    }

    @Test
    fun `NavigateBack event is sent when BackClickAction is handled`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(MasterPasswordGeneratorAction.BackClickAction)
            assertEquals(MasterPasswordGeneratorEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `NavigateToPreventLockout event is sent when PreventLockoutClickAction is handled`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.eventFlow.test {
                viewModel.trySendAction(MasterPasswordGeneratorAction.PreventLockoutClickAction)
                assertEquals(MasterPasswordGeneratorEvent.NavigateToPreventLockout, awaitItem())
            }
        }

    @Test
    fun `NavigateBackWithPassword event is sent when SavePasswordClickAction is handled`() =
        runTest {
            val viewModel = createViewModel(
                initialState = MasterPasswordGeneratorState(generatedPassword = "saved-pw"),
            )
            viewModel.eventFlow.test {
                viewModel.trySendAction(MasterPasswordGeneratorAction.SavePasswordClickAction)
                assertEquals(
                    MasterPasswordGeneratorEvent.NavigateBackToRegistration,
                    awaitItem(),
                )
            }
        }

    // region helpers

    private fun createViewModel(
        initialState: MasterPasswordGeneratorState? = null,
    ): MasterPasswordGeneratorViewModel = MasterPasswordGeneratorViewModel(
        savedStateHandle = SavedStateHandle().apply { this["state"] = initialState },
        generatorRepository = fakeGeneratorRepository,
        policyManager = mockPolicyManager,
    )

    // endregion helpers
}
