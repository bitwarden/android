package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import kotlinx.coroutines.flow.Flow

/**
 * A manager for pulling policies from the local data store and filtering them as needed.
 */
interface PolicyManager {
    /**
     * Returns a flow of all the active policies of the given type.
     */
    fun getActivePoliciesFlow(type: PolicyTypeJson): Flow<List<SyncResponseJson.Policy>>

    /**
     * Get all the policies of the given [type] that are enabled and applicable to the user.
     */
    fun getActivePolicies(type: PolicyTypeJson): List<SyncResponseJson.Policy>
}
