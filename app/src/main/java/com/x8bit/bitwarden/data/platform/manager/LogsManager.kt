package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.repository.model.Environment

/**
 * Implementations of this interface provide a way to enable or disable the collection of crash
 * logs, giving control over whether crash logs are generated and stored.
 */
interface LogsManager {
    /**
     * Gets or sets whether the collection of crash logs is enabled.
     */
    var isEnabled: Boolean

    /**
     * Tracks a [Throwable] if logs are enabled.
     */
    fun trackNonFatalException(throwable: Throwable)

    /**
     * Tracks the current user data.
     */
    fun setUserData(userId: String?, environmentType: Environment.Type)
}
