package com.x8bit.bitwarden.ui.platform.feature.rootnav

import androidx.lifecycle.SavedStateHandle
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthState
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthState.Authenticated
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthState.Unauthenticated
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RootNavViewModelTest : BaseViewModelTest() {

    @Test
    fun `initial state should be the state in savedStateHandle`() {
        val authRepository = mockk<AuthRepository> {
            every { this@mockk.authStateFlow } returns MutableStateFlow(mockk<Authenticated>())
        }
        val handle = SavedStateHandle(mapOf(("nav_state" to RootNavState.VaultUnlocked)))
        val viewModel = RootNavViewModel(
            authRepository = authRepository,
            savedStateHandle = handle,
        )
        assertEquals(RootNavState.VaultUnlocked, viewModel.stateFlow.value)
    }

    @Test
    fun `when auth state is Uninitialized nav state should be Splash`() {
        val viewModel = RootNavViewModel(
            authRepository = mockk {
                every { this@mockk.authStateFlow } returns MutableStateFlow(AuthState.Uninitialized)
            },
            savedStateHandle = SavedStateHandle(),
        )
        assertEquals(RootNavState.Splash, viewModel.stateFlow.value)
    }

    @Test
    fun `when auth state is Authenticated nav state should be VaultUnlocked`() {
        val authRepository = mockk<AuthRepository> {
            every { this@mockk.authStateFlow } returns MutableStateFlow(mockk<Authenticated>())
        }
        val viewModel = RootNavViewModel(
            authRepository = authRepository,
            savedStateHandle = SavedStateHandle(),
        )
        assertEquals(RootNavState.VaultUnlocked, viewModel.stateFlow.value)
    }

    @Test
    fun `when auth state is Unauthenticated nav state should be Auth`() = runTest {
        val viewModel = RootNavViewModel(
            authRepository = mockk {
                every { this@mockk.authStateFlow } returns MutableStateFlow(Unauthenticated)
            },
            savedStateHandle = SavedStateHandle(),
        )
        assertEquals(RootNavState.Auth, viewModel.stateFlow.value)
    }
}
