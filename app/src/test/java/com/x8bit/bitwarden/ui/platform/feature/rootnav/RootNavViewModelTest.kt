package com.x8bit.bitwarden.ui.platform.feature.rootnav

import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RootNavViewModelTest : BaseViewModelTest() {
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(null)
    private val authRepository = mockk<AuthRepository>() {
        every { userStateFlow } returns mutableUserStateFlow
    }

    @Test
    fun `when there are no accounts the nav state should be Auth`() {
        mutableUserStateFlow.tryEmit(null)
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
                        isVaultUnlocked = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(RootNavState.VaultUnlocked, viewModel.stateFlow.value)
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
                        isVaultUnlocked = false,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(RootNavState.VaultLocked, viewModel.stateFlow.value)
    }

    private fun createViewModel(): RootNavViewModel =
        RootNavViewModel(
            authRepository = authRepository,
        )
}
