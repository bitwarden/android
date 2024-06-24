package com.x8bit.bitwarden.data.platform.datasource.network.service

import com.x8bit.bitwarden.data.platform.datasource.network.model.OrganizationEventJson

/**
 * Provides an API for submitting events.
 */
interface EventService {
    /**
     * Attempts to submit all of the given organizations events.
     */
    suspend fun sendOrganizationEvents(events: List<OrganizationEventJson>): Result<Unit>
}
