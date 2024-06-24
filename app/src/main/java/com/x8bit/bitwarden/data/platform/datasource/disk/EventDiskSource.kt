package com.x8bit.bitwarden.data.platform.datasource.disk

import com.x8bit.bitwarden.data.platform.datasource.network.model.OrganizationEventJson

/**
 * Primary access point for disk information related to event data.
 */
interface EventDiskSource {
    /**
     * Deletes all organization events associated with the given [userId].
     */
    suspend fun deleteOrganizationEvents(userId: String)

    /**
     * Adds a new organization event associated with the given [userId].
     */
    suspend fun addOrganizationEvent(userId: String, event: OrganizationEventJson)

    /**
     * Retrieves all organization events associated with the given [userId].
     */
    suspend fun getOrganizationEvents(userId: String): List<OrganizationEventJson>
}
