package com.x8bit.bitwarden.data.platform.manager.policy

import com.x8bit.bitwarden.data.platform.manager.policy.model.UserNotificationPolicyData
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the organization user notification policy, exposing the data required to display the
 * notification banner on the vault screen and tracking whether the active user has dismissed it.
 */
interface UserNotificationPolicyManager {
    /**
     * The data required to display the notification banner for the active user, or `null` if there
     * is no active policy or the active user has already dismissed the banner.
     */
    val displayData: UserNotificationPolicyData?

    /**
     * Emits updates that track [displayData].
     */
    val displayDataFlow: StateFlow<UserNotificationPolicyData?>

    /**
     * Marks the notification banner as dismissed for the active user, preventing it from being
     * displayed again.
     */
    fun dismissBanner()

    /**
     * Indicates whether the dismissed state of the banner should be cleared when the user
     * is soft-logged out. When `true` the banner is displayed again after the next login.
     */
    fun shouldClearOnSoftLogout(userId: String): Boolean
}
