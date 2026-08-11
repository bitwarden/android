package com.x8bit.bitwarden.data.platform.manager

import com.bitwarden.policies.PolicyType
import com.bitwarden.policies.PolicyView
import com.x8bit.bitwarden.data.platform.manager.model.EffectiveSendPolicy
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
     * Returns the current, precedence-resolved [EffectiveSendPolicy] for the active user. When
     * the `pm-31885-send-controls` feature flag is disabled, or no organization has an active
     * SendControls policy, this is equivalent to today's DisableSend/SendOptions logic.
     */
    fun getEffectiveSendPolicy(): EffectiveSendPolicy

    /**
     * Returns a flow that emits the current [EffectiveSendPolicy] for the active user whenever
     * the underlying policies or the `pm-31885-send-controls` feature flag change.
     */
    fun getEffectiveSendPolicyFlow(): Flow<EffectiveSendPolicy>

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
