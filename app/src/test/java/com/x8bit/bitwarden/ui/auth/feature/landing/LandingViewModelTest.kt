package com.x8bit.bitwarden.ui.auth.feature.landing

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toAccountSummaries
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toAccountSummary
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

class LandingViewModelTest : BaseViewModelTest() {

    private val authRepository: AuthRepository = mockk(relaxed = true) {
        every { logout(any()) } just runs
    }
    private val vaultRepository: VaultRepository = mockk(relaxed = true) {
        every { lockVault(any()) } just runs
    }
    private val fakeEnvironmentRepository = FakeEnvironmentRepository()

    private val featureFlagManager: FeatureFlagManager = mockk(relaxed = true) {
        every { getFeatureFlag(FlagKey.EmailVerification) } returns false
    }

    @Test
    fun `initial state should be correct when there is no remembered email`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    @Test
    fun `initial state should be correct when there is a remembered email`() = runTest {
        val rememberedEmail = "remembered@gmail.com"
        val viewModel = createViewModel(rememberedEmail = rememberedEmail)
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(
                    emailInput = rememberedEmail,
                    isContinueButtonEnabled = true,
                    isRememberMeEnabled = true,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `initial state should set the account summaries based on the UserState`() {
        val userState = UserState(
            activeUserId = "activeUserId",
            accounts = listOf(
                UserState.Account(
                    userId = "activeUserId",
                    name = "name",
                    email = "email",
                    avatarColorHex = "avatarColorHex",
                    environment = Environment.Us,
                    isPremium = true,
                    isLoggedIn = true,
                    isVaultUnlocked = true,
                    needsPasswordReset = false,
                    isBiometricsEnabled = false,
                    organizations = emptyList(),
                    needsMasterPassword = false,
                    trustedDevice = null,
                    hasMasterPassword = true,
                    isUsingKeyConnector = false,
                    onboardingStatus = OnboardingStatus.COMPLETE,
                    firstTimeState = FirstTimeState(showImportLoginsCard = true),
                ),
            ),
        )
        val viewModel = createViewModel(userState = userState)
        assertEquals(
            DEFAULT_STATE.copy(
                accountSummaries = userState.toAccountSummaries(),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `initial state should pull from saved state handle when present`() = runTest {
        val expectedState = DEFAULT_STATE.copy(
            emailInput = "test",
            isContinueButtonEnabled = false,
            isRememberMeEnabled = true,
        )
        val handle = SavedStateHandle(mapOf("state" to expectedState))
        val viewModel = createViewModel(savedStateHandle = handle)
        viewModel.stateFlow.test {
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `LockAccountClick should call lockVault for the given account`() {
        val accountUserId = "userId"
        val accountSummary = mockk<AccountSummary> {
            every { userId } returns accountUserId
        }
        val viewModel = createViewModel()

        viewModel.trySendAction(LandingAction.LockAccountClick(accountSummary))

        verify { vaultRepository.lockVault(userId = accountUserId) }
    }

    @Test
    fun `LogoutAccountClick should call logout for the given account`() {
        val accountUserId = "userId"
        val accountSummary = mockk<AccountSummary> {
            every { userId } returns accountUserId
        }
        val viewModel = createViewModel()

        viewModel.trySendAction(LandingAction.LogoutAccountClick(accountSummary))

        verify { authRepository.logout(userId = accountUserId) }
    }

    @Test
    fun `SwitchAccountClick should call switchAccount for the given account`() {
        val matchingAccountUserId = "matchingAccountUserId"
        val accountSummary = mockk<AccountSummary> {
            every { userId } returns matchingAccountUserId
        }
        val viewModel = createViewModel()

        viewModel.trySendAction(LandingAction.SwitchAccountClick(accountSummary))

        verify { authRepository.switchAccount(userId = matchingAccountUserId) }
    }

    @Test
    fun `ConfirmSwitchToMatchingAccountClick should call switchAccount for the given account`() {
        val matchingAccountUserId = "matchingAccountUserId"
        val accountSummary = mockk<AccountSummary> {
            every { userId } returns matchingAccountUserId
        }
        val viewModel = createViewModel()

        viewModel.trySendAction(LandingAction.ConfirmSwitchToMatchingAccountClick(accountSummary))

        verify { authRepository.switchAccount(userId = matchingAccountUserId) }
    }

    @Test
    fun `ContinueButtonClick with valid email should emit NavigateToLogin`() = runTest {
        val validEmail = "email@bitwarden.com"
        val viewModel = createViewModel()
        viewModel.trySendAction(LandingAction.EmailInputChanged(validEmail))
        viewModel.eventFlow.test {
            viewModel.trySendAction(LandingAction.ContinueButtonClick)
            assertEquals(
                LandingEvent.NavigateToLogin(validEmail),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ContinueButtonClick with invalid email should display an error dialog`() = runTest {
        val invalidEmail = "bitwarden.com"
        val viewModel = createViewModel()
        viewModel.trySendAction(LandingAction.EmailInputChanged(invalidEmail))
        val initialState = DEFAULT_STATE.copy(
            emailInput = invalidEmail,
            isContinueButtonEnabled = true,
        )
        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())

            viewModel.trySendAction(LandingAction.ContinueButtonClick)
            assertEquals(
                initialState.copy(
                    dialog = LandingState.DialogState.Error(
                        message = R.string.invalid_email.asText(),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ContinueButtonClick with an email input matching an existing account on same environment that is logged in should show the account already added dialog`() {
        val rememberedEmail = "active@bitwarden.com"
        val activeAccount = UserState.Account(
            userId = "activeUserId",
            name = "name",
            email = rememberedEmail,
            avatarColorHex = "avatarColorHex",
            environment = Environment.Us,
            isPremium = true,
            isLoggedIn = true,
            isVaultUnlocked = true,
            needsPasswordReset = false,
            isBiometricsEnabled = false,
            organizations = emptyList(),
            needsMasterPassword = false,
            trustedDevice = null,
            hasMasterPassword = true,
            isUsingKeyConnector = false,
            onboardingStatus = OnboardingStatus.COMPLETE,
            firstTimeState = FirstTimeState(showImportLoginsCard = true),
        )
        val userState = UserState(
            activeUserId = "activeUserId",
            accounts = listOf(activeAccount),
        )
        val viewModel = createViewModel(
            rememberedEmail = rememberedEmail,
            userState = userState,
        )
        val activeAccountSummary = activeAccount.toAccountSummary(isActive = true)
        val accountSummaries = userState.toAccountSummaries()
        val initialState = DEFAULT_STATE.copy(
            emailInput = rememberedEmail,
            isContinueButtonEnabled = true,
            isRememberMeEnabled = true,
            accountSummaries = accountSummaries,
        )
        assertEquals(
            initialState,
            viewModel.stateFlow.value,
        )

        viewModel.trySendAction(LandingAction.ContinueButtonClick)

        assertEquals(
            initialState.copy(
                dialog = LandingState.DialogState.AccountAlreadyAdded(
                    accountSummary = activeAccountSummary,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ContinueButtonClick with an email input matching an existing account on different environment that is logged in should emit NavigateToLogin`() =
        runTest {
            val rememberedEmail = "active@bitwarden.com"
            val activeAccount = UserState.Account(
                userId = "activeUserId",
                name = "name",
                email = rememberedEmail,
                avatarColorHex = "avatarColorHex",
                environment = Environment.Us,
                isPremium = true,
                isLoggedIn = true,
                isVaultUnlocked = true,
                needsPasswordReset = false,
                isBiometricsEnabled = false,
                organizations = emptyList(),
                needsMasterPassword = false,
                trustedDevice = null,
                hasMasterPassword = true,
                isUsingKeyConnector = false,
                onboardingStatus = OnboardingStatus.COMPLETE,
                firstTimeState = FirstTimeState(showImportLoginsCard = true),
            )
            val userState = UserState(
                activeUserId = "activeUserId",
                accounts = listOf(activeAccount),
            )
            val viewModel = createViewModel(
                rememberedEmail = rememberedEmail,
                userState = userState,
            )
            val accountSummaries = userState.toAccountSummaries()
            val initialState = DEFAULT_STATE.copy(
                emailInput = rememberedEmail,
                isContinueButtonEnabled = true,
                isRememberMeEnabled = true,
                accountSummaries = accountSummaries,
            )
            assertEquals(
                initialState,
                viewModel.stateFlow.value,
            )

            viewModel.eventFlow.test {
                viewModel.trySendAction(LandingAction.EnvironmentTypeSelect(Environment.Eu.type))
                assertEquals(
                    initialState.copy(
                        selectedEnvironmentLabel = Environment.Eu.label,
                        selectedEnvironmentType = Environment.Eu.type,
                    ),
                    viewModel.stateFlow.value,
                )
                viewModel.trySendAction(LandingAction.ContinueButtonClick)
                assertEquals(
                    LandingEvent.NavigateToLogin(rememberedEmail),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `ContinueButtonClick with an email input matching an existing account that is logged out should emit NavigateToLogin`() =
        runTest {
            val rememberedEmail = "active@bitwarden.com"
            val activeAccount = UserState.Account(
                userId = "activeUserId",
                name = "name",
                email = rememberedEmail,
                avatarColorHex = "avatarColorHex",
                environment = Environment.Us,
                isPremium = true,
                isLoggedIn = false,
                isVaultUnlocked = true,
                needsPasswordReset = false,
                isBiometricsEnabled = false,
                organizations = emptyList(),
                needsMasterPassword = false,
                trustedDevice = null,
                hasMasterPassword = true,
                isUsingKeyConnector = false,
                onboardingStatus = OnboardingStatus.COMPLETE,
                firstTimeState = FirstTimeState(showImportLoginsCard = true),
            )
            val userState = UserState(
                activeUserId = "activeUserId",
                accounts = listOf(activeAccount),
            )
            val viewModel = createViewModel(
                rememberedEmail = rememberedEmail,
                userState = userState,
            )
            val accountSummaries = userState.toAccountSummaries()
            val initialState = DEFAULT_STATE.copy(
                emailInput = rememberedEmail,
                isContinueButtonEnabled = true,
                isRememberMeEnabled = true,
                accountSummaries = accountSummaries,
            )
            assertEquals(
                initialState,
                viewModel.stateFlow.value,
            )

            viewModel.eventFlow.test {
                viewModel.trySendAction(LandingAction.ContinueButtonClick)
                assertEquals(
                    LandingEvent.NavigateToLogin(rememberedEmail),
                    awaitItem(),
                )
                assertEquals(
                    initialState,
                    viewModel.stateFlow.value,
                )
            }
        }

    @Test
    fun `CreateAccountClick should emit NavigateToCreateAccount`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(LandingAction.CreateAccountClick)
            assertEquals(
                LandingEvent.NavigateToCreateAccount,
                awaitItem(),
            )
        }
    }

    @Test
    fun `When feature is enabled CreateAccountClick should emit NavigateToStartRegistration`() =
        runTest {
            every { featureFlagManager.getFeatureFlag(FlagKey.EmailVerification) } returns true
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(LandingAction.CreateAccountClick)
                assertEquals(
                    LandingEvent.NavigateToStartRegistration,
                    awaitItem(),
                )
            }
        }

    @Test
    fun `DialogDismiss should clear the active dialog`() {
        val initialState = DEFAULT_STATE.copy(
            dialog = LandingState.DialogState.Error(
                message = "Error".asText(),
            ),
        )
        val viewModel = createViewModel(initialState = initialState)
        assertEquals(
            initialState,
            viewModel.stateFlow.value,
        )

        viewModel.trySendAction(LandingAction.DialogDismiss)

        assertEquals(
            initialState.copy(dialog = null),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `RememberMeToggle should update value of isRememberMeToggled`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(LandingAction.RememberMeToggle(true))
            assertEquals(
                viewModel.stateFlow.value,
                DEFAULT_STATE.copy(isRememberMeEnabled = true),
            )
        }
    }

    @Test
    fun `EmailInputUpdated should update value of email input and continue button state`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                // Ignore initial state
                awaitItem()

                val nonEmptyInput = "input"
                viewModel.trySendAction(LandingAction.EmailInputChanged(nonEmptyInput))
                assertEquals(
                    DEFAULT_STATE.copy(
                        emailInput = nonEmptyInput,
                        isContinueButtonEnabled = true,
                    ),
                    awaitItem(),
                )

                val emptyInput = ""
                viewModel.trySendAction(LandingAction.EmailInputChanged(emptyInput))
                assertEquals(
                    DEFAULT_STATE.copy(
                        emailInput = emptyInput,
                        isContinueButtonEnabled = false,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `EnvironmentTypeSelect should update value of selected region for US or EU`() = runTest {
        val inputEnvironmentType = Environment.Type.EU
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            awaitItem()
            viewModel.trySendAction(LandingAction.EnvironmentTypeSelect(inputEnvironmentType))
            assertEquals(
                DEFAULT_STATE.copy(
                    selectedEnvironmentType = Environment.Type.EU,
                    selectedEnvironmentLabel = Environment.Eu.label,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `EnvironmentTypeSelect should emit NavigateToEnvironment for self-hosted`() = runTest {
        val inputEnvironmentType = Environment.Type.SELF_HOSTED
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(LandingAction.EnvironmentTypeSelect(inputEnvironmentType))
            assertEquals(
                LandingEvent.NavigateToEnvironment,
                awaitItem(),
            )
        }
    }

    @Test
    fun `Active Logged Out user causes email field to prepopulate`() = runTest {
        val expectedEmail = "frodo@hobbit.on"
        val userId = "1"

        val userAccount: UserState.Account = UserState.Account(
            userId = userId,
            name = null,
            email = expectedEmail,
            avatarColorHex = "lorem",
            environment = Environment.Us,
            isPremium = false,
            isLoggedIn = false,
            isVaultUnlocked = false,
            needsPasswordReset = false,
            needsMasterPassword = false,
            trustedDevice = null,
            organizations = listOf(),
            isBiometricsEnabled = false,
            vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
            hasMasterPassword = true,
            isUsingKeyConnector = false,
            onboardingStatus = OnboardingStatus.COMPLETE,
            firstTimeState = FirstTimeState(showImportLoginsCard = true),
        )

        val userState = UserState(
            activeUserId = userId,
            accounts = listOf(userAccount),
        )

        val viewModel = createViewModel(userState = userState)

        assertEquals(expectedEmail, viewModel.stateFlow.value.emailInput)
    }

    @Test
    fun `Email input will not change based on active user when adding new account`() = runTest {
        val expectedEmail = "frodo@hobbit.on"
        val userId = "1"

        val userAccount: UserState.Account = UserState.Account(
            userId = userId,
            name = null,
            email = expectedEmail,
            avatarColorHex = "lorem",
            environment = Environment.Us,
            isPremium = false,
            isLoggedIn = false,
            isVaultUnlocked = false,
            needsPasswordReset = false,
            needsMasterPassword = false,
            trustedDevice = null,
            organizations = listOf(),
            isBiometricsEnabled = false,
            vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
            hasMasterPassword = true,
            isUsingKeyConnector = false,
            onboardingStatus = OnboardingStatus.COMPLETE,
            firstTimeState = FirstTimeState(showImportLoginsCard = true),
        )

        val userState = UserState(
            activeUserId = userId,
            accounts = listOf(userAccount),
        )

        every { authRepository.hasPendingAccountAddition } returns true

        val viewModel = createViewModel(userState = userState)

        assertTrue(viewModel.stateFlow.value.emailInput.isEmpty())
    }

    //region Helper methods

    private fun createViewModel(
        initialState: LandingState? = null,
        rememberedEmail: String? = null,
        userState: UserState? = null,
        savedStateHandle: SavedStateHandle = SavedStateHandle(
            initialState = mapOf("state" to initialState),
        ),
    ): LandingViewModel = LandingViewModel(
        authRepository = authRepository.apply {
            every { rememberedEmailAddress } returns rememberedEmail
            every { userStateFlow } returns MutableStateFlow(userState)
        },
        vaultRepository = vaultRepository,
        environmentRepository = fakeEnvironmentRepository,
        featureFlagManager = featureFlagManager,
        savedStateHandle = savedStateHandle,
    )

    //endregion Helper methods

    companion object {
        private val DEFAULT_STATE = LandingState(
            emailInput = "",
            isContinueButtonEnabled = false,
            isRememberMeEnabled = false,
            selectedEnvironmentType = Environment.Type.US,
            selectedEnvironmentLabel = Environment.Us.label,
            dialog = null,
            accountSummaries = emptyList(),
        )
    }
}
