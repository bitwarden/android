package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.util.activeUserIdChangesFlow
import com.x8bit.bitwarden.data.vault.datasource.network.model.OrganizationStatusType
import com.x8bit.bitwarden.data.vault.datasource.network.model.OrganizationType
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull

/**
 * The default [PolicyManager] implementation. This class is responsible for
 * loading policies for the current user and filtering them as needed.
 */
class PolicyManagerImpl(
    private val authDiskSource: AuthDiskSource,
) : PolicyManager {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getActivePoliciesFlow(type: PolicyTypeJson): Flow<List<SyncResponseJson.Policy>> =
        authDiskSource
            .activeUserIdChangesFlow
            .flatMapLatest { activeUserId ->
                activeUserId
                    ?.let { userId ->
                        authDiskSource
                            .getPoliciesFlow(userId)
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

    override fun getActivePolicies(type: PolicyTypeJson): List<SyncResponseJson.Policy> =
        authDiskSource
            .userState
            ?.activeUserId
            ?.let { userId ->
                filterPolicies(
                    userId = userId,
                    type = type,
                    policies = authDiskSource.getPolicies(userId = userId),
                )
            }
            ?: emptyList()

    /**
     * A helper method to filter policies.
     */
    private fun filterPolicies(
        userId: String,
        type: PolicyTypeJson,
        policies: List<SyncResponseJson.Policy>?,
    ): List<SyncResponseJson.Policy>? {
        policies ?: return null
        if (policies.isEmpty()) return emptyList()

        // Get a list of the user's organizations that enforce policies.
        val organizationIdsWithActivePolicies = authDiskSource
            .getOrganizations(userId)
            ?.filter {
                it.shouldUsePolicies &&
                    it.isEnabled &&
                    it.status >= OrganizationStatusType.ACCEPTED &&
                    !isOrganizationExemptFromPolicies(it, type)
            }
            ?.map { it.id }
            .orEmpty()

        // Filter the policies based on the type, whether the policy is active,
        // and whether the organization rules except the user from the policy.
        return policies.filter {
            it.type == type &&
                it.isEnabled &&
                organizationIdsWithActivePolicies.contains(it.organizationId)
        }
    }

    /**
     * A helper method to determine if the organization is exempt from policies.
     */
    private fun isOrganizationExemptFromPolicies(
        organization: SyncResponseJson.Profile.Organization,
        policyType: PolicyTypeJson,
    ): Boolean =
        if (policyType == PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT) {
            organization.type == OrganizationType.OWNER
        } else if (policyType == PolicyTypeJson.PASSWORD_GENERATOR) {
            false
        } else {
            (organization.type == OrganizationType.OWNER ||
                organization.type == OrganizationType.ADMIN) ||
                organization.permissions.shouldManagePolicies
        }
}
