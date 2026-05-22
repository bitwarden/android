package com.x8bit.bitwarden.data.platform.manager

import com.bitwarden.network.model.OrganizationStatusType
import com.bitwarden.network.model.OrganizationType
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.policies.PolicyType
import com.bitwarden.policies.PolicyView
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.util.activeUserIdChangesFlow
import com.x8bit.bitwarden.data.vault.repository.util.toSdkPolicyViews
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

/**
 * The default [PolicyManager] implementation. This class is responsible for
 * loading policies for the current user and filtering them as needed.
 */
class PolicyManagerImpl(
    private val authDiskSource: AuthDiskSource,
) : PolicyManager {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getActivePoliciesFlow(type: PolicyType): Flow<List<PolicyView>> =
        authDiskSource
            .activeUserIdChangesFlow
            .flatMapLatest { activeUserId ->
                activeUserId
                    ?.let { userId ->
                        authDiskSource
                            .getPoliciesFlow(userId = userId)
                            .map { it?.toSdkPolicyViews() }
                            .mapNotNull {
                                filterPolicies(
                                    userId = userId,
                                    type = type,
                                    policies = it,
                                )
                            }
                    }
                    ?: emptyFlow()
            }
            .distinctUntilChanged()

    override fun getActivePolicies(type: PolicyType): List<PolicyView> =
        authDiskSource
            .userState
            ?.activeUserId
            ?.let { userId ->
                filterPolicies(
                    userId = userId,
                    type = type,
                    policies = getPolicyViews(userId = userId),
                )
            }
            .orEmpty()

    override fun getUserPolicies(
        userId: String,
        type: PolicyType,
    ): List<PolicyView> =
        this
            .filterPolicies(
                userId = userId,
                type = type,
                policies = getPolicyViews(userId = userId),
            )
            .orEmpty()

    override fun getPersonalOwnershipPolicyOrganizationId(): String? =
        this
            .getActivePolicies(type = PolicyType.ORGANIZATION_DATA_OWNERSHIP)
            .sortedBy { it.revisionDate }
            .firstOrNull()
            ?.organizationId

    /**
     * A helper method to filter policies.
     */
    private fun filterPolicies(
        userId: String,
        type: PolicyType,
        policies: List<PolicyView>?,
    ): List<PolicyView>? {
        policies ?: return null
        if (policies.isEmpty()) return emptyList()

        // Get a list of the user's organizations that enforce policies.
        val organizationIdsWithActivePolicies = authDiskSource
            .getOrganizations(userId)
            ?.filter {
                it.shouldUsePolicies &&
                    it.status >= OrganizationStatusType.ACCEPTED &&
                    !isOrganizationExemptFromPolicies(organization = it, policyType = type)
            }
            ?.map { it.id }
            .orEmpty()

        // Filter the policies based on the type, whether the policy is active,
        // and whether the organization rules except the user from the policy.
        return policies.filter {
            it.type == type &&
                it.enabled &&
                organizationIdsWithActivePolicies.contains(it.organizationId)
        }
    }

    /**
     * A helper method to determine if the organization is exempt from policies.
     */
    private fun isOrganizationExemptFromPolicies(
        organization: SyncResponseJson.Profile.Organization,
        policyType: PolicyType,
    ): Boolean =
        when (policyType) {
            PolicyType.MAXIMUM_VAULT_TIMEOUT -> {
                organization.type == OrganizationType.OWNER
            }

            PolicyType.PASSWORD_GENERATOR,
            PolicyType.REMOVE_UNLOCK_WITH_PIN,
            PolicyType.RESTRICTED_ITEM_TYPES,
                -> {
                false
            }

            else -> {
                (organization.type == OrganizationType.OWNER ||
                    organization.type == OrganizationType.ADMIN) ||
                    organization.permissions.shouldManagePolicies
            }
        }

    private fun getPolicyViews(
        userId: String,
    ): List<PolicyView>? = authDiskSource
        .getPolicies(userId = userId)
        ?.toSdkPolicyViews()
}
