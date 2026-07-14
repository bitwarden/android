package com.x8bit.bitwarden.data.platform.manager

import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.organizations.OrganizationUserStatusType
import com.bitwarden.organizations.OrganizationUserType
import com.bitwarden.policies.OrganizationUserPolicyContext
import com.bitwarden.policies.PolicyType
import com.bitwarden.policies.PolicyView
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.repository.util.activeUserIdChangesFlow
import com.x8bit.bitwarden.data.vault.repository.util.toSdkOrganizationPolicyContext
import com.x8bit.bitwarden.data.vault.repository.util.toSdkPolicyViews
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * The default [PolicyManager] implementation. This class is responsible for
 * loading policies for the current user and filtering them as needed.
 */
class PolicyManagerImpl(
    private val authDiskSource: AuthDiskSource,
    private val authSdkSource: AuthSdkSource,
    private val featureFlagManager: FeatureFlagManager,
) : PolicyManager {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getActivePoliciesFlow(type: PolicyType): Flow<List<PolicyView>> =
        authDiskSource
            .activeUserIdChangesFlow
            .flatMapLatest { activeUserId ->
                activeUserId
                    ?.let { userId -> getAppliedPolicyViewsFlow(userId = userId, type = type) }
                    ?: emptyFlow()
            }
            .distinctUntilChanged()

    override fun getActivePolicies(type: PolicyType): List<PolicyView> =
        authDiskSource
            .userState
            ?.activeUserId
            ?.let { userId -> getUserPolicies(userId = userId, type = type) }
            .orEmpty()

    override fun getUserPolicies(
        userId: String,
        type: PolicyType,
    ): List<PolicyView> =
        this
            .filterPolicies(
                type = type,
                policies = authDiskSource
                    .getPolicies(userId = userId)
                    ?.toSdkPolicyViews(),
                organizations = authDiskSource
                    .getOrganizations(userId = userId)
                    ?.map {
                        OrganizationPolicyData(
                            organizationUserPolicyContext = it.toSdkOrganizationPolicyContext(),
                            organizationShouldUsePolicies = it.permissions.shouldManagePolicies,
                        )
                    },
                isPoliciesInAcceptedStateEnabled = featureFlagManager
                    .getFeatureFlag(key = FlagKey.PoliciesInAcceptedState),
            )
            .orEmpty()

    override fun getPersonalOwnershipPolicyOrganizationId(): String? =
        this
            .getActivePolicies(type = PolicyType.ORGANIZATION_DATA_OWNERSHIP)
            .sortedBy { it.revisionDate }
            .firstOrNull()
            ?.organizationId

    private fun getAppliedPolicyViewsFlow(
        userId: String,
        type: PolicyType,
    ): Flow<List<PolicyView>> = combine(
        authDiskSource
            .getPoliciesFlow(userId = userId)
            .map { it?.toSdkPolicyViews() },
        authDiskSource
            .getOrganizationsFlow(userId = userId)
            .map { organizations ->
                organizations?.map {
                    OrganizationPolicyData(
                        organizationUserPolicyContext = it.toSdkOrganizationPolicyContext(),
                        organizationShouldUsePolicies = it.permissions.shouldManagePolicies,
                    )
                }
            },
        featureFlagManager.getFeatureFlagFlow(key = FlagKey.PoliciesInAcceptedState),
    ) { policies, organizations, isEnabled ->
        filterPolicies(
            type = type,
            policies = policies,
            organizations = organizations,
            isPoliciesInAcceptedStateEnabled = isEnabled,
        )
    }
        // We do not have any policies yet if it is null, so do not emit at all.
        .filterNotNull()

    private fun filterPolicies(
        type: PolicyType,
        policies: List<PolicyView>?,
        organizations: List<OrganizationPolicyData>?,
        isPoliciesInAcceptedStateEnabled: Boolean,
    ): List<PolicyView>? =
        when {
            policies == null -> null
            policies.isEmpty() -> emptyList()
            isPoliciesInAcceptedStateEnabled -> {
                authSdkSource
                    .filterPolicies(
                        policies = policies,
                        policyType = type,
                        organizations = organizations
                            ?.map { it.organizationUserPolicyContext }
                            .orEmpty(),
                    )
                    .getOrElse { emptyList() }
            }

            else -> {
                // Legacy flow
                val organizationIdsWithActivePolicies = organizations
                    ?.filter {
                        @Suppress("MaxLineLength")
                        it.organizationUserPolicyContext.usePolicies &&
                            (it.organizationUserPolicyContext.status == OrganizationUserStatusType.ACCEPTED ||
                                it.organizationUserPolicyContext.status == OrganizationUserStatusType.CONFIRMED) &&
                            !it.isOrganizationExemptFromPolicies(policyType = type)
                    }
                    ?.map { it.organizationUserPolicyContext.id }
                    .orEmpty()
                return policies.filter {
                    it.type == type &&
                        it.enabled &&
                        organizationIdsWithActivePolicies.contains(it.organizationId)
                }
            }
        }

    /**
     * A helper method to determine if the organization is exempt from policies.
     */
    private fun OrganizationPolicyData.isOrganizationExemptFromPolicies(
        policyType: PolicyType,
    ): Boolean =
        when (policyType) {
            PolicyType.MAXIMUM_VAULT_TIMEOUT -> {
                this.organizationUserPolicyContext.role == OrganizationUserType.OWNER
            }

            PolicyType.MASTER_PASSWORD,
            PolicyType.PASSWORD_GENERATOR,
            PolicyType.REMOVE_UNLOCK_WITH_PIN,
            PolicyType.RESTRICTED_ITEM_TYPES,
                -> false

            else -> {
                this.organizationUserPolicyContext.role == OrganizationUserType.OWNER ||
                    this.organizationUserPolicyContext.role == OrganizationUserType.ADMIN ||
                    this.organizationShouldUsePolicies
            }
        }
}

private data class OrganizationPolicyData(
    val organizationUserPolicyContext: OrganizationUserPolicyContext,
    val organizationShouldUsePolicies: Boolean,
)
