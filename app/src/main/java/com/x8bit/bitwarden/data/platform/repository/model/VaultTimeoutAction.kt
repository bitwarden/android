package com.x8bit.bitwarden.data.platform.repository.model

/**
 * Represents different type of actions that may be performed when a vault times out.
 *
 * The [value] is used for consistent storage purposes.
 */
enum class VaultTimeoutAction(
    val value: String,
) {
    /**
     * The vault should lock when it times out.
     */
    LOCK("0"),

    /**
     * The user should be logged out when their vault times out.
     */
    LOGOUT("1"),
}
