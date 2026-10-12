package com.x8bit.bitwarden.data.platform.manager.policy.model

/**
 * Models all the relevant information required to display a Notification from an organization
 * policy.
 */
data class UserNotificationPolicyData(
    val organizationId: String,
    val headerText: String?,
    val descriptionText: String,
    val buttonText: String?,
)
