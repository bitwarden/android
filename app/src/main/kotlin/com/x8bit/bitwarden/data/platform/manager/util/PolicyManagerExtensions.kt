package com.x8bit.bitwarden.data.platform.manager.util

import com.bitwarden.policies.PolicyType
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.auth.repository.util.policyInformation
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Get a list of active policies with the data decoded to the specified type.
 */
inline fun <reified T : PolicyInformation> PolicyManager.getActivePolicies(): List<T> =
    this
        .getActivePolicies(type = getPolicyType<T>())
        .mapNotNull { it.policyInformation as? T }

/**
 * Gets a flow of all the active policies with the data decoded to the specified type.
 */
inline fun <reified T : PolicyInformation> PolicyManager.getActivePoliciesFlow(): Flow<List<T>> =
    this
        .getActivePoliciesFlow(type = getPolicyType<T>())
        .map { policies ->
            policies.mapNotNull { policy -> policy.policyInformation as? T }
        }

/**
 * Helper method for mapping a specific [PolicyInformation] type to its [PolicyType]
 * counterpart.
 */
inline fun <reified T : PolicyInformation> getPolicyType(): PolicyType =
    when (T::class.java) {
        PolicyInformation.MasterPassword::class.java -> PolicyType.MASTER_PASSWORD
        PolicyInformation.PasswordGenerator::class.java -> PolicyType.PASSWORD_GENERATOR
        PolicyInformation.SendOptions::class.java -> PolicyType.SEND_OPTIONS
        PolicyInformation.SendControls::class.java -> PolicyType.SEND_CONTROLS
        PolicyInformation.VaultTimeout::class.java -> PolicyType.MAXIMUM_VAULT_TIMEOUT

        else -> {
            throw IllegalStateException(
                "Looks like you are missing a branch in your when statement. Update " +
                    "getPolicyTypeJson() to handle all PolicyInformation implementations.",
            )
        }
    }

/**
 * Helper method for verifying if user has enabled the restrict item policy.
 */
fun PolicyManager.hasRestrictItemTypes(): Boolean =
    getActivePolicies(type = PolicyType.RESTRICTED_ITEM_TYPES)
        .any { it.enabled }
