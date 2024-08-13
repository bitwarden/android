package com.x8bit.bitwarden.data.platform.repository.util

import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A faked implementation of [EnvironmentRepository] based on in-memory caching.
 */
class FakeEnvironmentRepository : EnvironmentRepository {
    override var environment: Environment
        get() = mutableEnvironmentStateFlow.value
        set(value) {
            mutableEnvironmentStateFlow.value = value
        }
    override val environmentStateFlow: StateFlow<Environment>
        get() = mutableEnvironmentStateFlow.asStateFlow()

    override fun saveCurrentEnvironmentForEmail(userEmail: String) = Unit

    override fun loadEnvironmentForEmail(userEmail: String): Boolean = true

    private val mutableEnvironmentStateFlow = MutableStateFlow<Environment>(Environment.Us)
}
