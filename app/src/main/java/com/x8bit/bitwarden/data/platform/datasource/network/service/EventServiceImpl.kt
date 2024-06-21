package com.x8bit.bitwarden.data.platform.datasource.network.service

import com.x8bit.bitwarden.data.platform.datasource.network.api.EventApi
import com.x8bit.bitwarden.data.platform.datasource.network.model.OrganizationEvent

/**
 * The default implementation of the [EventService].
 */
class EventServiceImpl(
    private val eventApi: EventApi,
) : EventService {
    override suspend fun sendOrganizationEvents(
        events: List<OrganizationEvent>,
    ): Result<Unit> = eventApi.collectOrganizationEvents(events = events)
}
