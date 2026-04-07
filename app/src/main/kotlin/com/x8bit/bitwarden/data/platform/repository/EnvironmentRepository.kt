package com.x8bit.bitwarden.data.platform.repository

import com.bitwarden.data.repository.model.Environment
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides an API for observing and modifying environment state.
 */
interface EnvironmentRepository {
    /**
     * The currently set environment.
     */
    var environment: Environment

    /**
     * Emits updates that track [environment].
     */
    val environmentStateFlow: StateFlow<Environment>

    /**
     * Initializes the [EnvironmentRepository].
     */
    fun initialize()

    /**
     * Stores the current environment for the given [userEmail].
     */
    fun saveCurrentEnvironmentForEmail(userEmail: String)

    /**
     * Loads the environment for the given [userEmail].
     * returns boolean indicates if the load was successful
     */
    fun loadEnvironmentForEmail(userEmail: String): Boolean
}
