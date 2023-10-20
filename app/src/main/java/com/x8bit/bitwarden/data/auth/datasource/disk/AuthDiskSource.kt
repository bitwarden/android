package com.x8bit.bitwarden.data.auth.datasource.disk

import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import kotlinx.coroutines.flow.Flow

/**
 * Primary access point for disk information.
 */
interface AuthDiskSource {
    /**
     * The currently persisted saved email address (or `null` if not set).
     */
    var rememberedEmailAddress: String?

    /**
     * The currently persisted user state information (or `null` if not set).
     */
    var userState: UserStateJson?

    /**
     * Emits updates that track [userState]. This will replay the last known value, if any.
     */
    val userStateFlow: Flow<UserStateJson?>
}
