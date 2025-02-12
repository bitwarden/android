package com.bitwarden.authenticator.data.platform.manager

/**
 * Implementations of this interface provide a way to enable or disable the collection of crash
 * logs, giving control over whether crash logs are generated and stored.
 */
interface CrashLogsManager {
    /**
     * Gets or sets whether the collection of crash logs is enabled.
     */
    var isEnabled: Boolean
}
