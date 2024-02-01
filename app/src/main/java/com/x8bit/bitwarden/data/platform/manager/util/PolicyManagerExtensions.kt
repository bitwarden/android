package com.x8bit.bitwarden.data.platform.manager.util

import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.auth.repository.util.policyInformation
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson

/**
 * Get a list of active policies with the data decoded to the specified type.
 */
inline fun <reified T : PolicyInformation> PolicyManager.getActivePolicies(): List<T> {
    val type = when (T::class.java) {
        PolicyInformation.MasterPassword::class.java -> PolicyTypeJson.MASTER_PASSWORD
        PolicyInformation.PasswordGenerator::class.java -> PolicyTypeJson.PASSWORD_GENERATOR
        PolicyInformation.SendOptions::class.java -> PolicyTypeJson.SEND_OPTIONS
        PolicyInformation.VaultTimeout::class.java -> PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT

        else -> {
            throw IllegalStateException(
                "Looks like you are missing a branch in your when statement. Update " +
                    "getActivePolicies() to handle all PolicyInformation implementations.",
            )
        }
    }
    return this
        .getActivePolicies(type = type)
        .mapNotNull { it.policyInformation as? T }
}
