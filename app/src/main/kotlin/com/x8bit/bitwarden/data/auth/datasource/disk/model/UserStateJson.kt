package com.x8bit.bitwarden.data.auth.datasource.disk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the overall "user state" of the current active user as well as any users that may be
 * switched to.
 *
 * @property activeUserId The ID of the current active user.
 * @property accounts A mapping between user IDs and the [AccountJson] information associated with
 * that user.
 */
@Serializable
data class UserStateJson(
    @SerialName("activeUserId")
    val activeUserId: String,

    @SerialName("accounts")
    val accounts: Map<String, AccountJson>,
) {
    init {
        requireNotNull(accounts[activeUserId])
    }

    /**
     * The current active account.
     */
    @Suppress("UnsafeCallOnNullableType")
    val activeAccount: AccountJson
        get() = accounts[activeUserId]!!
}
