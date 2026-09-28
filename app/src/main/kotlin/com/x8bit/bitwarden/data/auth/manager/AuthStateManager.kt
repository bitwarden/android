package com.x8bit.bitwarden.data.auth.manager

import com.x8bit.bitwarden.data.auth.repository.model.AuthState
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the authentication state.
 */
interface AuthStateManager {
    /**
     * Models the current auth state.
     */
    val authStateFlow: StateFlow<AuthState>
}
