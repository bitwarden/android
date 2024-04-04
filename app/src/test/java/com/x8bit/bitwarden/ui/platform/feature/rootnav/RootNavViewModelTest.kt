package com.x8bit.bitwarden.ui.platform.feature.rootnav

import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManagerImpl
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RootNavViewModelTest : BaseViewModelTest() {
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(null)
    private val authRepository = mockk<AuthRepository>() {
        every { userStateFlow } returns mutableUserStateFlow
        every { updateLastActiveTime() } just runs
    }
    private val specialCircumstanceManager = SpecialCircumstanceManagerImpl()

    @Test
    fun `when there are no accounts the nav state should be Auth`() {
        mutableUserStateFlow.tryEmit(null)
        val viewModel = createViewModel()
        assertEquals(RootNavState.Auth, viewModel.stateFlow.value)
    }

    @Test
    fun `when the active user is not logged in the nav state should be Auth`() {
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
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
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(RootNavState.Auth, viewModel.stateFlow.value)
    }

    @Test
    fun `when the active user needs a password reset the nav state should be ResetPassword`() {
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = false,
                        isVaultUnlocked = false,
                        needsPasswordReset = true,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(RootNavState.ResetPassword, viewModel.stateFlow.value)
    }

    @Test
    fun `when the active user needs a master password the nav state should be SetPassword`() {
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = false,
                        isVaultUnlocked = false,
                        needsPasswordReset = true,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = true,
                        trustedDevice = null,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.SetPassword,
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user but there are pending account additions the nav state should be Auth`() {
        mutableUserStateFlow.tryEmit(
            UserState(
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
                    ),
                ),
                hasPendingAccountAddition = true,
            ),
        )
        val viewModel = createViewModel()
        assertEquals(RootNavState.Auth, viewModel.stateFlow.value)
    }

    @Test
    fun `when the active user has an unlocked vault the nav state should be VaultUnlocked`() {
        mutableUserStateFlow.tryEmit(
            UserState(
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
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.VaultUnlocked(activeUserId = "activeUserId"),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault but the is a ShareNewSend special circumstance the nav state should be VaultUnlockedForNewSend`() {
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.ShareNewSend(
                data = mockk(),
                shouldFinishWhenComplete = true,
            )
        mutableUserStateFlow.tryEmit(
            UserState(
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
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.VaultUnlockedForNewSend,
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault but there is an AutofillSave special circumstance the nav state should be VaultUnlockedForAutofillSave`() {
        val autofillSaveItem: AutofillSaveItem = mockk()
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.AutofillSave(
                autofillSaveItem = autofillSaveItem,
            )
        mutableUserStateFlow.tryEmit(
            UserState(
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
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.VaultUnlockedForAutofillSave(
                autofillSaveItem = autofillSaveItem,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault but there is an AutofillSelection special circumstance the nav state should be VaultUnlockedForAutofillSelection`() {
        val autofillSelectionData = AutofillSelectionData(
            type = AutofillSelectionData.Type.LOGIN,
            uri = "uri",
        )
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.AutofillSelection(
                autofillSelectionData = autofillSelectionData,
                shouldFinishWhenComplete = true,
            )
        mutableUserStateFlow.tryEmit(
            UserState(
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
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.VaultUnlockedForAutofillSelection(
                activeUserId = "activeUserId",
                type = AutofillSelectionData.Type.LOGIN,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `when the active user has a locked vault the nav state should be VaultLocked`() {
        mutableUserStateFlow.tryEmit(
            UserState(
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
                        isVaultUnlocked = false,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(RootNavState.VaultLocked, viewModel.stateFlow.value)
    }

    @Test
    fun `BackStackUpdate should call updateLastActiveTime`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(RootNavAction.BackStackUpdate)
        verify { authRepository.updateLastActiveTime() }
    }

    private fun createViewModel(): RootNavViewModel =
        RootNavViewModel(
            authRepository = authRepository,
            specialCircumstanceManager = specialCircumstanceManager,
        )
}
