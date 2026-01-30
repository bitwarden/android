package com.bitwarden.authenticator.ui.auth.unlock

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.authenticator.data.auth.repository.AuthRepository
import com.bitwarden.authenticator.data.platform.repository.model.BiometricsUnlockResult
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import javax.crypto.Cipher

class UnlockViewModelTest : BaseViewModelTest() {

    private val mockCipher: Cipher = mockk()
    private val mockAuthRepository: AuthRepository = mockk {
        every { isUnlockWithBiometricsEnabled } returns true
        every { isBiometricIntegrityValid() } returns true
        every { isAccountBiometricIntegrityValid() } returns true
    }

    @Test
    fun `initial state should be correct when no saved state`() {
        every { mockAuthRepository.getOrCreateCipher() } returns null

        val viewModel = createViewModel()

        assertEquals(
            UnlockState(
                isBiometricsEnabled = true,
                isBiometricsValid = true,
                showBiometricInvalidatedMessage = false,
                dialog = null,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `initial state should restore from SavedStateHandle`() {
        every { mockAuthRepository.getOrCreateCipher() } returns null

        val savedState = UnlockState(
            isBiometricsEnabled = false,
            isBiometricsValid = false,
            showBiometricInvalidatedMessage = true,
            dialog = UnlockState.Dialog.Loading,
        )

        val viewModel = createViewModel(initialState = savedState)

        assertEquals(savedState, viewModel.stateFlow.value)
    }

    @Test
    fun `init should emit PromptForBiometrics when cipher available`() = runTest {
        every { mockAuthRepository.getOrCreateCipher() } returns mockCipher

        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            assertEquals(UnlockEvent.PromptForBiometrics(mockCipher), awaitItem())
        }
    }

    @Test
    fun `init should not emit event when cipher unavailable`() = runTest {
        every { mockAuthRepository.getOrCreateCipher() } returns null

        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            expectNoEvents()
        }
    }

    @Test
    fun `BiometricsUnlockClick with valid cipher should emit PromptForBiometrics`() = runTest {
        every { mockAuthRepository.getOrCreateCipher() } returns mockCipher

        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            // Skip init event
            skipItems(1)

            viewModel.trySendAction(UnlockAction.BiometricsUnlockClick)

            assertEquals(UnlockEvent.PromptForBiometrics(mockCipher), awaitItem())
        }
    }

    @Test
    fun `BiometricsUnlockClick with null cipher should update state`() = runTest {
        every { mockAuthRepository.getOrCreateCipher() } returns null
        every { mockAuthRepository.isAccountBiometricIntegrityValid() } returns false

        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            assertEquals(
                UnlockState(
                    isBiometricsEnabled = true,
                    isBiometricsValid = true,
                    showBiometricInvalidatedMessage = false,
                    dialog = null,
                ),
                awaitItem(),
            )

            viewModel.trySendAction(UnlockAction.BiometricsUnlockClick)

            assertEquals(
                UnlockState(
                    isBiometricsEnabled = true,
                    isBiometricsValid = false,
                    showBiometricInvalidatedMessage = true,
                    dialog = null,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `BiometricsUnlockSuccess should show Loading dialog`() = runTest {
        every { mockAuthRepository.getOrCreateCipher() } returns null
        coEvery {
            mockAuthRepository.unlockWithBiometrics(mockCipher)
        } returns BiometricsUnlockResult.Success

        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            assertEquals(
                UnlockState(
                    isBiometricsEnabled = true,
                    isBiometricsValid = true,
                    showBiometricInvalidatedMessage = false,
                    dialog = null,
                ),
                awaitItem(),
            )

            viewModel.trySendAction(UnlockAction.BiometricsUnlockSuccess(mockCipher))

            assertEquals(
                UnlockState(
                    isBiometricsEnabled = true,
                    isBiometricsValid = true,
                    showBiometricInvalidatedMessage = false,
                    dialog = UnlockState.Dialog.Loading,
                ),
                awaitItem(),
            )

            assertEquals(
                UnlockState(
                    isBiometricsEnabled = true,
                    isBiometricsValid = true,
                    showBiometricInvalidatedMessage = false,
                    dialog = null,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `BiometricsUnlockSuccess should call repository unlockWithBiometrics`() = runTest {
        every { mockAuthRepository.getOrCreateCipher() } returns null
        coEvery {
            mockAuthRepository.unlockWithBiometrics(mockCipher)
        } returns BiometricsUnlockResult.Success

        val viewModel = createViewModel()

        viewModel.trySendAction(UnlockAction.BiometricsUnlockSuccess(mockCipher))

        // Allow coroutine to complete
        testScheduler.advanceUntilIdle()

        coVerify { mockAuthRepository.unlockWithBiometrics(mockCipher) }
    }

    @Test
    fun `Success result should navigate to item listing`() = runTest {
        every { mockAuthRepository.getOrCreateCipher() } returns null
        coEvery {
            mockAuthRepository.unlockWithBiometrics(mockCipher)
        } returns BiometricsUnlockResult.Success

        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            expectNoEvents()

            viewModel.trySendAction(UnlockAction.BiometricsUnlockSuccess(mockCipher))

            assertEquals(UnlockEvent.NavigateToItemListing, awaitItem())
        }
    }

    @Test
    fun `BiometricDecodingError should clear biometrics`() = runTest {
        every { mockAuthRepository.getOrCreateCipher() } returns null
        every { mockAuthRepository.clearBiometrics() } just runs
        coEvery {
            mockAuthRepository.unlockWithBiometrics(mockCipher)
        } returns BiometricsUnlockResult.BiometricDecodingError(null)

        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            assertEquals(
                UnlockState(
                    isBiometricsEnabled = true,
                    isBiometricsValid = true,
                    showBiometricInvalidatedMessage = false,
                    dialog = null,
                ),
                awaitItem(),
            )

            viewModel.trySendAction(UnlockAction.BiometricsUnlockSuccess(mockCipher))

            assertEquals(
                UnlockState(
                    isBiometricsEnabled = true,
                    isBiometricsValid = true,
                    showBiometricInvalidatedMessage = false,
                    dialog = UnlockState.Dialog.Loading,
                ),
                awaitItem(),
            )

            assertEquals(
                UnlockState(
                    isBiometricsEnabled = true,
                    isBiometricsValid = false,
                    showBiometricInvalidatedMessage = false,
                    dialog = UnlockState.Dialog.Error(
                        title = BitwardenString.biometrics_failed.asText(),
                        message = BitwardenString.biometrics_decoding_failure.asText(),
                    ),
                ),
                awaitItem(),
            )
        }

        verify { mockAuthRepository.clearBiometrics() }
    }

    @Test
    fun `InvalidStateError should show error dialog`() = runTest {
        every { mockAuthRepository.getOrCreateCipher() } returns null
        val testError = IllegalStateException("Test error")
        coEvery {
            mockAuthRepository.unlockWithBiometrics(mockCipher)
        } returns BiometricsUnlockResult.InvalidStateError(testError)

        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            assertEquals(
                UnlockState(
                    isBiometricsEnabled = true,
                    isBiometricsValid = true,
                    showBiometricInvalidatedMessage = false,
                    dialog = null,
                ),
                awaitItem(),
            )

            viewModel.trySendAction(UnlockAction.BiometricsUnlockSuccess(mockCipher))

            assertEquals(
                UnlockState(
                    isBiometricsEnabled = true,
                    isBiometricsValid = true,
                    showBiometricInvalidatedMessage = false,
                    dialog = UnlockState.Dialog.Loading,
                ),
                awaitItem(),
            )

            assertEquals(
                UnlockState(
                    isBiometricsEnabled = true,
                    isBiometricsValid = true,
                    showBiometricInvalidatedMessage = false,
                    dialog = UnlockState.Dialog.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.generic_error_message.asText(),
                        throwable = testError,
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `BiometricsLockout should show error dialog`() = runTest {
        every { mockAuthRepository.getOrCreateCipher() } returns null

        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            assertEquals(
                UnlockState(
                    isBiometricsEnabled = true,
                    isBiometricsValid = true,
                    showBiometricInvalidatedMessage = false,
                    dialog = null,
                ),
                awaitItem(),
            )

            viewModel.trySendAction(UnlockAction.BiometricsLockout)

            assertEquals(
                UnlockState(
                    isBiometricsEnabled = true,
                    isBiometricsValid = true,
                    showBiometricInvalidatedMessage = false,
                    dialog = UnlockState.Dialog.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.too_many_failed_biometric_attempts.asText(),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `DismissDialog should clear dialog state`() = runTest {
        every { mockAuthRepository.getOrCreateCipher() } returns null

        val viewModel = createViewModel()

        // First set a dialog
        viewModel.trySendAction(UnlockAction.BiometricsLockout)

        viewModel.stateFlow.test {
            assertEquals(
                UnlockState(
                    isBiometricsEnabled = true,
                    isBiometricsValid = true,
                    showBiometricInvalidatedMessage = false,
                    dialog = UnlockState.Dialog.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.too_many_failed_biometric_attempts.asText(),
                    ),
                ),
                awaitItem(),
            )

            viewModel.trySendAction(UnlockAction.DismissDialog)

            assertEquals(
                UnlockState(
                    isBiometricsEnabled = true,
                    isBiometricsValid = true,
                    showBiometricInvalidatedMessage = false,
                    dialog = null,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ReceiveVaultUnlockResult with Success should navigate and clear dialog`() = runTest {
        every { mockAuthRepository.getOrCreateCipher() } returns null
        coEvery {
            mockAuthRepository.unlockWithBiometrics(mockCipher)
        } returns BiometricsUnlockResult.Success

        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            assertEquals(
                UnlockState(
                    isBiometricsEnabled = true,
                    isBiometricsValid = true,
                    showBiometricInvalidatedMessage = false,
                    dialog = null,
                ),
                awaitItem(),
            )

            viewModel.trySendAction(UnlockAction.BiometricsUnlockSuccess(mockCipher))

            assertEquals(
                UnlockState(
                    isBiometricsEnabled = true,
                    isBiometricsValid = true,
                    showBiometricInvalidatedMessage = false,
                    dialog = UnlockState.Dialog.Loading,
                ),
                awaitItem(),
            )

            assertEquals(
                UnlockState(
                    isBiometricsEnabled = true,
                    isBiometricsValid = true,
                    showBiometricInvalidatedMessage = false,
                    dialog = null,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ReceiveVaultUnlockResult with BiometricDecodingError should update state`() = runTest {
        every { mockAuthRepository.getOrCreateCipher() } returns null
        every { mockAuthRepository.clearBiometrics() } just runs

        val viewModel = createViewModel()

        viewModel.trySendAction(
            UnlockAction.Internal.ReceiveVaultUnlockResult(
                BiometricsUnlockResult.BiometricDecodingError(null),
            ),
        )

        assertEquals(
            UnlockState(
                isBiometricsEnabled = true,
                isBiometricsValid = false,
                showBiometricInvalidatedMessage = false,
                dialog = UnlockState.Dialog.Error(
                    title = BitwardenString.biometrics_failed.asText(),
                    message = BitwardenString.biometrics_decoding_failure.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `ReceiveVaultUnlockResult with InvalidStateError should show error`() = runTest {
        every { mockAuthRepository.getOrCreateCipher() } returns null

        val viewModel = createViewModel()
        val testError = RuntimeException("Invalid state")

        viewModel.trySendAction(
            UnlockAction.Internal.ReceiveVaultUnlockResult(
                BiometricsUnlockResult.InvalidStateError(testError),
            ),
        )

        assertEquals(
            UnlockState(
                isBiometricsEnabled = true,
                isBiometricsValid = true,
                showBiometricInvalidatedMessage = false,
                dialog = UnlockState.Dialog.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.generic_error_message.asText(),
                    throwable = testError,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    private fun createViewModel(
        initialState: UnlockState? = null,
    ): UnlockViewModel = UnlockViewModel(
        savedStateHandle = SavedStateHandle().apply { set("state", initialState) },
        authRepository = mockAuthRepository,
    )
}
