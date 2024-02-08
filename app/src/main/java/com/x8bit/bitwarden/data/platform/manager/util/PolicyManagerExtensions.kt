package com.x8bit.bitwarden.data.platform.manager.util

import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.auth.repository.util.policyInformation
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Get a list of active policies with the data decoded to the specified type.
 */
inline fun <reified T : PolicyInformation> PolicyManager.getActivePolicies(): List<T> =
    this
        .getActivePolicies(type = getPolicyTypeJson<T>())
        .mapNotNull { it.policyInformation as? T }

/**
 * Gets a flow of all the active policies with the data decoded to the specified type.
 */
inline fun <reified T : PolicyInformation> PolicyManager.getActivePoliciesFlow(): Flow<List<T>> =
    this
        .getActivePoliciesFlow(type = getPolicyTypeJson<T>())
        .map { policies ->
            policies.mapNotNull { policy -> policy.policyInformation as? T }
        }

/**
 * Helper method for mapping a specific [PolicyInformation] type to its [PolicyTypeJson]
 * counterpart.
 */
inline fun <reified T : PolicyInformation> getPolicyTypeJson(): PolicyTypeJson =
    when (T::class.java) {
        PolicyInformation.MasterPassword::class.java -> PolicyTypeJson.MASTER_PASSWORD
        PolicyInformation.PasswordGenerator::class.java -> PolicyTypeJson.PASSWORD_GENERATOR
        PolicyInformation.SendOptions::class.java -> PolicyTypeJson.SEND_OPTIONS
        PolicyInformation.VaultTimeout::class.java -> PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT

        else -> {
            throw IllegalStateException(
                "Looks like you are missing a branch in your when statement. Update " +
                    "getPolicyTypeJson() to handle all PolicyInformation implementations.",
            )
        }
    }
