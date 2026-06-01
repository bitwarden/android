package com.x8bit.bitwarden.data.platform.manager

import com.bitwarden.policies.PolicyType
import com.bitwarden.policies.PolicyView
import kotlinx.coroutines.flow.Flow

/**
 * A manager for pulling policies from the local data store and filtering them as needed.
 */
interface PolicyManager {
    /**
     * Returns a flow of all the active policies of the given type.
     */
    fun getActivePoliciesFlow(type: PolicyType): Flow<List<PolicyView>>

    /**
     * Get all the policies of the given [type] that are enabled and applicable to the user.
     */
    fun getActivePolicies(type: PolicyType): List<PolicyView>

    /**
     * Get all the policies of the given [type] that are enabled and applicable to the [userId].
     */
    fun getUserPolicies(
        userId: String,
        type: PolicyType,
    ): List<PolicyView>

    /**
     * Get the organization id of the personal ownership policy.
     * If multiple organizations enforce the policy, return the first to set it.
     */
    fun getPersonalOwnershipPolicyOrganizationId(): String?
}
