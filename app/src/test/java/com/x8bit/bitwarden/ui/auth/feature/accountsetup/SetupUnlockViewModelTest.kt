package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import javax.crypto.Cipher

class SetupUnlockViewModelTest : BaseViewModelTest() {

    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val authRepository: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
    }
    private val settingsRepository = mockk<SettingsRepository> {
        every { isUnlockWithPinEnabled } returns false
        every { isUnlockWithBiometricsEnabled } returns false
    }
    private val biometricsEncryptionManager: BiometricsEncryptionManager = mockk {
        every { getOrCreateCipher(userId = DEFAULT_USER_ID) } returns CIPHER
        every {
            isBiometricIntegrityValid(userId = DEFAULT_USER_ID, cipher = CIPHER)
        } returns false
    }

    @Test
    fun `initial state should be correct`() {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `ContinueClick should emit NavigateToSetupAutofill`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SetupUnlockAction.ContinueClick)
            assertEquals(SetupUnlockEvent.NavigateToSetupAutofill, awaitItem())
        }
    }

    @Test
    fun `SetUpLaterClick should emit NavigateToSetupAutofill`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SetupUnlockAction.SetUpLaterClick)
            assertEquals(SetupUnlockEvent.NavigateToSetupAutofill, awaitItem())
        }
    }

    private fun createViewModel(
        state: SetupUnlockState? = null,
    ): SetupUnlockViewModel =
        SetupUnlockViewModel(
            savedStateHandle = SavedStateHandle(mapOf("state" to state)),
            authRepository = authRepository,
            settingsRepository = settingsRepository,
            biometricsEncryptionManager = biometricsEncryptionManager,
        )
}

private val DEFAULT_STATE: SetupUnlockState = SetupUnlockState(
    isUnlockWithPinEnabled = false,
    isUnlockWithPasswordEnabled = true,
    isUnlockWithBiometricsEnabled = false,
)

private val CIPHER = mockk<Cipher>()
private const val DEFAULT_USER_ID: String = "activeUserId"
private val DEFAULT_USER_STATE: UserState = UserState(
    activeUserId = DEFAULT_USER_ID,
    accounts = listOf(
        UserState.Account(
            userId = DEFAULT_USER_ID,
            name = "Active User",
            email = "active@bitwarden.com",
            avatarColorHex = "#aa00aa",
            environment = Environment.Us,
            isPremium = true,
            isLoggedIn = true,
            isVaultUnlocked = true,
            needsPasswordReset = false,
            isBiometricsEnabled = false,
            organizations = emptyList(),
            needsMasterPassword = false,
            trustedDevice = null,
        ),
    ),
)
