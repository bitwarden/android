package com.x8bit.bitwarden.data.platform.manager.policy

import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.policies.PolicyType
import com.bitwarden.policies.PolicyView
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.auth.repository.util.activeUserIdChangesFlow
import com.x8bit.bitwarden.data.auth.repository.util.policyInformation
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.policy.model.UserNotificationPolicyData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * The default implementation of the [UserNotificationPolicyManager].
 */
internal class UserNotificationPolicyManagerImpl(
    private val authDiskSource: AuthDiskSource,
    private val settingsDiskSource: SettingsDiskSource,
    private val policyManager: PolicyManager,
    dispatcherManager: DispatcherManager,
) : UserNotificationPolicyManager {
    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)
    private val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    override val displayData: UserNotificationPolicyData?
        get() = activeUserId?.let { userId ->
            policyManager
                .getActivePolicies(type = PolicyType.ORGANIZATION_USER_NOTIFICATION)
                .firstOrNull()
                ?.let { policy ->
                    val revisionDate = settingsDiskSource
                        .getVaultPolicyBannerDismissedDate(userId = userId)
                    policy.userNotificationPolicyData?.takeIf {
                        policy.revisionDate?.toEpochMilli() != revisionDate?.toEpochMilli() ||
                            revisionDate == null
                    }
                }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val displayDataFlow: StateFlow<UserNotificationPolicyData?>
        get() = authDiskSource
            .activeUserIdChangesFlow
            .flatMapLatest { userId ->
                userId ?: return@flatMapLatest flowOf(null)
                combine(
                    settingsDiskSource.getVaultPolicyBannerDismissedDateFlow(userId = userId),
                    policyManager
                        .getActivePoliciesFlow(type = PolicyType.ORGANIZATION_USER_NOTIFICATION)
                        .map { it.firstOrNull() },
                ) { revisionDate, policy ->
                    (policy?.userNotificationPolicyData)?.takeIf {
                        policy.revisionDate?.toEpochMilli() != revisionDate?.toEpochMilli() ||
                            revisionDate == null
                    }
                }
            }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Lazily,
                initialValue = displayData,
            )

    override fun dismissBanner() {
        policyManager
            .getActivePolicies(type = PolicyType.ORGANIZATION_USER_NOTIFICATION)
            .firstOrNull()
            ?.let { dismissBanner(policy = it) }
    }

    override fun shouldClearOnSoftLogout(
        userId: String,
    ): Boolean =
        policyManager
            .getUserPolicies(userId = userId, type = PolicyType.ORGANIZATION_USER_NOTIFICATION)
            .firstNotNullOfOrNull {
                it.policyInformation as? PolicyInformation.OrganizationUserNotification
            }
            ?.showAfterEveryLogin != false

    private fun dismissBanner(policy: PolicyView) {
        activeUserId?.let { userId ->
            settingsDiskSource.storeVaultPolicyBannerDismissedDate(
                userId = userId,
                dismissalRevisionDate = policy.revisionDate,
            )
        }
    }
}

private val PolicyView.userNotificationPolicyData: UserNotificationPolicyData?
    get() = (this.policyInformation as? PolicyInformation.OrganizationUserNotification)?.let {
        UserNotificationPolicyData(
            organizationId = this.organizationId,
            headerText = it.headerText,
            descriptionText = it.descriptionText,
            buttonText = it.buttonText,
        )
    }
